import SwiftUI
import Shared

struct ProfileScreen: View {
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
                holder.sendEvent(ProfileEvent.OnCreate())
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
                Section("Email") { Text(state.profile.email) }
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
