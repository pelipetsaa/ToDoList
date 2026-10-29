import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            apiBaseUrl: AppConfig.apiBaseUrl,
            googleSignIn: AppConfig.googleClientId == nil ? nil : GoogleSignInBridgeImpl()
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}

/// Values injected into Info.plist from Configuration/Config.xcconfig.
enum AppConfig {
    static var apiBaseUrl: String? { value(for: "TodoApiBaseUrl") }
    static var googleClientId: String? { value(for: "GIDClientID") }

    private static func value(for key: String) -> String? {
        guard let value = Bundle.main.object(forInfoDictionaryKey: key) as? String,
              !value.trimmingCharacters(in: .whitespaces).isEmpty else { return nil }
        return value
    }
}
