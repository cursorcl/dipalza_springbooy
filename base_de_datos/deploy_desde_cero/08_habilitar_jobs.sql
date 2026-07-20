/* ============================================================================
   08_habilitar_jobs.sql
   Habilita los 4 jobs del SQL Server Agent creados (deshabilitados) por
   05_jobs_msdb.sql.

   EJECUTAR SOLO CUANDO SE CUMPLAN AMBAS CONDICIONES:
     1) 07_poblado_inicial_ventas.sql terminó con éxito (COMMIT, sin errores).
        Los jobs quedan deshabilitados en 05 justo para evitar que sus
        procesadores (04_procesadores_ventas.sql / usp_ProcessListaPrecioActivaQueue
        en 03) hagan MERGE sobre dbo.producto/ruta/etc. al mismo tiempo que 07
        hace el INSERT masivo dentro de su propia transacción — la carrera
        podía violar la PK y abortar todo el poblado inicial, o generar deadlock.
     2) El seed manual de ListaPrecioActiva de 06_configuracion_inicial.sql ya
        fue aplicado. Si se habilitan los jobs antes de ese seed, el job de
        precios corre sin ninguna lista activa configurada — no rompe nada
        (el procesador simplemente no encuentra filas que procesar), pero no
        tiene ningún efecto útil hasta que el seed exista.
   ============================================================================ */
USE msdb;
GO

EXEC sp_update_job @job_name = N'Dipalza - Procesar StockUpdateQueue', @enabled = 1;
EXEC sp_update_job @job_name = N'Dipalza - Procesar MasterDataUpdateQueue', @enabled = 1;
EXEC sp_update_job @job_name = N'Dipalza - Procesar PriceUpdateQueue', @enabled = 1;
EXEC sp_update_job @job_name = N'Dipalza - Procesar ListaPrecioActivaQueue', @enabled = 1;
GO
