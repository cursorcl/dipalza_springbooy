#!/usr/bin/env bash
# ============================================================================
# test_sync_precios.sh — Prueba automatizada del flujo de sincronizacion de
# precios (Mastersoft -> ventas) descrito en diseno_sincronizacion_dipalza.md
#
# Verifica, contra una instancia real, que:
#   1) Los objetos de sincronizacion (tablas, triggers, procs, jobs) existen.
#   2) producto.VentaNeto / PrecioLista2 estan alineados con las listas
#      activas (P y S) configuradas en ventas.ListaPrecioActiva.
#   3) Un cambio en vivo en la lista PRINCIPAL solo mueve VentaNeto.
#   4) Un cambio en vivo en la lista SECUNDARIA solo mueve PrecioLista2.
#   5) El guard "siempre debe existir P" rechaza el borrado de la principal.
#   6) (opcional, --full) Desactivar S pone PrecioLista2=0 en todo el
#      catalogo, y reactivarla lo repuebla correctamente.
#
# No requiere reasignar las listas activas: usa las que ya estan
# configuradas en ventas.ListaPrecioActiva (P obligatoria, S opcional).
#
# USO
#   export SYNC_TEST_PASSWORD='********'      # password del usuario sa
#   ./test_sync_precios.sh                    # pruebas no destructivas
#   ./test_sync_precios.sh --full             # incluye paso 6 (mas invasivo)
#   ./test_sync_precios.sh --articulo 010     # fuerza el articulo de prueba
#
# VARIABLES DE ENTORNO
#   SYNC_TEST_SERVER    host,puerto del SQL Server (default 192.168.100.102,1433)
#   SYNC_TEST_USER      usuario (default sa)
#   SYNC_TEST_PASSWORD  password (OBLIGATORIO, sin default por seguridad)
#
# SALIDA
#   Imprime cada verificacion como OK/FAIL y termina con un resumen.
#   Codigo de salida 0 si todo paso, 1 si hubo al menos un FAIL.
# ============================================================================
set -uo pipefail

SERVER="${SYNC_TEST_SERVER:-192.168.100.102,1433}"
SQLUSER="${SYNC_TEST_USER:-sa}"
ARTICULO_FORZADO=""
FULL=0

while [ $# -gt 0 ]; do
  case "$1" in
    --full) FULL=1 ;;
    --articulo) ARTICULO_FORZADO="$2"; shift ;;
    *) echo "Argumento desconocido: $1" >&2; exit 2 ;;
  esac
  shift
done

if [ -z "${SYNC_TEST_PASSWORD:-}" ]; then
  echo "ERROR: defina SYNC_TEST_PASSWORD con la clave del usuario '$SQLUSER' antes de ejecutar." >&2
  exit 2
fi
export SQLCMDPASSWORD="$SYNC_TEST_PASSWORD"

if ! command -v sqlcmd >/dev/null 2>&1; then
  echo "ERROR: no se encontro 'sqlcmd' en el PATH." >&2
  exit 2
fi

PASS=0
FAIL=0
declare -a RESULTS

ok()   { RESULTS+=("OK   - $1"); PASS=$((PASS+1)); echo "OK   - $1"; }
bad()  { RESULTS+=("FAIL - $1"); FAIL=$((FAIL+1)); echo "FAIL - $1"; }
info() { echo "----  $1"; }

# sql <db> <query>  -> imprime el resultado tabular (con headers) a stdout
sql() {
  sqlcmd -S "$SERVER" -U "$SQLUSER" -C -d "$1" -Q "SET NOCOUNT ON; $2" 2>&1
}

# sqlval <db> <query>  -> retorna SOLO el primer valor escalar, sin headers
sqlval() {
  sqlcmd -S "$SERVER" -U "$SQLUSER" -C -d "$1" -h -1 -W -Q "SET NOCOUNT ON; $2" 2>&1 | tr -d ' \r' | head -n1
}

