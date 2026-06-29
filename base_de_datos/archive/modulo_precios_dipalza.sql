/* ============================================================================
   MÓDULO DE ACTUALIZACIÓN DE PRECIOS  —  Dipalza (Mastersoft <-> ventas)
   ----------------------------------------------------------------------------
   Script autocontenido y RE-EJECUTABLE: primero ELIMINA todos los elementos
   del módulo (si existen) y luego los CREA.

   FLUJO COMPLETO
     - La app edita  ventas.ListaPrecioActiva  (FUENTE de verdad).
     - Un trigger outbox encola el cambio en  ventas.ListaPrecioActivaQueue.
     - El job 'ListaPrecioActivaQueue' (cada 10 s) ejecuta, EN MASTERSOFT, el
       procesador que sincroniza (espejo)  Mastersoft.ListaPrecioActiva.
     - Esa escritura en Mastersoft dispara el resync, que encola en
       PriceUpdateQueue los artículos a re-cotizar.
     - El job 'PriceUpdateQueue' (cada 15 s) aplica los precios a producto.
     - Por su parte, cualquier cambio directo en PRECIOS también encola en
       PriceUpdateQueue.

   OBJETOS QUE GESTIONA
     En [ventas]:
       - Tabla   dbo.ListaPrecioActiva            (FUENTE de verdad)
       - Tabla   dbo.ListaPrecioActivaQueue + ix  (cola hacia Mastersoft)
       - Trigger dbo.trg_listaprecioactiva_outbox (encola cambios)
       - Trigger dbo.trg_listaprecioactiva_guard  (al menos una 'P')
       - Columna producto.PrecioLista2            (ver NOTA 1)
       - Proc    dbo.usp_ProcessPriceUpdateQueue
     En [Mastersoft]:
       - Tabla   dbo.ListaPrecioActiva            (RÉPLICA gobernada por ventas)
       - Tabla   dbo.PriceUpdateQueue + ix        (cola de precios)
       - Trigger dbo.trg_precios_priceupdate            (sobre PRECIOS)
       - Trigger dbo.trg_listaprecioactiva_resync       (sobre ListaPrecioActiva)
       - Trigger dbo.trg_listaprecioactiva_guard        (defensa, al menos 'P')
       - Proc    dbo.usp_ProcessListaPrecioActivaQueue  (espejo ventas->Mastersoft)
     En [msdb]:
       - Job 'Dipalza - Procesar PriceUpdateQueue'        (cada 15 s, corre en ventas)
       - Job 'Dipalza - Procesar ListaPrecioActivaQueue'  (cada 10 s, corre en Mastersoft)

   AVISOS
     NOTA 1 — producto.PrecioLista2 NO se elimina (es columna de producto, con
              datos). Solo se AGREGA si no existe.
     NOTA 2 — Re-ejecutar ELIMINA ambas ListaPrecioActiva (ventas y Mastersoft) y
              las dos colas: se pierde la configuración de listas activas y los
              eventos pendientes. Tras instalar, vuelve a sembrar la 'P' en
              VENTAS (ver SECCIÓN C); el job de 10 s la propaga a Mastersoft.

   PRERREQUISITOS
     - Bases [Mastersoft] y [ventas] en la MISMA instancia.
     - En [Mastersoft] existe PRECIOS; en [ventas] existe producto.
     - SQL Server Agent activo.

   EJECUCIÓN EN DBEAVER
     Usar "Execute SQL Script" (Alt+X), que respeta los GO y los USE.
     Con Ctrl+Enter (una sola sentencia) NO funciona.
   ============================================================================ */


/* ============================================================================
   SECCIÓN A — ELIMINACIÓN (orden inverso de dependencias)
   ============================================================================ */

-- A.1  Jobs (msdb)
USE msdb;
GO
IF EXISTS (SELECT job_id FROM msdb.dbo.sysjobs WHERE name = N'Dipalza - Procesar PriceUpdateQueue')
    EXEC sp_delete_job @job_name = N'Dipalza - Procesar PriceUpdateQueue', @delete_unused_schedule = 1;
