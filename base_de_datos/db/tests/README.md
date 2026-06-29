# Manual rápido — `test_sync_precios.sh`

Prueba automatizada del flujo de sincronización de **precios** (Mastersoft → ventas) descrito en [`diseno_sincronizacion_dipalza.md`](../diseno_sincronizacion_dipalza.md).

## Qué hace

1. Verifica que existan las tablas, triggers, procedimientos y jobs de la sincronización.
2. Detecta automáticamente qué listas están activas como `P` (principal) y `S` (secundaria) en `ventas.ListaPrecioActiva` — no asume códigos fijos.
3. Verifica que `producto.VentaNeto`/`PrecioLista2` estén alineados con esas listas.
4. Hace un cambio real en `Mastersoft.PRECIOS` para la lista **principal** y comprueba que solo se mueva `VentaNeto` (revierte el cambio al final).
5. Hace lo mismo para la lista **secundaria** y comprueba que solo se mueva `PrecioLista2`.
6. Intenta borrar la lista principal y confirma que el *guard* lo rechaza (error 50002).
7. Con `--full`: desactiva la secundaria (verifica que `PrecioLista2 = 0` en todo el catálogo) y la reactiva, dejando todo como estaba.

Cada chequeo imprime `OK`/`FAIL` y al final hay un resumen con conteo total. Código de salida: `0` si todo pasó, `1` si algo falló.

## Requisitos

- `sqlcmd` instalado y en el `PATH`.
- Acceso de red al SQL Server de prueba (192.168.100.102:1433 por defecto).
- Que `ventas.ListaPrecioActiva` ya tenga al menos la fila `'P'` configurada (si no hay ninguna lista activa, el script aborta con un mensaje claro).

## Uso

```bash
cd base_de_datos/db/tests
export SYNC_TEST_PASSWORD='********'   # clave del usuario sa — nunca la dejes escrita en el script

./test_sync_precios.sh                 # pruebas estándar (no destructivas)
./test_sync_precios.sh --full          # + desactivación/reactivación de la lista secundaria
./test_sync_precios.sh --articulo 010  # fuerza un artículo de prueba específico
```

### Variables de entorno

| Variable | Default | Obligatoria |
|---|---|---|
| `SYNC_TEST_PASSWORD` | — | Sí (sin default por seguridad) |
| `SYNC_TEST_SERVER` | `192.168.100.102,1433` | No |
| `SYNC_TEST_USER` | `sa` | No |

## Cuándo usarlo

- Después de reinstalar o modificar `db/install_dipalza_sync.sql`, para confirmar que el flujo de precios sigue funcionando de punta a punta.
- Antes de cambiar la configuración de listas activas en producción, como humo (smoke test) en el ambiente de prueba.
- `--full` es más invasivo (toca `PrecioLista2` de **todo** el catálogo durante unos segundos) — úsalo solo en ambientes de prueba, no en producción.

## Si algo falla

- El script ya revierte los precios que modifica, incluso si una verificación falla a mitad de camino (cada `UPDATE` de prueba tiene su contraparte de reversión más abajo en el flujo) — pero si corta a mitad de un `--full` revisa manualmente que `ListaPrecioActiva` tenga ambas filas (`P` y `S`) antes de irte.
- Un `FAIL` en los pasos 1-2 (objetos/colas) generalmente indica que `install_dipalza_sync.sql` no se ejecutó completo en ese ambiente.
- Un `FAIL` en los pasos 4-5 (cambio en vivo) indica que algún job de SQL Agent está deshabilitado o caído — revisa con `SELECT * FROM msdb.dbo.sysjobs WHERE name LIKE 'Dipalza%'`.
