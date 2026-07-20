/* ============================================================================
   DEPLOY DESDE CERO — Sincronización Mastersoft <-> ventas (Dipalza)
   ----------------------------------------------------------------------------
   Paquete de 9 scripts ejecutables EN ORDEN sobre una instancia SQL Server
   donde ya existe [Mastersoft] pero [ventas] todavía no existe.

   TOPOLOGÍA
     [Mastersoft] = base del ERP. Ya contiene las tablas operacionales/maestras
                    (invdetallepartes, detalledocumento, encabezadocumento,
                     msosttablas, ARTICULO, articulosnumerados, PRECIOS,
                     msoclientes, msovendedor, ...). Aquí se crean las COLAS,
                    la config de listas y los TRIGGERS.
     [ventas]     = base de la app Dipalza (la crea este mismo script 00).
                    Aquí se crea TODO el esquema de la app y los
                    PROCEDIMIENTOS que consumen las colas.
     [msdb]       = SQL Server Agent. Aquí se crean los JOBS (05, deshabilitados)
                    y se habilitan (08).

   CRUCES DE BASE: todas las bases están en la misma instancia -> las
   transacciones cross-database son locales, no requieren MSDTC.

   PRERREQUISITOS
     - [Mastersoft] ya existe, con sus tablas ERP y SQL Server Agent activo.
     - [ventas] NO debe existir todavía (este script la crea).

   ORDEN DE EJECUCIÓN (archivos de esta carpeta)
     00_crear_base_datos.sql          -> crea [ventas]
     01_esquema_ventas.sql            -> esquema completo + seed de roles
     02_listaprecioactiva_fuente.sql  -> ListaPrecioActiva fuente + triggers en [ventas]
     03_colas_triggers_mastersoft.sql -> colas + triggers + procesador inverso en [Mastersoft]
     04_procesadores_ventas.sql       -> los 3 procesadores de sincronización en [ventas]
     05_jobs_msdb.sql                 -> los 4 jobs del Agent en [msdb] (creados @enabled = 0)
     06_configuracion_inicial.sql     -> instrucciones manuales (ListaPrecioActiva + verificación)
     07_poblado_inicial_ventas.sql    -> carga masiva inicial desde [Mastersoft]
     08_habilitar_jobs.sql            -> habilita los 4 jobs recién después de que 07 y el
                                          seed manual de 06 estén confirmados (evita condición
                                          de carrera entre los jobs y la carga masiva de 07)

   Extraído y adaptado de base_de_datos/db/install_dipalza_sync.sql, que
   permanece intacto como referencia histórica (ver docs/superpowers/specs/
   2026-07-19-deploy-desde-cero-ventas-design.md para el detalle completo).
   ============================================================================ */

/* ============================================================================
   00_crear_base_datos.sql
   Crea la base de datos [ventas] desde cero. Ejecutar antes que cualquier
   otro script de esta carpeta. Requiere que [Mastersoft] ya exista en la
   misma instancia (no se crea aquí).
   ============================================================================ */
USE master;
GO

IF DB_ID(N'ventas') IS NOT NULL
BEGIN
    RAISERROR (N'La base de datos [ventas] ya existe. Este script asume una instalación desde cero; abortando para no pisar una base existente.', 16, 1);
END
GO

CREATE DATABASE ventas
    COLLATE Modern_Spanish_CI_AS;
GO
