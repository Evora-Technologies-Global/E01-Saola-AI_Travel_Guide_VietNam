import SwiftUI
import VietLensShared

/**
 The whole iOS app.

 Every screen, every ViewModel and the entire object graph live in the shared Kotlin module —
 the same code the Android build runs. What is left here is what SwiftUI has to own: the app
 entry point, and a `UIViewControllerRepresentable` to hand Compose a window.
 */
@main
struct iOSApp: App {

    init() {
        // Once, before the first controller. Mirrors `VietLensApplication.onCreate` on Android.
        #if DEBUG
        MainViewControllerKt.startVietLens(debug: true)
        #else
        MainViewControllerKt.startVietLens(debug: false)
        #endif
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                // The lens screen is a viewfinder edge to edge; the shared UI applies its own
                // safe-area insets to the controls that need them.
                .ignoresSafeArea(.all)
        }
    }
}
