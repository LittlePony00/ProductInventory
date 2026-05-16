import SwiftUI
import Shared

struct AddProductScreen: View {
    let householdId: String
    let productId: String?
    let draft: ProductDraftInput?

    @StateObject private var holder = SharedVMHolder<AddProductState, AddProductEvent, AddProductAction, AddProductViewModel>(
        viewModel: DIContainer.shared.addProductViewModel(),
        initialState: AddProductState(
            name: "",
            brand: "",
            barcode: "",
            category: .other,
            categoryId: nil,
            categories: ProductCategoryOption.companion.systemDefaults(),
            newCategoryName: "",
            isCreatingCategory: false,
            isSuggestingProduct: false,
            suggestionMessage: nil,
            quantity: "",
            quantityUnit: .pieces,
            remainingAmount: "",
            lowStockThreshold: "",
            expirationDate: "",
            packageAmount: "",
            packageUnit: .pieces,
            ingredientsText: "",
            calories: "",
            protein: "",
            fat: "",
            carbs: "",
            isBarcodePrefilled: false,
            isLoading: false,
            error: nil
        )
    )
    @EnvironmentObject private var router: AppRouter
    @State private var didApplyInitialDraft = false

    init(householdId: String, productId: String? = nil, draft: ProductDraftInput? = nil) {
        self.householdId = householdId
        self.productId = productId
        self.draft = draft
    }

    private let categories: [(ProductCategory, String)] = [
        (.dairy, "Молочные"), (.meatFish, "Мясо/Рыба"),
        (.vegetablesFruits, "Овощи/Фрукты"), (.cereals, "Крупы"),
        (.beverages, "Напитки"), (.other, "Другое")
    ]

    private let units: [(QuantityUnit, String)] = [
        (.grams, "Граммы"), (.milliliters, "Миллилитры"), (.pieces, "Штуки")
    ]

    var body: some View {
        formContent
            .task {
                holder.viewModel.householdId = householdId
                holder.start { [weak router] action in
                    guard let router = router else { return }
                    DispatchQueue.main.async {
                        switch action {
                        case is AddProductAction.ProductAdded:
                            router.pop()
                        case is AddProductAction.NavigateBack:
                            router.pop()
                        default:
                            break
                        }
                    }
                }
                holder.sendEvent(AddProductEvent.OnCreate(householdId: householdId))
                if let productId {
                    holder.sendEvent(AddProductEvent.OnLoadProduct(productId: productId))
                }
                applyInitialDraftIfNeeded()
            }
            .navigationTitle(productId == nil ? "Добавить продукт" : "Редактировать продукт")
    }

