
-- Tabla para el update del stock de productos en Mastersoft
CREATE TABLE dbo.StockUpdateQueue (
    EventID BIGINT IDENTITY(1,1) PRIMARY KEY,
    Articulo NVARCHAR(50) NOT NULL, -- O el tipo de dato que uses para el código de artículo
    DeltaStock DECIMAL(18, 4) NOT NULL,
    CreatedAt DATETIME2(7) DEFAULT SYSUTCDATETIME(),
    ProcessedAt DATETIME2(7) NULL -- Se marcará cuando el evento sea procesado
);

-- Procedimiento almacenado en ventas para procesar los cambios en la cola de stock
CREATE OR ALTER PROCEDURE dbo.usp_ProcessStockUpdateQueue
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @batchSize INT = 500;

    DECLARE @eventsToProcess TABLE (
        EventID BIGINT PRIMARY KEY,
        Articulo NVARCHAR(50),
        TotalDeltaStock DECIMAL(18, 4)
    );

    -- Paso A: Capturar el lote de eventos (esto no cambia)
    ;WITH cte_batch AS (
        SELECT TOP (@batchSize) EventID, Articulo, DeltaStock, ProcessedAt
        FROM [Mastersoft].[dbo].[StockUpdateQueue]
        WHERE ProcessedAt IS NULL
        ORDER BY CreatedAt
    )
    UPDATE cte_batch
    SET ProcessedAt = SYSUTCDATETIME()
    OUTPUT inserted.EventID, inserted.Articulo, inserted.DeltaStock
    INTO @eventsToProcess (EventID, Articulo, TotalDeltaStock);

    IF NOT EXISTS (SELECT 1 FROM @eventsToProcess)
    BEGIN
        RETURN;
    END

    -- Paso B: Agregar los deltas (esto no cambia)
    ;WITH agg_batch AS (
        SELECT Articulo, SUM(TotalDeltaStock) AS FinalDelta
        FROM @eventsToProcess
        GROUP BY Articulo
    )
    -- Paso C: Realizar el MERGE directamente sobre la tabla 'producto'
    MERGE dbo.producto AS T -- <<-- DESTINO CORREGIDO
    USING (
        -- La fuente de datos une los deltas con la tabla ARTICULO de la BD ANTIGUA
        -- para obtener los detalles en caso de que el producto sea nuevo.
        SELECT
            ab.Articulo,
            ab.FinalDelta,
            a.Descripcion,
            a.VentaNeto,
            a.PorcIla,
            a.PorcCarne,
            a.Unidad,
            a.CodigoIla,
            CASE WHEN an.Articulo IS NOT NULL THEN 1 ELSE 0 END AS EsNumerado
        FROM agg_batch ab
        -- Se une con la tabla de la base de datos antigua
        LEFT JOIN [Mastersoft].[dbo].[ARTICULO] a ON a.Articulo = ab.Articulo COLLATE DATABASE_DEFAULT
        LEFT JOIN [Mastersoft].[dbo].[articulosnumerados] an ON an.Articulo = ab.Articulo COLLATE DATABASE_DEFAULT
    ) AS S
    ON T.Articulo = S.Articulo COLLATE DATABASE_DEFAULT -- La condición de cruce es el código del artículo
    
    -- CASO 1: El producto YA EXISTE en la tabla dbo.producto
    WHEN MATCHED THEN
        UPDATE SET
            Stock = COALESCE(T.Stock, 0) + S.FinalDelta, -- Actualiza solo el stock
            last_update = SYSUTCDATETIME()

    -- CASO 2: El producto NO EXISTE en la tabla dbo.producto
    WHEN NOT MATCHED BY TARGET AND S.Descripcion IS NOT NULL THEN
        -- Lo inserta con todos sus datos traídos de la BD antigua
        INSERT (Articulo, Descripcion, VentaNeto, PorcIla, PorcCarne, Unidad,
                Stock, last_update, Numbered, CodigoIla)
        VALUES (S.Articulo, S.Descripcion, S.VentaNeto, S.PorcIla, S.PorcCarne, S.Unidad,
                S.FinalDelta, SYSUTCDATETIME(), S.EsNumerado, S.CodigoIla);
END;
-- Un índice es crucial para que el proceso de lectura sea rápido
CREATE INDEX IX_StockUpdateQueue_ForProcessing ON dbo.StockUpdateQueue (ProcessedAt, CreatedAt);

-- triggers para actualizar la tabla de stock

