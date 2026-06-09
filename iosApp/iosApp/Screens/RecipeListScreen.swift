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
            .navigationBarTitleDisplayMode(.inline)
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
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
            }
    }

    @ViewBuilder
    private var content: some View {
        switch holder.state {
        case is RecipeListState.Loading:
            RecipeLoadingView()
        case let state as RecipeListState.Content:
            RecipeScreenScaffold(
                selectedTabName: state.selectedRecipeTabName,
                onDiscover: { holder.sendEvent(RecipeListEvent.OnDiscoverTabClick()) },
                onLiked: { holder.sendEvent(RecipeListEvent.OnLikedTabClick()) },
                onGenerateCurrent: { holder.sendEvent(RecipeListEvent.OnGenerateFromCurrentProductsClick()) },
                onUseSoon: { holder.sendEvent(RecipeListEvent.OnUseSoonClick()) },
                showsActionBar: state.selectedRecipeTabName != "LIKED"
            ) {
                LazyVStack(spacing: 14) {
                    ForEach(state.recipes, id: \.identityKey) { recipe in
                        RecipeRow(
                            recipe: recipe,
                            liked: state.recipeLikedIds.contains(recipe.identityKey),
                            onLike: { holder.sendEvent(RecipeListEvent.OnRecipeLikeClick(recipe: recipe)) }
                        )
                    }
                }
            }
        case let state as RecipeListState.Empty:
            RecipeScreenScaffold(
                selectedTabName: state.selectedRecipeTabName,
                onDiscover: { holder.sendEvent(RecipeListEvent.OnDiscoverTabClick()) },
                onLiked: { holder.sendEvent(RecipeListEvent.OnLikedTabClick()) },
                onGenerateCurrent: { holder.sendEvent(RecipeListEvent.OnGenerateFromCurrentProductsClick()) },
                onUseSoon: { holder.sendEvent(RecipeListEvent.OnUseSoonClick()) },
                showsActionBar: state.selectedRecipeTabName != "LIKED"
            ) {
                RecipeEmptyCard(
                    title: state.selectedRecipeTabName == "LIKED" ? "Нет понравившихся рецептов" : "Нет подходящих рецептов",
                    message: state.selectedRecipeTabName == "LIKED"
                        ? "Нажмите «В понравившиеся» на рецепте, чтобы сохранить его только на этом устройстве."
                        : "Добавьте продукты, измените остатки или попробуйте случайную подборку.",
                    systemImage: "sparkles",
                    primaryTitle: "Найти рецепт",
                    primaryAction: { holder.sendEvent(RecipeListEvent.OnGenerateFromCurrentProductsClick()) }
                )
            }
        case let state as RecipeListState.Error:
            RecipeErrorView(
                message: state.message ?? "Ошибка загрузки",
                onRetry: { holder.sendEvent(RecipeListEvent.OnRetry()) }
            )
        default:
            EmptyView()
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
