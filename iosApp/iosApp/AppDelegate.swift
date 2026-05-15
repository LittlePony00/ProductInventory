import FirebaseCore
import FirebaseMessaging
import SwiftUI
import UIKit
import UserNotifications

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        if ProcessInfo.processInfo.arguments.contains("--ios-ui-test") {
            return true
        }
        requestRemoteNotifications(application)
        requestAndRegisterFirebaseToken()
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
        requestAndRegisterFirebaseToken()
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        NSLog("iOS remote notification registration failed: %@", error.localizedDescription)
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken, !token.isEmpty else { return }
        NSLog("iOS FCM registration token received")
        PushNotificationBridge.shared.cacheFirebaseToken(token)
        PushNotificationBridge.shared.registerFirebaseToken(token)
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound, .badge]
    }

    private func requestRemoteNotifications(_ application: UIApplication) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            if let error {
                NSLog("iOS notification permission request failed: %@", error.localizedDescription)
            }
            guard granted else { return }
            DispatchQueue.main.async {
                application.registerForRemoteNotifications()
            }
        }
    }

    private func requestAndRegisterFirebaseToken() {
        Messaging.messaging().token { token, error in
            if let error {
                NSLog("iOS FCM token request failed: %@", error.localizedDescription)
                return
            }
            guard let token, !token.isEmpty else {
                NSLog("iOS FCM token request returned empty token")
                return
            }
            NSLog("iOS FCM token request succeeded")
            PushNotificationBridge.shared.cacheFirebaseToken(token)
            PushNotificationBridge.shared.registerFirebaseToken(token)
        }
    }
}
