import SwiftUI
import UIKit
import SaolaShared

/**
 Hands the shared Compose UI a `UIViewController` to live in.

 The controller comes from Kotlin rather than being wrapped here, because it is the controller
 that answers iOS's status-bar question — see `MainViewController.kt`.
 */
struct ContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea(.all)
    }
}

private struct ComposeView: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.SaolaViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Nothing to push down: state lives in the shared ViewModels, not in SwiftUI.
    }
}
