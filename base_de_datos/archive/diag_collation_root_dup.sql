/* ============================================================================
   DIAGNÓSTICO DE COLLATION — Dipalza
   Ejecutar en SSMS o DBeaver (Alt+X). Revela la collation real de cada pieza.
   ============================================================================ */

-- 1) Collation por defecto de cada base implicada
SELECT name AS BaseDeDatos, collation_name AS CollationPorDefecto
FROM sys.databases
WHERE name IN ('ventas', 'Mastersoft', 'tempdb');
GO

-- 2) Collation de las columnas de texto que participan en las comparaciones
--    (las que NO sean Modern_Spanish_CI_AS son las sospechosas)
SELECT 'Mastersoft' AS Base, 'PRECIOS'           AS TablaObjeto, c.name AS Columna, c.collation_name
FROM Mastersoft.sys.columns c
WHERE c.object_id = OBJECT_ID('Mastersoft.dbo.PRECIOS')
  AND c.name IN ('Articulo', 'CodigoLista')
UNION ALL
SELECT 'Mastersoft', 'PriceUpdateQueue', c.name, c.collation_name
FROM Mastersoft.sys.columns c
WHERE c.object_id = OBJECT_ID('Mastersoft.dbo.PriceUpdateQueue')
  AND c.name IN ('Articulo', 'Rol')
UNION ALL
SELECT 'Mastersoft', 'ListaPrecioActiva', c.name, c.collation_name
FROM Mastersoft.sys.columns c
WHERE c.object_id = OBJECT_ID('Mastersoft.dbo.ListaPrecioActiva')
  AND c.name IN ('Rol', 'CodigoLista')
UNION ALL
SELECT 'ventas', 'producto', c.name, c.collation_name
FROM ventas.sys.columns c
WHERE c.object_id = OBJECT_ID('ventas.dbo.producto')
  AND c.name IN ('Articulo')
UNION ALL
SELECT 'ventas', 'ListaPrecioActiva', c.name, c.collation_name
FROM ventas.sys.columns c
WHERE c.object_id = OBJECT_ID('ventas.dbo.ListaPrecioActiva')
  AND c.name IN ('Rol', 'CodigoLista')
ORDER BY Base, TablaObjeto, Columna;
GO

-- 3) ¿Quedó alguna columna de texto en estas tablas que NO sea Modern_Spanish?
--    (lista directa de "culpables")
SELECT DB_NAME() AS Base, t.name AS Tabla, c.name AS Columna, c.collation_name
FROM Mastersoft.sys.columns c
         INNER JOIN Mastersoft.sys.tables t ON t.object_id = c.object_id
WHERE c.collation_name IS NOT NULL
  AND c.collation_name <> 'Modern_Spanish_CI_AS'
  AND t.name IN ('PRECIOS', 'PriceUpdateQueue', 'ListaPrecioActiva',
                 'StockUpdateQueue', 'MasterDataUpdateQueue');
GO
