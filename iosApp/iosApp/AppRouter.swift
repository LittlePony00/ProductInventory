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
    case addProductFromDraft(householdId: String, draft: ProductDraftInput)
    case barcodeScan(householdId: String)
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
