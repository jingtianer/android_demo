//
//  LaunchTasks.swift
//  ComposeDemo
//
//  Created by ByteDance on 2026/11/18.
//
import ComposeDemoFramework

func initApp() {
    initWebViewFactory()
}

func initWebViewFactory() {
    ComposeDemoFramework.CommonWebViewKt.snapshotWebViewFactory = SnapshotWebViewFactoryImpl()
}
