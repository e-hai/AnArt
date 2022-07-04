package com.an.ffmpeg.widget;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.an.ffmpeg.R;

import java.util.ArrayList;
import java.util.List;

public class VideoTrimmerAdapter extends RecyclerView.Adapter<VideoTrimmerAdapter.TrimmerViewHolder> {
    private List<Bitmap> mBitmaps = new ArrayList<>();

    public VideoTrimmerAdapter() {
    }


    @NonNull
    @Override
    public TrimmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_thumb_item_layout, parent, false);
        return new TrimmerViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TrimmerViewHolder holder, int position) {
        holder.thumbImageView.setImageBitmap(mBitmaps.get(position));
    }

    @Override
    public int getItemCount() {
        return mBitmaps.size();
    }

    public void addBitmaps(Bitmap bitmap) {
        mBitmaps.add(bitmap);
        notifyItemChanged(0, mBitmaps.size());
    }

    public static final class TrimmerViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbImageView;

        TrimmerViewHolder(View itemView) {
            super(itemView);
            thumbImageView = itemView.findViewById(R.id.thumb);
        }
    }
}
