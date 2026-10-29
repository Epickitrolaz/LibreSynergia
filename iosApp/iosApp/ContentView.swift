import SwiftUI
import UIKit
import ComposeApp

struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            onIconChange: { iconId in
                guard UIApplication.shared.supportsAlternateIcons else { return }
                let iconName: String? = (iconId == "variant") ? "AppIconVariant" : nil
                UIApplication.shared.setAlternateIconName(iconName) { error in
                    if let error = error {
                        print("Failed to change icon: \(error.localizedDescription)")
                    }
                }
            }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
