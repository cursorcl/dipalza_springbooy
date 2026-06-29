-- Configuración de listas de precio activas (vive en Mastersoft)
CREATE TABLE dbo.ListaPrecioActiva
(
    Rol         CHAR(1)      NOT NULL,                 -- 'P' = principal (->VentaNeto), 'S' = secundaria (->PrecioLista2)
    CodigoLista VARCHAR(3)   NOT NULL,                 -- mismo tipo/largo que PRECIOS.CodigoLista
    UpdatedAt   DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME(),

    CONSTRAINT PK_ListaPrecioActiva       PRIMARY KEY (Rol),
    CONSTRAINT CK_ListaPrecioActiva_Rol   CHECK (Rol IN ('P', 'S')),
    CONSTRAINT UQ_ListaPrecioActiva_Lista UNIQUE (CodigoLista)
);

-- Cola de actualización de precios (vive en Mastersoft)
CREATE TABLE dbo.PriceUpdateQueue
(
    EventID     BIGINT IDENTITY(1,1) PRIMARY KEY,
    Articulo    VARCHAR(15)  NOT NULL,   -- mismo tipo/largo que PRECIOS.Articulo
    Rol         CHAR(1)      NOT NULL,   -- 'P' o 'S': a qué campo de producto va este precio
    CreatedAt   DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    ProcessedAt DATETIME2(7) NULL
);

CREATE INDEX IX_PriceUpdateQueue_ForProcessing ON dbo.PriceUpdateQueue (ProcessedAt, CreatedAt);

CREATE OR ALTER TRIGGER dbo.trg_precios_priceupdate
    ON dbo.PRECIOS
    AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @hasIns BIT = CASE WHEN EXISTS (SELECT 1 FROM inserted) THEN 1 ELSE 0 END;
    DECLARE @hasDel BIT = CASE WHEN EXISTS (SELECT 1 FROM deleted)  THEN 1 ELSE 0 END;

    -- INSERT o UPDATE: encolar SOLO el lado nuevo (inserted), y solo si
    -- (Articulo, CodigoLista, VentaNeto) realmente cambió respecto al estado previo.
    IF @hasIns = 1
        BEGIN
            ;WITH nuevos AS (
                SELECT Articulo, CodigoLista, VentaNeto FROM inserted
                EXCEPT
                SELECT Articulo, CodigoLista, VentaNeto FROM deleted
            )
            INSERT INTO dbo.PriceUpdateQueue (Articulo, Rol)
            SELECT DISTINCT n.Articulo, la.Rol
            FROM nuevos n
                     INNER JOIN dbo.ListaPrecioActiva la
                                ON la.CodigoLista = n.CodigoLista COLLATE DATABASE_DEFAULT;
        END

    -- DELETE puro: encolar el artículo borrado, si su lista estaba activa.
    IF @hasIns = 0 AND @hasDel = 1
        BEGIN
            INSERT INTO dbo.PriceUpdateQueue (Articulo, Rol)
            SELECT DISTINCT d.Articulo, la.Rol
            FROM deleted d
                     INNER JOIN dbo.ListaPrecioActiva la
                                ON la.CodigoLista = d.CodigoLista COLLATE DATABASE_DEFAULT;
        END
END;

USE ventas;
GO

