//
//  ComposeDemoApp.swift
//  ComposeDemo
//
//  Created by ByteDance on 2026/4/13.
//

import SwiftUI
import UIKit

@main
struct ComposeDemoApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeEntry()
        }
    }
}

struct WindowReader: UIViewRepresentable {
    var onWindow: (UIWindow?) -> Void
    func makeUIView(context: Context) -> UIView {
        let v = UIView(frame: .zero)
        v.isHidden = true
        DispatchQueue.main.async {
            onWindow(v.window)
        }
        return v
    }
    func updateUIView(_ uiView: UIView, context: Context) {
        onWindow(uiView.window)
    }
}

struct ComposeEntry: View {
    @State private var window: UIWindow? = nil
    var body: some View {
        ZStack {
            WindowReader { w in
                window = w
            }
            GeometryReader { proxy in
                let deviceRadius = getHardcodedScreenCornerRadius()
                let scale = window?.screen.scale ?? 1.0
                // todo: 升级xcode27后 调用 proxy.concentricCornerRadii
                ContentView(
                    topLeading: deviceRadius * scale,
                    topTrailing: deviceRadius * scale,
                    bottomLeading: deviceRadius * scale,
                    bottomTrailing: deviceRadius * scale
                ).ignoresSafeArea()
            }
        }
    }
}

/// 获取设备屏幕圆角半径（硬编码查表，仅用于 iOS <26 兜底）
/// - Returns: 屏幕圆角 pts，无匹配返回 0
func getHardcodedScreenCornerRadius() -> CGFloat {
    let model = getDeviceModel()
    switch model {
    // 15 全系列
    case "iPhone15,4", "iPhone15,5", "iPhone16,1", "iPhone16,2":
        return 55.0
    // 16 / 16 Plus
    case "iPhone16,4", "iPhone16,5":
        return 55.0
    // 16 Pro / 16 Pro Max
    case "iPhone17,1", "iPhone17,2":
        return 62.0
    // 17 / 17 Pro / 17 Pro Max
    case "iPhone17,4", "iPhone18,1", "iPhone18,2":
        return 62.0
    case "iPhone15,2", "iPhone15,3":        // iPhone 14 Pro / 14 Pro Max
        return 55.0
    case "iPhone14,5", "iPhone14,8", "iPhone13,4": // 13 Pro Max / 14 Plus / 12 Pro Max
        return 53.33
    case "iPhone14,7", "iPhone14,2", "iPhone13,3", "iPhone13,2": // 14 /13 Pro /12 Pro /12
        return 47.33
    case "iPhone14,4", "iPhone13,1":       // 13 Mini /12 Mini
        return 44.0
    case "iPhone12,3", "iPhone12,5", "iPhone10,3", "iPhone11,8", "iPhone11,6": // 11 Pro /11 Pro Max / X / XS / XS Max
        return 39.0
    case "iPhone11,2", "iPhone12,1":        // XR / 11
        return 41.5
    case "iPad13,1", "iPad13,2",
         "iPad14,1", "iPad14,2",
         "iPad8,1", "iPad8,2", "iPad8,3", "iPad8,4",
         "iPad8,5", "iPad8,6", "iPad8,7", "iPad8,8",
         "iPad8,9", "iPad8,10",
         "iPad13,4", "iPad13,5", "iPad13,6", "iPad13,7",
         "iPad14,3", "iPad14,4",
         "iPad14,5", "iPad14,6",
         "iPad14,8", "iPad14,9",
         "iPad14,10", "iPad14,11",
         "iPad15,1", "iPad15,2",
         "iPad15,3", "iPad15,4",
         "iPad16,1", "iPad16,2",
         "iPad16,3", "iPad16,4",
         "iPad17,1", "iPad17,2":
        return 18.0
    default:
        return 0.0
    }
}

/// 获取设备model identifier（如 iPhone15,2）
private func getDeviceModel() -> String {
    var size = 0
    sysctlbyname("hw.machine", nil, &size, nil, 0)
    var machine = [CChar](repeating: 0, count: size)
    sysctlbyname("hw.machine", &machine, &size, nil, 0)
    return String(cString: machine)
}
