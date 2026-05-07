import SwiftUI
import Shared

struct AddProductScreen: View {
    let householdId: String

    @StateObject private var holder = SharedVMHolder<AddProductState, AddProductEvent, AddProductAction, AddProductViewModel>(
        viewModel: DIContainer.shared.addProductViewModel(),
        initialState: AddProductState(name: "", category: .other, quantity: "", quantityUnit: .pieces, expirationDate: "", isLoading: false, error: nil)
    )
    @EnvironmentObject private var router: AppRouter

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
            }
            .navigationTitle("Добавить продукт")
    }

    private var formContent: some View {
        let state = holder.state

        return Form {
            Section("Основное") {
                TextField("Название продукта", text: Binding(
                    get: { state.name },
                    set: { holder.sendEvent(AddProductEvent.OnNameChanged(name: $0)) }
                ))
                Picker("Категория", selection: Binding(
                    get: { state.category },
                    set: { holder.sendEvent(AddProductEvent.OnCategoryChanged(category: $0)) }
                )) {
                    ForEach(categories, id: \.0) { cat, name in
                        Text(name).tag(cat)
                    }
                }
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
            }
            Section("Срок годности") {
                TextField("ГГГГ-ММ-ДД", text: Binding(
                    get: { state.expirationDate },
                    set: { holder.sendEvent(AddProductEvent.OnExpirationDateChanged(date: $0)) }
                ))
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
}
