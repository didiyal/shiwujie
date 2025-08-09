package com.swj.shiwujie.blind.ui.ai;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.swj.shiwujie.R;

public class AiFragment extends Fragment {
    private static final String TAG = "AiFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            Log.d(TAG, "AiFragment onCreateView called");
            View view = inflater.inflate(R.layout.fragment_ai_assistant, container, false);
            Log.d(TAG, "AiFragment layout inflated successfully");
            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error inflating AI fragment layout", e);
            // 返回一个简单的视图作为后备
            return new View(requireContext());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "AiFragment onViewCreated called");
        // 这里可以添加任何需要的初始化代码
    }
} 