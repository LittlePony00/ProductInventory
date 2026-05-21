# Recommendations v2 Android QA

This runbook reproduces the Android emulator flow for Recommendations v2 with local backend data and optional real GigaChat generation.

## Backend

The Android debug build uses `10.0.2.2` to reach the host machine from the emulator. Run backend on `8081` so it does not conflict with any already-running local service on `8080`.

```bash
POSTGRES_JDBC_URL=jdbc:postgresql://localhost:5432/product_inventory \
POSTGRES_USERNAME=postgres \
POSTGRES_PASSWORD=postgres \
JWT_SECRET=test-secret-key-for-jwt-that-is-at-least-256-bits-long-for-hmac-sha256 \
SPRING_PROFILES_ACTIVE=prod \
./gradlew :server:bootRun --args='--server.port=8081'
```

## Real GigaChat

The GigaChat OAuth and chat endpoints currently use a Russian CA chain that may not be present in the default JVM truststore. If backend logs show `PKIX path building failed` for `ngw.devices.sberbank.ru`, create a temporary truststore:

```bash
server/scripts/create-gigachat-truststore.sh /tmp/productinventory-gigachat-truststore.jks
```

Then run backend with the truststore and server-side API key:

```bash
POSTGRES_JDBC_URL=jdbc:postgresql://localhost:5432/product_inventory \
POSTGRES_USERNAME=postgres \
POSTGRES_PASSWORD=postgres \
JWT_SECRET=test-secret-key-for-jwt-that-is-at-least-256-bits-long-for-hmac-sha256 \
SPRING_PROFILES_ACTIVE=prod \
GIGACHAT_API_KEY='<server-side-key>' \
JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStore=/tmp/productinventory-gigachat-truststore.jks -Djavax.net.ssl.trustStorePassword=changeit' \
./gradlew :server:bootRun --args='--server.port=8081'
```

Production should install the same CA chain through deployment/runtime configuration rather than relying on a developer-local `/tmp` truststore.

## Android Build And Launch

```bash
./gradlew :composeApp:assembleDebug -PapiBaseUrl=http://10.0.2.2:8081
android emulator start Pixel_9_Pro_Fold_API_35
android run --device emulator-5554 --type ACTIVITY --activity .MainActivity --apks composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

Use `android layout --device emulator-5554 -p` to verify UI state. The Android CLI does not provide tap/text input, so use `adb shell input tap` and `adb shell input text` for manual interaction.

## Flow Checklist

- Register or log in.
- Create or open a household.
- Add current products, for example `rice` and `vegetables`.
- Set one product close to expiration, for example `2026-05-20`.
- Open `Рецепты`.
- Tap `Подобрать из текущих продуктов`; verify recipes use current products and show reasons.
- Tap `Использовать скоро`; verify expiring products are prioritized.
- Open `Профиль`, edit food preferences, and add an allergy such as `fish`.
- Return to recipes; verify fish recipes disappear.
- Tap `AI-рецепт`; verify the disclaimer dialog appears.
- Confirm generation.
- With real GigaChat configured, verify the returned card has an `AI` badge and warning text.
- If GigaChat returns unknown calories, verify the UI shows `ккал неизвестно` rather than `0 kcal`.

## Cleanup

```bash
android emulator stop Pixel_9_Pro_Fold_API_35
```

Stop backend with `Ctrl+C` or by terminating the `bootRun` process. A Gradle `bootRun` exit code `143` means it was intentionally stopped.
