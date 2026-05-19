import Foundation
import Shared
import UserNotifications

final class IOSRuntimeSmokeCoordinator {
    static func runIfRequested() {
        if ProcessInfo.processInfo.arguments.contains("--ios-runtime-smoke") {
            runFullSmoke()
        }
        if ProcessInfo.processInfo.arguments.contains("--ios-realtime-smoke") {
            runRealtimeSmoke()
        }
        if ProcessInfo.processInfo.arguments.contains("--ios-local-reminder-smoke") {
            runLocalReminderSmoke()
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

    private static func runLocalReminderSmoke() {
        let timestamp = String(Int(Date().timeIntervalSince1970))
        let email = "iosreminder\(timestamp)@e.co"
        let password = "password1"

        Task {
            let runner = IosRuntimeSmokeRunner()
            do {
                let result = try await runner.run(email: email, password: password, timestamp: timestamp)
                let reminders = try await DIContainer.shared.currentProductLocalReminders()
                let targetReminders = reminders.filter { $0.productId == result.productId }
                let targetIdentifiers = Set(targetReminders.map(localReminderIdentifier))
                let center = UNUserNotificationCenter.current()
                center.removeDeliveredNotifications(withIdentifiers: Array(targetIdentifiers))
                await ProductLocalReminderScheduler.schedule(reminders)
                let pending = await center.pendingNotificationRequests()
                let targetPending = pending.filter { targetIdentifiers.contains($0.identifier) }
                try? await Task.sleep(nanoseconds: 6_000_000_000)
                let delivered = await center.deliveredNotifications()
                let targetDelivered = delivered.filter { targetIdentifiers.contains($0.request.identifier) }
                guard !reminders.isEmpty else {
                    throw LocalReminderSmokeError.noRemindersPlanned
                }
                guard !targetReminders.isEmpty else {
                    throw LocalReminderSmokeError.noTargetReminderPlanned
                }
                guard !targetPending.isEmpty || !targetDelivered.isEmpty else {
                    throw LocalReminderSmokeError.noNotificationScheduledOrDelivered
                }
                emit(
                    "IOS_LOCAL_REMINDER_SMOKE_OK " +
                    "email=\(result.email) " +
                    "householdId=\(result.householdId) " +
                    "productId=\(result.productId) " +
                    "reminders=\(reminders.count) " +
                    "targetReminders=\(targetReminders.count) " +
                    "targetPending=\(targetPending.count) " +
                    "targetDelivered=\(targetDelivered.count)"
                )
            } catch {
                emit("IOS_LOCAL_REMINDER_SMOKE_FAIL \(error)")
            }
        }
    }

    private enum LocalReminderSmokeError: Error {
        case noRemindersPlanned
        case noTargetReminderPlanned
        case noNotificationScheduledOrDelivered
    }

    private static func localReminderIdentifier(for reminder: ProductLocalReminder) -> String {
        "product-inventory-local-reminder:\(reminder.id)"
    }

    private static func emit(_ message: String) {
        print(message)
        NSLog("%@", message)
    }
}
