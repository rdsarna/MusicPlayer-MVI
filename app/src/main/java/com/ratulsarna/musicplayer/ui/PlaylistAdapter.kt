package com.ratulsarna.musicplayer.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.ratulsarna.musicplayer.databinding.ItemPlaylistSongBinding
import com.ratulsarna.musicplayer.ui.model.PlaylistViewSong

class PlaylistAdapter(
    private val nextSongIdListener: (Int) -> Unit
) : RecyclerView.Adapter<SongViewHolder>() {

    private val diffCallback = PlaylistDiffCallback()
    private val differ = AsyncListDiffer(this, diffCallback)

    fun submitList(songList: List<PlaylistViewSong>) {
        differ.submitList(songList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        return SongViewHolder(ItemPlaylistSongBinding.inflate(LayoutInflater.from(parent.context),
            parent, false))
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.binding.apply {
            val playlistViewSong = differ.currentList[position]
            song = playlistViewSong
            root.setOnClickListener { nextSongIdListener(playlistViewSong.id) }
        }
    }

    override fun getItemCount(): Int = differ.currentList.size
}

class SongViewHolder(val binding: ItemPlaylistSongBinding) : RecyclerView.ViewHolder(binding.root)

class PlaylistDiffCallback : DiffUtil.ItemCallback<PlaylistViewSong>() {
    // only one kind of item
    override fun areItemsTheSame(oldItem: PlaylistViewSong, newItem: PlaylistViewSong): Boolean = true

    override fun areContentsTheSame(oldItem: PlaylistViewSong, newItem: PlaylistViewSong): Boolean =
        oldItem == newItem
}