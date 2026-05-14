import SwiftUI

@main
struct iOSApp: App {
    @StateObject private var router = AppRouter()

    init() {
        DIContainer.initKoin()
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

    var body: some View {
        NavigationStack(path: $router.path) {
            rootScreen
                .navigationDestination(for: AppRoute.self) { route in
                    screenFor(route)
                }
        }
    }

    @ViewBuilder
    private var rootScreen: some View {
        switch router.root {
        case .login:
            LoginScreen()
        case .householdList:
            HouseholdListScreen()
        default:
            LoginScreen()
        }
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
        case let .addProductFromDraft(id, draft):
            AddProductScreen(householdId: id, draft: draft)
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