IF EXISTS (SELECT job_id FROM msdb.dbo.sysjobs WHERE name = N'Dipalza - Procesar ListaPrecioActivaQueue')
    EXEC sp_delete_job @job_name = N'Dipalza - Procesar ListaPrecioActivaQueue', @delete_unused_schedule = 1;
GO

-- A.2  Procedimientos
USE ventas;
GO
DROP PROCEDURE IF EXISTS dbo.usp_ProcessPriceUpdateQueue;
GO
USE Mastersoft;
GO
DROP PROCEDURE IF EXISTS dbo.usp_ProcessListaPrecioActivaQueue;
GO

-- A.3  Objetos en [Mastersoft]
USE Mastersoft;
GO
DROP TRIGGER IF EXISTS dbo.trg_precios_priceupdate;
DROP TRIGGER IF EXISTS dbo.trg_listaprecioactiva_resync;
DROP TRIGGER IF EXISTS dbo.trg_listaprecioactiva_guard;
GO
DROP TABLE IF EXISTS dbo.PriceUpdateQueue;
DROP TABLE IF EXISTS dbo.ListaPrecioActiva;
GO

-- A.4  Objetos en [ventas]
USE ventas;
GO
DROP TRIGGER IF EXISTS dbo.trg_listaprecioactiva_outbox;
DROP TRIGGER IF EXISTS dbo.trg_listaprecioactiva_guard;
GO
DROP TABLE IF EXISTS dbo.ListaPrecioActivaQueue;
DROP TABLE IF EXISTS dbo.ListaPrecioActiva;
GO


/* ============================================================================
   SECCIÓN B — CREACIÓN
   ============================================================================ */

-- ----------------------------------------------------------------------------
-- B.1  [ventas]: tabla FUENTE, cola, trigger outbox y guard
-- ----------------------------------------------------------------------------
USE ventas;
GO

-- Tabla fuente de verdad (la edita la app)
CREATE TABLE dbo.ListaPrecioActiva (
    Rol         CHAR(1) COLLATE Modern_Spanish_CI_AS      NOT NULL,                 -- 'P' principal (->VentaNeto), 'S' secundaria (->PrecioLista2)
    CodigoLista VARCHAR(3) COLLATE Modern_Spanish_CI_AS   NOT NULL,
    UpdatedAt   DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_ListaPrecioActiva       PRIMARY KEY (Rol),
    CONSTRAINT CK_ListaPrecioActiva_Rol   CHECK (Rol IN ('P','S')),
    CONSTRAINT UQ_ListaPrecioActiva_Lista UNIQUE (CodigoLista)
);

-- Cola de salida hacia Mastersoft
CREATE TABLE dbo.ListaPrecioActivaQueue (
    EventID     BIGINT IDENTITY(1,1) PRIMARY KEY,
    Rol         CHAR(1) COLLATE Modern_Spanish_CI_AS      NOT NULL,
    ChangeType  CHAR(1) COLLATE Modern_Spanish_CI_AS      NOT NULL,   -- 'I' / 'U' / 'D' (informativo)
    CreatedAt   DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    ProcessedAt DATETIME2(7) NULL
);
CREATE INDEX IX_ListaPrecioActivaQueue_ForProcessing
    ON dbo.ListaPrecioActivaQueue (ProcessedAt, CreatedAt);
GO

-- Trigger outbox: encola el rol afectado en cada cambio
CREATE OR ALTER TRIGGER dbo.trg_listaprecioactiva_outbox
    ON dbo.ListaPrecioActiva
    AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.ListaPrecioActivaQueue (Rol, ChangeType)
    SELECT COALESCE(i.Rol, d.Rol) AS Rol,
           CASE WHEN i.Rol IS NOT NULL AND d.Rol IS NOT NULL THEN 'U'
                WHEN i.Rol IS NOT NULL THEN 'I'
                ELSE 'D' END AS ChangeType
    FROM inserted i
             FULL OUTER JOIN deleted d ON i.Rol = d.Rol;
END;
GO

-- Guard: siempre debe existir la principal 'P' (validación en la FUENTE)
CREATE OR ALTER TRIGGER dbo.trg_listaprecioactiva_guard
    ON dbo.ListaPrecioActiva
    AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    IF NOT EXISTS (SELECT 1 FROM dbo.ListaPrecioActiva WHERE Rol = 'P')
        BEGIN
            ROLLBACK TRANSACTION;
            THROW 50002, 'ventas.ListaPrecioActiva debe tener siempre una lista principal (Rol = ''P''). Operacion cancelada.', 1;
        END
