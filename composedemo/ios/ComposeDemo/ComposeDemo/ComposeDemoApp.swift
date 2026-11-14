//
//  ComposeDemoApp.swift
//  ComposeDemo
//
//  Created by ByteDance on 2026/4/13.
//

import SwiftUI

@main
struct ComposeDemoApp: App {
    var body: some Scene {
        WindowGroup {
            if #available(iOS 26.0, *) {
                ContentView().ignoresSafeArea().containerShape(.rect).clipShape(ConcentricRectangle())
            } else {
                ContentView().ignoresSafeArea()
            }
        }
    }
}
