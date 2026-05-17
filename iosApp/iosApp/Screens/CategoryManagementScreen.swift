import SwiftUI
import Shared

struct CategoryManagementScreen: View {
    let householdId: String

    @StateObject private var holder = SharedVMHolder<CategoryManagementState, CategoryManagementEvent, CategoryManagementAction, CategoryManagementViewModel>(
        viewModel: DIContainer.shared.categoryManagementViewModel(),
        initialState: CategoryManagementState(
            categories: [],
            editableNames: [:],
            newCategoryName: "",
            isLoading: true,
            isSaving: false,
            error: nil
        )
    )
    @EnvironmentObject private var router: AppRouter

    var body: some View {
        content
            .task {
                holder.start()
                holder.sendEvent(CategoryManagementEvent.OnCreate(householdId: householdId))
            }
            .navigationTitle("Категории")
    }

    @ViewBuilder
    private var content: some View {
        if holder.state.isLoading {
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            Form {
                Section("Новая категория") {
                    TextField("Название", text: Binding(
                        get: { holder.state.newCategoryName },
                        set: { holder.sendEvent(CategoryManagementEvent.OnNewCategoryNameChanged(name: $0)) }
                    ))
                    Button(action: { holder.sendEvent(CategoryManagementEvent.OnCreateCategoryClick()) }) {
                        if holder.state.isSaving {
                            ProgressView()
                        } else {
                            Text("Создать")
                        }
                    }
                    .disabled(holder.state.isSaving || holder.state.newCategoryName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }

                if let error = holder.state.error {
                    Section { Text(error).foregroundColor(.red) }
                }

                Section("Системные") {
                    ForEach(holder.state.categories.filter(\.system), id: \.id) { category in
                        HStack {
                            Text(categoryDisplayName(category))
                            Spacer()
                            Text("Системная")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }

                Section("Свои") {
                    let customCategories = holder.state.categories.filter { !$0.system }
                    if customCategories.isEmpty {
                        Text("Нет своих категорий").foregroundColor(.secondary)
                    } else {
                        ForEach(customCategories, id: \.id) { category in
                            CustomCategoryRow(
                                category: category,
                                name: holder.state.editableNames[category.id] ?? category.name,
                                isSaving: holder.state.isSaving,
                                onNameChanged: {
                                    holder.sendEvent(CategoryManagementEvent.OnCategoryNameChanged(categoryId: category.id, name: $0))
                                },
                                onSave: {
                                    holder.sendEvent(CategoryManagementEvent.OnUpdateCategoryClick(categoryId: category.id))
                                },
                                onArchive: {
                                    holder.sendEvent(CategoryManagementEvent.OnArchiveCategoryClick(categoryId: category.id))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private struct CustomCategoryRow: View {
    let category: ProductCategoryOption
    let name: String
    let isSaving: Bool
    let onNameChanged: (String) -> Void
    let onSave: () -> Void
    let onArchive: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            TextField("Название", text: Binding(get: { name }, set: onNameChanged))
            HStack {
                Button("Сохранить", action: onSave).disabled(isSaving)
                Button("Архивировать", role: .destructive, action: onArchive).disabled(isSaving)
            }
        }
    }
}
