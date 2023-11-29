//
//  iosAppApp.swift
//  iosApp
//
//  Created by Ratul Sarna on 24/11/23.
//

import SwiftUI
import shared

@main
struct iOSApp: App {
    
    init() {
        KoinModuleKt.doInitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            MusicPlayerContentView()
        }
    }
}
