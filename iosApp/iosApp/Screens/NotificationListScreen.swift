import SwiftUI
import Shared
import UIKit
import UserNotifications

struct NotificationListScreen: View {
    @StateObject private var holder = SharedVMHolder<NotificationListState, NotificationListEvent, NotificationListAction, NotificationListViewModel>(
        viewModel: DIContainer.shared.notificationListViewModel(),
        initialState: NotificationListState.Loading()
    )
    @EnvironmentObject private var router: AppRouter

    var body: some View {
        content
            .task {
                holder.start { [weak router] action in
                    guard let router = router else { return }
                    DispatchQueue.main.async {
                        switch action {
                        case is NotificationListAction.NavigateBack:
                            router.pop()
                        default:
                            break
                        }
                    }
                }
                holder.sendEvent(NotificationListEvent.OnCreate())
            }
            .navigationTitle("Уведомления")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Прочитать все") {
                        holder.sendEvent(NotificationListEvent.OnMarkAllRead())
                    }
                }
            }
    }

    @ViewBuilder
    private var content: some View {
        switch holder.state {
        case is NotificationListState.Loading:
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        case let state as NotificationListState.Content:
            Form {
                NotificationSettingsSection(
                    settings: state.settings,
                    isSaving: state.isSavingSettings,
                    error: state.settingsError,
                    onExpirationEnabledChanged: {
                        holder.sendEvent(NotificationListEvent.OnExpirationRemindersEnabledChange(enabled: $0))
                    },
                    onLowStockEnabledChanged: {
                        holder.sendEvent(NotificationListEvent.OnLowStockRemindersEnabledChange(enabled: $0))
                    },
                    onPushEnabledChanged: updatePushEnabled,
                    onDaysChanged: {
                        holder.sendEvent(NotificationListEvent.OnExpirationReminderDaysChange(days: Int32($0)))
                    }
                )

                Section("История") {
                    if state.notifications.isEmpty {
                        InventoryEmptyState(
                            title: "Нет уведомлений",
                            message: "Здесь появятся напоминания о сроках годности и низком запасе.",
                            systemImage: "bell"
                        )
                        .listRowInsets(EdgeInsets())
                    } else {
                        ForEach(state.notifications, id: \.id) { notification in
                            NotificationRow(notification: notification) {
                                holder.sendEvent(NotificationListEvent.OnMarkRead(notificationId: notification.id))
                            }
                        }
                    }
                }
            }
        case let state as NotificationListState.Error:
            VStack(spacing: 8) {
                Text(state.message ?? "Ошибка загрузки")
                Button("Повторить") { holder.sendEvent(NotificationListEvent.OnRetry()) }
                    .buttonStyle(.borderedProminent)
            }.frame(maxWidth: .infinity, maxHeight: .infinity)
        default:
            EmptyView()
        }
    }

    private func updatePushEnabled(_ enabled: Bool) {
        if enabled {
            UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
                if let error {
                    print("iOS notification permission request failed: \(error.localizedDescription)")
                }
                if granted {
                    DispatchQueue.main.async {
                        UIApplication.shared.registerForRemoteNotifications()
                        holder.sendEvent(NotificationListEvent.OnPushEnabledChange(enabled: true))
                    }
                } else {
                    DispatchQueue.main.async {
                        holder.sendEvent(NotificationListEvent.OnPushEnabledChange(enabled: false))
                    }
                }
            }
        } else {
            holder.sendEvent(NotificationListEvent.OnPushEnabledChange(enabled: false))
        }
    }
}

private struct NotificationSettingsSection: View {
    let settings: NotificationSettings
    let isSaving: Bool
    let error: String?
    let onExpirationEnabledChanged: (Bool) -> Void
    let onLowStockEnabledChanged: (Bool) -> Void
    let onPushEnabledChanged: (Bool) -> Void
    let onDaysChanged: (Int) -> Void

    var body: some View {
        Section("Настройки") {
            Toggle("Напоминать о сроке годности", isOn: Binding(
                get: { settings.expirationRemindersEnabled },
                set: onExpirationEnabledChanged
            ))
            Toggle("Напоминать о низком остатке", isOn: Binding(
                get: { settings.lowStockRemindersEnabled },
                set: onLowStockEnabledChanged
            ))
            Toggle("Push-уведомления", isOn: Binding(
                get: { settings.pushEnabled },
                set: onPushEnabledChanged
            ))
            Stepper(
                "За \(settings.expirationReminderDays) дн.",
                value: Binding(
                    get: { Int(settings.expirationReminderDays) },
                    set: onDaysChanged
                ),
                in: 1...30
            )
            if isSaving {
                HStack {
                    ProgressView()
                    Text("Сохраняем настройки")
                }
            }
            if let error {
                Text(error).foregroundColor(.red)
            }
        }
    }
}

struct NotificationRow: View {
    let notification: Shared.Notification
    let onMarkRead: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(notification.title).font(.headline)
                    .fontWeight(notification.isRead ? .regular : .bold)
                Spacer()
                if !notification.isRead {
                    InventoryStatusBadge(text: "Новое", tone: .warning)
                }
            }
            Text(notification.message).font(.subheadline)
            Text(String(notification.sentAt.prefix(16)).replacingOccurrences(of: "T", with: " "))
                .font(.caption).foregroundColor(.secondary)
        }
        .padding(.vertical, 4)
        .opacity(notification.isRead ? 0.6 : 1.0)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(notification.isRead ? "Прочитано" : "Новое"), \(notification.title), \(notification.message)")
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            if !notification.isRead {
                Button("Прочитано", action: onMarkRead)
                    .tint(.green)
            }
        }
    }
}
