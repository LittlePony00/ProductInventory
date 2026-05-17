import Shared

final class DIContainer {
    static let shared = DIContainer()
    private let koinHelper = KoinHelper()

    private init() {}

    static func initKoin() {
        KoinHelperKt.doInitKoin()
    }

    func loginViewModel() -> LoginViewModel { koinHelper.loginViewModel() }
    func registerViewModel() -> RegisterViewModel { koinHelper.registerViewModel() }
    func householdListViewModel() -> HouseholdListViewModel { koinHelper.householdListViewModel() }
    func productListViewModel() -> ProductListViewModel { koinHelper.productListViewModel() }
    func addProductViewModel() -> AddProductViewModel { koinHelper.addProductViewModel() }
    func categoryManagementViewModel() -> CategoryManagementViewModel { koinHelper.categoryManagementViewModel() }
    func barcodeScanViewModel() -> BarcodeScanViewModel { koinHelper.barcodeScanViewModel() }
    func profileViewModel() -> ProfileViewModel { koinHelper.profileViewModel() }
    func notificationListViewModel() -> NotificationListViewModel { koinHelper.notificationListViewModel() }
    func recipeListViewModel() -> RecipeListViewModel { koinHelper.recipeListViewModel() }
    func restoreSession() async throws -> Bool { try await koinHelper.restoreSession().boolValue }
    func cacheIosPushToken(_ token: String) { koinHelper.cacheIosPushToken(token: token) }
    func registerIosPushToken(_ token: String) async throws {
        try await koinHelper.registerIosPushToken(token: token)
    }
}
