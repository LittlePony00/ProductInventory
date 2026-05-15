import Foundation
import Shared

final class IOSRuntimeSmokeCoordinator {
    static func runIfRequested() {
        if ProcessInfo.processInfo.arguments.contains("--ios-runtime-smoke") {
            runFullSmoke()
        }
        if ProcessInfo.processInfo.arguments.contains("--ios-realtime-smoke") {
            runRealtimeSmoke()
        }
    }

    private static func runFullSmoke() {

        let timestamp = String(Int(Date().timeIntervalSince1970))
        let email = "iossmoke\(timestamp)@e.co"
        let password = "password1"

        Task {
            let runner = IosRuntimeSmokeRunner()
            do {
                let result = try await runner.run(email: email, password: password, timestamp: timestamp)
                emit(
                    "IOS_RUNTIME_SMOKE_OK " +
                    "email=\(result.email) " +
                    "userId=\(result.userId) " +
                    "householdId=\(result.householdId) " +
                    "productId=\(result.productId) " +
                    "productName=\(result.productName) " +
                    "productsVisible=\(result.productsVisible) " +
                    "categoriesVisible=\(result.categoriesVisible) " +
                    "suggestionSource=\(result.suggestionSource) " +
                    "recipeCount=\(result.recipeCount) " +
                    "pushEnabled=\(result.pushEnabled)"
                )
            } catch {
                emit("IOS_RUNTIME_SMOKE_FAIL \(error)")
            }
        }
    }

    private static func runRealtimeSmoke() {
        let timestamp = String(Int(Date().timeIntervalSince1970))
        let email = "iosrealtime\(timestamp)@e.co"
        let password = "password1"

        Task {
            let runner = IosRuntimeSmokeRunner()
            do {
                let result = try await runner.runRealtime(email: email, password: password, timestamp: timestamp)
                emit(
                    "IOS_REALTIME_SMOKE_OK " +
                    "email=\(result.email) " +
                    "userId=\(result.userId) " +
                    "householdId=\(result.householdId) " +
                    "productId=\(result.productId) " +
                    "productName=\(result.productName) " +
                    "eventType=\(result.realtimeEventType) " +
                    "eventReason=\(result.realtimeEventReason ?? "null") " +
                    "productsVisible=\(result.productsVisible)"
                )
            } catch {
                emit("IOS_REALTIME_SMOKE_FAIL \(error)")
            }
        }
    }

    private static func emit(_ message: String) {
        print(message)
        NSLog("%@", message)
    }
}
