package com.shilpakala.artisan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends Activity {

    private LocalStore store;
    private LinearLayout root;

    private static final int BROWN = 0xFF7A3E16;
    private static final int GOLD = 0xFFD7A642;
    private static final int GOLD_DARK = 0xFF9A6A1E;
    private static final int GREEN = 0xFF2F6B4F;
    private static final int PAPER = 0xFFFFF8ED;
    private static final int TEXT = 0xFF2E241C;
    private static final int MUTED = 0xFF6D6259;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new LocalStore(this);
        renderHome();
    }

    private void renderHome() {
        LinearLayout content = scrollScreen("home", "Shilpa-Kala", "Photograph, brand, save and share artisan products.");
        
        boolean online = isOnline();
        content.addView(statusBand(online ? "Online" : "Offline", 
                online ? "Ready for future sync. Local tools remain active." : "No internet. Camera, gallery and sharing from local files still work.", 
                online));

        content.addView(card(new View[]{
                label("Create Product Photo", 22f, BROWN, true),
                label("Capture a product and add handmade Karnataka branding with price and craft details.", 14f, TEXT, false, 6),
                button("Open Camera", BROWN, v -> toast("Camera feature coming to Java soon!"))
        }));

        content.addView(card(new View[]{
                label("Portfolio", 22f, BROWN, true),
                label(store.products().size() + " saved product image(s) available offline.", 14f, TEXT, false, 6),
                button("Open Gallery", GREEN, v -> toast("Gallery feature coming to Java soon!"))
        }));

        content.addView(card(new View[]{
                label("Account", 22f, BROWN, true),
                label(store.profile().summary(), 14f, TEXT, false, 6),
                button("Edit Profile", GOLD_DARK, v -> toast("Profile feature coming to Java soon!")),
                button("Logout", Color.DKGRAY, v -> {
                    store.logout();
                    finish(); // Or navigate back to Login
                })
        }));

        setWrapped(content);
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private LinearLayout scrollScreen(String tag, String title, String subtitle) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int p = dp(18);
        int pb = dp(28);
        content.setPadding(p, dp(22), p, pb);
        content.setTag(tag);

        content.addView(label(title, 30f, BROWN, true));
        content.addView(label(subtitle, 14f, MUTED, false, 6));
        return content;
    }

    private void setWrapped(LinearLayout content) {
        root = content;
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(PAPER);
        scroll.addView(content);
        setContentView(scroll);
    }

    private LinearLayout card(View[] views) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        card.setPadding(p, p, p, p);
        card.setBackground(rounded(Color.WHITE, 12));
        card.setElevation(dp(2));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(18), 0, 0);
        card.setLayoutParams(params);

        for (View v : views) {
            card.addView(v);
        }
        return card;
    }

    private LinearLayout statusBand(String title, String body, boolean online) {
        LinearLayout band = new LinearLayout(this);
        band.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        band.setPadding(p, dp(14), p, dp(14));
        band.setBackground(rounded(online ? 0xFFE6F2EA : 0xFFFFEBCD, 12));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(16), 0, 0);
        band.setLayoutParams(params);

        band.addView(label(title, 18f, online ? GREEN : BROWN, true));
        band.addView(label(body, 13f, TEXT, false, 4));
        return band;
    }

    private TextView label(String text, float size, int color, boolean bold) {
        return label(text, size, color, bold, 0);
    }

    private TextView label(String text, float size, int color, boolean bold, int topMargin) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(size);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD);
        
        if (topMargin > 0) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, dp(topMargin), 0, 0);
            tv.setLayoutParams(params);
        }
        return tv;
    }

    private Button button(String text, int color, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(15f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setTextColor(Color.WHITE);
        btn.setBackground(rounded(color, 10));
        btn.setOnClickListener(listener);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(14), 0, 0);
        btn.setLayoutParams(params);
        return btn;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp(radius));
        return gd;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
