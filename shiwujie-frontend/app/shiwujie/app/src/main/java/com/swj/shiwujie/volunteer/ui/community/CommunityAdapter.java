package com.swj.shiwujie.volunteer.ui.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.swj.shiwujie.R;

import java.util.List;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {
    private List<CommunityActivity> activities;

    public CommunityAdapter(List<CommunityActivity> activities) {
        this.activities = activities;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityActivity activity = activities.get(position);
        holder.activityImage.setImageResource(activity.getImageResId());
        holder.statusLabel.setText(activity.getStatus());
        holder.dateText.setText(activity.getDate());
        holder.titleText.setText(activity.getTitle());
        holder.typeLabel.setText(activity.getType());
        
        // 根据状态设置不同的颜色
        switch (activity.getStatus()) {
            case "报名中":
                holder.statusLabel.setBackgroundResource(R.drawable.status_enrolling_background);
                break;
            case "进行中":
                holder.statusLabel.setBackgroundResource(R.drawable.status_ongoing_background);
                break;
            case "已结束":
                holder.statusLabel.setBackgroundResource(R.drawable.status_ended_background);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView activityImage;
        TextView statusLabel;
        TextView dateText;
        TextView titleText;
        TextView typeLabel;

        ViewHolder(View itemView) {
            super(itemView);
            activityImage = itemView.findViewById(R.id.activityImage);
            statusLabel = itemView.findViewById(R.id.statusLabel);
            dateText = itemView.findViewById(R.id.dateText);
            titleText = itemView.findViewById(R.id.titleText);
            typeLabel = itemView.findViewById(R.id.typeLabel);
        }
    }
} 