END;
GO

-- ----------------------------------------------------------------------------
-- B.2  [Mastersoft]: tabla RÉPLICA, cola de precios y triggers
-- ----------------------------------------------------------------------------
USE Mastersoft;
GO

-- Réplica (la escribe solo el procesador inverso)
CREATE TABLE dbo.ListaPrecioActiva (
    Rol         CHAR(1) COLLATE Modern_Spanish_CI_AS      NOT NULL,
    CodigoLista VARCHAR(3) COLLATE Modern_Spanish_CI_AS   NOT NULL,
    UpdatedAt   DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_ListaPrecioActiva       PRIMARY KEY (Rol),
    CONSTRAINT CK_ListaPrecioActiva_Rol   CHECK (Rol IN ('P','S')),
    CONSTRAINT UQ_ListaPrecioActiva_Lista UNIQUE (CodigoLista)
);

-- Cola de precios
CREATE TABLE dbo.PriceUpdateQueue (
    EventID     BIGINT IDENTITY(1,1) PRIMARY KEY,
    Articulo    VARCHAR(15) COLLATE Modern_Spanish_CI_AS  NOT NULL,
    Rol         CHAR(1) COLLATE Modern_Spanish_CI_AS      NOT NULL,
    CreatedAt   DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    ProcessedAt DATETIME2(7) NULL
);
CREATE INDEX IX_PriceUpdateQueue_ForProcessing
    ON dbo.PriceUpdateQueue (ProcessedAt, CreatedAt);
GO

-- Trigger sobre PRECIOS
CREATE OR ALTER TRIGGER dbo.trg_precios_priceupdate
    ON dbo.PRECIOS
    AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @hasIns BIT = CASE WHEN EXISTS (SELECT 1 FROM inserted) THEN 1 ELSE 0 END;
    DECLARE @hasDel BIT = CASE WHEN EXISTS (SELECT 1 FROM deleted)  THEN 1 ELSE 0 END;

    -- INSERT/UPDATE: encolar solo el lado nuevo y solo si el precio/lista cambió
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
                                ON la.CodigoLista = n.CodigoLista COLLATE Modern_Spanish_CI_AS;
        END

    -- DELETE puro
    IF @hasIns = 0 AND @hasDel = 1
        BEGIN
            INSERT INTO dbo.PriceUpdateQueue (Articulo, Rol)
            SELECT DISTINCT d.Articulo, la.Rol
            FROM deleted d
                     INNER JOIN dbo.ListaPrecioActiva la
                                ON la.CodigoLista = d.CodigoLista COLLATE Modern_Spanish_CI_AS;
        END
END;
GO

-- Trigger de resync sobre ListaPrecioActiva (réplica)
CREATE OR ALTER TRIGGER dbo.trg_listaprecioactiva_resync
    ON dbo.ListaPrecioActiva
    AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    -- (1) Activación o cambio de lista: re-encolar los artículos de la lista NUEVA
    ;WITH nueva_lista AS (
        SELECT i.Rol, i.CodigoLista
        FROM inserted i
                 LEFT JOIN deleted d ON d.Rol = i.Rol
        WHERE d.Rol IS NULL
           OR d.CodigoLista <> i.CodigoLista
    )
    INSERT INTO dbo.PriceUpdateQueue (Articulo, Rol)
    SELECT DISTINCT p.Articulo, nl.Rol
    FROM nueva_lista nl
             INNER JOIN dbo.PRECIOS p
                        ON p.CodigoLista = nl.CodigoLista COLLATE Modern_Spanish_CI_AS;

    -- (2) Si ya no existe 'S' activa: todos los PrecioLista2 a 0
    IF EXISTS (SELECT 1 FROM deleted WHERE Rol = 'S')
       AND NOT EXISTS (SELECT 1 FROM dbo.ListaPrecioActiva WHERE Rol = 'S')
        BEGIN
            UPDATE [ventas].[dbo].[producto]
            SET PrecioLista2 = 0
            WHERE PrecioLista2 IS NULL OR PrecioLista2 <> 0;
        END
