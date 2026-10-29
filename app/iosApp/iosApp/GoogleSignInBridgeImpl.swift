import UIKit
import Shared
#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

/// Shows the Google account picker and hands the ID token to the shared Kotlin code,
/// which exchanges it for the backend's own tokens.
final class GoogleSignInBridgeImpl: GoogleSignInBridge {

    func signIn(
        onSuccess: @escaping (String) -> Void,
        onCancel: @escaping () -> Void,
        onError: @escaping (String) -> Void
    ) {
        #if canImport(GoogleSignIn)
        guard let presenter = UIApplication.shared.topViewController else {
            onError("No screen to present Google Sign-In from")
            return
        }
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            if let error = error as NSError? {
                if error.domain == kGIDSignInErrorDomain, error.code == GIDSignInError.canceled.rawValue {
                    onCancel()
                } else {
                    onError(error.localizedDescription)
                }
                return
            }
            guard let idToken = result?.user.idToken?.tokenString else {
                onError("Google did not return an ID token")
                return
            }
            onSuccess(idToken)
        }
        #else
        onError("GoogleSignIn package is not linked")
        #endif
    }
}

private extension UIApplication {
    var topViewController: UIViewController? {
        let keyWindow = connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow }
            .first
        var top = keyWindow?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}
