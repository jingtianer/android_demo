//
//  ContentView.swift
//  ComposeDemo
//
//  Created by ByteDance on 2026/4/13.
//

import SwiftUI
import ComposeDemoFramework

struct ContentView: UIViewControllerRepresentable {
    /// The size of the top-leading corner inset.
    let topLeading: CGFloat
    /// The size of the top-trailing corner inset.
    let topTrailing: CGFloat
    /// The size of the bottom-leading corner inset.
    let bottomLeading: CGFloat
    /// The size of the bottom-trailing corner inset.
    let bottomTrailing: CGFloat

    func makeUIViewController(context: Context) -> UIViewController {
        // 这里调用的是 iosMain 里定义的 Kotlin 函数
        MainViewControllerKt.screenRoundedCornerInsets(
            topStart: topLeading,
            topEnd: topTrailing,
            bottomStart: bottomLeading,
            bottomEnd: bottomLeading
        )
        let controller = MainViewControllerKt.MainViewController()
        controller.edgesForExtendedLayout = .all
        controller.extendedLayoutIncludesOpaqueBars = true
        controller.modalPresentationStyle = .fullScreen
//        UIViewController()
        return controller
    }
                                                                                                                                                                                                                         
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        MainViewControllerKt.screenRoundedCornerInsets(
            topStart: topLeading,
            topEnd: topTrailing,
            bottomStart: bottomLeading,
            bottomEnd: bottomLeading
        )
    }
}

#Preview {
    ContentView(
        topLeading: 20,
        topTrailing: 20,
        bottomLeading: 20,
        bottomTrailing: 20
    )
}
