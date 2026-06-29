USE msdb;
GO

-- =============================================
-- PROCEDIMIENTO PARA LIMPIEZA PREVIA
-- =============================================

-- JOB 1: Stock
IF EXISTS (SELECT job_id FROM msdb.dbo.sysjobs WHERE name = N'Dipalza - Procesar StockUpdateQueue')
    BEGIN
        EXEC sp_delete_job @job_name = N'Dipalza - Procesar StockUpdateQueue', @delete_unused_schedule = 1;
    END

-- JOB 2: Master Data
IF EXISTS (SELECT job_id FROM msdb.dbo.sysjobs WHERE name = N'Dipalza - Procesar MasterDataUpdateQueue')
    BEGIN
        EXEC sp_delete_job @job_name = N'Dipalza - Procesar MasterDataUpdateQueue', @delete_unused_schedule = 1;
    END
GO

-- =============================================
-- RE-CREACIÓN DE JOB 1: Procesar cola de Stock
-- =============================================
EXEC sp_add_job
     @job_name = N'Dipalza - Procesar StockUpdateQueue',
     @enabled = 1,
     @description = N'Procesa la cola de actualizaciones de stock hacia dbo.producto';

EXEC sp_add_jobstep
     @job_name = N'Dipalza - Procesar StockUpdateQueue',
     @step_name = N'Ejecutar usp_ProcessStockUpdateQueue',
     @subsystem = N'TSQL',
     @database_name = N'ventas',
     @command = N'EXEC dbo.usp_ProcessStockUpdateQueue;',
     @on_success_action = 1,
     @on_fail_action = 2;

-- Nombre corregido para consistencia
EXEC sp_add_schedule
     @schedule_name = N'SCH_Stock_15Seg',
     @freq_type = 4,
     @freq_interval = 1,
     @freq_subday_type = 2,        -- Segundos
     @freq_subday_interval = 15,   -- Cada 15 Segundos
     @active_start_time = 0;

EXEC sp_attach_schedule
     @job_name = N'Dipalza - Procesar StockUpdateQueue',
     @schedule_name = N'SCH_Stock_15Seg';

EXEC sp_add_jobserver
     @job_name = N'Dipalza - Procesar StockUpdateQueue',
     @server_name = N'(LOCAL)';
GO

-- =============================================
-- RE-CREACIÓN DE JOB 2: Procesar cola de Tablas Maestras
-- =============================================
EXEC sp_add_job
     @job_name = N'Dipalza - Procesar MasterDataUpdateQueue',
     @enabled = 1,
     @description = N'Procesa la cola de cambios en tablas maestras (rutas, condiciones, conducción)';

EXEC sp_add_jobstep
     @job_name = N'Dipalza - Procesar MasterDataUpdateQueue',
     @step_name = N'Ejecutar usp_ProcessMasterDataQueue',
     @subsystem = N'TSQL',
     @database_name = N'ventas',
     @command = N'EXEC dbo.usp_ProcessMasterDataQueue;',
     @on_success_action = 1,
     @on_fail_action = 2;

-- Nombre corregido para consistencia
EXEC sp_add_schedule
     @schedule_name = N'SCH_MasterData_1Min',
     @freq_type = 4,
     @freq_interval = 1,
     @freq_subday_type = 4,        -- Minutos
     @freq_subday_interval = 1,    -- Cada 1 minuto
     @active_start_time = 0;

EXEC sp_attach_schedule
     @job_name = N'Dipalza - Procesar MasterDataUpdateQueue',
     @schedule_name = N'SCH_MasterData_1Min';

EXEC sp_add_jobserver
     @job_name = N'Dipalza - Procesar MasterDataUpdateQueue',
     @server_name = N'(LOCAL)';
GO