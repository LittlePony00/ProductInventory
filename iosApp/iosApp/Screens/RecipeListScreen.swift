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
    }

    @ViewBuilder
    private var content: some View {
        switch holder.state {
        case is RecipeListState.Loading:
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        case let state as RecipeListState.Content:
            if state.recipes.isEmpty {
                Text("Нет рецептов").font(.headline)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(state.recipes, id: \.id) { recipe in
                    RecipeRow(recipe: recipe)
                }
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

struct RecipeRow: View {
    let recipe: Recipe

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(recipe.title).font(.headline)
            Text(recipe.description_).font(.subheadline).foregroundColor(.secondary)
            Text("Ингредиенты:").font(.caption).fontWeight(.semibold)
            ForEach(recipe.ingredients, id: \.self) { ingredient in
                Text("  • \(ingredient)").font(.caption)
            }
            Text("Приготовление:").font(.caption).fontWeight(.semibold)
            Text(recipe.instructions).font(.caption)
        }.padding(.vertical, 4)
    }
}
