# Despliegue continuo del backend a servidor remoto — Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permitir desplegar manualmente (workflow_dispatch en GitHub Actions) cualquier versión ya publicada de `dipalza_server` a un servidor remoto: copia el jar versionado, detiene el servicio, mueve el symlink `current` a la nueva versión, lo reinicia, verifica que responde, y prunea versiones viejas.

**Architecture:** Un workflow de GitHub Actions (`workflow_dispatch`, sin disparo automático) descarga el jar de la GitHub Release indicada y lo copia por SCP a `/opt/dipalza-app/releases/<version>/dipalza.jar` en el servidor; luego invoca por SSH un script ya presente en el servidor (`scripts/deploy-remote.sh`) que hace el swap real (stop → symlink → start → health check → prune). La configuración inicial del servidor (usuario, llaves, sudoers, symlink inicial) es un prerrequisito manual documentado como runbook, no ejecutado por este plan.

**Tech Stack:** GitHub Actions (`workflow_dispatch`, `gh` CLI, `webfactory/ssh-agent`), Bash (script remoto + test harness sin framework), systemd (servicio remoto), Spring Boot Actuator (health check).

## Global Constraints

- Base de despliegue en el servidor: `/opt/dipalza-app`
- Symlink activo: `/opt/dipalza-app/current` → `/opt/dipalza-app/releases/<version>`
- Nombre del servicio systemd: `dipalza-app.service`
- Usuario SSH de despliegue: `deploy-dipalza`
- Endpoint de salud: `http://localhost:8080/actuator/health` (Actuator ya es dependencia del proyecto, sin config adicional)
- Retención: conservar solo las últimas 3 carpetas de `releases/`
- Trigger del workflow: `workflow_dispatch` únicamente, input requerido `version` (string) — sin disparo automático al publicar una release, sin gate de aprobación de GitHub Environments
- Secrets de GitHub requeridos: `DEPLOY_SSH_KEY`, `DEPLOY_SSH_HOST`, `DEPLOY_SSH_USER`
- Sin worktrees: todo el trabajo de este plan ocurre directo en el directorio del repo (`/Users/cursor/Dev/dipalza/application_v2.0/dipalza_server`), rama `feature/cd-deploy-backend` ya creada
- Swap de symlink: un solo `ln -sfn` (no `mv -T` — no existe en macOS/BSD, y no hace falta: el servicio ya está detenido cuando se hace el swap)

---

### Task 1: Runbook de configuración inicial del servidor

**Files:**
- Create: `docs/deploy/server-setup.md`

**Interfaces:**
- Produce: los nombres/rutas exactos que las Tasks 2 y 3 asumen como ya configurados en el servidor (usuario `deploy-dipalza`, base `/opt/dipalza-app`, servicio `dipalza-app.service`, script en `/opt/dipalza-app/scripts/deploy-remote.sh`).
- No consume nada de otras tasks.

Este runbook documenta pasos que el usuario ejecuta a mano en su servidor — no son ejecutados por CI ni por este plan. Es la única forma de que esta configuración quede registrada en el repo en vez de perderse.

- [ ] **Step 1: Escribir el runbook completo**

Crear `docs/deploy/server-setup.md` con este contenido exacto:

