#!/usr/bin/env bash
# Test harness para scripts/rollback-remote.sh — mismo enfoque que
# deploy-remote.test.sh: sin framework, stubea systemctl/sudo/curl vía
# PATH, apunta DEPLOY_BASE a un directorio temporal.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROLLBACK_SCRIPT="$SCRIPT_DIR/rollback-remote.sh"
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
  local base="$1" version_with_v="$2"
  mkdir -p "$base/releases/$version_with_v"
  echo "fake jar $version_with_v" > "$base/releases/$version_with_v/dipalza.jar"
}

test_bare_version_prepends_v_and_swaps() {
  local tmp_root; tmp_root="$(setup_fake_env)"
  local base="$tmp_root/base" bin="$tmp_root/bin" log="$tmp_root/systemctl.log"
  mkdir -p "$base/releases" "$bin"

  make_fake_systemctl "$bin" "$log"
  make_fake_sudo "$bin"
  make_fake_curl "$bin" 0
  make_release "$base" "v1.2.3"

  PATH="$bin:$PATH" DEPLOY_BASE="$base" DEPLOY_SERVICE="test.service" \
    DEPLOY_HEALTH_URL="http://fake/health" "$ROLLBACK_SCRIPT" "1.2.3"

  local link_target
  link_target="$(readlink "$base/current")"
  assert_eq "$base/releases/v1.2.3" "$link_target" "el rollback antepone 'v' y apunta el symlink a esa carpeta"

  assert_eq "$(printf 'stop test.service\nstart test.service')" "$(cat "$log")" \
    "systemctl se llama stop y luego start, en ese orden"

  rm -rf "$tmp_root"
}

test_rejects_v_prefixed_input() {
  local tmp_root; tmp_root="$(setup_fake_env)"
  local base="$tmp_root/base" bin="$tmp_root/bin" log="$tmp_root/systemctl.log"
  mkdir -p "$base/releases" "$bin"

  make_fake_systemctl "$bin" "$log"
  make_fake_sudo "$bin"
  make_fake_curl "$bin" 0
  make_release "$base" "v1.2.3"

  set +e
  PATH="$bin:$PATH" DEPLOY_BASE="$base" DEPLOY_SERVICE="test.service" \
    DEPLOY_HEALTH_URL="http://fake/health" "$ROLLBACK_SCRIPT" "v1.2.3"
  local exit_code=$?
  set -e

  assert_eq "1" "$exit_code" "rechaza un input que ya trae 'v' (se espera el número pelado)"
  assert_eq "0" "$(wc -l < "$log" | tr -d ' ')" "no llama a systemctl si el formato del input es inválido"

  rm -rf "$tmp_root"
}

test_rejects_shell_metacharacters() {
  local tmp_root; tmp_root="$(setup_fake_env)"
  local base="$tmp_root/base" bin="$tmp_root/bin" log="$tmp_root/systemctl.log"
  mkdir -p "$base/releases" "$bin"

  make_fake_systemctl "$bin" "$log"
  make_fake_sudo "$bin"
  make_fake_curl "$bin" 0

  set +e
  PATH="$bin:$PATH" DEPLOY_BASE="$base" DEPLOY_SERVICE="test.service" \
    DEPLOY_HEALTH_URL="http://fake/health" "$ROLLBACK_SCRIPT" "1.2.3; rm -rf /"
  local exit_code=$?
  set -e

  assert_eq "1" "$exit_code" "rechaza un input con metacaracteres de shell"
  assert_eq "0" "$(wc -l < "$log" | tr -d ' ')" "no llama a systemctl si el input es inválido"

  rm -rf "$tmp_root"
}

test_missing_release_dir() {
  local tmp_root; tmp_root="$(setup_fake_env)"
  local base="$tmp_root/base" bin="$tmp_root/bin" log="$tmp_root/systemctl.log"
  mkdir -p "$base/releases" "$bin"

  make_fake_systemctl "$bin" "$log"
  make_fake_sudo "$bin"
  make_fake_curl "$bin" 0

  set +e
  PATH="$bin:$PATH" DEPLOY_BASE="$base" DEPLOY_SERVICE="test.service" \
    DEPLOY_HEALTH_URL="http://fake/health" "$ROLLBACK_SCRIPT" "9.9.9"
  local exit_code=$?
  set -e

  assert_eq "1" "$exit_code" "falla si releases/v<version>/dipalza.jar no existe"
  assert_eq "0" "$(wc -l < "$log" | tr -d ' ')" "no llama a systemctl si la versión no existe en el servidor"

  rm -rf "$tmp_root"
}

test_health_check_failure_exits_nonzero() {
  local tmp_root; tmp_root="$(setup_fake_env)"
  local base="$tmp_root/base" bin="$tmp_root/bin" log="$tmp_root/systemctl.log"
  mkdir -p "$base/releases" "$bin"

  make_fake_systemctl "$bin" "$log"
  make_fake_sudo "$bin"
  make_fake_curl "$bin" 1
  make_release "$base" "v1.2.3"

  set +e
  PATH="$bin:$PATH" DEPLOY_BASE="$base" DEPLOY_SERVICE="test.service" \
    DEPLOY_HEALTH_URL="http://fake/health" DEPLOY_HEALTH_RETRIES=2 \
    "$ROLLBACK_SCRIPT" "1.2.3"
  local exit_code=$?
  set -e

  assert_eq "1" "$exit_code" "sale con código 1 si el health check nunca responde tras el rollback"

  rm -rf "$tmp_root"
}

test_bare_version_prepends_v_and_swaps
test_rejects_v_prefixed_input
test_rejects_shell_metacharacters
test_missing_release_dir
test_health_check_failure_exits_nonzero

if [ "$FAILURES" -gt 0 ]; then
  echo "$FAILURES prueba(s) fallaron." >&2
  exit 1
fi
echo "Todas las pruebas pasaron."
