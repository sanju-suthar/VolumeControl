package com.realme.edgevolume;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    public static final String PREFS_NAME = "EdgeVolumePrefs";
    public static final String KEY_SERVICE_ENABLED = "service_enabled";
    public static final String KEY_LEFT_ENABLED = "left_enabled";
    public static final String KEY_RIGHT_ENABLED = "right_enabled";
    public static final String KEY_INVISIBLE = "strip_invisible";
    public static final String KEY_WIDTH_DP = "edge_width_dp";
    public static final String KEY_HEIGHT_PCT = "edge_height_pct";
    public static final String KEY_SWIPE_SPEED = "swipe_speed";

    public static final String ACTION_UPDATE_OVERLAY = "com.realme.edgevolume.ACTION_UPDATE_OVERLAY";
    public static final String CHANNEL_ID = "edge_volume_service_channel";

    private SharedPreferences prefs;
    private Switch masterSwitch;
    private TextView statusBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor("#0F172A"));
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(18);
        root.setPadding(pad, pad, pad, pad);
        scrollView.addView(root);

        // App Header
        TextView title = new TextView(this);
        title.setText("Realme 6 Edge Volume");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Swipe Up/Down on screen edges to control Media Volume");
        subtitle.setTextColor(Color.parseColor("#94A3B8"));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        subtitle.setPadding(0, dpToPx(4), 0, dpToPx(14));
        root.addView(subtitle);

        // Status Banner
        statusBanner = new TextView(this);
        statusBanner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        statusBanner.setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10));
        LinearLayout.LayoutParams bannerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bannerLp.bottomMargin = dpToPx(14);
        statusBanner.setLayoutParams(bannerLp);
        root.addView(statusBanner);

        // Card 1: Master Service & Edge Toggles
        LinearLayout toggleCard = createCard();
        root.addView(toggleCard);

        masterSwitch = addSwitchRow(toggleCard, "Enable 24/7 Edge Volume Service",
                prefs.getBoolean(KEY_SERVICE_ENABLED, true), new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked && !Settings.canDrawOverlays(MainActivity.this)) {
                            buttonView.setChecked(false);
                            requestOverlayPermission();
                            return;
                        }
                        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, isChecked).apply();
                        if (isChecked) {
                            startEdgeService();
                        } else {
                            stopService(new Intent(MainActivity.this, EdgeService.class));
                        }
                        updateStatusBanner();
                    }
                });

        addSwitchRow(toggleCard, "Left Screen Edge Active",
                prefs.getBoolean(KEY_LEFT_ENABLED, true), new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        prefs.edit().putBoolean(KEY_LEFT_ENABLED, isChecked).apply();
                        notifyServiceUpdate();
                    }
                });

        addSwitchRow(toggleCard, "Right Screen Edge Active",
                prefs.getBoolean(KEY_RIGHT_ENABLED, true), new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        prefs.edit().putBoolean(KEY_RIGHT_ENABLED, isChecked).apply();
                        notifyServiceUpdate();
                    }
                });

        addSwitchRow(toggleCard, "100% Invisible Edge Strip",
                prefs.getBoolean(KEY_INVISIBLE, false), new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        prefs.edit().putBoolean(KEY_INVISIBLE, isChecked).apply();
                        notifyServiceUpdate();
                    }
                });

        // Card 2: SeekBars for Edge Width, Height %, and Swipe Speed
        LinearLayout sliderCard = createCard();
        root.addView(sliderCard);

        int currentWidth = prefs.getInt(KEY_WIDTH_DP, 18);
        addSeekBarRow(sliderCard, "Edge Width", " dp", 5, 60, currentWidth, new SeekBarValueListener() {
            @Override
            public void onValueChanged(int value) {
                prefs.edit().putInt(KEY_WIDTH_DP, value).apply();
                notifyServiceUpdate();
            }
        });

        int currentHeight = prefs.getInt(KEY_HEIGHT_PCT, 60);
        addSeekBarRow(sliderCard, "Edge Height", "%", 20, 100, currentHeight, new SeekBarValueListener() {
            @Override
            public void onValueChanged(int value) {
                prefs.edit().putInt(KEY_HEIGHT_PCT, value).apply();
                notifyServiceUpdate();
            }
        });

        int currentSpeed = prefs.getInt(KEY_SWIPE_SPEED, 6);
        addSeekBarRow(sliderCard, "Swipe Speed (Sensitivity)", "x", 1, 10, currentSpeed, new SeekBarValueListener() {
            @Override
            public void onValueChanged(int value) {
                prefs.edit().putInt(KEY_SWIPE_SPEED, value).apply();
                notifyServiceUpdate();
            }
        });

        // Card 3: Quick Test Volume Buttons (Helpful when physical buttons are broken)
        LinearLayout testCard = createCard();
        root.addView(testCard);

        TextView testTitle = new TextView(this);
        testTitle.setText("Direct Media Volume Control");
        testTitle.setTextColor(Color.WHITE);
        testTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        testTitle.setTypeface(null, Typeface.BOLD);
        testTitle.setPadding(0, 0, 0, dpToPx(10));
        testCard.addView(testTitle);

        LinearLayout volBtnRow = new LinearLayout(this);
        volBtnRow.setOrientation(LinearLayout.HORIZONTAL);
        testCard.addView(volBtnRow);

        Button volDownBtn = createActionButton("Volume -", "#334155");
        LinearLayout.LayoutParams halfLp1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        halfLp1.rightMargin = dpToPx(6);
        volDownBtn.setLayoutParams(halfLp1);
        volDownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                }
            }
        });
        volBtnRow.addView(volDownBtn);

        Button volUpBtn = createActionButton("Volume +", "#2563EB");
        LinearLayout.LayoutParams halfLp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        halfLp2.leftMargin = dpToPx(6);
        volUpBtn.setLayoutParams(halfLp2);
        volUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                }
            }
        });
        volBtnRow.addView(volUpBtn);

        // Card 4: Permissions, Battery Optimization & Notification Shortcuts
        LinearLayout shortcutCard = createCard();
        root.addView(shortcutCard);

        TextView shortcutHeader = new TextView(this);
        shortcutHeader.setText("Permissions & Notification Fixes");
        shortcutHeader.setTextColor(Color.WHITE);
        shortcutHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        shortcutHeader.setTypeface(null, Typeface.BOLD);
        shortcutHeader.setPadding(0, 0, 0, dpToPx(10));
        shortcutCard.addView(shortcutHeader);

        Button overlayBtn = createActionButton("1. Grant 'Display Over Other Apps' Permission", "#0284C7");
        overlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOverlayPermission();
            }
        });
        shortcutCard.addView(overlayBtn);

        Button batteryBtn = createActionButton("2. Disable Battery Optimization (24/7 Active)", "#059669");
        batteryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestIgnoreBatteryOptimizations();
            }
        });
        shortcutCard.addView(batteryBtn);

        Button appNotifBtn = createActionButton("3. Turn Off App Background Notification", "#4F46E5");
        appNotifBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAppNotificationSettings();
            }
        });
        shortcutCard.addView(appNotifBtn);

        Button sysNotifBtn = createActionButton("4. Turn Off 'Displaying Over Other Apps' System Alert", "#D97706");
        sysNotifBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSystemOverlayNotificationSettings();
            }
        });
        shortcutCard.addView(sysNotifBtn);

        setContentView(scrollView);

        // Auto-start service if overlay permission is already granted
        if (Settings.canDrawOverlays(this) && prefs.getBoolean(KEY_SERVICE_ENABLED, true)) {
            startEdgeService();
        }
        updateStatusBanner();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Settings.canDrawOverlays(this) && prefs.getBoolean(KEY_SERVICE_ENABLED, true)) {
            masterSwitch.setChecked(true);
            startEdgeService();
        }
        updateStatusBanner();
    }

    private void updateStatusBanner() {
        boolean hasOverlay = Settings.canDrawOverlays(this);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean ignoringBattery = (pm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                && pm.isIgnoringBatteryOptimizations(getPackageName());
        boolean enabled = prefs.getBoolean(KEY_SERVICE_ENABLED, true);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(10));

        if (!hasOverlay) {
            bg.setColor(Color.parseColor("#7F1D1D"));
            statusBanner.setTextColor(Color.parseColor("#FECACA"));
            statusBanner.setText("Action Needed: Tap Button #1 below to allow 'Display over other apps'.");
        } else if (!enabled) {
            bg.setColor(Color.parseColor("#334155"));
            statusBanner.setTextColor(Color.parseColor("#E2E8F0"));
            statusBanner.setText("Service Paused: Turn on the top switch to activate edge gestures.");
        } else if (!ignoringBattery) {
            bg.setColor(Color.parseColor("#78350F"));
            statusBanner.setTextColor(Color.parseColor("#FDE68A"));
            statusBanner.setText("Edge Service Active! Tap Button #2 below to disable Battery Optimization for 24/7 reliability.");
        } else {
            bg.setColor(Color.parseColor("#064E3B"));
            statusBanner.setTextColor(Color.parseColor("#A7F3D0"));
            statusBanner.setText("Edge Service Active 24/7 (Overlay + Unrestricted Battery Ready)");
        }
        statusBanner.setBackground(bg);
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int p = dpToPx(16);
        card.setPadding(p, p, p, p);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1E293B"));
        bg.setCornerRadius(dpToPx(14));
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dpToPx(14);
        card.setLayoutParams(lp);
        return card;
    }

    private Switch addSwitchRow(LinearLayout parent, String labelText, boolean checked,
                                CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dpToPx(8), 0, dpToPx(8));

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        label.setLayoutParams(lp);

        Switch sw = new Switch(this);
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener(listener);

        row.addView(label);
        row.addView(sw);
        parent.addView(row);
        return sw;
    }

    private interface SeekBarValueListener {
        void onValueChanged(int value);
    }

    private void addSeekBarRow(LinearLayout parent, final String title, final String unit,
                               final int min, final int max, int current,
                               final SeekBarValueListener listener) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, dpToPx(8), 0, dpToPx(8));

        final TextView header = new TextView(this);
        header.setText(title + ": " + current + unit);
        header.setTextColor(Color.parseColor("#E2E8F0"));
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        container.addView(header);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(max - min);
        seekBar.setProgress(Math.max(0, Math.min(max - min, current - min)));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int val = min + progress;
                header.setText(title + ": " + val + unit);
                if (fromUser) {
                    listener.onValueChanged(val);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {}

            @Override
            public void onStopTrackingTouch(SeekBar sb) {}
        });
        container.addView(seekBar);
        parent.addView(container);
    }

    private Button createActionButton(String text, String hexColor) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(hexColor));
        bg.setCornerRadius(dpToPx(10));
        btn.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dpToPx(8);
        btn.setLayoutParams(lp);
        return btn;
    }

    private void requestOverlayPermission() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open Overlay settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestIgnoreBatteryOptimizations() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception ex) {
                Toast.makeText(this, "Unable to open Battery settings", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openAppNotificationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID);
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                fallback.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(fallback);
            } catch (Exception ex) {
                Toast.makeText(this, "Unable to open App Notification settings", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openSystemOverlayNotificationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, "android");
            startActivity(intent);
            Toast.makeText(this, "Turn off 'Displaying over other apps' toggle here", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:android"));
                startActivity(fallback);
            } catch (Exception ex) {
                Toast.makeText(this, "Unable to open Android System notification settings", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startEdgeService() {
        if (!Settings.canDrawOverlays(this)) return;
        Intent serviceIntent = new Intent(this, EdgeService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void notifyServiceUpdate() {
        if (!Settings.canDrawOverlays(this)) return;
        if (prefs.getBoolean(KEY_SERVICE_ENABLED, true)) {
            Intent intent = new Intent(this, EdgeService.class);
            intent.setAction(ACTION_UPDATE_OVERLAY);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // =========================================================================
    // 24/7 FOREGROUND OVERLAY SERVICE (NO VIBRATION, NO ACCESSIBILITY SERVICE)
    // =========================================================================
    public static class EdgeService extends Service {

        private WindowManager windowManager;
        private AudioManager audioManager;
        private SharedPreferences prefs;
        private View leftEdgeView;
        private View rightEdgeView;

        @Override
        public void onCreate() {
            super.onCreate();
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            startInForeground();
            rebuildEdgeOverlays();
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startInForeground();
            rebuildEdgeOverlays();
            return START_STICKY;
        }

        private void startInForeground() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (nm != null) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            "Edge Volume Background Service",
                            NotificationManager.IMPORTANCE_MIN
                    );
                    channel.setDescription("Keeps screen edge volume gestures active 24/7");
                    channel.setShowBadge(false);
                    channel.enableVibration(false);
                    channel.setSound(null, null);
                    nm.createNotificationChannel(channel);
                }

                Intent openUi = new Intent(this, MainActivity.class);
                int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    piFlags |= PendingIntent.FLAG_IMMUTABLE;
                }
                PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openUi, piFlags);

                Notification notification = new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle("Edge Volume Active")
                        .setContentText("Swipe screen edges to adjust media volume")
                        .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                        .setContentIntent(contentIntent)
                        .setOngoing(true)
                        .build();

                startForeground(1001, notification);
            }
        }

        private void rebuildEdgeOverlays() {
            removeEdgeOverlays();

            if (!Settings.canDrawOverlays(this) || !prefs.getBoolean(KEY_SERVICE_ENABLED, true)) {
                return;
            }

            boolean leftEnabled = prefs.getBoolean(KEY_LEFT_ENABLED, true);
            boolean rightEnabled = prefs.getBoolean(KEY_RIGHT_ENABLED, true);
            boolean invisible = prefs.getBoolean(KEY_INVISIBLE, false);
            int widthDp = prefs.getInt(KEY_WIDTH_DP, 18);
            int heightPct = prefs.getInt(KEY_HEIGHT_PCT, 60);

            DisplayMetrics dm = getResources().getDisplayMetrics();
            int widthPx = Math.max(10, Math.round(widthDp * dm.density));
            int heightPx = Math.max(100, Math.round(dm.heightPixels * (heightPct / 100f)));

            if (leftEnabled) {
                leftEdgeView = createEdgeStripView(invisible);
                WindowManager.LayoutParams leftLp = createOverlayLayoutParams(widthPx, heightPx,
                        Gravity.LEFT | Gravity.CENTER_VERTICAL);
                try {
                    windowManager.addView(leftEdgeView, leftLp);
                } catch (Exception ignored) {}
            }

            if (rightEnabled) {
                rightEdgeView = createEdgeStripView(invisible);
                WindowManager.LayoutParams rightLp = createOverlayLayoutParams(widthPx, heightPx,
                        Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                try {
                    windowManager.addView(rightEdgeView, rightLp);
                } catch (Exception ignored) {}
            }
        }

        private WindowManager.LayoutParams createOverlayLayoutParams(int widthPx, int heightPx, int gravity) {
            int overlayType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;

            int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    widthPx, heightPx, overlayType, flags, PixelFormat.TRANSLUCENT);
            lp.gravity = gravity;
            return lp;
        }

        private View createEdgeStripView(boolean invisible) {
            View view = new View(this);
            // Strictly disable all haptic feedback / vibration
            view.setHapticFeedbackEnabled(false);

            if (invisible) {
                view.setBackgroundColor(Color.TRANSPARENT);
            } else {
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.parseColor("#5538BDF8"));
                gd.setCornerRadius(16f);
                view.setBackground(gd);
            }

            view.setOnTouchListener(new View.OnTouchListener() {
                private float lastY = 0f;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int speed = prefs.getInt(KEY_SWIPE_SPEED, 6);
                    float density = getResources().getDisplayMetrics().density;
                    // Higher speed (1..10) = smaller swipe distance required per volume step
                    float thresholdPx = Math.max(8f * density, (48f - (speed * 4f)) * density);

                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            lastY = event.getRawY();
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float currentY = event.getRawY();
                            float deltaY = currentY - lastY;

                            if (Math.abs(deltaY) >= thresholdPx) {
                                int steps = (int) (Math.abs(deltaY) / thresholdPx);
                                int direction = (deltaY < 0)
                                        ? AudioManager.ADJUST_RAISE   // Swipe Up -> Volume Increase
                                        : AudioManager.ADJUST_LOWER;  // Swipe Down -> Volume Decrease

                                if (audioManager != null) {
                                    for (int i = 0; i < steps; i++) {
                                        audioManager.adjustStreamVolume(
                                                AudioManager.STREAM_MUSIC,
                                                direction,
                                                AudioManager.FLAG_SHOW_UI
                                        );
                                    }
                                }
                                lastY = currentY;
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            return true;
                    }
                    return false;
                }
            });

            return view;
        }

        private void removeEdgeOverlays() {
            if (windowManager != null) {
                if (leftEdgeView != null) {
                    try {
                        windowManager.removeView(leftEdgeView);
                    } catch (Exception ignored) {}
                    leftEdgeView = null;
                }
                if (rightEdgeView != null) {
                    try {
                        windowManager.removeView(rightEdgeView);
                    } catch (Exception ignored) {}
                    rightEdgeView = null;
                }
            }
        }

        @Override
        public void onTaskRemoved(Intent rootIntent) {
            super.onTaskRemoved(rootIntent);
            if (prefs != null && prefs.getBoolean(KEY_SERVICE_ENABLED, true)) {
                Intent restartIntent = new Intent(getApplicationContext(), EdgeService.class);
                int piFlags = PendingIntent.FLAG_ONE_SHOT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    piFlags |= PendingIntent.FLAG_IMMUTABLE;
                }
                PendingIntent pi = PendingIntent.getService(getApplicationContext(), 101, restartIntent, piFlags);
                AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (am != null) {
                    am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 1000, pi);
                }
            }
        }

        @Override
        public void onDestroy() {
            removeEdgeOverlays();
            super.onDestroy();
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    // =========================================================================
    // AUTO-START BOOT RECEIVER
    // =========================================================================
    public static class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            if (prefs.getBoolean(KEY_SERVICE_ENABLED, true) && Settings.canDrawOverlays(context)) {
                Intent serviceIntent = new Intent(context, EdgeService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
    }
}