```markdown
# Configuración inicial del servidor de despliegue

Pasos manuales de una sola vez, ejecutados como root (o con sudo) en el
servidor de producción, antes de que el workflow de deploy pueda usarse.

## 1. Crear el usuario dedicado para deploys

\`\`\`bash
sudo useradd -r -m -s /bin/bash deploy-dipalza
\`\`\`

## 2. Generar el par de llaves SSH (en tu máquina local, no en el servidor)

\`\`\`bash
ssh-keygen -t ed25519 -f ./deploy_dipalza_key -N "" -C "github-actions-deploy"
\`\`\`

La llave privada (`deploy_dipalza_key`) se carga como GitHub Secret
`DEPLOY_SSH_KEY` (Settings → Secrets and variables → Actions). Nunca se
commitea al repo.

## 3. Autorizar la llave pública en el servidor

\`\`\`bash
sudo mkdir -p /home/deploy-dipalza/.ssh
sudo tee /home/deploy-dipalza/.ssh/authorized_keys < deploy_dipalza_key.pub
sudo chown -R deploy-dipalza:deploy-dipalza /home/deploy-dipalza/.ssh
sudo chmod 700 /home/deploy-dipalza/.ssh
sudo chmod 600 /home/deploy-dipalza/.ssh/authorized_keys
\`\`\`

## 4. Dar permiso sudo sin contraseña, solo para los comandos exactos necesarios

\`\`\`bash
sudo visudo -f /etc/sudoers.d/deploy-dipalza
\`\`\`

Contenido del archivo (nombres de comando completos y fijos — nunca un
patrón genérico como `systemctl *`, para que este usuario no pueda tocar
ningún otro servicio del sistema):

\`\`\`
deploy-dipalza ALL=(root) NOPASSWD: /usr/bin/systemctl stop dipalza-app.service, /usr/bin/systemctl start dipalza-app.service, /usr/bin/systemctl restart dipalza-app.service, /usr/bin/systemctl status dipalza-app.service
\`\`\`

Verificar la sintaxis antes de salir de `visudo` (lo hace automáticamente
al guardar; si reporta un error, no guarda el archivo).

## 5. Dar al usuario la propiedad de la carpeta de despliegue

\`\`\`bash
sudo mkdir -p /opt/dipalza-app/releases
sudo chown -R deploy-dipalza:deploy-dipalza /opt/dipalza-app
\`\`\`

## 6. Migrar el jar actualmente en producción a la nueva estructura

Sustituir `<version-actual>` por la versión que está corriendo hoy (ver
`dipalza/pom.xml` o la última GitHub Release):

\`\`\`bash
sudo -u deploy-dipalza mkdir -p /opt/dipalza-app/releases/<version-actual>
sudo -u deploy-dipalza cp /opt/dipalza-app/dipalza.jar /opt/dipalza-app/releases/<version-actual>/dipalza.jar
sudo -u deploy-dipalza ln -sfn /opt/dipalza-app/releases/<version-actual> /opt/dipalza-app/current
\`\`\`

## 7. Actualizar el `ExecStart` del `.service` para apuntar al symlink

Editar el unit file de `dipalza-app.service` (típicamente
`/etc/systemd/system/dipalza-app.service`) y cambiar la línea `ExecStart`
a:

\`\`\`ini
ExecStart=/usr/bin/java -jar /opt/dipalza-app/current/dipalza.jar
\`\`\`

Luego:

\`\`\`bash
sudo systemctl daemon-reload
sudo systemctl restart dipalza-app.service
sudo systemctl status dipalza-app.service
\`\`\`

Confirmar que el servicio sigue respondiendo (`curl http://localhost:8080/actuator/health`)
antes de continuar.

## 8. Copiar el script de deploy al servidor

Una vez completada la Task 2 de este plan (que crea `scripts/deploy-remote.sh`
en el repo), copiarlo al servidor:

\`\`\`bash
scp scripts/deploy-remote.sh deploy-dipalza@<host>:/opt/dipalza-app/scripts/deploy-remote.sh
ssh deploy-dipalza@<host> chmod +x /opt/dipalza-app/scripts/deploy-remote.sh
\`\`\`

Este script no viaja versionado en cada release — vive fijo en el
servidor. Si el script cambia en el repo, hay que repetir este paso a
mano para actualizarlo ahí.

## 9. Cargar los secrets en GitHub

Settings → Secrets and variables → Actions → New repository secret:

- `DEPLOY_SSH_KEY`: contenido completo de la llave privada generada en el paso 2
- `DEPLOY_SSH_HOST`: hostname o IP del servidor
- `DEPLOY_SSH_USER`: `deploy-dipalza`

## 10. Rollback manual (si un deploy queda en mal estado)

El pipeline no hace rollback automático. Las versiones viejas quedan en
`/opt/dipalza-app/releases/` (las últimas 3). Para volver a la anterior:

\`\`\`bash
ssh deploy-dipalza@<host> "ln -sfn /opt/dipalza-app/releases/<version-anterior> /opt/dipalza-app/current"
ssh deploy-dipalza@<host> "sudo systemctl restart dipalza-app.service"
\`\`\`
```

- [ ] **Step 2: Verificar que el runbook cubre todos los prerrequisitos del spec**

