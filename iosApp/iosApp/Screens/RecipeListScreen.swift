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
                InventoryEmptyState(
                    title: "Нет рецептов",
                    message: "Сгенерируйте рекомендации из текущих запасов домохозяйства.",
                    systemImage: "sparkles",
                    primaryTitle: "Сгенерировать",
                    primaryAction: { holder.sendEvent(RecipeListEvent.OnGenerateClick()) }
                )
            } else {
                List(state.recipes, id: \.identityKey) { recipe in
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
            Text("\(recipe.time) • \(recipe.calories) kcal").font(.subheadline).foregroundColor(.secondary)
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
            .accessibilityLabel("\(recipe.title), \(recipe.time), \(recipe.calories) килокалорий")
    }
}

private extension Recipe {
    var identityKey: String {
        "\(title)|\(time)|\(calories)|\(ingredients.map(\.name).joined(separator: ","))|\(steps.joined(separator: ","))"
    }
}