CREATE OR ALTER PROCEDURE dbo.usp_ProcessPriceUpdateQueue
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;   -- cualquier error en runtime aborta la transacción

    DECLARE @batchSize INT = 500;

    DECLARE @eventsToProcess TABLE (
        EventID  BIGINT      PRIMARY KEY,
        Articulo VARCHAR(15),
        Rol      CHAR(1)
    );

    BEGIN TRY
        BEGIN TRANSACTION;

        -- 1. Reclamar lote
        ;WITH cte_batch AS (
            SELECT TOP (@batchSize) EventID, Articulo, Rol, ProcessedAt
            FROM [Mastersoft].[dbo].[PriceUpdateQueue]
            WHERE ProcessedAt IS NULL
            ORDER BY CreatedAt ASC
        )
        UPDATE cte_batch
           SET ProcessedAt = SYSUTCDATETIME()
        OUTPUT inserted.EventID, inserted.Articulo, inserted.Rol
            INTO @eventsToProcess (EventID, Articulo, Rol);

        -- Lote vacío: cerramos la transacción y salimos (no dejar tran abierta)
        IF NOT EXISTS (SELECT 1 FROM @eventsToProcess)
            BEGIN
                COMMIT TRANSACTION;
                RETURN;
            END

        -- 2. Resolver precios + 3. aplicar a producto
        ;WITH dedup AS (
            SELECT DISTINCT Articulo, Rol FROM @eventsToProcess
        ),
        resolved AS (
            SELECT d.Articulo, d.Rol, ap.NuevoPrecio
            FROM dedup d
            OUTER APPLY (
                SELECT MIN(p.VentaNeto) AS NuevoPrecio
                FROM [Mastersoft].[dbo].[ListaPrecioActiva] la
                         INNER JOIN [Mastersoft].[dbo].[PRECIOS] p
                                    ON p.CodigoLista = la.CodigoLista COLLATE DATABASE_DEFAULT
                                        AND p.Articulo    = d.Articulo   COLLATE DATABASE_DEFAULT
                WHERE la.Rol = d.Rol
            ) ap
        ),
        pivoted AS (
            SELECT
                Articulo,
                MAX(CASE WHEN Rol = 'P' THEN 1 ELSE 0 END)                 AS hasP,
                MAX(CASE WHEN Rol = 'P' THEN COALESCE(NuevoPrecio, 0) END) AS precioP,
                MAX(CASE WHEN Rol = 'S' THEN 1 ELSE 0 END)                 AS hasS,
                MAX(CASE WHEN Rol = 'S' THEN COALESCE(NuevoPrecio, 0) END) AS precioS
            FROM resolved
            GROUP BY Articulo
        )
        UPDATE prod
           SET prod.VentaNeto    = CASE WHEN pv.hasP = 1 THEN pv.precioP ELSE prod.VentaNeto    END,
               prod.PrecioLista2 = CASE WHEN pv.hasS = 1 THEN pv.precioS ELSE prod.PrecioLista2 END,
               prod.last_update  = CONVERT(date, SYSUTCDATETIME())
        FROM dbo.producto prod
                 INNER JOIN pivoted pv
                            ON pv.Articulo = prod.Articulo COLLATE DATABASE_DEFAULT;

        -- 4. Limpieza de la cola
        DELETE FROM [Mastersoft].[dbo].[PriceUpdateQueue]
        WHERE EventID IN (SELECT EventID FROM @eventsToProcess);

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;   -- deshace el claim: los eventos vuelven a ProcessedAt NULL y se reintentan
        THROW;                      -- propaga el error -> el job step falla (on_fail_action = 2)
    END CATCH
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_listaprecioactiva_resync
    ON dbo.ListaPrecioActiva
    AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    -- (1) Activación o cambio de lista: re-encolar los artículos de la lista NUEVA.
    ;WITH nueva_lista AS (
        SELECT i.Rol, i.CodigoLista
        FROM inserted i
                 LEFT JOIN deleted d ON d.Rol = i.Rol
        WHERE d.Rol IS NULL                       -- activación (rol no existía)
           OR d.CodigoLista <> i.CodigoLista      -- cambio de lista
    )
    INSERT INTO dbo.PriceUpdateQueue (Articulo, Rol)
    SELECT DISTINCT p.Articulo, nl.Rol
    FROM nueva_lista nl
             INNER JOIN dbo.PRECIOS p
                        ON p.CodigoLista = nl.CodigoLista COLLATE DATABASE_DEFAULT;

    -- (2) Desactivación de un rol (solo 'S' por el guard del paso 6): llevar todo a 0.
    --     Encolamos todos los artículos de PRECIOS para el rol eliminado; como ya
    --     no queda lista activa para ese rol, el procesador resuelve NuevoPrecio = NULL
    --     -> COALESCE(...,0) -> PrecioLista2 = 0.
    ;WITH roles_eliminados AS (
        SELECT d.Rol
        FROM deleted d
        WHERE NOT EXISTS (SELECT 1 FROM inserted i WHERE i.Rol = d.Rol)
    )
    INSERT INTO dbo.PriceUpdateQueue (Articulo, Rol)
    SELECT DISTINCT p.Articulo, re.Rol
    FROM roles_eliminados re
             CROSS JOIN dbo.PRECIOS p;
END;