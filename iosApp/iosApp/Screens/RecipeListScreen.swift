import SwiftUI
import Shared

struct RecipeListScreen: View {
    let householdId: String

    @StateObject private var holder = SharedVMHolder<RecipeListState, RecipeListEvent, RecipeListAction, RecipeListViewModel>(
        viewModel: DIContainer.shared.recipeListViewModel(),
        initialState: RecipeListState.Loading()
    )
    @EnvironmentObject private var router: AppRouter

    var body: some View {
        content
            .task {
                holder.start { [weak router] action in
                    guard let router = router else { return }
                    DispatchQueue.main.async {
                        switch action {
                        case is RecipeListAction.NavigateBack:
                            router.pop()
                        default:
                            break
                        }
                    }
                }
                holder.sendEvent(RecipeListEvent.OnCreate(householdId: householdId))
            }
            .navigationTitle("Рецепты")
            .sheet(isPresented: Binding(
                get: { holder.state.recipeIngredientDialogVisible },
                set: { visible in
                    if !visible {
                        holder.sendEvent(RecipeListEvent.OnIngredientDialogDismissed())
                    }
                }
            )) {
                RecipeIngredientSelectionSheet(
                    options: holder.state.recipeIngredientOptions,
                    selectedIds: holder.state.recipeSelectedIngredientIds,
                    loading: holder.state.recipeIngredientOptionsLoading,
                    error: holder.state.recipeIngredientOptionsError,
                    onToggle: { holder.sendEvent(RecipeListEvent.OnIngredientSelectionChanged(ingredientId: $0)) },
                    onFindSelected: { holder.sendEvent(RecipeListEvent.OnFindSelectedRecipeClick()) },
                    onRandom: { holder.sendEvent(RecipeListEvent.OnRandomRecipeClick()) },
                    onDismiss: { holder.sendEvent(RecipeListEvent.OnIngredientDialogDismissed()) }
                )
            }
    }

    @ViewBuilder
    private var content: some View {
        switch holder.state {
        case is RecipeListState.Loading:
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        case let state as RecipeListState.Content:
            VStack(spacing: 12) {
                RecipeTabs(
                    selectedTabName: state.selectedRecipeTabName,
                    onDiscover: { holder.sendEvent(RecipeListEvent.OnDiscoverTabClick()) },
                    onLiked: { holder.sendEvent(RecipeListEvent.OnLikedTabClick()) }
                )
                if state.selectedRecipeTabName != "LIKED" {
                    RecipeActionBar(
                        onGenerateCurrent: { holder.sendEvent(RecipeListEvent.OnGenerateFromCurrentProductsClick()) },
                        onUseSoon: { holder.sendEvent(RecipeListEvent.OnUseSoonClick()) }
                    )
                }
                List(state.recipes, id: \.identityKey) { recipe in
                    RecipeRow(
                        recipe: recipe,
                        liked: state.recipeLikedIds.contains(recipe.identityKey),
                        onLike: { holder.sendEvent(RecipeListEvent.OnRecipeLikeClick(recipe: recipe)) }
                    )
                }
            }
        case let state as RecipeListState.Empty:
            VStack(spacing: 12) {
                RecipeTabs(
                    selectedTabName: state.selectedRecipeTabName,
                    onDiscover: { holder.sendEvent(RecipeListEvent.OnDiscoverTabClick()) },
                    onLiked: { holder.sendEvent(RecipeListEvent.OnLikedTabClick()) }
                )
                if state.selectedRecipeTabName != "LIKED" {
                    RecipeActionBar(
                        onGenerateCurrent: { holder.sendEvent(RecipeListEvent.OnGenerateFromCurrentProductsClick()) },
                        onUseSoon: { holder.sendEvent(RecipeListEvent.OnUseSoonClick()) }
                    )
                }
                InventoryEmptyState(
                    title: state.selectedRecipeTabName == "LIKED" ? "Нет понравившихся рецептов" : "Нет подходящих рецептов",
                    message: state.selectedRecipeTabName == "LIKED"
                        ? "Нажмите «В понравившиеся» на рецепте, чтобы сохранить его только на этом устройстве."
                        : "В текущих запасах не нашлось сочетаний для рекомендаций. Добавьте продукты или измените остатки.",
                    systemImage: "sparkles",
                    primaryTitle: "Найти рецепт",
                    primaryAction: { holder.sendEvent(RecipeListEvent.OnGenerateFromCurrentProductsClick()) }
                )
            }
        case let state as RecipeListState.Error:
            VStack(spacing: 8) {
                Text(state.message ?? "Ошибка загрузки")
                Button("Повторить") { holder.sendEvent(RecipeListEvent.OnRetry()) }
                    .buttonStyle(.borderedProminent)
            }.frame(maxWidth: .infinity, maxHeight: .infinity)
        default:
            EmptyView()
        }
    }
}

