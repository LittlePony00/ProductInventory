import XCTest

final class ProductInventoryUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--ios-ui-test"]
    }

    func testLoginScreenRenders() {
        app.launch()

        XCTAssertTrue(app.staticTexts["Учёт продуктов"].waitForExistence(timeout: 15))
        XCTAssertTrue(app.textFields["login.email"].exists)
        XCTAssertTrue(app.secureTextFields["login.password"].exists)
        XCTAssertTrue(app.buttons["login.submit"].exists)
        XCTAssertTrue(app.buttons["login.register"].exists)
    }

    func testBarcodeScannerShowsManualFallbackWhenCameraDenied() {
        app.launchArguments += [
            "--ios-ui-test-root-barcode",
            "--ios-ui-test-camera-denied"
        ]
        app.launch()

        XCTAssertTrue(app.navigationBars["Сканирование"].waitForExistence(timeout: 15))
        XCTAssertTrue(app.staticTexts["Нужен доступ к камере"].waitForExistence(timeout: 15))
        XCTAssertTrue(app.buttons["barcode.manualAddDenied"].exists)
    }

    func testProductListExposesParityNavigationActions() {
        app.launchArguments += ["--ios-ui-test-root-products"]
        app.launch()

        XCTAssertTrue(app.navigationBars["Продукты"].waitForExistence(timeout: 15))
        XCTAssertTrue(app.buttons["productList.scanBarcode"].waitForExistence(timeout: 15))
        XCTAssertTrue(app.buttons["productList.addProduct"].exists)
        XCTAssertTrue(app.buttons["productList.categories"].exists)
        XCTAssertTrue(app.buttons["productList.recipes"].exists)
        XCTAssertTrue(app.buttons["productList.notifications"].exists)
    }
}
