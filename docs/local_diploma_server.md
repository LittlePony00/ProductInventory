# Local Diploma Server

For diploma/demo runs, the backend can run directly on the development Mac.
Mobile devices can access it when they are on the same Wi-Fi/VPN network.

Start backend, PostgreSQL, and MinIO for VPN devices:

```bash
./scripts/start_local_diploma_server.sh
```

The scripts prefer a `utun*` VPN IP by default. On the current machine this has
been `10.8.0.2`. If devices should connect through Wi-Fi/LAN instead, run:

```bash
HOST_NETWORK=lan ./scripts/start_local_diploma_server.sh
```

You can always override the address explicitly:

```bash
HOST_IP=10.8.0.2 ./scripts/start_local_diploma_server.sh
```

Run on a non-default API port when another backend is already using `8080`:

```bash
SERVER_PORT=8082 ./scripts/start_local_diploma_server.sh
```

Enable live GigaChat recipe generation by setting `GIGACHAT_API_KEY` before
starting the backend. The start script creates a local truststore for the
GigaChat certificate chain and passes it to the JVM automatically.

```bash
GIGACHAT_API_KEY=<secret> ./scripts/start_local_diploma_server.sh
```

To validate GigaChat without starting the full backend, run the live check:

```bash
GIGACHAT_API_KEY=<secret> server/scripts/check-gigachat-live.sh
```

The live check runs only the opt-in GigaChat integration test and does not
commit or print the key. You can override the truststore path/password with
`GIGACHAT_TRUSTSTORE_PATH` and `GIGACHAT_TRUSTSTORE_PASSWORD`.

Install Android app pointed at that backend:

```bash
./scripts/install_android_for_local_server.sh
```

The Android install script checks for an attached adb device before starting
Gradle. If needed, pass the same network override:

```bash
HOST_NETWORK=lan ./scripts/install_android_for_local_server.sh
```

Runtime endpoints:

- API: `http://<HOST_IP>:<SERVER_PORT>`
- Health check: `http://<HOST_IP>:<SERVER_PORT>/health`
- Product images: `http://<HOST_IP>:9000/product-images`
- MinIO console: `http://localhost:9001`

MinIO local credentials:

- Login: `productinventory`
- Password: `productinventory123`

Keep the server terminal open while demonstrating. Stop the backend with
`Ctrl+C`. The wrapper treats the usual `bootRun` `143`/`130` stop codes as a
normal shutdown. Docker containers can stay stopped/started by the script as
needed. Newly created Postgres and MinIO containers use named Docker volumes so
demo data survives container recreation.