CREATE OR ALTER TRIGGER dbo.trg_invdetallepartes_stockresumen
ON dbo.invdetallepartes
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    ;WITH base AS (
        SELECT i.articulo, i.[Local] AS [local], i.Tipoid, i.cantidad AS cant
        FROM inserted i
        UNION ALL
        SELECT d.articulo, d.[Local] AS [local], d.Tipoid, -d.cantidad AS cant
        FROM deleted d
    ),
    agg AS (
        SELECT  articulo,
                SUM(CASE WHEN Tipoid = 17 THEN  cant      -- entrada
                         WHEN Tipoid = 18 THEN -cant      -- salida
                         ELSE 0 END) AS delta_stock
        FROM base
        WHERE [local] = '000'
        GROUP BY articulo
        HAVING SUM(CASE WHEN Tipoid = 17 THEN  cant
                        WHEN Tipoid = 18 THEN -cant
                        ELSE 0 END) <> 0
    )
    -- Reemplazo final para los otros triggers:
	INSERT INTO dbo.StockUpdateQueue (Articulo, DeltaStock)
	SELECT articulo, delta_stock
	FROM agg; -- (o la CTE que contenga el delta final en cada trigger)
END;


CREATE OR ALTER TRIGGER dbo.trg_detalledocumento_stockresumen
ON dbo.detalledocumento
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    ;WITH base AS (
        SELECT d.articulo, d.[local], d.tipoid, d.cantidad AS cant, e.vigente
        FROM inserted d
        JOIN dbo.encabezadocumento e ON e.id = d.id
        UNION ALL
        SELECT d.articulo, d.[local], d.tipoid, -d.cantidad AS cant, e.vigente
        FROM deleted d
        JOIN dbo.encabezadocumento e ON e.id = d.id
    ),
    agg AS (
        SELECT  articulo,
                SUM(CASE
                      WHEN vigente = 1 AND tipoid = '09'         THEN  cant  -- entrada
                      WHEN vigente = 1 AND tipoid IN ('06','10') THEN -cant  -- salida
                      ELSE 0
                    END) AS delta_stock
        FROM base
        WHERE [local] = '000'
        GROUP BY articulo
        HAVING SUM(CASE
                      WHEN vigente = 1 AND tipoid = '09'         THEN  cant
                      WHEN vigente = 1 AND tipoid IN ('06','10') THEN -cant
                      ELSE 0
                    END) <> 0
    ),
    -- AQUÍ ESTÁ EL CAMBIO: Insertamos en la cola en lugar de hacer el MERGE
    INSERT INTO dbo.StockUpdateQueue (Articulo, DeltaStock)
    SELECT articulo, delta_stock
    FROM agg;
END;


CREATE OR ALTER TRIGGER dbo.trg_encabezadocumento_stockresumen_vigente
ON dbo.encabezadocumento
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    ;WITH chg AS (
        SELECT  i.id,
                ISNULL(d.vigente, 0) AS vigente_old,
                ISNULL(i.vigente, 0) AS vigente_new
        FROM inserted i
        LEFT JOIN deleted d ON d.id = i.id
        WHERE ISNULL(d.vigente, 0) <> ISNULL(i.vigente, 0)
    ),
    det AS (
        SELECT  c.id, d.articulo, d.[local], d.tipoid, d.cantidad AS cantidad,
                c.vigente_old, c.vigente_new
        FROM chg c
        JOIN dbo.detalledocumento d ON d.id = c.id
    ),
    agg AS (
        SELECT  d.articulo,
                SUM(CASE
                      -- Activación (0→1): aplicar
                      WHEN d.vigente_old = 0 AND d.vigente_new = 1 THEN
                           CASE WHEN d.tipoid = '09'         THEN  d.cantidad
                                WHEN d.tipoid IN ('06','10') THEN -d.cantidad
                                ELSE 0 END
                      -- Desactivación (1→0): revertir
                      WHEN d.vigente_old = 1 AND d.vigente_new = 0 THEN
                           CASE WHEN d.tipoid = '09'         THEN -d.cantidad
                                WHEN d.tipoid IN ('06','10') THEN  d.cantidad
                                ELSE 0 END
                      ELSE 0
                    END) AS delta_stock
        FROM det d
        WHERE d.[local] = '000'
        GROUP BY d.articulo
        HAVING SUM(CASE
                      WHEN d.vigente_old = 0 AND d.vigente_new = 1 THEN
                           CASE WHEN d.tipoid = '09'         THEN  d.cantidad
                                WHEN d.tipoid IN ('06','10') THEN -d.cantidad
                                ELSE 0 END
                      WHEN d.vigente_old = 1 AND d.vigente_new = 0 THEN
                           CASE WHEN d.tipoid = '09'         THEN -d.cantidad
                                WHEN d.tipoid IN ('06','10') THEN  d.cantidad
                                ELSE 0 END
                      ELSE 0
                    END) <> 0
    )
    -- Reemplazo final para los otros triggers:
	INSERT INTO dbo.StockUpdateQueue (Articulo, DeltaStock)
	SELECT articulo, delta_stock
	FROM agg; -- (o la CTE que contenga el delta final en cada trigger)
END;
