package com.example.smsotp;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class FirstFragment extends Fragment {
    private Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = requireActivity().getBaseContext();

//        view.findViewById(R.id.button_first).setOnClickListener(view12 ->
//                NavHostFragment.findNavController(FirstFragment.this)
//                        .navigate(R.id.action_FirstFragment_to_SecondFragment));

        Button firstButton = view.findViewById(R.id.button_first);
        firstButton.setOnClickListener(v -> {
            new Thread(() -> Log.d("TEST", "HELLO " + AppDatabase.getInstance(context).userDao().getAll()))
                    .start();
        });
    }
}