package com.swj.shiwujie.volunteer.ui.message;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.swj.shiwujie.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MessageFragment extends Fragment {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_volunteer_message, container, false);
        
        viewPager = root.findViewById(R.id.view_pager);
        tabLayout = root.findViewById(R.id.tab_layout);
        
        setupViewPager();
        return root;
    }

    private void setupViewPager() {
        MessagePagerAdapter pagerAdapter = new MessagePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> tab.setText(position == 0 ? "聊天" : "群聊")
        ).attach();
    }

    // 消息数据模型
    private static class MessageItem {
        private final String name;
        private final String message;
        private final String time;

        public MessageItem(String name, String message, String time) {
            this.name = name;
            this.message = message;
            this.time = time;
        }
    }

    // 消息数据提供者
    private static class MessageDataProvider {
        public static List<MessageItem> getChatMessages() {
            return Arrays.asList(
                new MessageItem("陪同就医发帖回复", "你好，我这边有时间", "15m ago"),
                new MessageItem("3.26视频志愿者", "在接收帮助过程有没有遇到突发情况", "15m ago"),
                new MessageItem("3.24视频志愿者", "在接收帮助过程有没有遇到突发情况", "15m ago")
            );
        }

        public static List<MessageItem> getGroupMessages() {
            return Arrays.asList(
                new MessageItem("志愿者交流群", "最新通知：关于下周志愿服务安排", "10m ago"),
                new MessageItem("医疗援助群", "有新的援助需求，请查看", "30m ago")
            );
        }
    }

    // ViewPager适配器
    private static class MessagePagerAdapter extends FragmentStateAdapter {
        public MessagePagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return position == 0 ? new ChatListFragment() : new GroupChatListFragment();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    // 聊天列表Fragment
    public static class ChatListFragment extends Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_chat_list, container, false);
            RecyclerView recyclerView = root.findViewById(R.id.recycler_view);
            setupRecyclerView(recyclerView, true);
            return root;
        }

        protected void setupRecyclerView(RecyclerView recyclerView, boolean isChat) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            MessageAdapter adapter = new MessageAdapter();
            recyclerView.setAdapter(adapter);
            adapter.setMessages(isChat ? MessageDataProvider.getChatMessages() : MessageDataProvider.getGroupMessages());
        }
    }

    // 群聊列表Fragment
    public static class GroupChatListFragment extends ChatListFragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_chat_list, container, false);
            RecyclerView recyclerView = root.findViewById(R.id.recycler_view);
            setupRecyclerView(recyclerView, false);
            return root;
        }
    }

    // 消息列表适配器
    private static class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
        private List<MessageItem> messages = new ArrayList<>();

        public void setMessages(List<MessageItem> messages) {
            this.messages = messages;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            MessageItem message = messages.get(position);
            holder.bind(message);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        static class MessageViewHolder extends RecyclerView.ViewHolder {
            private final ImageView avatarView;
            private final TextView nameView;
            private final TextView messageView;
            private final TextView timeView;

            public MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                avatarView = itemView.findViewById(R.id.iv_avatar);
                nameView = itemView.findViewById(R.id.tv_name);
                messageView = itemView.findViewById(R.id.tv_message);
                timeView = itemView.findViewById(R.id.tv_time);
            }

            public void bind(MessageItem message) {
                nameView.setText(message.name);
                messageView.setText(message.message);
                timeView.setText(message.time);
                avatarView.setImageResource(R.drawable.logo);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
} 