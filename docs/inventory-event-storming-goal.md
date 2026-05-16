# Inventory Event Storming Goal

Current date: 2026-05-15

## Locked Requirement

Inventory management flow:

1. Command: `AddProduct`.
2. Event: `ProductAdded`.
3. Next step: check expiration date.
4. If product is close to expiration, emit `ExpirationDateApproaching`.
5. Then emit or schedule `NotificationSent`.

Expected behavior:

- User can add product manually.
- User can add product via barcode.
- Product is saved in active household inventory.
- System checks expiration date after add.
- If expiration date is close, user receives a notification or one is scheduled.

## Implementation Notes

- Preserve existing module boundaries:
  - Backend: `server/`.
  - Shared KMP/mobile logic: `shared/`.
  - Android Compose UI: `composeApp/`.
  - iOS integration: `iosApp/`.
- Prefer existing event, notification, product, and household abstractions.
- Verify with backend/shared tests and Android CLI/Gradle where available.

## Evidence Checklist

- [x] Backend add-product path persists product in household inventory.
  - `ProductServiceImpl.addProduct` saves `Product(householdId = householdId, addedByUserId = userId)`.
  - Covered by `ProductServiceImplTest`.
- [x] Backend emits or records `ProductAdded`.
  - Implemented as `HouseholdEventType.PRODUCT_CREATED` in `ProductServiceImpl.addProduct`.
  - Covered by `ProductServiceImplTest`.
- [x] Backend checks expiration date on add.
  - `ProductServiceImpl.addProduct` calls `publishStateEvents`.
  - `Product.isExpiringSoon()` uses `ExpirationDate.status == EXPIRING_SOON`.
- [x] Backend emits or records `ExpirationDateApproaching` for close dates.
  - Implemented as `HouseholdEventType.EXPIRING_SOON`.
  - Covered by `ProductServiceImplTest`.
- [x] Backend emits/records/schedules `NotificationSent`.
  - Product add intentionally does not emit `NotificationSent` synchronously.
  - Scheduled path: `ReminderScheduler` calls `ReminderServiceImpl.runDueReminders`.
  - `ReminderServiceImpl` creates `Notification(type = REMINDER_EXPIRING_SOON)` and sends push when enabled.
  - Covered by `ReminderServiceImplTest`.
- [x] Manual mobile add flow calls product add API with active household id.
  - `AddProductViewModel` calls `AddProductUseCase(householdId = householdId, ...)`.
  - `ProductRemoteDataSource.addProduct` posts to `/api/v1/households/{householdId}/products`.
- [x] Barcode mobile add flow can resolve or prefill product and add to household.
  - Android/iOS barcode scan screens navigate to add product with draft data.
  - `BarcodeServiceImpl.lookupAndAddProduct` now routes barcode add through `IProductService.addProduct`, so membership, product events, expiration checks, and notification scheduling behavior stay centralized.
  - Covered by `BarcodeServiceImplTest`.
- [x] Tests cover close-expiration notification behavior.
  - `ProductServiceImplTest`: add publishes `PRODUCT_CREATED`, `INVENTORY_LOW`, `EXPIRING_SOON`.
  - `ReminderServiceImplTest`: expiring products create reminder notifications for household members and dedupe.
- [x] Tests/build run and results captured.
  - `./gradlew :server:test`: PASS.
  - `./gradlew :composeApp:assembleDebug :composeApp:testDebugUnitTest :shared:jvmTest :shared:testDebugUnitTest`: PASS.
  - Android CLI: `android emulator start Pixel_9_Pro_Fold_API_35`, `android run --apks composeApp/build/outputs/apk/debug/composeApp-debug.apk --device emulator-5554`, and `android layout --device emulator-5554 --pretty`: PASS.
- [ ] Pending host-dependent verification.
  - `./gradlew :shared:allTests`: blocked on this machine because iOS simulator linking requires full Xcode. Current `xcrun xcodebuild -version` fails because active developer directory is CommandLineTools, not Xcode.
