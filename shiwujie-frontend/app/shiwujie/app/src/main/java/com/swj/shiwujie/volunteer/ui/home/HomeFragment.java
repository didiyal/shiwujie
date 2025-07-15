package com.swj.shiwujie.volunteer.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.swj.shiwujie.R;

public class HomeFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_volunteer_home, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
} 