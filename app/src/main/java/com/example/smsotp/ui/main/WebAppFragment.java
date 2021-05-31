package com.example.smsotp.ui.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.example.smsotp.R;
import com.example.smsotp.WebService;
import com.example.smsotp.databinding.FragmentWebAppBinding;
import com.example.smsotp.ui.SettingsFragment;

public class WebAppFragment extends Fragment {
    private static final String TAG = "WebAppFragment";
    private FragmentWebAppBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        setHasOptionsMenu(true);
        binding = FragmentWebAppBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String port = sp.getString(SettingsFragment.KEY_PREF_PORT, "8080");

        WebView webView = binding.webView;
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (binding != null) {
                    binding.refreshLayout.setRefreshing(false);
                }
            }
        });
        binding.refreshLayout.setOnRefreshListener(webView::reload);

        Context context = requireContext();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED) {
            // Only if we have SMS permission, we start the server automatically
            context.startService(new Intent(context, WebService.class));
        }
        WebService.isRunning.observe(getViewLifecycleOwner(), isReady -> {
            webView.loadUrl("http://localhost:" + port);
//            Log.e(TAG, "Observed isRunning new value " + isReady);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_webapp, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            binding.refreshLayout.setRefreshing(true);
            binding.webView.reload();
            return true;
        }
        return false;
    }
}