Run: `grep -c "^## " docs/deploy/server-setup.md`
Expected: `10` (10 secciones numeradas: usuario, llaves, authorized_keys, sudoers, chown, migración inicial, ExecStart, copiar script, secrets de GitHub, rollback manual)

- [ ] **Step 3: Commit**

```bash
git add docs/deploy/server-setup.md
git commit -m "docs: add server setup runbook for backend deploy pipeline"
```

---

### Task 2: Script de deploy remoto (`scripts/deploy-remote.sh`)

**Files:**
- Create: `scripts/deploy-remote.sh`
- Test: `scripts/deploy-remote.test.sh`

**Interfaces:**
- Consume: ninguna de otra task (usa directamente los nombres fijados en Global Constraints: `dipalza-app.service`, `/opt/dipalza-app`, `http://localhost:8080/actuator/health`).
- Produce: `scripts/deploy-remote.sh <version>` — recibe la versión como único argumento posicional; usa `DEPLOY_BASE`, `DEPLOY_SERVICE`, `DEPLOY_HEALTH_URL`, `DEPLOY_HEALTH_RETRIES` como overrides opcionales por variable de entorno (default a los valores de Global Constraints) para permitir testearlo sin tocar el sistema real. Termina con `exit 0` si el deploy y el health check fueron exitosos, `exit 1` en cualquier otro caso. Task 3 lo invoca vía SSH como `/opt/dipalza-app/scripts/deploy-remote.sh <version>` (sin overrides, usa los defaults reales).

No hay framework de testing de bash instalado en este repo (es un proyecto Maven/Java) — el test harness de este Task no depende de ninguno; son funciones bash planas con aserciones caseras, ejecutables con `bash scripts/deploy-remote.test.sh`.

- [ ] **Step 1: Escribir el test harness (falla porque `deploy-remote.sh` no existe aún)**

Crear `scripts/deploy-remote.test.sh`:

```bash
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
```

- [ ] **Step 2: Correr el test para verificar que falla**

Run: `chmod +x scripts/deploy-remote.test.sh && bash scripts/deploy-remote.test.sh`
Expected: FAIL — error indicando que `scripts/deploy-remote.sh` no existe (`No such file or directory`), exit code distinto de 0.

- [ ] **Step 3: Escribir `scripts/deploy-remote.sh`**

```bash
#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?Uso: deploy-remote.sh <version>}"
BASE="${DEPLOY_BASE:-/opt/dipalza-app}"
SERVICE="${DEPLOY_SERVICE:-dipalza-app.service}"
HEALTH_URL="${DEPLOY_HEALTH_URL:-http://localhost:8080/actuator/health}"
HEALTH_RETRIES="${DEPLOY_HEALTH_RETRIES:-15}"
KEEP_RELEASES=3

RELEASE_DIR="$BASE/releases/$VERSION"
LIVE_LINK="$BASE/current"

if [ ! -f "$RELEASE_DIR/dipalza.jar" ]; then
  echo "ERROR: no existe $RELEASE_DIR/dipalza.jar" >&2
  exit 1
fi

sudo systemctl stop "$SERVICE"

# El servicio ya está detenido acá, así que no hace falta un truco de
# symlink temporal + mv para atomicidad frente a tráfico en vivo. `-n`
# evita que, si 'current' ya es un symlink a un directorio, ln siga el
# enlace y cree el nuevo symlink DENTRO de él en vez de reemplazarlo.
ln -sfn "$RELEASE_DIR" "$LIVE_LINK"

sudo systemctl start "$SERVICE"

healthy=0
for i in $(seq 1 "$HEALTH_RETRIES"); do
  if curl -sf "$HEALTH_URL" > /dev/null; then
    healthy=1
    break
  fi
  sleep 1
done

if [ "$healthy" -ne 1 ]; then
  echo "ERROR: el servicio no respondió en $HEALTH_URL tras el deploy." >&2
  exit 1
fi

echo "Servicio arriba y respondiendo en $HEALTH_URL."

cd "$BASE/releases"
ls -1dt */ | tail -n +$((KEEP_RELEASES + 1)) | xargs -r rm -rf --

echo "Deploy de la versión $VERSION completado."
```

- [ ] **Step 4: Dar permiso de ejecución**

Run: `chmod +x scripts/deploy-remote.sh`

