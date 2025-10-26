-- cola de actualización de tablas maestras en mastersoft
CREATE TABLE dbo.MasterDataUpdateQueue (
    EventID BIGINT IDENTITY(1,1) PRIMARY KEY,
    TableName NVARCHAR(128) NOT NULL,
    PrimaryKey NVARCHAR(255) NOT NULL,
    ChangeType CHAR(1) NOT NULL, -- 'I' (Insert), 'U' (Update), 'D' (Delete)
    CreatedAt DATETIME2(7) DEFAULT SYSUTCDATETIME(),
    ProcessedAt DATETIME2(7) NULL
);

-- Índice para que el "worker" pueda leer la cola eficientemente
CREATE INDEX IX_MasterDataUpdateQueue_ForProcessing ON dbo.MasterDataUpdateQueue (ProcessedAt, CreatedAt);

-- triggers en tablas maestras en mastersoft

CREATE OR ALTER TRIGGER dbo.trg_msosttablas_sync
ON dbo.msosttablas
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    -- Registrar INSERCIONES
    IF EXISTS (SELECT * FROM inserted) AND NOT EXISTS (SELECT * FROM deleted)
    BEGIN
        INSERT INTO dbo.MasterDataUpdateQueue (TableName, PrimaryKey, ChangeType)
        -- Concatena 'tabla' y 'codigo' para crear una clave única
        SELECT 'msosttablas', i.tabla + '|' + i.codigo, 'I'
        FROM inserted i;
    END

    -- Registrar ACTUALIZACIONES
    IF EXISTS (SELECT * FROM inserted) AND EXISTS (SELECT * FROM deleted)
    BEGIN
        INSERT INTO dbo.MasterDataUpdateQueue (TableName, PrimaryKey, ChangeType)
        SELECT 'msosttablas', i.tabla + '|' + i.codigo, 'U'
        FROM inserted i;
    END

    -- Registrar ELIMINACIONES
    IF NOT EXISTS (SELECT * FROM inserted) AND EXISTS (SELECT * FROM deleted)
    BEGIN
        INSERT INTO dbo.MasterDataUpdateQueue (TableName, PrimaryKey, ChangeType)
        SELECT 'msosttablas', d.tabla + '|' + d.codigo, 'D'
        FROM deleted d;
    END
END;

CREATE OR ALTER PROCEDURE dbo.usp_ProcessMasterDataQueue
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @batchSize INT = 100;

    DECLARE @eventsToProcess TABLE (
        EventID BIGINT PRIMARY KEY,
        TableName NVARCHAR(128),
        PrimaryKey NVARCHAR(255),
        ChangeType CHAR(1)
    );

    -- 1. Capturar lote de eventos (sin cambios aquí)
    ;WITH cte_batch AS (
        SELECT TOP (@batchSize) EventID, TableName, PrimaryKey, ChangeType, ProcessedAt
        FROM [Mastersoft].[dbo].[MasterDataUpdateQueue]
        WHERE ProcessedAt IS NULL ORDER BY CreatedAt
    )
    UPDATE cte_batch SET ProcessedAt = SYSUTCDATETIME()
    OUTPUT inserted.EventID, inserted.TableName, inserted.PrimaryKey, inserted.ChangeType
    INTO @eventsToProcess;

    -- ... (la lógica para procesar 'msoclientes' no cambia) ...

    -- 3. Procesar los eventos de msosttablas (LÓGICA COMPLETAMENTE NUEVA Y MÁS EFICIENTE)
    IF EXISTS (SELECT 1 FROM @eventsToProcess WHERE TableName = 'msosttablas')
    BEGIN
        -- Preparamos una tabla temporal con los datos completos de las filas que cambiaron
        ;WITH ParsedEvents AS (
            SELECT
                PrimaryKey,
                ChangeType,
                LEFT(PrimaryKey, CHARINDEX('|', PrimaryKey) - 1) AS tabla,
                SUBSTRING(PrimaryKey, CHARINDEX('|', PrimaryKey) + 1, LEN(PrimaryKey)) AS codigo
            FROM @eventsToProcess
            WHERE TableName = 'msosttablas'
        ),
        SourceData AS (
            SELECT
                p.ChangeType,
                s.tabla,
                s.codigo,
                s.descripcion,
                s.valor
            FROM [Mastersoft].[dbo].[msosttablas] s
            JOIN ParsedEvents p ON s.tabla = p.tabla COLLATE DATABASE_DEFAULT AND s.codigo = p.codigo COLLATE DATABASE_DEFAULT
        )
        -- Ahora aplicamos los cambios a cada tabla de destino por separado
            -- Para rutas (tabla '017') - no usa 'valor'
            MERGE dbo.ruta AS T
            USING (SELECT * FROM SourceData WHERE tabla = '017') AS S
            ON (T.codigo = S.codigo COLLATE DATABASE_DEFAULT)
            WHEN MATCHED AND S.ChangeType IN ('I', 'U') AND T.descripcion <> S.descripcion COLLATE DATABASE_DEFAULT THEN UPDATE SET T.descripcion = S.descripcion COLLATE DATABASE_DEFAULT
            WHEN MATCHED AND S.ChangeType = 'D' THEN DELETE
            WHEN NOT MATCHED BY TARGET AND S.ChangeType IN ('I', 'U') THEN INSERT (codigo, descripcion) VALUES (S.codigo, S.descripcion );

            -- Para condicion_venta (tabla '009') - no usa 'valor'
            MERGE dbo.condicion_venta AS T
            USING (SELECT * FROM SourceData WHERE tabla = '009') AS S
            ON (T.codigo = S.codigo)
            WHEN MATCHED AND S.ChangeType IN ('I', 'U') AND T.descripcion <> S.descripcion THEN UPDATE SET T.descripcion = S.descripcion COLLATE DATABASE_DEFAULT
            WHEN MATCHED AND S.ChangeType = 'D' THEN DELETE
            WHEN NOT MATCHED BY TARGET AND S.ChangeType IN ('I', 'U') THEN INSERT (codigo, descripcion) VALUES (S.codigo, S.descripcion);

            -- Para conducción (tabla '015') - SÍ usa 'valor'
            MERGE dbo.conduccion AS T
            USING (SELECT * FROM SourceData WHERE tabla = '015') AS S
            ON (T.codigo = S.codigo)
            WHEN MATCHED AND S.ChangeType IN ('I', 'U') AND (T.descripcion <> S.descripcion OR T.valor <> S.valor) THEN UPDATE SET T.descripcion = S.descripcion, T.valor = S.valor
            WHEN MATCHED AND S.ChangeType = 'D' THEN DELETE
            WHEN NOT MATCHED BY TARGET AND S.ChangeType IN ('I', 'U') THEN INSERT (codigo, descripcion, valor) VALUES (S.codigo, S.descripcion, S.valor);
    END
END;