struct RecipeTabs: View {
    let selectedTabName: String
    let onDiscover: () -> Void
    let onLiked: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            if selectedTabName == "DISCOVER" {
                Button("Поиск", action: onDiscover)
                    .buttonStyle(.borderedProminent)
            } else {
                Button("Поиск", action: onDiscover)
                    .buttonStyle(.bordered)
            }
            if selectedTabName == "LIKED" {
                Button("Понравившиеся", action: onLiked)
                    .buttonStyle(.borderedProminent)
            } else {
                Button("Понравившиеся", action: onLiked)
                    .buttonStyle(.bordered)
            }
        }
        .padding(.horizontal)
        .padding(.top)
    }
}

struct RecipeActionBar: View {
    let onGenerateCurrent: () -> Void
    let onUseSoon: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            Button("Найти рецепт", action: onGenerateCurrent)
                .buttonStyle(.borderedProminent)
            Button("Использовать скоро", action: onUseSoon)
                .buttonStyle(.bordered)
        }
        .padding(.horizontal)
        .padding(.top)
    }
}

struct RecipeRow: View {
    let recipe: Recipe
    let liked: Bool
    let onLike: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(recipe.title).font(.headline)
            }
            Button(action: onLike) {
                Label(
                    liked ? "Убрать из понравившихся" : "В понравившиеся",
                    systemImage: liked ? "heart.fill" : "heart"
                )
            }
            .font(.caption)
            .buttonStyle(.bordered)
            Text("\(recipe.time) • \(recipe.caloriesLabel)").font(.subheadline).foregroundColor(.secondary)
            RecipeStringList(title: "Скоро использовать:", values: recipe.usedExpiringProducts)
            RecipeStringList(title: "Использует:", values: recipe.usedHouseholdProducts)
            RecipeStringList(title: "Не хватает:", values: recipe.missingIngredients)
            RecipeStringList(title: "Почему подходит:", values: recipe.reasons.mapUserFacingRecipeNotes())
            RecipeStringList(title: "Предупреждения:", values: recipe.warnings.mapUserFacingRecipeNotes())
            Text("Ингредиенты:").font(.caption).fontWeight(.semibold)
            ForEach(recipe.ingredients, id: \.name) { ingredient in
                Text("- \(ingredient.name): \(ingredient.amount)").font(.caption)
            }
            Text("Приготовление:").font(.caption).fontWeight(.semibold)
            ForEach(Array(recipe.steps.enumerated()), id: \.offset) { index, step in
                Text("\(index + 1). \(step)").font(.caption)
            }
        }.padding(.vertical, 4)
            .accessibilityElement(children: .combine)
            .accessibilityLabel("\(recipe.title), \(recipe.time), \(recipe.caloriesAccessibilityLabel)")
    }
}

private extension Array where Element == String {
    func mapUserFacingRecipeNotes() -> [String] {
        self.compactMap { $0.userFacingRecipeNote }.uniqued()
    }
}

private extension String {
    var userFacingRecipeNote: String? {
        let value = self
            .replacingOccurrences(of: "AI-Assisted: ", with: "")
            .replacingOccurrences(of: "Рецепт создан ИИ. ", with: "")
            .replacingOccurrences(of: "Рецепт создан ИИ, потому что ", with: "")
            .replacingOccurrences(of: "Рецепт создан ИИ по ", with: "Рецепт создан по ")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? nil : value
    }
}

private extension Array where Element: Hashable {
    func uniqued() -> [Element] {
        var seen = Set<Element>()
        return filter { seen.insert($0).inserted }
    }
}

