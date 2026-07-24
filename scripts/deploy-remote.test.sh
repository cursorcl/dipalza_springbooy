#!/usr/bin/env bash
# Test harness para scripts/deploy-remote.sh — sin dependencia de framework,
# stubea systemctl/sudo/curl vía PATH y apunta DEPLOY_BASE a un directorio
# temporal, para no requerir systemctl real ni una ruta /opt/dipalza-app real.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_SCRIPT="$SCRIPT_DIR/deploy-remote.sh"
FAILURES=0

assert_eq() {
  local expected="$1" actual="$2" msg="$3"
  if [ "$expected" != "$actual" ]; then
    echo "FAIL: $msg (esperado: '$expected', obtenido: '$actual')" >&2
    FAILURES=$((FAILURES + 1))
  else
    echo "PASS: $msg"
  fi
}

setup_fake_env() {
  mktemp -d
}

make_fake_systemctl() {
  local bindir="$1" logfile="$2"
  # Crea el logfile vacío desde ya: si el script bajo prueba nunca llega a
  # invocar systemctl (ej. porque falla antes por jar faltante), `wc -l`
  # sobre un archivo inexistente abortaría el harness entero por `set -e`.
  touch "$logfile"
  cat > "$bindir/systemctl" <<EOF
#!/usr/bin/env bash
echo "\$@" >> "$logfile"
exit 0
EOF
  chmod +x "$bindir/systemctl"
}

make_fake_sudo() {
  local bindir="$1"
  cat > "$bindir/sudo" <<'EOF'
#!/usr/bin/env bash
exec "$@"
EOF
  chmod +x "$bindir/sudo"
}

make_fake_curl() {
  local bindir="$1" exit_code="$2"
  cat > "$bindir/curl" <<EOF
#!/usr/bin/env bash
exit $exit_code
EOF
  chmod +x "$bindir/curl"
}

make_release() {
  local base="$1" version="$2" mtime="$3"
  mkdir -p "$base/releases/$version"
  echo "fake jar $version" > "$base/releases/$version/dipalza.jar"
  touch -t "$mtime" "$base/releases/$version"
}

test_symlink_swap_and_success() {
  local tmp_root; tmp_root="$(setup_fake_env)"
  local base="$tmp_root/base" bin="$tmp_root/bin" log="$tmp_root/systemctl.log"
  mkdir -p "$base/releases" "$bin"

  make_fake_systemctl "$bin" "$log"
  make_fake_sudo "$bin"
  make_fake_curl "$bin" 0
  make_release "$base" "1.0.0" "202601010000"

  PATH="$bin:$PATH" DEPLOY_BASE="$base" DEPLOY_SERVICE="test.service" \
    DEPLOY_HEALTH_URL="http://fake/health" "$DEPLOY_SCRIPT" "1.0.0"

  local link_target
  link_target="$(readlink "$base/current")"
  assert_eq "$base/releases/1.0.0" "$link_target" "symlink apunta a la versión desplegada"

  assert_eq "$(printf 'stop test.service\nstart test.service')" "$(cat "$log")" \
    "systemctl se llama stop y luego start, en ese orden"

  rm -rf "$tmp_root"
}

test_health_check_failure_exits_nonzero() {
  local tmp_root; tmp_root="$(setup_fake_env)"
  local base="$tmp_root/base" bin="$tmp_root/bin" log="$tmp_root/systemctl.log"
  mkdir -p "$base/releases" "$bin"

  make_fake_systemctl "$bin" "$log"
  make_fake_sudo "$bin"
  make_fake_curl "$bin" 1
  make_release "$base" "1.0.0" "202601010000"

  set +e
  PATH="$bin:$PATH" DEPLOY_BASE="$base" DEPLOY_SERVICE="test.service" \
    DEPLOY_HEALTH_URL="http://fake/health" DEPLOY_HEALTH_RETRIES=2 \
    "$DEPLOY_SCRIPT" "1.0.0"
  local exit_code=$?
  set -e

  assert_eq "1" "$exit_code" "el script sale con código 1 si el health check nunca responde"

  rm -rf "$tmp_root"
}

test_retention_keeps_last_three() {
  local tmp_root; tmp_root="$(setup_fake_env)"
  local base="$tmp_root/base" bin="$tmp_root/bin" log="$tmp_root/systemctl.log"
  mkdir -p "$base/releases" "$bin"

  make_fake_systemctl "$bin" "$log"
  make_fake_sudo "$bin"
  make_fake_curl "$bin" 0

  make_release "$base" "1.0.0" "202601010000"
  make_release "$base" "1.0.1" "202601020000"
  make_release "$base" "1.0.2" "202601030000"
  make_release "$base" "1.0.3" "202601040000"
  make_release "$base" "1.0.4" "202601050000"

  PATH="$bin:$PATH" DEPLOY_BASE="$base" DEPLOY_SERVICE="test.service" \
    DEPLOY_HEALTH_URL="http://fake/health" "$DEPLOY_SCRIPT" "1.0.4"

  local remaining
  remaining="$(cd "$base/releases" && ls -1d */ | sed 's#/##' | sort | tr '\n' ' ' | sed 's/ $//')"
  assert_eq "1.0.2 1.0.3 1.0.4" "$remaining" "solo quedan las últimas 3 versiones tras el deploy"

  rm -rf "$tmp_root"
}

test_missing_release_dir_exits_nonzero() {
  local tmp_root; tmp_root="$(setup_fake_env)"
  local base="$tmp_root/base" bin="$tmp_root/bin" log="$tmp_root/systemctl.log"
  mkdir -p "$base/releases" "$bin"

  make_fake_systemctl "$bin" "$log"
  make_fake_sudo "$bin"
  make_fake_curl "$bin" 0

  set +e
  PATH="$bin:$PATH" DEPLOY_BASE="$base" DEPLOY_SERVICE="test.service" \
    DEPLOY_HEALTH_URL="http://fake/health" "$DEPLOY_SCRIPT" "9.9.9"
  local exit_code=$?
  set -e

  assert_eq "1" "$exit_code" "el script falla si releases/<version>/dipalza.jar no existe"
  assert_eq "0" "$(wc -l < "$log" | tr -d ' ')" "no llama a systemctl si el jar no existe"

  rm -rf "$tmp_root"
}

test_symlink_swap_and_success
test_health_check_failure_exits_nonzero
test_retention_keeps_last_three
test_missing_release_dir_exits_nonzero

if [ "$FAILURES" -gt 0 ]; then
  echo "$FAILURES prueba(s) fallaron." >&2
  exit 1
fi
echo "Todas las pruebas pasaron."
