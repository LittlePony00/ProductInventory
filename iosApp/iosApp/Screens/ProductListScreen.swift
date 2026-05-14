import SwiftUI
import Shared

struct ProductListScreen: View {
    let householdId: String

    @StateObject private var holder = SharedVMHolder<ProductListState, ProductListEvent, ProductListAction, ProductListViewModel>(
        viewModel: DIContainer.shared.productListViewModel(),
        initialState: ProductListState.Loading()
    )
    @EnvironmentObject private var router: AppRouter
    @State private var selectedCategory: ProductCategory?

    var body: some View {
        content
            .task {
                holder.start { [weak router, householdId] action in
                    guard let router = router else { return }
                    DispatchQueue.main.async {
                        switch action {
                        case is ProductListAction.OpenAddProduct:
                            router.push(.addProduct(householdId: householdId))
                        default:
                            break
                        }
                    }
                }
                holder.sendEvent(ProductListEvent.OnCreate(householdId: householdId))
            }
            .navigationTitle("Продукты")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack {
                        Button("Рецепты") { router.push(.recipes(householdId: householdId)) }
                        Button(action: { router.push(.notifications) }) {
                            Image(systemName: "bell")
                        }
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
                let products = filteredProducts(state.products)
                if state.products.isEmpty {
                    VStack(spacing: 8) {
                        Text("Нет продуктов").font(.headline)
                        Text("Сканируйте штрихкод или нажмите + чтобы добавить вручную")
                            .font(.subheadline).foregroundColor(.secondary)
                    }.frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if products.isEmpty {
                    VStack(spacing: 8) {
                        ProductCategoryFilter(selectedCategory: $selectedCategory)
                        Spacer()
                        Text("Нет продуктов в выбранной категории").font(.headline)
                        Spacer()
                    }
                    .padding()
                } else {
                    VStack(spacing: 8) {
                        ProductCategoryFilter(selectedCategory: $selectedCategory)
                            .padding(.horizontal)
                        List(products, id: \.id) { product in
                            ProductRow(product: product) {
                                holder.sendEvent(ProductListEvent.OnDeleteProduct(productId: product.id))
                            }
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
                Button { holder.sendEvent(ProductListEvent.OnAddProductClick()) } label: {
                    Image(systemName: "plus").font(.title2).padding(16)
                        .background(Color.accentColor).foregroundColor(.white).clipShape(Circle())
                }
            }.padding()
        }
    }

    private func filteredProducts(_ products: [Product]) -> [Product] {
        guard let selectedCategory else { return products }
        return products.filter { $0.category == selectedCategory }
    }
}

struct ProductRow: View {
    let product: Product
    let onDelete: () -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(product.name).font(.headline)
                Text("\(categoryName(product.category)) • \(String(format: "%.0f", product.quantity)) \(unitName(product.quantityUnit))")
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
            Button(action: onDelete) {
                Image(systemName: "trash").foregroundColor(.red)
            }.buttonStyle(.plain)
        }.padding(.vertical, 4)
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

private struct ProductCategoryFilter: View {
    @Binding var selectedCategory: ProductCategory?

    private let categories: [(ProductCategory?, String)] = [
        (nil, "Все"),
        (.dairy, "Молочные"),
        (.meatFish, "Мясо/Рыба"),
        (.vegetablesFruits, "Овощи/Фрукты"),
        (.cereals, "Крупы"),
        (.beverages, "Напитки"),
        (.other, "Другое")
    ]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(Array(categories.enumerated()), id: \.offset) { _, item in
                    Button(item.1) {
                        selectedCategory = item.0
                    }
                    .buttonStyle(.bordered)
                    .tint(selectedCategory == item.0 ? .accentColor : .secondary)
                }
            }
            .padding(.vertical, 4)
        }
    }
}
