//
//  ContentView.swift
//  iosApp
//
//  Created by Ratul Sarna on 24/11/23.
//

import SwiftUI
import shared
import KMMViewModelSwiftUI


struct MusicPlayerContentView: View {
    // Assuming a state for play/pause button
    @State private var isPlaying = false
    
    @StateViewModel var viewModel = MusicPlayerViewModel()
    
    var body: some View {
        VStack {
            Image("levitating_album_art") // Replace with your image name
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: 200, height: 200)
                .clipped()
            
            Text(viewModel.viewState.songTitle)
                .font(.title)
                .fontWeight(.bold)
                .foregroundColor(.white)
            
            Text(viewModel.viewState.songInfoLabel)
                .font(.subheadline)
                .foregroundColor(.white)
            
            HStack {
                Text(viewModel.viewState.elapsedTimeLabel)
                    .foregroundColor(.white)
                Spacer()
                Text(viewModel.viewState.totalTimeLabel)
                    .foregroundColor(.white)
            }
            .font(.caption)
            .padding([.leading, .trailing])
            
            Slider(value: .constant(0), in: 0...1)
                .accentColor(.blue)
            
            HStack {
                Button(action: {
                    // Previous track action
                }) {
                    Image(systemName: "backward.circle.fill")
                        .foregroundColor(.white)
                }
                .padding()
                
                Button(action: {
                    self.isPlaying.toggle()
                }) {
                    Image(systemName: isPlaying ? "pause.circle.fill" : "play.circle.fill")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 50, height: 50)
                        .foregroundColor(.white)
                }
                .padding()
                
                Button(action: {
                    // Next track action
                }) {
                    Image(systemName: "forward.circle.fill")
                        .foregroundColor(.white)
                }
                .padding()
            }
            .foregroundColor(.blue)
            .font(.title)
        }
        .frame(maxWidth: /*@START_MENU_TOKEN@*/.infinity/*@END_MENU_TOKEN@*/, maxHeight:  .infinity)
        .padding()
        .background(.black)
        .onAppear(perform: {
            viewModel.processInput(intent: MusicPlayerIntent.UiStartIntent())
        })
    }
}

struct MusicPlayerView_Previews: PreviewProvider {
    static var previews: some View {
        MusicPlayerContentView()
            .preferredColorScheme(.dark) // To mimic the dark background
    }
}

