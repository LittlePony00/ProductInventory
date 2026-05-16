import SwiftUI
import Shared

struct HouseholdListScreen: View {
    @StateObject private var holder = SharedVMHolder<HouseholdListState, HouseholdListEvent, HouseholdListAction, HouseholdListViewModel>(
        viewModel: DIContainer.shared.householdListViewModel(),
        initialState: HouseholdListState.Loading()
    )
    @EnvironmentObject private var router: AppRouter
    @State private var showCreateDialog = false
    @State private var showJoinDialog = false
    @State private var createName = ""
    @State private var joinCode = ""
    @State private var inviteCodeDialog: InviteCodeDialogState?

    var body: some View {
        content
            .task {
                holder.start { [weak router] action in
                    guard let router = router else { return }
                    DispatchQueue.main.async {
                        switch action {
                        case let a as HouseholdListAction.OpenHousehold:
                            router.push(.productList(householdId: a.householdId))
                        case is HouseholdListAction.ShowCreateDialog:
                            showCreateDialog = true
                        case is HouseholdListAction.ShowJoinDialog:
                            showJoinDialog = true
                        case let a as HouseholdListAction.ShowInviteCode:
                            inviteCodeDialog = InviteCodeDialogState(code: a.code, expiresAt: a.expiresAt)
                        case is HouseholdListAction.OpenProfile:
                            router.push(.profile)
                        default:
                            break
                        }
                    }
                }
                holder.sendEvent(HouseholdListEvent.OnCreate())
            }
            .navigationTitle("Мои домохозяйства")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Профиль") {
                        holder.sendEvent(HouseholdListEvent.OnProfileClick())
                    }
                }
            }
            .alert("Новое домохозяйство", isPresented: $showCreateDialog) {
                TextField("Название", text: $createName)
                Button("Создать") {
                    holder.sendEvent(HouseholdListEvent.OnCreateHouseholdConfirm(name: createName))
                    createName = ""
                }
                Button("Отмена", role: .cancel) { createName = "" }
            }
            .alert("Присоединиться", isPresented: $showJoinDialog) {
                TextField("Код приглашения", text: $joinCode)
                Button("Вступить") {
                    holder.sendEvent(HouseholdListEvent.OnJoinHouseholdConfirm(inviteCode: joinCode))
                    joinCode = ""
                }
                Button("Отмена", role: .cancel) { joinCode = "" }
            }
            .alert(item: $inviteCodeDialog) { invite in
                Alert(
                    title: Text("Код приглашения"),
                    message: Text("\(invite.code)\nДействует до: \(String(invite.expiresAt.prefix(16)))"),
                    dismissButton: .default(Text("OK"))
                )
            }
    }

    @ViewBuilder
    private var content: some View {
        ZStack(alignment: .bottomTrailing) {
            switch holder.state {
            case is HouseholdListState.Loading:
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            case let state as HouseholdListState.Content:
                if state.households.isEmpty {
                    VStack(spacing: 8) {
                        Text("Нет домохозяйств").font(.headline)
                        Text("Создайте новое или присоединитесь по коду")
                            .font(.subheadline).foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List(state.households, id: \.id) { household in
                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(household.name).font(.headline)
                                Text("Создано: \(String(household.createdAt.prefix(10)))")
                                    .font(.caption).foregroundColor(.secondary)
                            }
                            Spacer()
                            Button("Код") {
                                holder.sendEvent(HouseholdListEvent.OnGenerateInviteCodeClick(householdId: household.id))
                            }
                            .buttonStyle(.bordered)
                            Button("Открыть") {
                                holder.sendEvent(HouseholdListEvent.OnHouseholdClick(householdId: household.id))
                            }
                            .buttonStyle(.borderedProminent)
                        }
                    }
                }
            case let state as HouseholdListState.Error:
                VStack(spacing: 8) {
                    Text(state.message ?? "Ошибка загрузки")
                    Button("Повторить") { holder.sendEvent(HouseholdListEvent.OnRetry()) }
                        .buttonStyle(.borderedProminent)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            default:
                EmptyView()
            }

            VStack(spacing: 12) {
                Button { holder.sendEvent(HouseholdListEvent.OnJoinHouseholdClick()) } label: {
                    Image(systemName: "arrow.up.right").padding(12)
                        .background(Color.gray.opacity(0.2)).clipShape(Circle())
                }
                Button { holder.sendEvent(HouseholdListEvent.OnCreateHouseholdClick()) } label: {
                    Image(systemName: "plus").font(.title2).padding(16)
                        .background(Color.accentColor).foregroundColor(.white).clipShape(Circle())
                }
            }.padding()
        }
    }
}

private struct InviteCodeDialogState: Identifiable {
    let id = UUID()
    let code: String
    let expiresAt: String
}
