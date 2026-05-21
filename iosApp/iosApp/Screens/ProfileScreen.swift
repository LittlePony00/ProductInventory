import SwiftUI
import Shared

struct ProfileScreen: View {
    let householdId: String?

    @StateObject private var holder = SharedVMHolder<ProfileState, ProfileEvent, ProfileAction, ProfileViewModel>(
        viewModel: DIContainer.shared.profileViewModel(),
        initialState: ProfileState.Loading()
    )
    @EnvironmentObject private var router: AppRouter
    @State private var showLogoutConfirmation = false

    var body: some View {
        content
            .task {
                holder.start { [weak router] action in
                    guard let router = router else { return }
                    DispatchQueue.main.async {
                        switch action {
                        case is ProfileAction.NavigateToLogin:
                            router.setRoot(.login)
                        case is ProfileAction.NavigateBack:
                            router.pop()
                        default:
                            break
                        }
                    }
                }
                holder.sendEvent(ProfileEvent.OnCreate(householdId: householdId))
            }
            .navigationTitle("Профиль")
    }

    @ViewBuilder
    private var content: some View {
        switch holder.state {
        case is ProfileState.Loading:
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        case let state as ProfileState.Content:
            Form {
                Section {
                    VStack(spacing: 12) {
                        Circle()
                            .fill(Color.green.opacity(0.16))
                            .frame(width: 96, height: 96)
                            .overlay {
                                Text(String(state.profile.name.prefix(1)).uppercased())
                                    .font(.largeTitle.weight(.semibold))
                                    .foregroundStyle(.green)
                            }
                            .accessibilityHidden(true)
                        Text(state.profile.name)
                            .font(.title2.weight(.semibold))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
                    .listRowBackground(Color.clear)
                }
                Section("Эл. почта") { Text(state.profile.email) }
                Section("Имя") {
                    if state.isEditing {
                        TextField("Имя", text: Binding(
                            get: { state.editName },
                            set: { holder.sendEvent(ProfileEvent.OnNameChanged(name: $0)) }
                        ))
                        HStack {
                            Button("Отмена") { holder.sendEvent(ProfileEvent.OnCancelEdit()) }
                            Spacer()
                            Button("Сохранить") { holder.sendEvent(ProfileEvent.OnSaveClick()) }
                                .buttonStyle(.borderedProminent)
                        }
                    } else {
                        HStack {
                            Text(state.profile.name)
                            Spacer()
                            Button("Изменить") { holder.sendEvent(ProfileEvent.OnEditClick()) }
                        }
                    }
                }
                Section("Пищевые предпочтения") {
                    Text("Указывайте значения через запятую. Ограничения питания можно вводить по-русски: вегетарианство, веганство, без глютена, без молочного, без орехов, халяль, кошерно.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    if state.isEditingFoodPreferences {
                        TextField("Любимые кухни", text: Binding(
                            get: { state.editPreferredCuisines },
                            set: { holder.sendEvent(ProfileEvent.OnPreferredCuisinesChanged(value: $0)) }
                        ))
                        TextField("Любимые продукты", text: Binding(
                            get: { state.editPreferredProducts },
                            set: { holder.sendEvent(ProfileEvent.OnPreferredProductsChanged(value: $0)) }
                        ))
                        TextField("Нежелательные ингредиенты", text: Binding(
                            get: { state.editDislikedIngredients },
                            set: { holder.sendEvent(ProfileEvent.OnDislikedIngredientsChanged(value: $0)) }
                        ))
                        TextField("Продукты, которых избегать", text: Binding(
                            get: { state.editAvoidedProducts },
                            set: { holder.sendEvent(ProfileEvent.OnAvoidedProductsChanged(value: $0)) }
                        ))
                        TextField("Аллергии", text: Binding(
                            get: { state.editAllergies },
                            set: { holder.sendEvent(ProfileEvent.OnAllergiesChanged(value: $0)) }
                        ))
                        TextField("Ограничения питания", text: Binding(
                            get: { state.editDietaryRestrictions },
                            set: { holder.sendEvent(ProfileEvent.OnDietaryRestrictionsChanged(value: $0)) }
                        ))
                        if state.foodPreferenceOptions.hasStructuredOptions {
                            PreferenceOptionChips(
                                title: "Любимые продукты из запасов",
                                options: state.foodPreferenceOptions.products.map { (id: $0.id, name: $0.name) },
                                selectedIds: state.editPreferredProductIds,
                                accessibilityPrefix: "profile.foodPreferences.preferredProduct",
                                onToggle: { holder.sendEvent(ProfileEvent.OnPreferredProductToggled(id: $0)) }
                            )
                            PreferenceOptionChips(
                                title: "Продукты из запасов, которых избегать",
                                options: state.foodPreferenceOptions.products.map { (id: $0.id, name: $0.name) },
                                selectedIds: state.editAvoidedProductIds,
                                accessibilityPrefix: "profile.foodPreferences.avoidedProduct",
                                onToggle: { holder.sendEvent(ProfileEvent.OnAvoidedProductToggled(id: $0)) }
                            )
                            PreferenceOptionChips(
                                title: "Любимые категории",
                                options: state.foodPreferenceOptions.categories.map { (id: $0.id, name: $0.name) },
                                selectedIds: state.editPreferredCategoryIds,
                                accessibilityPrefix: "profile.foodPreferences.preferredCategory",
                                onToggle: { holder.sendEvent(ProfileEvent.OnPreferredCategoryToggled(id: $0)) }
                            )
                            PreferenceOptionChips(
                                title: "Категории, которых избегать",
                                options: state.foodPreferenceOptions.categories.map { (id: $0.id, name: $0.name) },
                                selectedIds: state.editAvoidedCategoryIds,
                                accessibilityPrefix: "profile.foodPreferences.avoidedCategory",
                                onToggle: { holder.sendEvent(ProfileEvent.OnAvoidedCategoryToggled(id: $0)) }
                            )
                        }
                        TextField("Макс. время, мин", text: Binding(
                            get: { state.editMaxCookingTimeMinutes },
                            set: { holder.sendEvent(ProfileEvent.OnMaxCookingTimeChanged(value: $0)) }
                        ))
                        TextField("Сложность", text: Binding(
                            get: { state.editPreferredDifficulty },
                            set: { holder.sendEvent(ProfileEvent.OnPreferredDifficultyChanged(value: $0)) }
                        ))
                        TextField("Порции", text: Binding(
                            get: { state.editServings },
                            set: { holder.sendEvent(ProfileEvent.OnServingsChanged(value: $0)) }
                        ))
                        HStack {
                            Button("Отмена") { holder.sendEvent(ProfileEvent.OnCancelFoodPreferencesEdit()) }
                            Spacer()
                            Button("Сохранить") { holder.sendEvent(ProfileEvent.OnSaveFoodPreferencesClick()) }
                                .buttonStyle(.borderedProminent)
                                .accessibilityIdentifier("profile.foodPreferences.save")
                        }
                    } else {
                        PreferenceRow(title: "Любимые кухни", values: state.foodPreferences.preferredCuisines)
                        PreferenceRow(title: "Любимые продукты", values: state.foodPreferences.preferredProducts)
                        PreferenceRow(title: "Нежелательные ингредиенты", values: state.foodPreferences.dislikedIngredients)
                        PreferenceRow(title: "Продукты, которых избегать", values: state.foodPreferences.avoidedProducts)
                        PreferenceRow(title: "Аллергии", values: state.foodPreferences.allergies)
                        PreferenceRow(title: "Ограничения питания", values: state.foodPreferences.dietaryRestrictions)
                        if state.foodPreferenceOptions.hasStructuredOptions {
                            PreferenceRow(
                                title: "Любимые продукты из запасов",
                                values: selectedNames(
                                    ids: state.foodPreferences.preferredProductIds,
                                    options: state.foodPreferenceOptions.products.map { (id: $0.id, name: $0.name) }
                                )
                            )
                            PreferenceRow(
                                title: "Продукты из запасов, которых избегать",
                                values: selectedNames(
                                    ids: state.foodPreferences.avoidedProductIds,
                                    options: state.foodPreferenceOptions.products.map { (id: $0.id, name: $0.name) }
                                )
                            )
                            PreferenceRow(
                                title: "Любимые категории",
                                values: selectedNames(
                                    ids: state.foodPreferences.preferredCategoryIds,
                                    options: state.foodPreferenceOptions.categories.map { (id: $0.id, name: $0.name) }
                                )
                            )
                            PreferenceRow(
                                title: "Категории, которых избегать",
                                values: selectedNames(
                                    ids: state.foodPreferences.avoidedCategoryIds,
                                    options: state.foodPreferenceOptions.categories.map { (id: $0.id, name: $0.name) }
                                )
                            )
                        }
                        HStack {
                            Text("Макс. время")
                            Spacer()
                            Text(state.foodPreferences.maxCookingTimeMinutes?.description ?? "Не указано")
                                .foregroundStyle(.secondary)
                        }
                        HStack {
                            Text("Порции")
                            Spacer()
                            Text(state.foodPreferences.servings?.description ?? "Не указано")
                                .foregroundStyle(.secondary)
                        }
                        Button("Изменить") { holder.sendEvent(ProfileEvent.OnEditFoodPreferencesClick()) }
                            .accessibilityIdentifier("profile.foodPreferences.edit")
                    }
                }
                Section {
                    Button("Выйти из аккаунта", role: .destructive) {
                        showLogoutConfirmation = true
                    }
                }
            }
            .confirmationDialog("Выйти из аккаунта?", isPresented: $showLogoutConfirmation, titleVisibility: .visible) {
                Button("Выйти из аккаунта", role: .destructive) {
                    holder.sendEvent(ProfileEvent.OnLogoutClick())
                }
                Button("Отмена", role: .cancel) { }
            } message: {
                Text("Для работы с домохозяйствами и запасами потребуется снова войти.")
            }
        case let state as ProfileState.Error:
            VStack(spacing: 8) {
                Text(state.message ?? "Ошибка загрузки")
                Button("Повторить") { holder.sendEvent(ProfileEvent.OnRetry()) }
                    .buttonStyle(.borderedProminent)
            }.frame(maxWidth: .infinity, maxHeight: .infinity)
        default:
            EmptyView()
        }
    }
}

private struct PreferenceOptionChips<Values: Sequence>: View {
    let title: String
    let options: [(id: String, name: String)]
    let selectedIds: Values
    let accessibilityPrefix: String
    let onToggle: (String) -> Void

    var body: some View {
        let selected = Set(selectedIds.map { String(describing: $0) })
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.caption.weight(.semibold))
            if options.isEmpty {
                Text("Не указано").font(.caption).foregroundStyle(.secondary)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
                        ForEach(options, id: \.id) { option in
                            Button(option.name) { onToggle(option.id) }
                                .buttonStyle(.bordered)
                                .tint(selected.contains(option.id) ? .green : .secondary)
                                .accessibilityIdentifier("\(accessibilityPrefix).\(option.id)")
                        }
                    }
                }
            }
        }
    }
}

private func selectedNames<Values: Sequence>(
    ids: Values,
    options: [(id: String, name: String)]
) -> [String] {
    let namesById = Dictionary(uniqueKeysWithValues: options.map { ($0.id, $0.name) })
    return ids.compactMap { namesById[String(describing: $0)] }.sorted()
}

private struct PreferenceRow<Values: Sequence>: View {
    let title: String
    let values: Values

    var body: some View {
        let text = values.map { String(describing: $0) }.sorted().joined(separator: ", ")
        HStack {
            Text(title)
            Spacer()
            Text(text.isEmpty ? "Не указано" : text)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.trailing)
        }
    }
}
