package com.an.ffmpeg.widget

import com.an.ffmpeg.widget.VideoCropAdapter.TrimmerViewHolder
import com.an.ffmpeg.code.VideoThumbItem
import java.util.ArrayList
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import com.an.ffmpeg.R
import com.bumptech.glide.Glide
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class VideoCropAdapter : RecyclerView.Adapter<TrimmerViewHolder>() {
    private var dataList: List<VideoThumbItem> = ArrayList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrimmerViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.video_thumb_item_layout, parent, false)
        return TrimmerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TrimmerViewHolder, position: Int) {
        val (srcVideoPath, frameTimeMicros) = dataList[position]
        Glide.with(holder.thumbImageView.context)
            .load(srcVideoPath)
            .frame(frameTimeMicros)
            .centerCrop()
            .into(holder.thumbImageView)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun updateThumbs(dataList: List<VideoThumbItem>) {
        this.dataList = dataList
        notifyItemChanged(0, dataList.size)
    }

    class TrimmerViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var thumbImageView: ImageView

        init {
            thumbImageView = itemView.findViewById(R.id.thumb)
        }
    }
}