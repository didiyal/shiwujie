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

public class VolunteerHelppostAdapter extends RecyclerView.Adapter<VolunteerHelppostAdapter.VolunteerHelppostViewHolder> {
    
    private List<HelppostVO> helppostList;
    private OnHelppostClickListener listener;
    private OnAcceptClickListener acceptListener;
    
    public VolunteerHelppostAdapter() {
        this.helppostList = new ArrayList<>();
    }
    
    public void setHelppostList(List<HelppostVO> helppostList) {
        this.helppostList = helppostList;
        notifyDataSetChanged();
    }
    
    public void setOnHelppostClickListener(OnHelppostClickListener listener) {
        this.listener = listener;
    }
    
    public void setOnAcceptClickListener(OnAcceptClickListener acceptListener) {
        this.acceptListener = acceptListener;
    }
    
    @NonNull
    @Override
    public VolunteerHelppostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_volunteer_helppost, parent, false);
        return new VolunteerHelppostViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull VolunteerHelppostViewHolder holder, int position) {
        HelppostVO helppost = helppostList.get(position);
        holder.bind(helppost);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHelppostClick(helppost);
            }
        });
        
        holder.acceptButton.setOnClickListener(v -> {
            if (acceptListener != null) {
                acceptListener.onAcceptClick(helppost);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return helppostList.size();
    }
    
    static class VolunteerHelppostViewHolder extends RecyclerView.ViewHolder {
        private TextView helppostIdText;
        private TextView postStatusText;
        private TextView helpContentText;
        private TextView helpLocationText;
        private TextView postTimeText;
        private TextView communityNameText;
        private Button acceptButton;
        
        public VolunteerHelppostViewHolder(@NonNull View itemView) {
            super(itemView);
            helppostIdText = itemView.findViewById(R.id.helppostIdText);
            postStatusText = itemView.findViewById(R.id.postStatusText);
            helpContentText = itemView.findViewById(R.id.helpContentText);
            helpLocationText = itemView.findViewById(R.id.helpLocationText);
            postTimeText = itemView.findViewById(R.id.postTimeText);
            communityNameText = itemView.findViewById(R.id.communityNameText);
            acceptButton = itemView.findViewById(R.id.acceptButton);
        }
        
        public void bind(HelppostVO helppost) {
            // 设置求助帖ID
            helppostIdText.setText("求助帖 #" + helppost.getHelppostId());
            
            // 设置状态
            String status = getStatusText(helppost.getPostStatus());
            postStatusText.setText(status);
            
            // 设置求助内容
            helpContentText.setText(helppost.getHelpContent());
            
            // 设置求助地点
            helpLocationText.setText(helppost.getHelpLocation());
            
            // 设置发布时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            if (helppost.getCreateTime() != null) {
                postTimeText.setText(sdf.format(helppost.getCreateTime()));
            } else {
                postTimeText.setText(sdf.format(new Date()));
            }
            
            // 设置社区名称
            communityNameText.setText("我的社区");
            
            // 根据状态设置按钮 - 只显示接受求助按钮，不显示已处理按钮
            String postStatus = helppost.getPostStatus();
            
            // 如果状态是待响应，显示接受求助按钮
            if (postStatus != null && ("PENDING".equals(postStatus) || "0".equals(postStatus))) {
                acceptButton.setVisibility(View.VISIBLE);
                acceptButton.setText("接受求助");
                acceptButton.setEnabled(true);
            } else {
                // 如果状态不是待响应，隐藏按钮
                acceptButton.setVisibility(View.GONE);
            }
        }
        
        private String getStatusText(String status) {
            if (status == null) {
                return "待响应";
            }
            switch (status) {
                case "PENDING":
                    return "待响应";
                case "RESPONDED":
                    return "已响应";
                case "COMPLETED":
                    return "已完成";
                case "CANCELLED":
                    return "已取消";
                default:
                    return "待响应";
            }
        }
    }
    
    public interface OnHelppostClickListener {
        void onHelppostClick(HelppostVO helppost);
    }
    
    public interface OnAcceptClickListener {
        void onAcceptClick(HelppostVO helppost);
    }
} 