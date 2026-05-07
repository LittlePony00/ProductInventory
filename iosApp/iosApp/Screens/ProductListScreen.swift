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
                if state.products.isEmpty {
                    VStack(spacing: 8) {
                        Text("Нет продуктов").font(.headline)
                        Text("Нажмите + чтобы добавить").font(.subheadline).foregroundColor(.secondary)
                    }.frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List(state.products, id: \.id) { product in
                        ProductRow(product: product) {
                            holder.sendEvent(ProductListEvent.OnDeleteProduct(productId: product.id))
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

            Button { holder.sendEvent(ProductListEvent.OnAddProductClick()) } label: {
                Image(systemName: "plus").font(.title2).padding(16)
                    .background(Color.accentColor).foregroundColor(.white).clipShape(Circle())
            }.padding()
        }
    }
}

struct ProductRow: View {
    let product: Product
    let onDelete: () -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(product.name).font(.headline)
                Text("\(String(format: "%.0f", product.quantity)) \(unitName(product.quantityUnit))")
                    .font(.subheadline).foregroundColor(.secondary)
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

    private func statusColor(_ status: ExpirationStatus) -> Color {
        if status == .expired { return .red }
        else if status == .expiringSoon { return .orange }
        else { return .secondary }
    }
}
