package dev.umid.firetvlauncher;

import android.app.Service;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Fire OS 6 fallback. Reads only recent foreground transitions and acts only
 * when Amazon's protected Home activity appears. Nothing is saved or sent.
 */
public final class HomeMonitorService extends Service {
    private static final String TAG = "MinimalMonitor";
    private static final String AMAZON_HOME_PACKAGE = "com.amazon.tv.launcher";
    private static final String AMAZON_HOME_CLASS = "com.amazon.tv.launcher.ui.HomeActivity_vNext";
    private static final long CHECK_INTERVAL_MS = 250L;
    private static final long STARTUP_LOOKBACK_MS = 600_000L;
    private static final long MASK_TIMEOUT_MS = 8_000L;
    @SuppressLint("StaticFieldLeak")
    private static volatile HomeMonitorService connectedInstance;

    private final Handler handler = new Handler();
    private UsageStatsManager usageStats;
    private long lastHandledAmazonHomeEvent;
    private WindowManager windowManager;
    private FrameLayout mask;
    private final Runnable hardMaskTimeout = this::removeMaskImmediately;

    private final Runnable checkForeground = new Runnable() {
        @Override public void run() {
            inspectRecentEvents();
            handler.postDelayed(this, CHECK_INTERVAL_MS);
        }
    };

    public static void notifyLauncherWindowReady() {
        HomeMonitorService service = connectedInstance;
        if (service != null) service.handler.postDelayed(service::fadeAndRemoveMask, 100L);
    }

    public static boolean hasUsageAccess(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) return false;
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    @Override public void onCreate() {
        super.onCreate();
        connectedInstance = this;
        usageStats = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        handler.post(checkForeground);
        Log.i(TAG, "Fire OS 6 Home monitor ready");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void inspectRecentEvents() {
        if (usageStats == null) return;
        long now = System.currentTimeMillis();
        // Fire OS may delay BOOT_COMPLETED for several minutes behind Amazon's
        // startup queue, so the first pass needs to see the earlier Home event.
        UsageEvents events = usageStats.queryEvents(now - STARTUP_LOOKBACK_MS, now);
        UsageEvents.Event event = new UsageEvents.Event();
        long newestAmazonHomeEvent = 0L;
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND
                    && AMAZON_HOME_PACKAGE.equals(event.getPackageName())
                    && AMAZON_HOME_CLASS.equals(event.getClassName())) {
                newestAmazonHomeEvent = Math.max(newestAmazonHomeEvent, event.getTimeStamp());
            }
        }
        if (newestAmazonHomeEvent == 0L
                || newestAmazonHomeEvent <= lastHandledAmazonHomeEvent) return;
        lastHandledAmazonHomeEvent = newestAmazonHomeEvent;

        showMask();
        launchMinimalHome();
    }

    private void showMask() {
        removeMaskImmediately();
        if (windowManager == null) return;

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.rgb(8, 11, 16));

        ImageView wallpaper = new ImageView(this);
        wallpaper.setImageResource(R.drawable.wallpaper_mountains_public);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));

        View shade = new View(this);
        shade.setBackgroundColor(0x55000000);
        frame.addView(shade, new FrameLayout.LayoutParams(-1, -1));

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextColor(Color.WHITE);
        title.setTextSize(25);
        title.setTypeface(Typeface.create("sans", Typeface.BOLD));
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(-2, -2);
        titleParams.gravity = Gravity.TOP | Gravity.START;
        titleParams.leftMargin = dp(48);
        titleParams.topMargin = dp(25);
        frame.addView(title, titleParams);

        TextView status = new TextView(this);
        status.setText(R.string.opening);
        status.setTextColor(0xCCFFFFFF);
        status.setTextSize(17);
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(-2, -2);
        statusParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        statusParams.bottomMargin = dp(42);
        frame.addView(status, statusParams);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                -1, -1, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        try {
            windowManager.addView(frame, params);
            mask = frame;
            handler.removeCallbacks(hardMaskTimeout);
            handler.postDelayed(hardMaskTimeout, MASK_TIMEOUT_MS);
        } catch (RuntimeException error) {
            Log.w(TAG, "Could not show Home transition mask", error);
            mask = null;
        }
    }

    private void fadeAndRemoveMask() {
        if (mask == null) return;
        final FrameLayout fading = mask;
        fading.animate().alpha(0f).setDuration(220L).withEndAction(() -> {
            if (mask == fading) removeMaskImmediately();
        }).start();
    }

    private void removeMaskImmediately() {
        handler.removeCallbacks(hardMaskTimeout);
        if (mask != null && windowManager != null) {
            try {
                windowManager.removeView(mask);
            } catch (RuntimeException ignored) {
                // Already detached by Fire OS.
            }
        }
        mask = null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void launchMinimalHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(intent);
            Log.i(TAG, "Redirected Amazon Home to Minimal Home");
        } catch (RuntimeException error) {
            Log.e(TAG, "Home redirect failed", error);
        }
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(checkForeground);
        removeMaskImmediately();
        if (connectedInstance == this) connectedInstance = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
