//
//  ContentView.swift
//  ComposeDemo
//
//  Created by ByteDance on 2026/4/13.
//

import SwiftUI
import SharedComposeDemo

struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // 这里调用的是 iosMain 里定义的 Kotlin 函数
        let controller = MainViewControllerKt.MainViewController()
        controller.edgesForExtendedLayout = .all
        controller.extendedLayoutIncludesOpaqueBars = true
        controller.modalPresentationStyle = .fullScreen
//        UIViewController()
        return controller
    }
                                                                                                                                                                                                                         
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // 通常不需要做更新
    }
}

#Preview {
    ContentView()
}