- [ ] **Step 5: Correr el test para verificar que pasa**

Run: `bash scripts/deploy-remote.test.sh`
Expected:
```
PASS: symlink apunta a la versión desplegada
PASS: systemctl se llama stop y luego start, en ese orden
PASS: el script sale con código 1 si el health check nunca responde
PASS: solo quedan las últimas 3 versiones tras el deploy
PASS: el script falla si releases/<version>/dipalza.jar no existe
PASS: no llama a systemctl si el jar no existe
Todas las pruebas pasaron.
```

- [ ] **Step 6: Commit**

```bash
git add scripts/deploy-remote.sh scripts/deploy-remote.test.sh
git commit -m "feat: add remote deploy script with symlink swap, health check and retention"
```

---

### Task 3: Workflow de GitHub Actions (`deploy.yml`)

**Files:**
- Create: `.github/workflows/deploy.yml`

**Interfaces:**
- Consume: `scripts/deploy-remote.sh <version>` (Task 2) — lo invoca por SSH exactamente así, sin argumentos extra, asumiendo que ya está copiado en `/opt/dipalza-app/scripts/deploy-remote.sh` en el servidor (Task 1, paso 8, prerrequisito manual).
- Consume: los secrets `DEPLOY_SSH_KEY`, `DEPLOY_SSH_HOST`, `DEPLOY_SSH_USER` (configurados a mano en GitHub, Task 1 paso 9).
- No produce nada que otra task consuma.

No existe forma de "correr" un workflow de GitHub Actions localmente sin disparar un deploy real — la verificación de este Task se limita a validar que el YAML es sintácticamente correcto. La prueba end-to-end real (con secrets y servidor reales) queda fuera del alcance de este plan — el usuario la hace la primera vez que dispare el workflow manualmente.

- [ ] **Step 1: Escribir el workflow**

Crear `.github/workflows/deploy.yml`:

```yaml
name: Deploy

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Versión (tag de la GitHub Release) a desplegar, ej. 1.2.4'
        required: true
        type: string

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Verificar que la release existe y descargar el jar
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          gh release download "${{ inputs.version }}" \
            --repo "${{ github.repository }}" \
            --pattern "dipalza-*.jar" \
            --dir /tmp/deploy-artifact \
            --clobber

      - name: Configurar agente SSH
        uses: webfactory/ssh-agent@v0.9.0
        with:
          ssh-private-key: ${{ secrets.DEPLOY_SSH_KEY }}

      - name: Agregar el host a known_hosts
        run: |
          mkdir -p ~/.ssh
          ssh-keyscan -H "${{ secrets.DEPLOY_SSH_HOST }}" >> ~/.ssh/known_hosts

      - name: Crear carpeta de la versión en el servidor
        run: |
          ssh "${{ secrets.DEPLOY_SSH_USER }}@${{ secrets.DEPLOY_SSH_HOST }}" \
            "mkdir -p /opt/dipalza-app/releases/${{ inputs.version }}"

      - name: Copiar el jar al servidor
        run: |
          JAR_FILE=$(ls /tmp/deploy-artifact/dipalza-*.jar)
          scp "$JAR_FILE" \
            "${{ secrets.DEPLOY_SSH_USER }}@${{ secrets.DEPLOY_SSH_HOST }}:/opt/dipalza-app/releases/${{ inputs.version }}/dipalza.jar"

      - name: Ejecutar el deploy remoto
        run: |
          ssh "${{ secrets.DEPLOY_SSH_USER }}@${{ secrets.DEPLOY_SSH_HOST }}" \
            "/opt/dipalza-app/scripts/deploy-remote.sh ${{ inputs.version }}"
```

- [ ] **Step 2: Verificar que el YAML es sintácticamente válido**

Run: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/deploy.yml')); print('YAML válido')"`
Expected: `YAML válido`

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/deploy.yml
git commit -m "feat: add manual deploy workflow (workflow_dispatch)"
```

---

## Después de este plan (fuera de su alcance, a cargo del usuario)

1. Ejecutar el runbook (`docs/deploy/server-setup.md`) en el servidor real — es manual, no automatizable desde este repo.
2. Correr el workflow por primera vez (`gh workflow run deploy.yml -f version=<version-actual>`) para confirmar el flujo end-to-end contra el servidor real.
