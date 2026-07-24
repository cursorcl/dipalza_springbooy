# Configuración inicial del servidor de despliegue

Pasos manuales de una sola vez, ejecutados como root (o con sudo) en el
servidor de producción, antes de que el workflow de deploy pueda usarse.

## 1. Crear el usuario dedicado para deploys

```bash
sudo useradd -r -m -s /bin/bash deploy-dipalza
```

## 2. Generar el par de llaves SSH (en tu máquina local, no en el servidor)

```bash
ssh-keygen -t ed25519 -f ./deploy_dipalza_key -N "" -C "github-actions-deploy"
```

La llave privada (`deploy_dipalza_key`) se carga como GitHub Secret
`DEPLOY_SSH_KEY` (Settings → Secrets and variables → Actions). Nunca se
commitea al repo.

## 3. Autorizar la llave pública en el servidor

```bash
sudo mkdir -p /home/deploy-dipalza/.ssh
sudo tee /home/deploy-dipalza/.ssh/authorized_keys < deploy_dipalza_key.pub
sudo chown -R deploy-dipalza:deploy-dipalza /home/deploy-dipalza/.ssh
sudo chmod 700 /home/deploy-dipalza/.ssh
sudo chmod 600 /home/deploy-dipalza/.ssh/authorized_keys
```

## 4. Dar permiso sudo sin contraseña, solo para los comandos exactos necesarios

```bash
sudo visudo -f /etc/sudoers.d/deploy-dipalza
```

Contenido del archivo (nombres de comando completos y fijos — nunca un
patrón genérico como `systemctl *`, para que este usuario no pueda tocar
ningún otro servicio del sistema):

```
deploy-dipalza ALL=(root) NOPASSWD: /usr/bin/systemctl stop dipalza-app.service, /usr/bin/systemctl start dipalza-app.service, /usr/bin/systemctl restart dipalza-app.service, /usr/bin/systemctl status dipalza-app.service
```

Verificar la sintaxis antes de salir de `visudo` (lo hace automáticamente
al guardar; si reporta un error, no guarda el archivo).

## 5. Dar al usuario la propiedad de la carpeta de despliegue

```bash
sudo mkdir -p /opt/dipalza-app/releases
sudo chown -R deploy-dipalza:deploy-dipalza /opt/dipalza-app
```

## 6. Migrar el jar actualmente en producción a la nueva estructura

Sustituir `<version-actual>` por el **tag de la última GitHub Release**
(con el prefijo `v`, ej. `v1.2.2`) — no por el valor de `<version>` en
`dipalza/pom.xml` (que no lleva `v`). El workflow de deploy siempre usa
el tag con `v`, así que usar aquí un valor sin `v` crearía una carpeta
`releases/1.2.2` que ningún deploy futuro (`releases/v1.2.x`) reconocería
como la misma línea de versiones.

```bash
sudo -u deploy-dipalza mkdir -p /opt/dipalza-app/releases/<version-actual>
sudo -u deploy-dipalza cp /opt/dipalza-app/dipalza.jar /opt/dipalza-app/releases/<version-actual>/dipalza.jar
sudo -u deploy-dipalza ln -sfn /opt/dipalza-app/releases/<version-actual> /opt/dipalza-app/current
```

## 7. Actualizar el `ExecStart` del `.service` para apuntar al symlink

**Antes que nada, revisar si existe un drop-in override** — tiene prioridad
sobre el archivo principal, así que si existe y no se actualiza ahí
también, el cambio de abajo no tiene ningún efecto (esto pasó la primera
vez: el `.service` principal quedó bien pero el servicio siguió
arrancando con la ruta vieja porque un `override.conf` la redefinía):

```bash
sudo systemctl status dipalza-app.service | grep -i "drop-in" -A1
cat /etc/systemd/system/dipalza-app.service.d/override.conf 2>/dev/null
```

Si existe un `override.conf` con su propia línea `ExecStart=`, editar
**ese** archivo (no solo el `.service` principal), preservando cualquier
flag de JVM que ya tenga (ej. `-Xms`/`-Xmx`):

```bash
sudo tee /etc/systemd/system/dipalza-app.service.d/override.conf <<'EOF'
[Service]
ExecStart=
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /opt/dipalza-app/current/dipalza.jar
EOF
```

(La línea vacía `ExecStart=` antes de la real es necesaria — así es como
systemd permite que un drop-in reemplace, en vez de acumular, el
`ExecStart` del archivo base.)

Si no hay drop-in, editar directamente el unit file de
`dipalza-app.service` (típicamente `/etc/systemd/system/dipalza-app.service`)
y cambiar la línea `ExecStart` a:

```ini
ExecStart=/usr/bin/java -jar /opt/dipalza-app/current/dipalza.jar
```

Luego, en cualquiera de los dos casos:

```bash
sudo systemctl daemon-reload
sudo systemctl restart dipalza-app.service
sudo systemctl status dipalza-app.service
```

Confirmar que el servicio sigue respondiendo (`curl http://localhost:8080/actuator/health`)
antes de continuar.

## 8. Copiar los scripts de deploy y rollback al servidor

Una vez completadas las tareas de este plan que crean `scripts/deploy-remote.sh`
y `scripts/rollback-remote.sh` en el repo, copiarlos al servidor. Si tu SSH
atiende en un puerto no estándar, agrega `-P <puerto>` a `scp` y `-p <puerto>`
a `ssh`:

```bash
scp -P <puerto> scripts/deploy-remote.sh scripts/rollback-remote.sh \
  deploy-dipalza@<host>:/opt/dipalza-app/scripts/
ssh -p <puerto> deploy-dipalza@<host> \
  "chmod +x /opt/dipalza-app/scripts/deploy-remote.sh /opt/dipalza-app/scripts/rollback-remote.sh"
```

Ninguno de los dos scripts viaja versionado en cada release — viven fijos
en el servidor. Si cambian en el repo, hay que repetir este paso a mano
para actualizarlos ahí.

## 9. Cargar los secrets en GitHub

Settings → Secrets and variables → Actions → New repository secret:

- `DEPLOY_SSH_KEY`: contenido completo de la llave privada generada en el paso 2
- `DEPLOY_SSH_HOST`: hostname o IP del servidor
- `DEPLOY_SSH_USER`: `deploy-dipalza`
- `DEPLOY_SSH_PORT`: puerto SSH del servidor, solo si no es el 22 por defecto (si no se carga este secret, el workflow usa 22)

## 10. Rollback manual (si un deploy queda en mal estado)

El pipeline no hace rollback automático. Las versiones viejas quedan en
`/opt/dipalza-app/releases/` (las últimas 3). Para volver a una anterior,
usar `rollback-remote.sh` directamente en el servidor — a diferencia de
`deploy-remote.sh`, este recibe el número de versión **sin** el prefijo
`v` (más rápido de teclear a mano en una emergencia; el script antepone
la `v` internamente para encontrar la carpeta real):

```bash
ssh -p <puerto> deploy-dipalza@<host> "/opt/dipalza-app/scripts/rollback-remote.sh 1.2.3"
```
