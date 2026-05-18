import SwiftUI
import Shared
import UserNotifications
import UIKit

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
                Group {
                    let products = state.visibleProducts
                    if state.products.isEmpty {
                        InventoryEmptyState(
                            title: "Нет продуктов",
                            message: "Добавьте продукт вручную или отсканируйте штрихкод.",
                            systemImage: "basket",
                            primaryTitle: "Добавить продукт",
                            primaryAction: { holder.sendEvent(ProductListEvent.OnAddProductClick()) },
                            secondaryTitle: "Сканировать",
                            secondaryAction: { router.push(.barcodeScan(householdId: householdId)) }
                        )
                    } else if products.isEmpty {
                        VStack(spacing: 8) {
                            ProductFilters(
                                categories: state.categories,
                                filters: state.filters,
                                onCategoryChanged: { holder.sendEvent(ProductListEvent.OnCategoryFilterChanged(categoryId: $0)) },
                                onInventoryChanged: { holder.sendEvent(ProductListEvent.OnInventoryFilterChanged(filter: $0)) }
                            )
                            Spacer()
                            InventoryEmptyState(
                                title: "Нет продуктов по выбранным фильтрам",
                                message: "Сбросьте фильтры, чтобы увидеть весь список запасов.",
                                systemImage: "line.3.horizontal.decrease.circle",
                                primaryTitle: "Сбросить фильтры",
                                primaryAction: {
                                    holder.sendEvent(ProductListEvent.OnCategoryFilterChanged(categoryId: nil))
                                    holder.sendEvent(ProductListEvent.OnInventoryFilterChanged(filter: .all))
                                }
                            )
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
                }
                .task(id: localReminderSignature(state.localReminders)) {
                    await ProductLocalReminderScheduler.schedule(state.localReminders)
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
                InventoryFloatingButton(
                    systemImage: "barcode.viewfinder",
                    label: "Сканировать штрихкод",
                    prominent: false,
                    action: { router.push(.barcodeScan(householdId: householdId)) }
                )
                .accessibilityIdentifier("productList.scanBarcode")
                InventoryFloatingButton(
                    systemImage: "plus",
                    label: "Добавить продукт",
                    prominent: true,
                    action: { holder.sendEvent(ProductListEvent.OnAddProductClick()) }
                )
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
    @State private var isShowingDeleteConfirmation = false
    @State private var consumeAmount = ""
    @State private var consumeError: String?

    var body: some View {
        HStack {
            ProductThumbnail(product: product)
            VStack(alignment: .leading, spacing: 4) {
                Text(product.name).font(.headline)
                Text("\(product.categoryName ?? categoryName(product.category)) • \(String(format: "%.0f", product.quantity)) \(unitName(product.quantityUnit))")
                    .font(.subheadline).foregroundColor(.secondary)
                if product.remainingAmount <= (product.lowStockThreshold?.doubleValue ?? 0) {
                    InventoryStatusBadge(text: "Низкий остаток", tone: .warning)
                }
                if let date = product.expirationDate {
                    HStack(spacing: 6) {
                        InventoryStatusBadge(text: statusText(product.expirationStatus), tone: statusTone(product.expirationStatus))
                        Text("до \(date)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
            Spacer()
            VStack(spacing: 12) {
                Button(action: onOpen) {
                    Image(systemName: "pencil").foregroundColor(.accentColor)
                }.buttonStyle(.plain)
                    .accessibilityLabel("Редактировать \(product.name)")

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
                .accessibilityLabel("Списать \(product.name)")

                Button(action: { isShowingDeleteConfirmation = true }) {
                    Image(systemName: "trash").foregroundColor(.red)
                }.buttonStyle(.plain)
                    .accessibilityLabel("Удалить \(product.name)")
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
        .confirmationDialog("Удалить продукт?", isPresented: $isShowingDeleteConfirmation, titleVisibility: .visible) {
            Button("Удалить", role: .destructive, action: onDelete)
            Button("Отмена", role: .cancel) { }
        } message: {
            Text("«\(product.name)» исчезнет из списка запасов.")
        }
    }

    private struct ProductThumbnail: View {
        let product: Product

        var body: some View {
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(.secondarySystemBackground))
                if let localImagePath = product.localImagePath,
                   let image = UIImage(contentsOfFile: localImagePath) {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                } else if let imageUrl = product.imageUrl,
                          let url = URL(string: imageUrl) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case let .success(image):
                            image.resizable().scaledToFill()
                        default:
                            Image(systemName: "photo")
                                .foregroundColor(.secondary)
                        }
                    }
                } else {
                    Image(systemName: "camera")
                        .foregroundColor(.secondary)
                }
            }
            .frame(width: 56, height: 56)
            .clipShape(RoundedRectangle(cornerRadius: 12))
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

    private func statusText(_ status: ExpirationStatus) -> String {
        if status == .expired { return "Просрочен" }
        else if status == .expiringSoon { return "Скоро истекает" }
        else if status == .fresh { return "Свежий" }
        else { return "Без срока" }
    }

    private func statusTone(_ status: ExpirationStatus) -> InventoryTone {
        if status == .expired { return .danger }
        else if status == .expiringSoon { return .warning }
        else if status == .fresh { return .success }
        else { return .neutral }
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

private func localReminderSignature(_ reminders: [ProductLocalReminder]) -> String {
    reminders
        .map { "\($0.id):\($0.triggerDateIso ?? "now")" }
        .sorted()
        .joined(separator: "|")
}

private enum ProductLocalReminderScheduler {
    private static let identifierPrefix = "product-inventory-local-reminder:"
    private static let scheduledIdentifiersKey = "scheduledProductLocalReminderIdentifiers"

    static func schedule(_ reminders: [ProductLocalReminder]) async {
        guard await requestAuthorizationIfNeeded() else { return }

        let center = UNUserNotificationCenter.current()
        let desiredIdentifiers = Set(reminders.map(identifier(for:)))
        let scheduledIdentifiers = Set(UserDefaults.standard.stringArray(forKey: scheduledIdentifiersKey) ?? [])
        let staleIdentifiers = scheduledIdentifiers.subtracting(desiredIdentifiers)

        if !staleIdentifiers.isEmpty {
            center.removePendingNotificationRequests(withIdentifiers: Array(staleIdentifiers))
        }

        for reminder in reminders where !scheduledIdentifiers.contains(identifier(for: reminder)) {
            try? await center.add(request(for: reminder))
        }

        UserDefaults.standard.set(Array(desiredIdentifiers), forKey: scheduledIdentifiersKey)
    }

    private static func request(for reminder: ProductLocalReminder) -> UNNotificationRequest {
        let content = UNMutableNotificationContent()
        content.title = reminder.title
        content.body = reminder.message
        content.sound = .default
        content.userInfo = [
            "reminderId": reminder.id,
            "productId": reminder.productId,
            "householdId": reminder.householdId
        ]

        return UNNotificationRequest(
            identifier: identifier(for: reminder),
            content: content,
            trigger: trigger(for: reminder)
        )
    }

    private static func trigger(for reminder: ProductLocalReminder) -> UNNotificationTrigger {
        guard let triggerDateIso = reminder.triggerDateIso,
              let triggerDate = date(from: triggerDateIso)
        else {
            return UNTimeIntervalNotificationTrigger(timeInterval: 5, repeats: false)
        }

        let scheduledAt = Calendar.current.date(
            bySettingHour: 9,
            minute: 0,
            second: 0,
            of: triggerDate
        ) ?? triggerDate

        if scheduledAt <= Date() {
            return UNTimeIntervalNotificationTrigger(timeInterval: 5, repeats: false)
        }

        let components = Calendar.current.dateComponents(
            [.year, .month, .day, .hour, .minute],
            from: scheduledAt
        )
        return UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
    }

    private static func requestAuthorizationIfNeeded() async -> Bool {
        let center = UNUserNotificationCenter.current()
        let settings = await center.notificationSettings()
        if settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional {
            return true
        }
        if settings.authorizationStatus == .denied {
            return false
        }
        return (try? await center.requestAuthorization(options: [.alert, .badge, .sound])) ?? false
    }

    private static func identifier(for reminder: ProductLocalReminder) -> String {
        "\(identifierPrefix)\(reminder.id)"
    }

    private static func date(from iso: String) -> Date? {
        let parts = iso.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        var components = DateComponents()
        components.year = parts[0]
        components.month = parts[1]
        components.day = parts[2]
        return Calendar.current.date(from: components)
    }
}
