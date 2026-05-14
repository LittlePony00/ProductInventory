import SwiftUI
import Shared

struct BarcodeScannerScreen: View {
    let householdId: String

    @StateObject private var holder = SharedVMHolder<BarcodeScanState, BarcodeScanEvent, BarcodeScanAction, BarcodeScanViewModel>(
        viewModel: DIContainer.shared.barcodeScanViewModel(),
        initialState: BarcodeScanState.Scanning()
    )
    @StateObject private var camera = BarcodeCameraModel()
    @EnvironmentObject private var router: AppRouter

    var body: some View {
        content
            .task {
                holder.viewModel.householdId = householdId
                holder.start { [weak router, householdId] action in
                    guard let router = router else { return }
                    DispatchQueue.main.async {
                        switch action {
                        case is BarcodeScanAction.NavigateBack:
                            router.pop()
                        case is BarcodeScanAction.ProductAdded:
                            router.pop()
                        case let action as BarcodeScanAction.NavigateToManualEntry:
                            router.push(.addProductFromDraft(
                                householdId: householdId,
                                draft: ProductDraftInput(barcode: action.barcode)
                            ))
                        case let action as BarcodeScanAction.NavigateToDraftEntry:
                            router.push(.addProductFromDraft(
                                householdId: householdId,
                                draft: ProductDraftInput(draft: action.draft)
                            ))
                        default:
                            break
                        }
                    }
                }
                camera.requestAccessAndStart()
            }
            .onDisappear { camera.stop() }
            .onReceive(camera.$detectedCode.compactMap { $0 }) { code in
                holder.sendEvent(BarcodeScanEvent.OnBarcodeScanned(code: code))
            }
            .navigationTitle("Сканирование")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Вручную") { router.push(.addProduct(householdId: householdId)) }
                }
            }
    }

    @ViewBuilder
    private var content: some View {
        switch holder.state {
        case is BarcodeScanState.Scanning:
            scannerContent
        case is BarcodeScanState.Loading:
            ProgressView("Ищем продукт...")
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        case let state as BarcodeScanState.DraftFound:
            DraftFoundView(
                draft: state.draft,
                onUseDraft: { holder.sendEvent(BarcodeScanEvent.OnUseDraftClick(barcode: state.draft.barcode)) },
                onManualEntry: { holder.sendEvent(BarcodeScanEvent.OnDraftManualEntryClick(draft: state.draft)) },
                onRetry: {
                    camera.resetDetection()
                    holder.sendEvent(BarcodeScanEvent.OnRetry())
                }
            )
        case let state as BarcodeScanState.ProductFound:
            ProductFoundView(
                product: state.product,
                onScanNext: {
                    camera.resetDetection()
                    holder.sendEvent(BarcodeScanEvent.OnDismissProduct())
                }
            )
        case let state as BarcodeScanState.ManualEntry:
            ManualBarcodeView(
                barcode: state.barcode,
                onManualEntry: { router.push(.addProductFromDraft(
                    householdId: householdId,
                    draft: ProductDraftInput(barcode: state.barcode)
                )) },
                onRetry: {
                    camera.resetDetection()
                    holder.sendEvent(BarcodeScanEvent.OnRetry())
                }
            )
        case let state as BarcodeScanState.Error:
            VStack(spacing: 16) {
                Text(state.message).foregroundColor(.red)
                Button("Повторить") {
                    camera.resetDetection()
                    holder.sendEvent(BarcodeScanEvent.OnRetry())
                }
                .buttonStyle(.borderedProminent)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        default:
            EmptyView()
        }
    }

    @ViewBuilder
    private var scannerContent: some View {
        switch camera.authorizationState {
        case .notDetermined:
            ProgressView("Запрашиваем доступ к камере...")
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .denied:
            VStack(spacing: 16) {
                Text("Нужен доступ к камере").font(.headline)
                Text("Разрешите доступ в настройках или добавьте продукт вручную.")
                    .font(.subheadline).foregroundColor(.secondary).multilineTextAlignment(.center)
                Button("Добавить вручную") { router.push(.addProduct(householdId: householdId)) }
                    .buttonStyle(.borderedProminent)
            }
            .padding()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .authorized:
            ZStack(alignment: .bottom) {
                BarcodeCameraPreview(session: camera.session)
                    .ignoresSafeArea(edges: .bottom)
                VStack(spacing: 12) {
                    RoundedRectangle(cornerRadius: 18)
                        .stroke(Color.white, lineWidth: 3)
                        .frame(width: 260, height: 160)
                        .shadow(radius: 4)
                    Text("Наведите камеру на штрихкод")
                        .font(.headline)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(.thinMaterial)
                        .clipShape(Capsule())
                    Button("Ввести вручную") { router.push(.addProduct(householdId: householdId)) }
                        .buttonStyle(.borderedProminent)
                }
                .padding(.bottom, 36)
            }
        }
    }
}

private struct DraftFoundView: View {
    let draft: BarcodeProductDraft
    let onUseDraft: () -> Void
    let onManualEntry: () -> Void
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("Продукт найден").font(.title2).fontWeight(.semibold)
            Text(draft.name ?? draft.barcode).font(.title3).multilineTextAlignment(.center)
            if let brand = draft.brand {
                Text(brand).foregroundColor(.secondary)
            }
            if let category = draft.category {
                Text("Категория: \(categoryName(category))")
                    .font(.subheadline).foregroundColor(.secondary)
            }
            if draft.confidence > 0 {
                Text("AI confidence: \(Int(draft.confidence * 100))%")
                    .font(.caption).foregroundColor(.secondary)
            }
            Button("Сохранить") { onUseDraft() }
                .buttonStyle(.borderedProminent)
            Button("Заполнить вручную") { onManualEntry() }
                .buttonStyle(.bordered)
            Button("Сканировать ещё раз") { onRetry() }
                .buttonStyle(.borderless)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func categoryName(_ category: ProductCategory) -> String {
        if category == .dairy { return "Молочные" }
        else if category == .meatFish { return "Мясо/Рыба" }
        else if category == .vegetablesFruits { return "Овощи/Фрукты" }
        else if category == .cereals { return "Крупы" }
        else if category == .beverages { return "Напитки" }
        else { return "Другое" }
    }
}

private struct ProductFoundView: View {
    let product: Product
    let onScanNext: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("Продукт добавлен").font(.title2).fontWeight(.semibold).foregroundColor(.green)
            Text(product.name).font(.title3).multilineTextAlignment(.center)
            Text("\(String(format: "%.0f", product.quantity)) \(unitName(product.quantityUnit))")
                .foregroundColor(.secondary)
            Button("Сканировать ещё") { onScanNext() }
                .buttonStyle(.borderedProminent)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func unitName(_ unit: QuantityUnit) -> String {
        if unit == .grams { return "г" }
        else if unit == .milliliters { return "мл" }
        else { return "шт" }
    }
}

private struct ManualBarcodeView: View {
    let barcode: String
    let onManualEntry: () -> Void
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("Штрихкод не найден").font(.title2).fontWeight(.semibold)
            Text(barcode).font(.body).monospaced()
            Button("Добавить вручную") { onManualEntry() }
                .buttonStyle(.borderedProminent)
            Button("Сканировать ещё раз") { onRetry() }
                .buttonStyle(.bordered)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
