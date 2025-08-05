package com.swj.shiwujie.blind.ui.community;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.swj.shiwujie.R;
import com.swj.shiwujie.data.model.ActivityVO;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * 盲人端活动列表适配器
 */
public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    private Context context;
    private List<ActivityVO> activityList;
    private OnActivityClickListener listener;

    public interface OnActivityClickListener {
        void onActivityDetailClick(ActivityVO activity);
        void onActivitySignUpClick(ActivityVO activity);
    }

    public ActivityAdapter(Context context, List<ActivityVO> activityList) {
        this.context = context;
        this.activityList = activityList;
    }

    public void setOnActivityClickListener(OnActivityClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ActivityVO activity = activityList.get(position);
        holder.bind(activity);
    }

    @Override
    public int getItemCount() {
        return activityList != null ? activityList.size() : 0;
    }

    public void updateData(List<ActivityVO> newActivityList) {
        this.activityList = newActivityList;
        notifyDataSetChanged();
    }

    class ActivityViewHolder extends RecyclerView.ViewHolder {
        private TextView activityNameText;
        private TextView activityContentText;
        private TextView activityLocationText;
        private TextView activityTimeText;
        private TextView activityStatusText;
        private TextView maxParticipantsText;
        private Button detailButton;
        private Button signUpButton;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            activityNameText = itemView.findViewById(R.id.activity_name);
            activityContentText = itemView.findViewById(R.id.activity_content);
            activityLocationText = itemView.findViewById(R.id.activity_location);
            activityTimeText = itemView.findViewById(R.id.activity_time);
            activityStatusText = itemView.findViewById(R.id.activity_status);
            maxParticipantsText = itemView.findViewById(R.id.max_participants);
            detailButton = itemView.findViewById(R.id.btn_detail);
            signUpButton = itemView.findViewById(R.id.btn_sign_up);
        }

        public void bind(ActivityVO activity) {
            activityNameText.setText(activity.getActivityName());
            activityContentText.setText(activity.getActivityContent());
            activityLocationText.setText("地点: " + activity.getActivityLocation());
            
            // 格式化时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String timeText = "";
            if (activity.getStartTime() != null) {
                timeText += "开始: " + sdf.format(activity.getStartTime());
            }
            if (activity.getEndTime() != null) {
                timeText += "\n结束: " + sdf.format(activity.getEndTime());
            }
            activityTimeText.setText(timeText);
            
            // 设置状态
            activityStatusText.setText("状态: " + getStatusText(activity.getActivityStatus()));
            
            // 设置人数限制
            if (activity.getMaxParticipants() != null) {
                maxParticipantsText.setText("人数限制: " + activity.getMaxParticipants() + "人");
            } else {
                maxParticipantsText.setText("人数限制: 不限");
            }

            // 设置按钮点击事件
            detailButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActivityDetailClick(activity);
                }
            });

            signUpButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActivitySignUpClick(activity);
                }
            });
        }

        private String getStatusText(String status) {
            switch (status) {
                case "0": return "未开始";
                case "1": return "进行中";
                case "2": return "已结束";
                case "3": return "已取消";
                default: return "未知";
            }
        }
    }
} 