struct RecipeIngredientSelectionSheet: View {
    let options: [RecipeIngredientOption]
    let selectedIds: Set<String>
    let loading: Bool
    let error: String?
    let onToggle: (String) -> Void
    let onFindSelected: () -> Void
    let onRandom: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        NavigationStack {
            VStack(spacing: 12) {
                Text("Отметьте продукты, из которых нужно найти рецепт.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if loading {
                    ProgressView()
                } else if let error {
                    Text(error).foregroundStyle(.red)
                } else if options.isEmpty {
                    Text("Нет доступных продуктов").foregroundStyle(.secondary)
                } else {
                    List(options, id: \.id) { option in
                        Button {
                            onToggle(option.id)
                        } label: {
                            HStack {
                                Image(systemName: selectedIds.contains(option.id) ? "checkmark.circle.fill" : "circle")
                                VStack(alignment: .leading) {
                                    Text(option.name)
                                    Text(option.subtitle)
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                            }
                        }
                    }
                }
                HStack {
                    Button("Рандомные рецепты", action: onRandom)
                        .buttonStyle(.bordered)
                    Spacer()
                    Button("Найти рецепт", action: onFindSelected)
                        .buttonStyle(.borderedProminent)
                        .disabled(selectedIds.isEmpty)
                }
            }
            .padding()
            .navigationTitle("Выберите продукты")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Закрыть", action: onDismiss)
                }
            }
        }
    }
}

struct RecipeStringList: View {
    let title: String
    let values: [String]

    var body: some View {
        if !values.isEmpty {
            Text(title).font(.caption).fontWeight(.semibold)
            ForEach(values, id: \.self) { value in
                Text("- \(value)").font(.caption)
            }
        }
    }
}

private extension RecipeListState {
    var recipeIngredientDialogVisible: Bool {
        switch self {
        case let state as RecipeListState.Content:
            return state.showIngredientDialog
        case let state as RecipeListState.Empty:
            return state.showIngredientDialog
        case let state as RecipeListState.Error:
            return state.showIngredientDialog
        default:
            return false
        }
    }

    var recipeIngredientOptions: [RecipeIngredientOption] {
        switch self {
        case let state as RecipeListState.Content:
            return state.ingredientOptions
        case let state as RecipeListState.Empty:
            return state.ingredientOptions
        case let state as RecipeListState.Error:
            return state.ingredientOptions
        default:
            return []
        }
    }

    var recipeSelectedIngredientIds: Set<String> {
        switch self {
        case let state as RecipeListState.Content:
            return Set(state.selectedIngredientIds.map { String(describing: $0) })
        case let state as RecipeListState.Empty:
            return Set(state.selectedIngredientIds.map { String(describing: $0) })
        case let state as RecipeListState.Error:
            return Set(state.selectedIngredientIds.map { String(describing: $0) })
        default:
            return []
        }
    }

    var recipeIngredientOptionsLoading: Bool {
        switch self {
        case let state as RecipeListState.Content:
            return state.ingredientOptionsLoading
        case let state as RecipeListState.Empty:
            return state.ingredientOptionsLoading
        case let state as RecipeListState.Error:
            return state.ingredientOptionsLoading
        default:
            return false
        }
    }

    var recipeIngredientOptionsError: String? {
        switch self {
        case let state as RecipeListState.Content:
            return state.ingredientOptionsError
        case let state as RecipeListState.Empty:
            return state.ingredientOptionsError
        case let state as RecipeListState.Error:
            return state.ingredientOptionsError
        default:
            return nil
        }
    }

    var selectedRecipeTabName: String {
        switch self {
        case let state as RecipeListState.Content:
            return state.selectedTab.name
        case let state as RecipeListState.Empty:
            return state.selectedTab.name
        case let state as RecipeListState.Error:
            return state.selectedTab.name
        default:
            return "DISCOVER"
        }
    }

    var recipeLikedIds: Set<String> {
        switch self {
        case let state as RecipeListState.Content:
            return Set(state.likedRecipeIds.map { String(describing: $0) })
        case let state as RecipeListState.Empty:
            return Set(state.likedRecipeIds.map { String(describing: $0) })
        case let state as RecipeListState.Error:
            return Set(state.likedRecipeIds.map { String(describing: $0) })
        default:
            return []
        }
    }
}

private extension Recipe {
    var identityKey: String {
        localIdentity
    }

    var caloriesLabel: String {
        caloriesKnown ? "\(calories) ккал" : "ккал неизвестно"
    }

    var caloriesAccessibilityLabel: String {
        caloriesKnown ? "\(calories) килокалорий" : "калорийность неизвестна"
    }
}

private extension RecipeIngredientOption {
    var subtitle: String {
        [categoryName, "\(remainingAmount) \(unit)", expiring ? "Использовать скоро" : nil]
            .compactMap { $0 }
            .joined(separator: " • ")
    }
}
