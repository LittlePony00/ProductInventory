import SwiftUI
import Shared

struct ProductListScreen: View {
    let householdId: String

    @StateObject private var holder = SharedVMHolder<ProductListState, ProductListEvent, ProductListAction, ProductListViewModel>(
        viewModel: DIContainer.shared.productListViewModel(),
        initialState: ProductListState.Loading()
    )
    @EnvironmentObject private var router: AppRouter

    var body: some View {
        content
            .task {
                holder.start { [weak router, householdId] action in
                    guard let router = router else { return }
                    DispatchQueue.main.async {
                        switch action {
                        case is ProductListAction.OpenAddProduct:
                            router.push(.addProduct(householdId: householdId))
                        case let action as ProductListAction.OpenProductDetail:
                            router.push(.editProduct(householdId: householdId, productId: action.productId))
                        default:
                            break
                        }
                    }
                }
                holder.sendEvent(ProductListEvent.OnCreate(householdId: householdId))
            }
            .onAppear {
                holder.sendEvent(ProductListEvent.OnResume())
            }
            .navigationTitle("Продукты")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack {
                        Button("Категории") { router.push(.categories(householdId: householdId)) }
                            .accessibilityIdentifier("productList.categories")
                        Button("Рецепты") { router.push(.recipes(householdId: householdId)) }
                            .accessibilityIdentifier("productList.recipes")
                        Button(action: { router.push(.notifications) }) {
                            Image(systemName: "bell")
                        }
                        .accessibilityIdentifier("productList.notifications")
                    }
                }
            }
    }

    @ViewBuilder
    private var content: some View {
        ZStack(alignment: .bottomTrailing) {
            switch holder.state {
            case is ProductListState.Loading:
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            case let state as ProductListState.Content:
                let products = state.visibleProducts
                if state.products.isEmpty {
                    VStack(spacing: 8) {
                        Text("Нет продуктов").font(.headline)
                        Text("Сканируйте штрихкод или нажмите + чтобы добавить вручную")
                            .font(.subheadline).foregroundColor(.secondary)
                    }.frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if products.isEmpty {
                    VStack(spacing: 8) {
                        ProductFilters(
                            categories: state.categories,
                            filters: state.filters,
                            onCategoryChanged: { holder.sendEvent(ProductListEvent.OnCategoryFilterChanged(categoryId: $0)) },
                            onInventoryChanged: { holder.sendEvent(ProductListEvent.OnInventoryFilterChanged(filter: $0)) }
                        )
                        Spacer()
                        Text("Нет продуктов по выбранным фильтрам").font(.headline)
                        Spacer()
                    }
                    .padding()
                } else {
                    VStack(spacing: 8) {
                        ProductFilters(
                            categories: state.categories,
                            filters: state.filters,
                            onCategoryChanged: { holder.sendEvent(ProductListEvent.OnCategoryFilterChanged(categoryId: $0)) },
                            onInventoryChanged: { holder.sendEvent(ProductListEvent.OnInventoryFilterChanged(filter: $0)) }
                        )
                            .padding(.horizontal)
                        List(products, id: \.id) { product in
                            ProductRow(
                                product: product,
                                onOpen: {
                                    holder.sendEvent(ProductListEvent.OnProductClick(productId: product.id))
                                },
                                onConsume: { amount in
                                    holder.sendEvent(ProductListEvent.OnConsumeProduct(productId: product.id, amount: amount))
                                },
                                onDelete: {
                                holder.sendEvent(ProductListEvent.OnDeleteProduct(productId: product.id))
                                }
                            )
                        }
                    }
                }
            case let state as ProductListState.Error:
                VStack(spacing: 8) {
                    Text(state.message ?? "Ошибка загрузки")
                    Button("Повторить") { holder.sendEvent(ProductListEvent.OnRetry()) }
                        .buttonStyle(.borderedProminent)
                }.frame(maxWidth: .infinity, maxHeight: .infinity)
            default:
                EmptyView()
            }

            VStack(spacing: 12) {
                Button { router.push(.barcodeScan(householdId: householdId)) } label: {
                    Image(systemName: "barcode.viewfinder").font(.title2).padding(14)
                        .background(Color.secondary.opacity(0.18)).foregroundColor(.primary).clipShape(Circle())
                }
                .accessibilityIdentifier("productList.scanBarcode")
                Button { holder.sendEvent(ProductListEvent.OnAddProductClick()) } label: {
                    Image(systemName: "plus").font(.title2).padding(16)
                        .background(Color.accentColor).foregroundColor(.white).clipShape(Circle())
                }
                .accessibilityIdentifier("productList.addProduct")
            }.padding()
        }
    }
}

