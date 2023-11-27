//
//  ContentView.swift
//  iosApp
//
//  Created by Ratul Sarna on 24/11/23.
//

import SwiftUI
import shared
import KMMViewModelSwiftUI

struct ContentView: View {
    
    @StateViewModel var viewModel = MusicPlayerViewModel()
    
    var body: some View {
        VStack {
            Image(systemName: "globe")
                .imageScale(.large)
                .foregroundStyle(.tint)
            Text(viewModel.viewState.value.songTitle)
        }
        .padding()
    }
}

#Preview {
    ContentView()
}
