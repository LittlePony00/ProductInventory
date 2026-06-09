import SwiftUI
import Shared

struct RecipeLoadingView: View {
    var body: some View {
        ProgressView()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(.systemGroupedBackground))
    }
}

struct RecipeErrorView: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 34, weight: .semibold))
                .foregroundStyle(.orange)
            Text(message)
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
            Button("Повторить", action: onRetry)
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemGroupedBackground))
    }
}

struct RecipeScreenScaffold<Content: View>: View {
    let selectedTabName: String
    let onDiscover: () -> Void
    let onLiked: () -> Void
    let onGenerateCurrent: () -> Void
    let onUseSoon: () -> Void
    let showsActionBar: Bool
    let content: () -> Content

    init(
        selectedTabName: String,
        onDiscover: @escaping () -> Void,
        onLiked: @escaping () -> Void,
        onGenerateCurrent: @escaping () -> Void,
        onUseSoon: @escaping () -> Void,
        showsActionBar: Bool,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.selectedTabName = selectedTabName
        self.onDiscover = onDiscover
        self.onLiked = onLiked
        self.onGenerateCurrent = onGenerateCurrent
        self.onUseSoon = onUseSoon
        self.showsActionBar = showsActionBar
        self.content = content
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                RecipeTabs(
                    selectedTabName: selectedTabName,
                    onDiscover: onDiscover,
                    onLiked: onLiked
                )
                if showsActionBar {
                    RecipeActionBar(
                        onGenerateCurrent: onGenerateCurrent,
                        onUseSoon: onUseSoon
                    )
                }
                content()
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 28)
        }
        .background(Color(.systemGroupedBackground))
    }
}

struct RecipeTabs: View {
    let selectedTabName: String
    let onDiscover: () -> Void
    let onLiked: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            RecipeTabButton(
                title: "Поиск",
                systemImage: "sparkles",
                selected: selectedTabName == "DISCOVER",
                action: onDiscover
            )
            RecipeTabButton(
                title: "Понравившиеся",
                systemImage: "heart",
                selected: selectedTabName == "LIKED",
                action: onLiked
            )
        }
        .padding(4)
        .background(Color(.secondarySystemGroupedBackground), in: Capsule())
    }
}

