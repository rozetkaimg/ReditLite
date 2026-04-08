import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    @State private var authCode: String? = nil

    init() {
        InitKoinKt.doInitKoin(appDeclaration: { _ in })
    }

    var body: some Scene {
        WindowGroup {
            ContentView(
                authCode: $authCode,
                onLoginClick: {
                    let clientId = "yH0aTnJEt6qUgGn835B4vg"
                    let redirectUri = "redreader://rr_oauth_redir"
                    let urlString = "https://www.reddit.com/api/v1/authorize.compact?client_id=\(clientId)&response_type=code&state=random_state_string&redirect_uri=\(redirectUri)&duration=permanent&scope=identity%20read%20vote%20submit%20subscribe%20history%20mysubreddits"

                    if let url = URL(string: urlString) {
                        UIApplication.shared.open(url)
                    }
                }
            )
            .id(authCode) // Пересоздает View при получении кода
            .onOpenURL { url in
                if url.scheme == "redreader" {
                    if let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
                       let code = components.queryItems?.first(where: { $0.name == "code" })?.value {
                        self.authCode = code
                    }
                }
            }
        }
    }
}
