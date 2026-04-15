//
//  ContentView.swift
//  ComposeDemo
//
//  Created by ByteDance on 2026/4/13.
//

import SwiftUI
import composedemo

struct ContentView: View {
    var body: some View {
        VStack {
            Image(systemName: "globe")
                .imageScale(.large)
                .foregroundStyle(.tint)
            Text("Hello, world!")
        }
        .padding()
    }
}

#Preview {
    ContentView()
}