private struct RecipeTabButton: View {
    let title: String
    let systemImage: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .font(.subheadline.weight(.semibold))
                .lineLimit(1)
                .minimumScaleFactor(0.85)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .foregroundStyle(selected ? .white : .primary)
                .background(selected ? Color.accentColor : Color.clear, in: Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

struct RecipeActionBar: View {
    let onGenerateCurrent: () -> Void
    let onUseSoon: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Подобрать рецепт")
                .font(.headline)
            VStack(spacing: 10) {
                Button(action: onGenerateCurrent) {
                    Label("Найти рецепт", systemImage: "magnifyingglass")
                        .lineLimit(1)
                        .minimumScaleFactor(0.85)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)

                Button(action: onUseSoon) {
                    Label("Использовать скоро", systemImage: "clock")
                        .lineLimit(1)
                        .minimumScaleFactor(0.85)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .controlSize(.large)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

struct RecipeRow: View {
    let recipe: Recipe
    let liked: Bool
    let onLike: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(recipe.title)
                        .font(.headline)
                        .fixedSize(horizontal: false, vertical: true)
                    Text("\(recipe.time) • \(recipe.caloriesLabel)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer(minLength: 8)
                Button(action: onLike) {
                    Image(systemName: liked ? "heart.fill" : "heart")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(liked ? .red : .secondary)
                        .frame(width: 38, height: 38)
                        .background(Color(.tertiarySystemGroupedBackground), in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(liked ? "Убрать из понравившихся" : "В понравившиеся")
            }

            RecipeInfoSection(
                title: "Скоро использовать",
                systemImage: "clock.badge.exclamationmark",
                values: recipe.usedExpiringProducts,
                tone: .warning
            )
            RecipeInfoSection(
                title: "Использует",
                systemImage: "basket",
                values: recipe.usedHouseholdProducts,
                tone: .success
            )
            RecipeInfoSection(
                title: "Не хватает",
                systemImage: "cart.badge.plus",
                values: recipe.missingIngredients,
                tone: .danger
            )
            RecipeInfoSection(
                title: "Почему подходит",
                systemImage: "checkmark.seal",
                values: recipe.reasons.mapUserFacingRecipeNotes(),
                tone: .neutral
            )
            RecipeInfoSection(
                title: "Предупреждения",
                systemImage: "exclamationmark.triangle",
                values: recipe.warnings.mapUserFacingRecipeNotes(),
                tone: .warning
            )
            RecipeIngredientsSection(ingredients: recipe.ingredients)
            RecipeStepsSection(steps: recipe.steps)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .stroke(Color.black.opacity(0.04), lineWidth: 1)
        )
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(recipe.title), \(recipe.time), \(recipe.caloriesAccessibilityLabel)")
    }
}

private struct RecipeInfoSection: View {
    let title: String
    let systemImage: String
    let values: [String]
    let tone: InventoryTone

    var body: some View {
        if !values.isEmpty {
            VStack(alignment: .leading, spacing: 7) {
                Label(title, systemImage: systemImage)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(color)
                VStack(alignment: .leading, spacing: 4) {
                    ForEach(values, id: \.self) { value in
                        Text("• \(value)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }
        }
    }

    private var color: Color {
        switch tone {
        case .neutral: return .secondary
        case .success: return .green
        case .warning: return .orange
        case .danger: return .red
        }
    }
}

private struct RecipeIngredientsSection: View {
    let ingredients: [RecipeIngredient]

    var body: some View {
        if !ingredients.isEmpty {
            VStack(alignment: .leading, spacing: 7) {
                Label("Ингредиенты", systemImage: "list.bullet")
                    .font(.caption.weight(.semibold))
                VStack(alignment: .leading, spacing: 4) {
                    ForEach(ingredients, id: \.name) { ingredient in
                        Text("• \(ingredient.name): \(ingredient.amount)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }
        }
    }
}

private struct RecipeStepsSection: View {
    let steps: [String]

    var body: some View {
        if !steps.isEmpty {
            VStack(alignment: .leading, spacing: 9) {
                Label("Приготовление", systemImage: "fork.knife")
                    .font(.caption.weight(.semibold))
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(Array(steps.enumerated()), id: \.offset) { index, step in
                        HStack(alignment: .top, spacing: 8) {
                            Text("\(index + 1)")
                                .font(.caption2.weight(.bold))
                                .foregroundStyle(.white)
                                .frame(width: 22, height: 22)
                                .background(Color.accentColor, in: Circle())
                            Text(step)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                }
            }
        }
    }
}

struct RecipeEmptyCard: View {
    let title: String
    let message: String
    let systemImage: String
    let primaryTitle: String
    let primaryAction: () -> Void

    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: systemImage)
                .font(.system(size: 34, weight: .semibold))
                .foregroundStyle(.green)
                .frame(width: 72, height: 72)
                .background(Color.green.opacity(0.14), in: Circle())
            Text(title)
                .font(.title3.weight(.semibold))
                .multilineTextAlignment(.center)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            Button(primaryTitle, action: primaryAction)
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
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
            Group {
                if loading {
                    RecipeSheetStatusView(
                        systemImage: "hourglass",
                        title: "Загружаем продукты",
                        message: "Сейчас покажем доступные остатки."
                    ) {
                        ProgressView()
                    }
                } else if let error {
                    RecipeSheetStatusView(
                        systemImage: "exclamationmark.triangle.fill",
                        title: "Не удалось загрузить продукты",
                        message: error,
                        tint: .orange
                    )
                } else if options.isEmpty {
                    RecipeSheetStatusView(
                        systemImage: "basket",
                        title: "Нет доступных продуктов",
                        message: "Добавьте продукты в домохозяйство или запустите случайную подборку.",
                        tint: .secondary
                    )
                } else {
                    List {
                        Section {
                            ForEach(options, id: \.id) { option in
                                Button {
                                    onToggle(option.id)
                                } label: {
                                    RecipeIngredientOptionRow(
                                        option: option,
                                        selected: selectedIds.contains(option.id)
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        } header: {
                            Text("Отметьте продукты, из которых нужно найти рецепт.")
                        }
                    }
                    .listStyle(.insetGrouped)
                }
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("Выберите продукты")
            .navigationBarTitleDisplayMode(.inline)
            .safeAreaInset(edge: .bottom) {
                RecipeSheetActionBar(
                    selectedCount: selectedIds.count,
                    onFindSelected: onFindSelected,
                    onRandom: onRandom
                )
            }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Закрыть", action: onDismiss)
                }
            }
        }
    }
}

private struct RecipeIngredientOptionRow: View {
    let option: RecipeIngredientOption
    let selected: Bool

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                .font(.title3)
                .foregroundStyle(selected ? .green : .secondary)
            VStack(alignment: .leading, spacing: 4) {
                Text(option.name)
                    .font(.body.weight(.semibold))
                    .foregroundStyle(.primary)
                Text(option.subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
        }
        .padding(.vertical, 6)
        .contentShape(Rectangle())
    }
}

private struct RecipeSheetStatusView<Accessory: View>: View {
    let systemImage: String
    let title: String
    let message: String
    var tint: Color = .green
    let accessory: Accessory

    init(
        systemImage: String,
        title: String,
        message: String,
        tint: Color = .green,
        @ViewBuilder accessory: () -> Accessory = { EmptyView() }
    ) {
        self.systemImage = systemImage
        self.title = title
        self.message = message
        self.tint = tint
        self.accessory = accessory()
    }

    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: systemImage)
                .font(.system(size: 34, weight: .semibold))
                .foregroundStyle(tint)
                .frame(width: 72, height: 72)
                .background(tint.opacity(0.14), in: Circle())
            Text(title)
                .font(.headline)
                .multilineTextAlignment(.center)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            accessory
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct RecipeSheetActionBar: View {
    let selectedCount: Int
    let onFindSelected: () -> Void
    let onRandom: () -> Void

    var body: some View {
        VStack(spacing: 10) {
            Button(action: onRandom) {
                Label("Рандомные рецепты", systemImage: "shuffle")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .controlSize(.large)

            Button(action: onFindSelected) {
                Label(selectedCountText, systemImage: "magnifyingglass")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .disabled(selectedCount == 0)
        }
        .padding(.horizontal, 16)
        .padding(.top, 12)
        .padding(.bottom, 10)
        .background(.regularMaterial)
    }

    private var selectedCountText: String {
        selectedCount == 0 ? "Найти по выбранным" : "Найти по выбранным (\(selectedCount))"
    }
}

private extension Array where Element == String {
    func mapUserFacingRecipeNotes() -> [String] {
        compactMap { $0.userFacingRecipeNote }.uniqued()
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

extension Recipe {
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