struct ProductRow: View {
    let product: Product
    let onOpen: () -> Void
    let onConsume: (Double) -> Void
    let onDelete: () -> Void
    @State private var isShowingConsumeDialog = false
    @State private var consumeAmount = ""
    @State private var consumeError: String?

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(product.name).font(.headline)
                Text("\(product.categoryName ?? categoryName(product.category)) • \(String(format: "%.0f", product.quantity)) \(unitName(product.quantityUnit))")
                    .font(.subheadline).foregroundColor(.secondary)
                if product.remainingAmount <= (product.lowStockThreshold?.doubleValue ?? 0) {
                    Text("Низкий остаток")
                        .font(.caption).foregroundColor(.orange)
                }
                if let date = product.expirationDate {
                    Text("Годен до: \(date)")
                        .font(.caption).foregroundColor(statusColor(product.expirationStatus))
                }
            }
            Spacer()
            VStack(spacing: 12) {
                Button(action: onOpen) {
                    Image(systemName: "pencil").foregroundColor(.accentColor)
                }.buttonStyle(.plain)

                Button {
                    consumeAmount = String(product.remainingAmount)
                    consumeError = nil
                    isShowingConsumeDialog = true
                } label: {
                    Image(systemName: "minus.circle")
                        .foregroundColor(product.remainingAmount > 0 ? .accentColor : .secondary)
                }
                .disabled(product.remainingAmount <= 0)
                .buttonStyle(.plain)

                Button(action: onDelete) {
                    Image(systemName: "trash").foregroundColor(.red)
                }.buttonStyle(.plain)
            }
        }
        .padding(.vertical, 4)
        .alert("Списать продукт", isPresented: $isShowingConsumeDialog) {
            TextField("Количество", text: $consumeAmount)
                .keyboardType(.decimalPad)
            Button("Отмена", role: .cancel) {
                consumeError = nil
            }
            Button("Списать") {
                guard let amount = Double(consumeAmount.replacingOccurrences(of: ",", with: ".")),
                      amount > 0,
                      amount <= product.remainingAmount
                else {
                    consumeError = "Введите количество больше 0 и не больше остатка"
                    isShowingConsumeDialog = true
                    return
                }
                consumeError = nil
                onConsume(amount)
            }
        } message: {
            VStack(alignment: .leading) {
                Text("Доступно: \(String(format: "%.2f", product.remainingAmount)) \(unitName(product.quantityUnit))")
                if let consumeError {
                    Text(consumeError)
                }
            }
        }
    }

    private func unitName(_ unit: QuantityUnit) -> String {
        if unit == .grams { return "г" }
        else if unit == .milliliters { return "мл" }
        else if unit == .pieces { return "шт" }
        else { return "" }
    }

    private func categoryName(_ category: ProductCategory) -> String {
        if category == .dairy { return "Молочные" }
        else if category == .meatFish { return "Мясо/Рыба" }
        else if category == .vegetablesFruits { return "Овощи/Фрукты" }
        else if category == .cereals { return "Крупы" }
        else if category == .beverages { return "Напитки" }
        else { return "Другое" }
    }

    private func statusColor(_ status: ExpirationStatus) -> Color {
        if status == .expired { return .red }
        else if status == .expiringSoon { return .orange }
        else { return .secondary }
    }
}

private struct ProductFilters: View {
    let categories: [ProductCategoryOption]
    let filters: ProductListFilters
    let onCategoryChanged: (String?) -> Void
    let onInventoryChanged: (InventoryFilter) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    Button("Все") { onCategoryChanged(nil) }
                        .buttonStyle(.bordered)
                        .tint(filters.categoryId == nil ? .accentColor : .secondary)
                    ForEach(categories, id: \.id) { category in
                        Button(categoryDisplayName(category)) {
                            onCategoryChanged(category.id)
                        }
                        .buttonStyle(.bordered)
                        .tint(filters.categoryId == category.id ? .accentColor : .secondary)
                    }
                }
                .padding(.vertical, 4)
            }
            Picker("Фильтр", selection: Binding(
                get: { filters.inventory },
                set: onInventoryChanged
            )) {
                Text("Все").tag(InventoryFilter.all)
                Text("Мало").tag(InventoryFilter.lowStock)
                Text("Скоро истекают").tag(InventoryFilter.expiringSoon)
                Text("Просрочены").tag(InventoryFilter.expired)
            }
            .pickerStyle(.segmented)
        }
    }
}

func categoryDisplayName(_ category: ProductCategoryOption) -> String {
    if let code = category.code {
        if code == .dairy { return "Молочные" }
        if code == .meatFish { return "Мясо/Рыба" }
        if code == .vegetablesFruits { return "Овощи/Фрукты" }
        if code == .cereals { return "Крупы" }
        if code == .beverages { return "Напитки" }
        if code == .other { return "Другое" }
    }
    return category.name
}
