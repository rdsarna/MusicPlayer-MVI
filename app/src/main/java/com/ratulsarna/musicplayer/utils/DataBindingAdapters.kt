package com.ratulsarna.musicplayer.utils

import android.graphics.drawable.TransitionDrawable
import android.view.View
import android.view.View.VISIBLE
import android.widget.ImageView
import androidx.annotation.Keep
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
import com.ratulsarna.musicplayer.ui.PlaylistAdapter
import com.ratulsarna.musicplayer.ui.model.PlaylistViewSong

@Keep
@BindingAdapter("imageDrawableRes")
fun imageDrawableRes(imageView: ImageView, resId: Int?) {
    if (resId == null) return
    if (imageView.visibility != VISIBLE) {
        imageView.setImageResource(resId)
        return
    }
    val previousDrawable = imageView.drawable
    if (previousDrawable == null) {
        imageView.setImageResource(resId)
    } else {
        imageView.fadeNewDrawableResource(resId)
    }
}

private fun ImageView.fadeNewDrawableResource(resId: Int) {
    val previousDrawable = drawable ?: return
    TransitionDrawable(arrayOf(previousDrawable,
        AppCompatResources.getDrawable(context, resId))).apply {
        setImageDrawable(this)
        startTransition(200)
    }
}

@Keep
@BindingAdapter("songList")
fun songList(recyclerView: RecyclerView, songList: List<PlaylistViewSong>?) {
    if (songList == null) return
    (recyclerView.adapter as? PlaylistAdapter)?.submitList(songList)
}

@Keep
@BindingAdapter("isVisible")
fun isVisible(view: View, visible: Boolean?) {
    if (visible == null) return
    view.visibility = if (visible) View.VISIBLE else View.INVISIBLE
}

@Keep
@BindingAdapter("isGone")
fun isGone(view: View, gone: Boolean?) {
    if (gone == null) return
    view.visibility = if (gone) View.GONE else View.VISIBLE
}

@Keep
@BindingAdapter("seekBarValueTo")
fun seekBarValueTo(slider: Slider, valueTo: Float?) {
    if (valueTo == null) return
    slider.valueTo = valueTo
}