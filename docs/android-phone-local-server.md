# Android Phone + Local Server

Debug builds default to the Android emulator backend URL:

```bash
http://10.0.2.2:8080
```

For a physical Android phone, build the debug APK with the Mac Wi-Fi IP:

```bash
MAC_IP=$(ipconfig getifaddr en1)

./gradlew :composeApp:assembleDebug \
  -PapiBaseUrl=http://$MAC_IP:8080 \
  --no-daemon

adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

Check that the phone can reach the backend:

```bash
adb shell curl -fsS http://$MAC_IP:8080/health
```

Expected:

```json
{"status":"UP"}
```

Cleartext HTTP is allowed only for debug builds via `composeApp/src/debug/res/xml/network_security_config.xml`. Main/release config does not use the debug-wide cleartext override.

## iPhone

For a physical iPhone, build with the same Mac Wi-Fi IP:

```bash
MAC_IP=$(ipconfig getifaddr en1)

DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -destination 'platform=iOS,id=<DEVICE_UDID>' \
  DEVELOPMENT_TEAM=<TEAM_ID> \
  API_BASE_URL=http://$MAC_IP:8080 \
  -allowProvisioningUpdates \
  build

DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
xcrun devicectl device install app \
  --device <COREDEVICE_ID> \
  ~/Library/Developer/Xcode/DerivedData/iosApp-*/Build/Products/Debug-iphoneos/ProductInventory.app

DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
xcrun devicectl device process launch \
  --device <COREDEVICE_ID> \
  com.android.rut.miit.productinventory.ProductInventory
```

The iOS app reads `API_BASE_URL` from `Info.plist`. Default is `http://localhost:8080`, useful for simulator; physical iPhone builds should override it with the Mac LAN IP.

For a free/personal Apple team, Push Notifications cannot be provisioned. The local physical-device Debug build is configured without push entitlements so it can be signed and installed. If iOS blocks first launch, trust the developer profile on the phone: Settings > General > VPN & Device Management > Developer App.
