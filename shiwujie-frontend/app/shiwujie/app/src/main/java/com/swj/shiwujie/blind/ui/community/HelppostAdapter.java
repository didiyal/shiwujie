package com.swj.shiwujie.blind.ui.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class HelppostAdapter extends RecyclerView.Adapter<HelppostAdapter.HelppostViewHolder> {
    
    private List<HelppostVO> helppostList;
    private OnHelppostClickListener listener;
    
    public HelppostAdapter() {
        this.helppostList = new ArrayList<>();
    }
    
    public void setHelppostList(List<HelppostVO> helppostList) {
        this.helppostList = helppostList;
        notifyDataSetChanged();
    }
    
    public void setOnHelppostClickListener(OnHelppostClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public HelppostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_helppost, parent, false);
        return new HelppostViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull HelppostViewHolder holder, int position) {
        HelppostVO helppost = helppostList.get(position);
        holder.bind(helppost);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHelppostClick(helppost);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return helppostList.size();
    }
    
    static class HelppostViewHolder extends RecyclerView.ViewHolder {
        private TextView helppostIdText;
        private TextView postStatusText;
        private TextView helpContentText;
        private TextView helpLocationText;
        private TextView postTimeText;
        private TextView communityNameText;
        
        public HelppostViewHolder(@NonNull View itemView) {
            super(itemView);
            helppostIdText = itemView.findViewById(R.id.helppostIdText);
            postStatusText = itemView.findViewById(R.id.postStatusText);
            helpContentText = itemView.findViewById(R.id.helpContentText);
            helpLocationText = itemView.findViewById(R.id.helpLocationText);
            postTimeText = itemView.findViewById(R.id.postTimeText);
            communityNameText = itemView.findViewById(R.id.communityNameText);
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
            
            // 设置发布时间（这里使用当前时间，实际应该从后端获取）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            postTimeText.setText(sdf.format(new Date()));
            
            // 设置社区名称（这里暂时使用默认值，实际应该从社区信息获取）
            communityNameText.setText("我的社区");
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
} 