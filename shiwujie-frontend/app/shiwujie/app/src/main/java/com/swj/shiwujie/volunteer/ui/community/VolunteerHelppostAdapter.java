package com.swj.shiwujie.volunteer.ui.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.swj.shiwujie.R;
import com.swj.shiwujie.data.model.HelppostVO;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VolunteerHelppostAdapter extends RecyclerView.Adapter<VolunteerHelppostAdapter.HelppostViewHolder> {
    
    private List<HelppostVO> helppostList = new ArrayList<>();
    private OnHelppostClickListener onHelppostClickListener;
    private OnHelppostRespondClickListener onHelppostRespondClickListener;

    public interface OnHelppostClickListener {
        void onHelppostClick(HelppostVO helppost);
    }

    public interface OnHelppostRespondClickListener {
        void onHelppostRespondClick(HelppostVO helppost);
    }

    public void setHelppostList(List<HelppostVO> helppostList) {
        this.helppostList = helppostList;
        notifyDataSetChanged();
    }

    public void setOnHelppostClickListener(OnHelppostClickListener listener) {
        this.onHelppostClickListener = listener;
    }

    public void setOnHelppostRespondClickListener(OnHelppostRespondClickListener listener) {
        this.onHelppostRespondClickListener = listener;
    }

    @NonNull
    @Override
    public HelppostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_volunteer_helppost, parent, false);
        return new HelppostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HelppostViewHolder holder, int position) {
        HelppostVO helppost = helppostList.get(position);
        holder.bind(helppost);
    }

    @Override
    public int getItemCount() {
        return helppostList.size();
    }

    class HelppostViewHolder extends RecyclerView.ViewHolder {
        private TextView helppostIdText;
        private TextView postStatusText;
        private TextView helpContentText;
        private TextView helpLocationText;
        private TextView postTimeText;
        private TextView communityNameText;
        private Button respondHelppostButton;

        public HelppostViewHolder(@NonNull View itemView) {
            super(itemView);
            helppostIdText = itemView.findViewById(R.id.helppostIdText);
            postStatusText = itemView.findViewById(R.id.postStatusText);
            helpContentText = itemView.findViewById(R.id.helpContentText);
            helpLocationText = itemView.findViewById(R.id.helpLocationText);
            postTimeText = itemView.findViewById(R.id.postTimeText);
            communityNameText = itemView.findViewById(R.id.communityNameText);
            respondHelppostButton = itemView.findViewById(R.id.respondHelppostButton);
        }

        public void bind(HelppostVO helppost) {
            helppostIdText.setText("求助帖 #" + helppost.getHelppostId());
            postStatusText.setText(getStatusText(helppost.getPostStatus()));
            helpContentText.setText(helppost.getHelpContent());
            helpLocationText.setText(helppost.getHelpLocation());
            
            // 设置时间（使用当前时间，因为HelppostVO没有postTime字段）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            postTimeText.setText("发布时间: " + sdf.format(new Date()));
            
            // 设置社区名称（使用社区ID，因为HelppostVO没有communityName字段）
            if (helppost.getCommunityId() != null) {
                communityNameText.setText("社区ID: " + helppost.getCommunityId());
            } else {
                communityNameText.setText("社区: 未知");
            }

            // 设置点击事件
            itemView.setOnClickListener(v -> {
                if (onHelppostClickListener != null) {
                    onHelppostClickListener.onHelppostClick(helppost);
                }
            });

            // 设置响应按钮点击事件
            respondHelppostButton.setOnClickListener(v -> {
                if (onHelppostRespondClickListener != null) {
                    onHelppostRespondClickListener.onHelppostRespondClick(helppost);
                }
            });

            // 根据状态设置按钮可见性
            if ("待响应".equals(helppost.getPostStatus())) {
                respondHelppostButton.setVisibility(View.VISIBLE);
            } else {
                respondHelppostButton.setVisibility(View.GONE);
            }
        }

        private String getStatusText(String status) {
            if (status == null) return "未知状态";
            switch (status) {
                case "待响应":
                    return "待响应";
                case "已响应":
                    return "已响应";
                case "已完成":
                    return "已完成";
                case "已取消":
                    return "已取消";
                default:
                    return status;
            }
        }
    }
} 