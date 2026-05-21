import SwiftUI
import Shared

struct ProductDraftInput: Hashable {
    let barcode: String
    let name: String?
    let brand: String?
    let category: String?
    let packageQuantity: Double?
    let packageQuantityUnit: String?
    let ingredients: String?
    let imageUrl: String?
    let localImagePath: String?
    let caloriesKcal: Double?
    let proteinGrams: Double?
    let fatGrams: Double?
    let carbohydratesGrams: Double?

    init(
        barcode: String,
        name: String? = nil,
        brand: String? = nil,
        category: String? = nil,
        packageQuantity: Double? = nil,
        packageQuantityUnit: String? = nil,
        ingredients: String? = nil,
        imageUrl: String? = nil,
        localImagePath: String? = nil,
        caloriesKcal: Double? = nil,
        proteinGrams: Double? = nil,
        fatGrams: Double? = nil,
        carbohydratesGrams: Double? = nil
    ) {
        self.barcode = barcode
        self.name = name
        self.brand = brand
        self.category = category
        self.packageQuantity = packageQuantity
        self.packageQuantityUnit = packageQuantityUnit
        self.ingredients = ingredients
        self.imageUrl = imageUrl
        self.localImagePath = localImagePath
        self.caloriesKcal = caloriesKcal
        self.proteinGrams = proteinGrams
        self.fatGrams = fatGrams
        self.carbohydratesGrams = carbohydratesGrams
    }

    init(draft: BarcodeProductDraft) {
        self.init(
            barcode: draft.barcode,
            name: draft.name,
            brand: draft.brand,
            category: draft.category?.name,
            packageQuantity: draft.packageQuantity?.doubleValue,
            packageQuantityUnit: draft.packageQuantityUnit?.name,
            ingredients: draft.ingredients,
            imageUrl: draft.imageUrl,
            localImagePath: draft.localImagePath,
            caloriesKcal: draft.caloriesKcal?.doubleValue,
            proteinGrams: draft.proteinGrams?.doubleValue,
            fatGrams: draft.fatGrams?.doubleValue,
            carbohydratesGrams: draft.carbohydratesGrams?.doubleValue
        )
    }
}

enum AppRoute: Hashable {
    case login
    case register
    case householdList
    case productList(householdId: String)
    case addProduct(householdId: String)
    case editProduct(householdId: String, productId: String)
    case addProductFromDraft(householdId: String, draft: ProductDraftInput)
    case categories(householdId: String)
    case barcodeScan(householdId: String)
    case recipes(householdId: String)
    case notifications
    case profile(householdId: String?)
}

final class AppRouter: ObservableObject {
    @Published var path: [AppRoute] = []
    @Published var root: AppRoute = .login

    init() {
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        if arguments.contains("--ios-ui-test-root-barcode") {
            root = .barcodeScan(householdId: Self.uiTestHouseholdId)
        } else if arguments.contains("--ios-ui-test-root-products") {
            root = .productList(householdId: Self.uiTestHouseholdId)
        }
        #endif
    }

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

    private static let uiTestHouseholdId = "00000000-0000-0000-0000-000000000001"
}
