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
            ComposeEntry()
        }
    }
}

struct ComposeEntry: View {
    var body: some View {
        GeometryReader { proxy in
            ContentView().ignoresSafeArea()
        }
    }
}
