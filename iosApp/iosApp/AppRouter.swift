import SwiftUI

enum AppRoute: Hashable {
    case login
    case register
    case householdList
    case productList(householdId: String)
    case addProduct(householdId: String)
    case recipes(householdId: String)
    case notifications
    case profile
}

final class AppRouter: ObservableObject {
    @Published var path: [AppRoute] = []
    @Published var root: AppRoute = .login

    func push(_ route: AppRoute) {
        path.append(route)
    }

    func pop() {
        if !path.isEmpty {
            path.removeLast()
        }
    }

    func setRoot(_ route: AppRoute) {
        path.removeAll()
        root = route
    }
}
