import SwiftUI

enum InventoryTone {
    case neutral
    case success
    case warning
    case danger
}

struct InventoryEmptyState: View {
    let title: String
    let message: String
    let systemImage: String
    var primaryTitle: String? = nil
    var primaryAction: (() -> Void)? = nil
    var secondaryTitle: String? = nil
    var secondaryAction: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: systemImage)
                .font(.system(size: 36, weight: .semibold))
                .foregroundStyle(.green)
                .frame(width: 72, height: 72)
                .background(.green.opacity(0.14), in: Circle())
            Text(title)
                .font(.title3.weight(.semibold))
                .multilineTextAlignment(.center)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            if let primaryTitle, let primaryAction {
                Button(primaryTitle, action: primaryAction)
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
            }
            if let secondaryTitle, let secondaryAction {
                Button(secondaryTitle, action: secondaryAction)
                    .buttonStyle(.bordered)
                    .controlSize(.large)
            }
        }
        .padding(28)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct InventoryStatusBadge: View {
    let text: String
    let tone: InventoryTone

    var body: some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .foregroundStyle(foreground)
            .background(background, in: Capsule())
            .accessibilityLabel(text)
    }

    private var foreground: Color {
        switch tone {
        case .neutral: return .secondary
        case .success: return .green
        case .warning: return .orange
        case .danger: return .red
        }
    }

    private var background: Color {
        switch tone {
        case .neutral: return Color.secondary.opacity(0.12)
        case .success: return Color.green.opacity(0.14)
        case .warning: return Color.orange.opacity(0.16)
        case .danger: return Color.red.opacity(0.14)
        }
    }
}

struct InventoryFloatingButton: View {
    let systemImage: String
    let label: String
    let prominent: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.title2.weight(.semibold))
                .frame(width: 56, height: 56)
                .background(prominent ? Color.accentColor : Color.secondary.opacity(0.18), in: Circle())
                .foregroundStyle(prominent ? .white : .primary)
                .shadow(color: .black.opacity(prominent ? 0.18 : 0.08), radius: 8, y: 4)
        }
        .accessibilityLabel(label)
        .accessibilityHint("Открывает экран: \(label)")
    }
}