END;
GO

-- Guard defensivo en la réplica
CREATE OR ALTER TRIGGER dbo.trg_listaprecioactiva_guard
    ON dbo.ListaPrecioActiva
    AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    IF NOT EXISTS (SELECT 1 FROM dbo.ListaPrecioActiva WHERE Rol = 'P')
        BEGIN
            ROLLBACK TRANSACTION;
            THROW 50001, 'Mastersoft.ListaPrecioActiva debe tener siempre una lista principal (Rol = ''P''). Operacion cancelada.', 1;
        END
END;
GO

-- ----------------------------------------------------------------------------
-- B.3  [ventas]: columna PrecioLista2 y procesador de precios
-- ----------------------------------------------------------------------------
USE ventas;
GO

-- Se AGREGA solo si no existe (ver NOTA 1)
IF NOT EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID('dbo.producto') AND name = 'PrecioLista2'
    )
    ALTER TABLE dbo.producto ADD PrecioLista2 money NULL;
GO

CREATE OR ALTER PROCEDURE dbo.usp_ProcessPriceUpdateQueue
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @batchSize INT = 500;

    DECLARE @eventsToProcess TABLE (
        EventID  BIGINT      PRIMARY KEY,
        Articulo VARCHAR(15) COLLATE Modern_Spanish_CI_AS,
        Rol      CHAR(1) COLLATE Modern_Spanish_CI_AS
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
                                    ON p.CodigoLista COLLATE Modern_Spanish_CI_AS = la.CodigoLista COLLATE Modern_Spanish_CI_AS
                                        AND p.Articulo COLLATE Modern_Spanish_CI_AS = d.Articulo COLLATE Modern_Spanish_CI_AS
                WHERE la.Rol COLLATE Modern_Spanish_CI_AS = d.Rol COLLATE Modern_Spanish_CI_AS
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
                            ON pv.Articulo COLLATE Modern_Spanish_CI_AS = prod.Articulo COLLATE Modern_Spanish_CI_AS;

        -- 4. Limpieza de la cola
        DELETE FROM [Mastersoft].[dbo].[PriceUpdateQueue]
        WHERE EventID IN (SELECT EventID FROM @eventsToProcess);

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- ----------------------------------------------------------------------------
-- B.4  [Mastersoft]: procesador inverso (espejo ventas -> Mastersoft)
-- ----------------------------------------------------------------------------
USE Mastersoft;
GO

CREATE OR ALTER PROCEDURE dbo.usp_ProcessListaPrecioActivaQueue
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @batchSize INT = 100;

    DECLARE @events TABLE (
        EventID    BIGINT PRIMARY KEY,
        Rol        CHAR(1) COLLATE Modern_Spanish_CI_AS,
        ChangeType CHAR(1) COLLATE Modern_Spanish_CI_AS
    );

    BEGIN TRY
        BEGIN TRANSACTION;

        -- 1. Reclamar lote desde la cola en ventas
        ;WITH cte_batch AS (
            SELECT TOP (@batchSize) EventID, Rol, ChangeType, ProcessedAt
            FROM [ventas].[dbo].[ListaPrecioActivaQueue]
            WHERE ProcessedAt IS NULL
            ORDER BY CreatedAt ASC
        )
        UPDATE cte_batch
           SET ProcessedAt = SYSUTCDATETIME()
        OUTPUT inserted.EventID, inserted.Rol, inserted.ChangeType
            INTO @events (EventID, Rol, ChangeType);

        IF NOT EXISTS (SELECT 1 FROM @events)
            BEGIN
                COMMIT TRANSACTION;
                RETURN;
            END

        -- 2. Sincronización ESPEJO completa: Mastersoft.ListaPrecioActiva = ventas.ListaPrecioActiva
        --    (la tabla es de <=2 filas; sincronizar todo evita estados intermedios sin 'P').
        --    Esta escritura dispara el resync y el guard de Mastersoft.
        MERGE dbo.ListaPrecioActiva AS T
        USING [ventas].[dbo].[ListaPrecioActiva] AS S
            ON T.Rol = S.Rol COLLATE Modern_Spanish_CI_AS
        WHEN MATCHED AND T.CodigoLista <> S.CodigoLista COLLATE Modern_Spanish_CI_AS
            THEN UPDATE SET T.CodigoLista = S.CodigoLista, T.UpdatedAt = SYSUTCDATETIME()
        WHEN NOT MATCHED BY TARGET
            THEN INSERT (Rol, CodigoLista) VALUES (S.Rol, S.CodigoLista)
        WHEN NOT MATCHED BY SOURCE
            THEN DELETE;

        -- 3. Limpieza de la cola
        DELETE FROM [ventas].[dbo].[ListaPrecioActivaQueue]
        WHERE EventID IN (SELECT EventID FROM @events);

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- ----------------------------------------------------------------------------
-- B.5  [msdb]: jobs
-- ----------------------------------------------------------------------------
USE msdb;
GO

-- Job de PRECIOS (cada 15 s, corre en ventas)
EXEC sp_add_job
     @job_name = N'Dipalza - Procesar PriceUpdateQueue',
     @enabled = 1,
     @description = N'Procesa la cola de precios hacia dbo.producto (VentaNeto y PrecioLista2)';
EXEC sp_add_jobstep
     @job_name = N'Dipalza - Procesar PriceUpdateQueue',
     @step_name = N'Ejecutar usp_ProcessPriceUpdateQueue',
     @subsystem = N'TSQL',
     @database_name = N'ventas',
     @command = N'EXEC dbo.usp_ProcessPriceUpdateQueue;',
     @on_success_action = 1,
     @on_fail_action = 2;
EXEC sp_add_schedule
     @schedule_name = N'SCH_Price_15Seg',
     @freq_type = 4, @freq_interval = 1,
     @freq_subday_type = 2, @freq_subday_interval = 15,
     @active_start_time = 0;
EXEC sp_attach_schedule
     @job_name = N'Dipalza - Procesar PriceUpdateQueue',
     @schedule_name = N'SCH_Price_15Seg';
EXEC sp_add_jobserver
     @job_name = N'Dipalza - Procesar PriceUpdateQueue',
     @server_name = N'(LOCAL)';
GO

-- Job de LISTAS ACTIVAS (cada 10 s, corre en Mastersoft)
EXEC sp_add_job
     @job_name = N'Dipalza - Procesar ListaPrecioActivaQueue',
     @enabled = 1,
     @description = N'Sincroniza ventas.ListaPrecioActiva -> Mastersoft.ListaPrecioActiva (espejo)';
EXEC sp_add_jobstep
     @job_name = N'Dipalza - Procesar ListaPrecioActivaQueue',
     @step_name = N'Ejecutar usp_ProcessListaPrecioActivaQueue',
     @subsystem = N'TSQL',
     @database_name = N'Mastersoft',
     @command = N'EXEC dbo.usp_ProcessListaPrecioActivaQueue;',
     @on_success_action = 1,
     @on_fail_action = 2;
EXEC sp_add_schedule
     @schedule_name = N'SCH_ListaActiva_10Seg',
     @freq_type = 4, @freq_interval = 1,
     @freq_subday_type = 2, @freq_subday_interval = 10,
     @active_start_time = 0;
EXEC sp_attach_schedule
     @job_name = N'Dipalza - Procesar ListaPrecioActivaQueue',
     @schedule_name = N'SCH_ListaActiva_10Seg';
EXEC sp_add_jobserver
     @job_name = N'Dipalza - Procesar ListaPrecioActivaQueue',
     @server_name = N'(LOCAL)';
GO


/* ============================================================================
   SECCIÓN C — CONFIGURACIÓN INICIAL (obligatoria tras re-instalar)
   Sembrar en VENTAS (la fuente). El job de 10 s lo propaga a Mastersoft, y el
   resync re-cotiza producto. Reemplaza los códigos por los reales.
   ----------------------------------------------------------------------------

USE ventas;
INSERT INTO dbo.ListaPrecioActiva (Rol, CodigoLista) VALUES ('P', '<codigo>');    -- principal  -> VentaNeto
-- INSERT INTO dbo.ListaPrecioActiva (Rol, CodigoLista) VALUES ('S', '<codigo>');  -- secundaria -> PrecioLista2

   ============================================================================ */