# wait_zero <db> <query_que_retorna_count> <timeout_s> <intervalo_s> <descripcion>
wait_zero() {
  local db="$1" q="$2" timeout="$3" interval="$4" desc="$5"
  local elapsed=0 val
  while [ "$elapsed" -lt "$timeout" ]; do
    val=$(sqlval "$db" "$q")
    if [ "$val" = "0" ]; then
      ok "$desc (drenado en ${elapsed}s)"
      return 0
    fi
    sleep "$interval"
    elapsed=$((elapsed+interval))
  done
  bad "$desc (no drenó tras ${timeout}s, quedaron $val pendientes)"
  return 1
}

# wait_eq <db> <query_escalar> <valor_esperado> <timeout_s> <intervalo_s> <descripcion>
wait_eq() {
  local db="$1" q="$2" esperado="$3" timeout="$4" interval="$5" desc="$6"
  local elapsed=0 val
  while [ "$elapsed" -lt "$timeout" ]; do
    val=$(sqlval "$db" "$q")
    if [ "$val" = "$esperado" ]; then
      ok "$desc = $val"
      return 0
    fi
    sleep "$interval"
    elapsed=$((elapsed+interval))
  done
  bad "$desc (esperado: $esperado, obtenido: $val tras ${timeout}s)"
  return 1
}

echo "============================================================"
echo " Prueba de sincronizacion de precios — $(date '+%Y-%m-%d %H:%M:%S')"
echo " Servidor: $SERVER   Modo: $([ $FULL -eq 1 ] && echo 'FULL (incluye desactivacion)' || echo 'estandar')"
echo "============================================================"

# ---------------------------------------------------------------------------
# 1) Objetos instalados
# ---------------------------------------------------------------------------
info "1) Verificando objetos instalados"

n=$(sqlval Mastersoft "SELECT COUNT(*) FROM sys.tables WHERE name IN ('StockUpdateQueue','MasterDataUpdateQueue','ListaPrecioActiva','PriceUpdateQueue');")
[ "$n" = "4" ] && ok "Tablas de colas en Mastersoft (4/4)" || bad "Tablas de colas en Mastersoft (encontradas: $n/4)"

n=$(sqlval Mastersoft "SELECT COUNT(*) FROM sys.triggers WHERE name IN ('trg_precios_priceupdate','trg_listaprecioactiva_resync','trg_listaprecioactiva_guard');")
[ "$n" = "3" ] && ok "Triggers de precios en Mastersoft (3/3)" || bad "Triggers de precios en Mastersoft (encontrados: $n/3)"

n=$(sqlval Mastersoft "SELECT COUNT(*) FROM sys.procedures WHERE name = 'usp_ProcessPriceUpdateQueue';")
[ "$n" -ge "1" ] 2>/dev/null && ok "usp_ProcessPriceUpdateQueue existe" || bad "usp_ProcessPriceUpdateQueue no encontrado"

n=$(sqlval ventas "SELECT COUNT(*) FROM sys.tables WHERE name IN ('ListaPrecioActiva','ListaPrecioActivaQueue','producto');")
[ "$n" = "3" ] && ok "Tablas en ventas (ListaPrecioActiva/Queue/producto) (3/3)" || bad "Tablas en ventas (encontradas: $n/3)"

n=$(sqlval ventas "SELECT COUNT(*) FROM sys.triggers WHERE name IN ('trg_listaprecioactiva_outbox','trg_listaprecioactiva_guard');")
[ "$n" = "2" ] && ok "Triggers de sync inverso en ventas (2/2)" || bad "Triggers de sync inverso en ventas (encontrados: $n/2)"

n=$(sqlval msdb "SELECT COUNT(*) FROM sysjobs WHERE name='Dipalza - Procesar PriceUpdateQueue' AND enabled=1;")
[ "$n" = "1" ] && ok "Job 'Procesar PriceUpdateQueue' habilitado" || bad "Job 'Procesar PriceUpdateQueue' no habilitado"

n=$(sqlval msdb "SELECT COUNT(*) FROM sysjobs WHERE name='Dipalza - Procesar ListaPrecioActivaQueue' AND enabled=1;")
[ "$n" = "1" ] && ok "Job 'Procesar ListaPrecioActivaQueue' habilitado" || bad "Job 'Procesar ListaPrecioActivaQueue' no habilitado"

