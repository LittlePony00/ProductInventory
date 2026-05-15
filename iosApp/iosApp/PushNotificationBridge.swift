import Foundation
import Shared

final class PushNotificationBridge {
    static let shared = PushNotificationBridge()

    private init() {}

    func cacheFirebaseToken(_ token: String) {
        DIContainer.shared.cacheIosPushToken(token)
    }

    func registerFirebaseToken(_ token: String) {
        Task {
            do {
                try await DIContainer.shared.registerIosPushToken(token)
            } catch {
                NSLog("iOS FCM token backend registration failed: %@", error.localizedDescription)
            }
        }
    }
}
