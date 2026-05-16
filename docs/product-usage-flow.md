# Product Usage Flow Goal

Current date: 2026-05-15

## Locked Requirement

Product usage flow:

1. Command: `ConsumeProduct`.
2. Event: `ProductQuantityChanged`.
3. If quantity becomes `0`, event: `ProductDepleted`.
4. Optionally, event: `RecommendationGenerated`.

Expected behavior:

- A user can fully or partially consume a product.
- Product quantity is updated correctly.
- If quantity reaches zero, the product is treated as depleted.
- The system may generate recommendations based on remaining products.

## Implementation Boundaries

- Backend command/API/service/event behavior lives in `server/`.
- Shared KMP product/recommendation client logic lives in `shared/`.
- Android Compose UI flow lives in `composeApp/`.
- iOS shell integration lives in `iosApp/` where existing shared APIs require UI parity.
- Android runtime verification must use `android` CLI.

## Evidence Checklist

- [x] Backend exposes `ConsumeProduct` API and request validation.
  - `ProductController.consumeProduct` handles `POST /api/v1/households/{householdId}/products/{productId}/consume`.
  - `ConsumeProductRequest.amount` uses `@Positive`.
  - Covered by `ProductControllerTest` and `ProductRequestValidationTest`.
- [x] Backend service decreases remaining amount for partial consumption.
  - `ProductServiceImpl.consumeProduct` saves `existing.remainingAmount - amount`.
  - Covered by `ProductServiceImplTest`: partial consume from `2.0` to `1.25`.
- [x] Backend service sets remaining amount to `0` for full consumption.
  - Covered by `ProductServiceImplTest`: full consume from `1.0` to `0.0`.
- [x] Backend publishes `PRODUCT_QUANTITY_CHANGED` on every successful consume.
  - `ProductServiceImpl.consumeProduct` publishes `HouseholdEventType.PRODUCT_QUANTITY_CHANGED`.
  - Covered by `ProductServiceImplTest`.
- [x] Backend publishes `PRODUCT_DEPLETED` when remaining amount reaches `0`.
  - `ProductServiceImpl.consumeProduct` publishes `HouseholdEventType.PRODUCT_DEPLETED` on transition from positive remaining amount to zero.
  - Covered by `ProductServiceImplTest`.
- [x] Backend rejects invalid consumption amounts.
  - Service rejects non-positive amounts and amounts above remaining amount.
  - Request validation rejects non-positive DTO amounts.
  - Covered by `ProductServiceImplTest` and `ProductRequestValidationTest`.
- [x] Shared repository/use case calls consume endpoint and updates local product state.
  - `ConsumeProductUseCase` delegates to `ProductRepository.consumeProduct`.
  - `ProductRemoteDataSource.consumeProduct` posts to `/api/v1/households/{householdId}/products/{productId}/consume`.
  - `ProductRepositoryImpl.consumeProduct` saves returned product to local cache.
  - Covered by `ProductRepositoryImplTest`.
- [x] Shared ViewModel exposes consume event and updates UI state without stale product quantity.
  - `ProductListEvent.OnConsumeProduct` routes through `ProductListViewModel.onConsumeProduct`.
  - Successful consume upserts the returned product without reloading the full list.
  - Covered by `ProductListViewModelTest`.
- [x] Android Compose UI lets user enter partial/full consume amount.
  - `ProductListScreen` shows consume dialog with amount input and validates `0 < amount <= remainingAmount`.
  - Covered by `PlatformScreenSmokeTest.productListConsumeDialogSubmitsPartialConsumption`.
  - Full depletion disables the consume action.
  - Covered by `PlatformScreenSmokeTest.productListFullConsumptionDisablesConsumeAction`.
- [x] Recommendation flow remains available based on remaining products.
  - `RecommendationServiceImpl.getRecipes` now filters out depleted products with `remainingAmount == 0`.
  - Covered by `RecommendationServiceImplTest`.
  - Controller/API smoke coverage verifies recipes after `ProductServiceImpl.consumeProduct(..., amount = remaining)` excludes the depleted product from response.
  - Covered by `RecommendationControllerConsumptionIntegrationTest`.
- [x] Tests/build run and results captured.
  - `./gradlew :server:test :shared:jvmTest :shared:testDebugUnitTest :composeApp:testDebugUnitTest :composeApp:assembleDebug`: PASS.
  - Targeted tests for product service/controller/request validation/recommendation/repository/viewmodel/realtime mapper: PASS.
- [x] Android CLI install/run/runtime verification captured.
  - `android emulator start Pixel_9_Pro_Fold_API_35`: PASS, device `emulator-5554`.
  - `android run --apks composeApp/build/outputs/apk/debug/composeApp-debug.apk --device emulator-5554`: PASS.
  - `SPRING_PROFILES_ACTIVE=prod POSTGRES_JDBC_URL=... POSTGRES_USERNAME=... POSTGRES_PASSWORD=... JWT_SECRET=... ./gradlew :server:bootRun`: PASS, PostgreSQL-backed server on `8080`.
  - `android layout --device emulator-5554 --pretty`: PASS through runtime flow:
    - registered a user;
    - created household `Home`;
    - added product `Milk` with quantity `2`;
    - partially consumed `0.75`, layout showed `Остаток: 1.25 / 2.00 шт`;
    - fully consumed remaining `1.25`, layout showed `Остаток: 0.00 / 2.00 шт`;
    - depleted product consume button was no longer focusable in the layout tree.
  - Caveat: installed `android` CLI version `0.7.15232955` rejects `--no-metric`, so CLI verification ran without that unsupported flag.
  - Replay script added: `scripts/android_product_usage_e2e.sh`.
- [ ] Pending host-dependent verification.
  - `./gradlew :shared:allTests`: blocked by local Xcode/CommandLineTools setup at `:shared:linkDebugTestIosSimulatorArm64`; `xcrun xcodebuild -version` fails in Gradle output.

## Work Log

- 2026-05-15: Goal started. Existing worktree already contains broad uncommitted changes; treat as user/session work and preserve.
- 2026-05-15: Audited existing consume flow. Added recommendation filtering for depleted products, consume request validation test, shared repository consume test, and Android product list consume smoke test.
