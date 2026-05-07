import SwiftUI
import Shared

struct RegisterScreen: View {
    @StateObject private var holder = SharedVMHolder<RegisterState, RegisterEvent, RegisterAction, RegisterViewModel>(
        viewModel: DIContainer.shared.registerViewModel(),
        initialState: RegisterState.Input(email: "", password: "", name: "", isLoading: false)
    )
    @EnvironmentObject private var router: AppRouter

    var body: some View {
        content
            .task {
                holder.start { [weak router] action in
                    guard let router = router else { return }
                    DispatchQueue.main.async {
                        switch action {
                        case is RegisterAction.NavigateToHome:
                            router.setRoot(.householdList)
                        case is RegisterAction.NavigateBack:
                            router.pop()
                        default:
                            break
                        }
                    }
                }
            }
            .navigationBarBackButtonHidden(true)
    }

    private var content: some View {
        let inputState = holder.state as? RegisterState.Input

        return VStack(spacing: 24) {
            Spacer()

            Text("Регистрация")
                .font(.largeTitle)
                .fontWeight(.bold)

            VStack(spacing: 16) {
                TextField("Имя", text: Binding(
                    get: { inputState?.name ?? "" },
                    set: { holder.sendEvent(RegisterEvent.OnNameChanged(name: $0)) }
                ))
                .textFieldStyle(.roundedBorder)

                TextField("Email", text: Binding(
                    get: { inputState?.email ?? "" },
                    set: { holder.sendEvent(RegisterEvent.OnEmailChanged(email: $0)) }
                ))
                .keyboardType(.emailAddress)
                .autocapitalization(.none)
                .textFieldStyle(.roundedBorder)

                SecureField("Пароль", text: Binding(
                    get: { inputState?.password ?? "" },
                    set: { holder.sendEvent(RegisterEvent.OnPasswordChanged(password: $0)) }
                ))
                .textFieldStyle(.roundedBorder)
            }
            .padding(.horizontal)

            if let errorState = holder.state as? RegisterState.Error {
                Text(errorState.message ?? "Ошибка")
                    .foregroundColor(.red)
                    .font(.caption)
            }

            Button(action: {
                holder.sendEvent(RegisterEvent.OnRegisterClick())
            }) {
                if inputState?.isLoading == true {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                } else {
                    Text("Зарегистрироваться")
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(inputState?.isLoading == true)
            .padding(.horizontal)

            Button("Уже есть аккаунт? Войти") {
                holder.sendEvent(RegisterEvent.OnBackToLogin())
            }
            .font(.footnote)

            Spacer()
        }
        .padding()
    }
}