# ---------------------------------------------------------------------------
# 2) Configuracion activa + estado base
# ---------------------------------------------------------------------------
info "2) Detectando configuracion activa de listas"

LISTA_P=$(sqlval ventas "SELECT CodigoLista FROM dbo.ListaPrecioActiva WHERE Rol='P';")
LISTA_S=$(sqlval ventas "SELECT CodigoLista FROM dbo.ListaPrecioActiva WHERE Rol='S';")

if [ -z "$LISTA_P" ]; then
  bad "No hay lista PRINCIPAL activa en ventas.ListaPrecioActiva — no se puede continuar"
  echo ""
  echo "Resumen: $PASS OK / $FAIL FAIL"
  exit 1
fi
ok "Lista principal activa detectada: '$LISTA_P'"

if [ -n "$LISTA_S" ]; then
  ok "Lista secundaria activa detectada: '$LISTA_S'"
else
  info "No hay lista secundaria activa; se omiten las pruebas sobre PrecioLista2"
fi

n=$(sqlval ventas "
SELECT COUNT(*) FROM dbo.producto p
INNER JOIN Mastersoft.dbo.PRECIOS pr ON pr.Articulo=p.Articulo AND pr.CodigoLista='$LISTA_P'
WHERE p.VentaNeto <> pr.VentaNeto;")
[ "$n" = "0" ] && ok "producto.VentaNeto alineado con lista '$LISTA_P' (0 diferencias)" || bad "producto.VentaNeto con $n diferencias contra lista '$LISTA_P'"

if [ -n "$LISTA_S" ]; then
  n=$(sqlval ventas "
SELECT COUNT(*) FROM dbo.producto p
INNER JOIN Mastersoft.dbo.PRECIOS pr ON pr.Articulo=p.Articulo AND pr.CodigoLista='$LISTA_S'
WHERE p.PrecioLista2 <> pr.VentaNeto;")
  [ "$n" = "0" ] && ok "producto.PrecioLista2 alineado con lista '$LISTA_S' (0 diferencias)" || bad "producto.PrecioLista2 con $n diferencias contra lista '$LISTA_S'"
fi

# ---------------------------------------------------------------------------
# 3) Elegir articulo de prueba
# ---------------------------------------------------------------------------
info "3) Eligiendo articulo de prueba"

# Se evitan articulos "basura" (codigos de 1 caracter / simbolos, precio 0)
# para que el cambio de precio sea inequivoco de observar.
if [ -n "$ARTICULO_FORZADO" ]; then
  ARTICULO="$ARTICULO_FORZADO"
elif [ -n "$LISTA_S" ]; then
  ARTICULO=$(sqlval Mastersoft "
SELECT TOP 1 a.Articulo FROM dbo.PRECIOS a
INNER JOIN dbo.PRECIOS b ON b.Articulo=a.Articulo AND b.CodigoLista='$LISTA_S'
WHERE a.CodigoLista='$LISTA_P' AND a.VentaNeto<>b.VentaNeto
  AND a.VentaNeto>0 AND b.VentaNeto>0 AND LEN(LTRIM(RTRIM(a.Articulo)))>1
ORDER BY a.Articulo;")
else
  ARTICULO=$(sqlval Mastersoft "
SELECT TOP 1 Articulo FROM dbo.PRECIOS
WHERE CodigoLista='$LISTA_P' AND VentaNeto>0 AND LEN(LTRIM(RTRIM(Articulo)))>1
ORDER BY Articulo;")
fi

if [ -z "$ARTICULO" ]; then
  bad "No se encontro un articulo de prueba valido"
  echo ""
  echo "Resumen: $PASS OK / $FAIL FAIL"
  exit 1
fi
ok "Articulo de prueba: '$ARTICULO'"

# IMPORTANTE: usar siempre CAST(... AS varchar(30)) para leer valores money.
# CAST da formato fijo de 2 decimales; el SELECT crudo via sqlcmd usa otro
# formato (4 decimales, sin cero inicial). Mezclar ambos rompe las
# comparaciones de string mas adelante.
PRECIO_P_ORIG=$(sqlval Mastersoft "SELECT CAST(VentaNeto AS varchar(30)) FROM dbo.PRECIOS WHERE Articulo='$ARTICULO' AND CodigoLista='$LISTA_P';")
info "Precio original en lista principal '$LISTA_P': $PRECIO_P_ORIG"
if [ -n "$LISTA_S" ]; then
  PRECIO_S_ORIG=$(sqlval Mastersoft "SELECT CAST(VentaNeto AS varchar(30)) FROM dbo.PRECIOS WHERE Articulo='$ARTICULO' AND CodigoLista='$LISTA_S';")
  info "Precio original en lista secundaria '$LISTA_S': $PRECIO_S_ORIG"
fi

# ---------------------------------------------------------------------------
# 4) Cambio en vivo sobre la lista PRINCIPAL
# ---------------------------------------------------------------------------
info "4) Cambio en vivo sobre la lista PRINCIPAL ('$LISTA_P')"

NUEVO_P="12345.67"
sql Mastersoft "UPDATE dbo.PRECIOS SET VentaNeto=$NUEVO_P WHERE Articulo='$ARTICULO' AND CodigoLista='$LISTA_P';" >/dev/null
wait_eq ventas "SELECT CAST(VentaNeto AS varchar(30)) FROM dbo.producto WHERE Articulo='$ARTICULO';" "$NUEVO_P" 60 4 \
  "VentaNeto se actualizo tras cambio en lista principal"

if [ -n "$LISTA_S" ]; then
  val=$(sqlval ventas "SELECT CAST(PrecioLista2 AS varchar(30)) FROM dbo.producto WHERE Articulo='$ARTICULO';")
  [ "$val" = "${PRECIO_S_ORIG}" ] && ok "PrecioLista2 no se altero por el cambio en la lista principal ($val)" \
    || bad "PrecioLista2 cambio inesperadamente tras tocar solo la lista principal (esperado: $PRECIO_S_ORIG, obtenido: $val)"
fi

sql Mastersoft "UPDATE dbo.PRECIOS SET VentaNeto=$PRECIO_P_ORIG WHERE Articulo='$ARTICULO' AND CodigoLista='$LISTA_P';" >/dev/null
wait_eq ventas "SELECT CAST(VentaNeto AS varchar(30)) FROM dbo.producto WHERE Articulo='$ARTICULO';" "${PRECIO_P_ORIG}" 60 4 \
  "VentaNeto restaurado a su valor original"

# ---------------------------------------------------------------------------
# 5) Cambio en vivo sobre la lista SECUNDARIA (si existe)
# ---------------------------------------------------------------------------
if [ -n "$LISTA_S" ]; then
  info "5) Cambio en vivo sobre la lista SECUNDARIA ('$LISTA_S')"

  NUEVO_S="9999.99"
  sql Mastersoft "UPDATE dbo.PRECIOS SET VentaNeto=$NUEVO_S WHERE Articulo='$ARTICULO' AND CodigoLista='$LISTA_S';" >/dev/null
  wait_eq ventas "SELECT CAST(PrecioLista2 AS varchar(30)) FROM dbo.producto WHERE Articulo='$ARTICULO';" "${NUEVO_S}" 60 4 \
    "PrecioLista2 se actualizo tras cambio en lista secundaria"

  val=$(sqlval ventas "SELECT CAST(VentaNeto AS varchar(30)) FROM dbo.producto WHERE Articulo='$ARTICULO';")
  [ "$val" = "${PRECIO_P_ORIG}" ] && ok "VentaNeto no se altero por el cambio en la lista secundaria ($val)" \
    || bad "VentaNeto cambio inesperadamente tras tocar solo la lista secundaria (esperado: $PRECIO_P_ORIG, obtenido: $val)"

  sql Mastersoft "UPDATE dbo.PRECIOS SET VentaNeto=$PRECIO_S_ORIG WHERE Articulo='$ARTICULO' AND CodigoLista='$LISTA_S';" >/dev/null
  wait_eq ventas "SELECT CAST(PrecioLista2 AS varchar(30)) FROM dbo.producto WHERE Articulo='$ARTICULO';" "${PRECIO_S_ORIG}" 60 4 \
    "PrecioLista2 restaurado a su valor original"
else
  info "5) Omitido (no hay lista secundaria activa)"
fi

# ---------------------------------------------------------------------------
# 6) Guard "al menos una principal"
# ---------------------------------------------------------------------------
info "6) Guard de integridad sobre ListaPrecioActiva"

out=$(sql ventas "DELETE FROM dbo.ListaPrecioActiva WHERE Rol='P';")
if echo "$out" | grep -q "50002"; then
  ok "Borrado de la lista principal fue rechazado por el guard (error 50002)"
else
  bad "El borrado de la lista principal NO fue rechazado (salida: $out)"
fi

val=$(sqlval ventas "SELECT CodigoLista FROM dbo.ListaPrecioActiva WHERE Rol='P';")
[ "$val" = "$LISTA_P" ] && ok "La lista principal sigue intacta tras el intento de borrado ($val)" \
  || bad "La lista principal NO quedo intacta tras el intento de borrado (encontrado: '$val')"

# ---------------------------------------------------------------------------
# 7) (opcional) Desactivacion y reactivacion de la secundaria
# ---------------------------------------------------------------------------
if [ $FULL -eq 1 ] && [ -n "$LISTA_S" ]; then
  info "7) [--full] Desactivacion masiva de la lista secundaria"

  sql ventas "DELETE FROM dbo.ListaPrecioActiva WHERE Rol='S';" >/dev/null
  wait_zero ventas "SELECT COUNT(*) FROM dbo.ListaPrecioActivaQueue WHERE ProcessedAt IS NULL;" 30 3 \
    "Cola ListaPrecioActivaQueue drenada tras desactivar S"

  n=$(sqlval ventas "SELECT COUNT(*) FROM dbo.producto WHERE PrecioLista2 <> 0;")
  [ "$n" = "0" ] && ok "PrecioLista2 = 0 en todo el catalogo tras desactivar S" \
    || bad "PrecioLista2 distinto de 0 en $n productos tras desactivar S"

  info "Reactivando lista secundaria '$LISTA_S' para dejar el ambiente como estaba"
  sql ventas "INSERT INTO dbo.ListaPrecioActiva (Rol, CodigoLista) VALUES ('S','$LISTA_S');" >/dev/null
  wait_zero ventas "SELECT COUNT(*) FROM dbo.ListaPrecioActivaQueue WHERE ProcessedAt IS NULL;" 30 3 \
    "Cola ListaPrecioActivaQueue drenada tras reactivar S"
  wait_zero Mastersoft "SELECT COUNT(*) FROM dbo.PriceUpdateQueue WHERE ProcessedAt IS NULL;" 90 5 \
    "Cola PriceUpdateQueue drenada tras reactivar S"

  val=$(sqlval ventas "SELECT CAST(PrecioLista2 AS varchar(30)) FROM dbo.producto WHERE Articulo='$ARTICULO';")
  [ "$val" = "${PRECIO_S_ORIG}" ] && ok "PrecioLista2 del articulo de prueba repoblado correctamente ($val)" \
    || bad "PrecioLista2 del articulo de prueba quedo en $val (esperado: $PRECIO_S_ORIG)"
elif [ $FULL -eq 1 ]; then
  info "7) [--full] Omitido (no hay lista secundaria activa para desactivar)"
fi

# ---------------------------------------------------------------------------
# Resumen
# ---------------------------------------------------------------------------
echo ""
echo "============================================================"
echo " RESUMEN"
echo "============================================================"
for r in "${RESULTS[@]}"; do echo "$r"; done
echo "------------------------------------------------------------"
echo " Total: $((PASS+FAIL))   OK: $PASS   FAIL: $FAIL"
echo "============================================================"

[ "$FAIL" -eq 0 ] && exit 0 || exit 1
