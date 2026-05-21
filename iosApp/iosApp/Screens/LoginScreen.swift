import SwiftUI
import Shared

struct LoginScreen: View {
    @StateObject private var holder = SharedVMHolder<LoginState, LoginEvent, LoginAction, LoginViewModel>(
        viewModel: DIContainer.shared.loginViewModel(),
        initialState: LoginState.Input(email: "", password: "", isLoading: false)
    )
    @EnvironmentObject private var router: AppRouter

    var body: some View {
        content
            .task {
                holder.start { [weak router] action in
                    guard let router = router else { return }
                    DispatchQueue.main.async {
                        switch action {
                        case is LoginAction.NavigateToHome:
                            router.setRoot(.householdList)
                        case is LoginAction.NavigateToRegister:
                            router.push(.register)
                        default:
                            break
                        }
                    }
                }
            }
            .navigationBarHidden(true)
    }

    private var content: some View {
        let inputState = holder.state as? LoginState.Input

        return VStack(spacing: 24) {
            Spacer()

            Text("Учёт продуктов")
                .font(.largeTitle)
                .fontWeight(.bold)

            Text("Войдите в аккаунт")
                .font(.subheadline)
                .foregroundColor(.secondary)

            VStack(spacing: 16) {
                TextField("Эл. почта", text: Binding(
                    get: { inputState?.email ?? "" },
                    set: { holder.sendEvent(LoginEvent.OnEmailChanged(email: $0)) }
                ))
                .keyboardType(.emailAddress)
                .autocapitalization(.none)
                .textFieldStyle(.roundedBorder)
                .accessibilityIdentifier("login.email")

                SecureField("Пароль", text: Binding(
                    get: { inputState?.password ?? "" },
                    set: { holder.sendEvent(LoginEvent.OnPasswordChanged(password: $0)) }
                ))
                .textFieldStyle(.roundedBorder)
                .accessibilityIdentifier("login.password")
            }
            .padding(.horizontal)

            if let errorState = holder.state as? LoginState.Error {
                Text(errorState.message ?? "Ошибка")
                    .foregroundColor(.red)
                    .font(.caption)
            }

            Button(action: {
                holder.sendEvent(LoginEvent.OnLoginClick())
            }) {
                if inputState?.isLoading == true {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                } else {
                    Text("Войти")
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(inputState?.isLoading == true || inputState?.email.isEmpty != false || inputState?.password.isEmpty != false)
            .padding(.horizontal)
            .accessibilityIdentifier("login.submit")
            .accessibilityHint("Вход доступен после ввода email и пароля")

            Button("Нет аккаунта? Зарегистрироваться") {
                holder.sendEvent(LoginEvent.OnRegisterClick())
            }
            .font(.footnote)
            .accessibilityIdentifier("login.register")

            Spacer()
        }
        .padding()
    }
}
