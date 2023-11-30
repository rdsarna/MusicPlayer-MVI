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
    @StateViewModel var viewModel = MusicPlayerViewModel()
    
    var body: some View {
            GeometryReader { geometry in
                VStack {
                    Spacer() // Pushes the content to the center

                    // Album Art
                    SongAlbumArt(geometry)

                    Spacer(minLength: 32)

                    // Song Title and Information
                    SongTitle(viewModel)

                    // Song Seek Bar
                    SongSeekBar(viewModel)

                    // Controls
                    SongControls(viewModel)
                    
                    Spacer()
                }
            }
            .padding(32)
            .background(LinearGradient(gradient: Gradient(colors: [Color.blue.opacity(0.6), Color.blue]), startPoint: .top, endPoint: .bottom))
            .onAppear {
                viewModel.processInput(intent: MusicPlayerIntent.UiStartIntent())
            }
        }
}



struct MusicPlayerView_Previews: PreviewProvider {
    static var previews: some View {
        MusicPlayerContentView()
            .preferredColorScheme(.dark) // To mimic the dark background
    }
}


struct SongSeekBar: View {
    @ObservedViewModel var viewModel: MusicPlayerViewModel
    
    init(_ viewModel: MusicPlayerViewModel) {
        self.viewModel = viewModel
    }
    
    var body: some View {
        VStack {
            Slider(
                value: .constant(0),
                in: 0...1
            )
            .accentColor(.white)
            
            HStack {
                Text(viewModel.viewState.elapsedTimeLabel)
                    .foregroundColor(.white)
                    .padding(.leading)
                Spacer()
                Text(viewModel.viewState.totalTimeLabel)
                    .foregroundColor(.white)
                    .padding(.trailing)
            }
        }
    }
}

struct SongTitle: View {
    @ObservedViewModel var viewModel: MusicPlayerViewModel
    
    init(_ viewModel: MusicPlayerViewModel) {
        self.viewModel = viewModel
    }
    
    var body: some View {
        VStack {
            Text(viewModel.viewState.songTitle)
                .font(.title)
                .fontWeight(.bold)
                .foregroundColor(.white)
            
            Text(viewModel.viewState.songInfoLabel)
                .font(.subheadline)
                .foregroundColor(.white.opacity(0.7))
        }
        .padding(.bottom, 32)
    }
}

struct SongAlbumArt: View {
    private var geometry: GeometryProxy
    
    init(_ geometry: GeometryProxy) {
        self.geometry = geometry
    }
    
    var body: some View {
        Image("levitating_album_art")
            .resizable()
            .aspectRatio(contentMode: .fit)
            .frame(height: geometry.size.height * 0.35)
            .clipped()
    }
}

struct SongControls: View {
    @ObservedViewModel var viewModel: MusicPlayerViewModel
    
    init(_ viewModel: MusicPlayerViewModel) {
        self.viewModel = viewModel
    }
    
    var body: some View {
        HStack() {
            SongControlButton(systemName: "backward.fill", action: {
                viewModel.processInput(intent: MusicPlayerIntent.SeekBackwardIntent())
            })
            
            Spacer()
            
            SongControlButton(systemName: "backward.end.fill", action: {
                viewModel.processInput(intent: MusicPlayerIntent.PreviousSongIntent())
            })
            
            Spacer()
            
            SongControlButton(
                systemName: viewModel.viewState.playing ? "pause.circle.fill" : "play.circle.fill",
                width: 50,
                height: 50,
                action: {
                    if (viewModel.viewState.playing) {
                        viewModel.processInput(intent: MusicPlayerIntent.PauseIntent())
                    } else {
                        viewModel.processInput(intent: MusicPlayerIntent.PlayIntent())
                    }
                }
            )
            
            Spacer()
            
            SongControlButton(systemName: "forward.end.fill", action: {
                viewModel.processInput(intent: MusicPlayerIntent.NextSongIntent())
            })
            
            Spacer()
            
            SongControlButton(systemName: "forward.fill", action: {
                viewModel.processInput(intent: MusicPlayerIntent.SeekForwardIntent())
            })
        }
        .padding()
        .foregroundColor(.white)
    }
}

struct SongControlButton: View {
    var systemName: String
    var width: CGFloat = 30
    var height: CGFloat = 30
    var action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: width, height: height)
        }
    }
}
