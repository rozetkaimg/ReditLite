import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    var authCode: String?
    var onLoginClick: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            authCode: authCode,
            onLoginClick: onLoginClick
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @Binding var authCode: String?
    var onLoginClick: () -> Void

    var body: some View {
        ComposeView(authCode: authCode, onLoginClick: onLoginClick)
            .ignoresSafeArea()
    }
}
