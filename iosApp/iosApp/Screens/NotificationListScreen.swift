import SwiftUI
import Shared

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
            if state.notifications.isEmpty {
                Text("Нет уведомлений").font(.headline)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(state.notifications, id: \.id) { notification in
                    NotificationRow(notification: notification) {
                        holder.sendEvent(NotificationListEvent.OnMarkRead(notificationId: notification.id))
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
                    Button("Прочитано", action: onMarkRead).font(.caption)
                }
            }
            Text(notification.message).font(.subheadline)
            Text(String(notification.sentAt.prefix(16)).replacingOccurrences(of: "T", with: " "))
                .font(.caption).foregroundColor(.secondary)
        }
        .padding(.vertical, 4)
        .opacity(notification.isRead ? 0.6 : 1.0)
    }
}
