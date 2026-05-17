import SwiftUI

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @StateObject private var router = AppRouter()

    init() {
        DIContainer.initKoin()
        IOSRuntimeSmokeCoordinator.runIfRequested()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(router)
        }
    }
}

struct RootView: View {
    @EnvironmentObject var router: AppRouter
    @State private var isRestoringSession = ProcessInfo.processInfo.shouldRunStartupRestore

    var body: some View {
        Group {
            if isRestoringSession {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                NavigationStack(path: $router.path) {
                    rootScreen
                        .navigationDestination(for: AppRoute.self) { route in
                            screenFor(route)
                        }
                }
            }
        }
        .task {
            await restoreSessionIfNeeded()
        }
    }

    @MainActor
    private func restoreSessionIfNeeded() async {
        guard isRestoringSession else { return }
        let restored: Bool
        if let startupRestoreOverride = ProcessInfo.processInfo.startupRestoreOverride {
            restored = startupRestoreOverride
        } else {
            restored = (try? await DIContainer.shared.restoreSession()) ?? false
        }
        router.setRoot(restored ? .householdList : .login)
        isRestoringSession = false
    }

    @ViewBuilder
    private var rootScreen: some View {
        screenFor(router.root)
    }

    @ViewBuilder
    private func screenFor(_ route: AppRoute) -> some View {
        switch route {
        case .login:
            LoginScreen()
        case .register:
            RegisterScreen()
        case .householdList:
            HouseholdListScreen()
        case .productList(let id):
            ProductListScreen(householdId: id)
        case .addProduct(let id):
            AddProductScreen(householdId: id)
        case let .editProduct(id, productId):
            AddProductScreen(householdId: id, productId: productId)
        case let .addProductFromDraft(id, draft):
            AddProductScreen(householdId: id, draft: draft)
        case .categories(let id):
            CategoryManagementScreen(householdId: id)
        case .barcodeScan(let id):
            BarcodeScannerScreen(householdId: id)
        case .recipes(let id):
            RecipeListScreen(householdId: id)
        case .notifications:
            NotificationListScreen()
        case .profile:
            ProfileScreen()
        }
    }
}

private extension ProcessInfo {
    var shouldRunStartupRestore: Bool {
        startupRestoreOverride != nil || !arguments.contains("--ios-ui-test")
    }

    var startupRestoreOverride: Bool? {
        #if DEBUG
        if arguments.contains("--ios-ui-test-restore-success") {
            return true
        }
        if arguments.contains("--ios-ui-test-restore-failure") {
            return false
        }
        #endif
        return nil
    }
}