    private var formContent: some View {
        let state = holder.state

        return Form {
            Section("Основное") {
                TextField("Название продукта", text: Binding(
                    get: { state.name },
                    set: { holder.sendEvent(AddProductEvent.OnNameChanged(name: $0)) }
                ))
                TextField("Бренд", text: Binding(
                    get: { state.brand },
                    set: { holder.sendEvent(AddProductEvent.OnBrandChanged(brand: $0)) }
                ))
                TextField("Штрихкод", text: Binding(
                    get: { state.barcode },
                    set: { holder.sendEvent(AddProductEvent.OnBarcodeChanged(barcode: $0)) }
                ))
                .keyboardType(.numberPad)
                Picker("Категория", selection: Binding(
                    get: { state.categoryId ?? categoryId(for: state.category, in: state.categories) ?? "" },
                    set: { categoryId in
                        if let option = state.categories.first(where: { $0.id == categoryId }) {
                            holder.sendEvent(AddProductEvent.OnCategoryChanged(
                                categoryId: categoryId,
                                category: option.legacyCategory
                            ))
                        }
                    }
                )) {
                    ForEach(state.categories, id: \.id) { option in
                        Text(option.name).tag(option.id)
                    }
                }
                TextField("Новая категория", text: Binding(
                    get: { state.newCategoryName },
                    set: { holder.sendEvent(AddProductEvent.OnNewCategoryNameChanged(name: $0)) }
                ))
                Button(action: { holder.sendEvent(AddProductEvent.OnCreateCategoryClick()) }) {
                    if state.isCreatingCategory {
                        ProgressView()
                    } else {
                        Text("Создать категорию")
                    }
                }
                .disabled(state.isCreatingCategory || state.newCategoryName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            Section("Количество") {
                TextField("Количество", text: Binding(
                    get: { state.quantity },
                    set: { holder.sendEvent(AddProductEvent.OnQuantityChanged(quantity: $0)) }
                )).keyboardType(.decimalPad)

                Picker("Единица измерения", selection: Binding(
                    get: { state.quantityUnit },
                    set: { holder.sendEvent(AddProductEvent.OnQuantityUnitChanged(unit: $0)) }
                )) {
                    ForEach(units, id: \.0) { unit, name in
                        Text(name).tag(unit)
                    }
                }
                TextField("Количество в упаковке", text: Binding(
                    get: { state.packageAmount },
                    set: { holder.sendEvent(AddProductEvent.OnPackageAmountChanged(amount: $0)) }
                )).keyboardType(.decimalPad)
                TextField("Остаток", text: Binding(
                    get: { state.remainingAmount },
                    set: { holder.sendEvent(AddProductEvent.OnRemainingAmountChanged(amount: $0)) }
                )).keyboardType(.decimalPad)
                TextField("Порог низкого запаса", text: Binding(
                    get: { state.lowStockThreshold },
                    set: { holder.sendEvent(AddProductEvent.OnLowStockThresholdChanged(threshold: $0)) }
                )).keyboardType(.decimalPad)

                Picker("Единица упаковки", selection: Binding(
                    get: { state.packageUnit },
                    set: { holder.sendEvent(AddProductEvent.OnPackageUnitChanged(unit: $0)) }
                )) {
                    ForEach(units, id: \.0) { unit, name in
                        Text(name).tag(unit)
                    }
                }
            }
            Section("Срок годности") {
                TextField("ГГГГ-ММ-ДД", text: Binding(
                    get: { state.expirationDate },
                    set: { holder.sendEvent(AddProductEvent.OnExpirationDateChanged(date: $0)) }
                ))
            }
            Section("AI-подсказки") {
                Button(action: { holder.sendEvent(AddProductEvent.OnSuggestProductClick()) }) {
                    HStack {
                        if state.isSuggestingProduct {
                            ProgressView()
                        }
                        Text(state.isSuggestingProduct ? "Получаем подсказку..." : "Подсказать ИИ")
                    }
                }
                .disabled(state.isSuggestingProduct)
                if let message = state.suggestionMessage {
                    Text(message).font(.caption).foregroundColor(.secondary)
                }
                TextField("Состав", text: Binding(
                    get: { state.ingredientsText },
                    set: { holder.sendEvent(AddProductEvent.OnIngredientsChanged(ingredients: $0)) }
                ))
                TextField("Ккал", text: Binding(
                    get: { state.calories },
                    set: { holder.sendEvent(AddProductEvent.OnCaloriesChanged(calories: $0)) }
                )).keyboardType(.decimalPad)
                TextField("Белки, г", text: Binding(
                    get: { state.protein },
                    set: { holder.sendEvent(AddProductEvent.OnProteinChanged(protein: $0)) }
                )).keyboardType(.decimalPad)
                TextField("Жиры, г", text: Binding(
                    get: { state.fat },
                    set: { holder.sendEvent(AddProductEvent.OnFatChanged(fat: $0)) }
                )).keyboardType(.decimalPad)
                TextField("Углеводы, г", text: Binding(
                    get: { state.carbs },
                    set: { holder.sendEvent(AddProductEvent.OnCarbsChanged(carbs: $0)) }
                )).keyboardType(.decimalPad)
            }
            if let error = state.error {
                Section { Text(error).foregroundColor(.red).font(.caption) }
            }
            Section {
                Button(action: { holder.sendEvent(AddProductEvent.OnSaveClick()) }) {
                    HStack {
                        Spacer()
                        if state.isLoading { ProgressView() } else { Text("Сохранить") }
                        Spacer()
                    }
                }.disabled(state.isLoading)
            }
        }
    }

    private func applyInitialDraftIfNeeded() {
        guard !didApplyInitialDraft, let draft else { return }
        didApplyInitialDraft = true
        holder.sendEvent(AddProductEvent.OnScannedDraftApplied(
            barcode: draft.barcode,
            name: draft.name,
            brand: draft.brand,
            category: productCategory(from: draft.category),
            packageAmount: decimalString(draft.packageQuantity),
            packageUnit: quantityUnit(from: draft.packageQuantityUnit),
            ingredientsText: draft.ingredients,
            calories: decimalString(draft.caloriesKcal),
            protein: decimalString(draft.proteinGrams),
            fat: decimalString(draft.fatGrams),
            carbs: decimalString(draft.carbohydratesGrams)
        ))
    }

    private func decimalString(_ value: Double?) -> String? {
        value.map { String($0) }
    }

    private func categoryId(for category: ProductCategory, in categories: [ProductCategoryOption]) -> String? {
        categories.first { $0.legacyCategory == category }?.id
    }

    private func productCategory(from name: String?) -> ProductCategory? {
        switch name {
        case "DAIRY": return .dairy
        case "MEAT_FISH": return .meatFish
        case "VEGETABLES_FRUITS": return .vegetablesFruits
        case "CEREALS": return .cereals
        case "BEVERAGES": return .beverages
        case "OTHER": return .other
        default: return nil
        }
    }

    private func quantityUnit(from name: String?) -> QuantityUnit? {
        switch name {
        case "GRAMS": return .grams
        case "MILLILITERS": return .milliliters
        case "PIECES": return .pieces
        default: return nil
        }
    }
}
