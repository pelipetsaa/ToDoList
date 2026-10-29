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
    static var apiBaseUrl: String? {
        let url = value(for: "TodoApiBaseUrl")
        #if !DEBUG
        // Without this the shared code falls back to localhost, which a released app can never reach.
        precondition(url?.hasPrefix("https://") == true, "Set API_BASE_URL[config=Release] in Config.xcconfig")
        #endif
        return url
    }
    static var googleClientId: String? { value(for: "GIDClientID") }

    private static func value(for key: String) -> String? {
        guard let value = Bundle.main.object(forInfoDictionaryKey: key) as? String,
              !value.trimmingCharacters(in: .whitespaces).isEmpty else { return nil }
        return value
    }
}
