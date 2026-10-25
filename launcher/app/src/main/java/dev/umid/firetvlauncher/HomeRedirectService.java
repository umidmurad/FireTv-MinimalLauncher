package dev.umid.firetvlauncher;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/** Fire OS 7 fallback that redirects the protected Amazon Home activity. */
public final class HomeRedirectService extends AccessibilityService {
    private static final String TAG = "MinimalRedirect";
    private static final String AMAZON_HOME_PACKAGE = "com.amazon.tv.launcher";
    private static final String AMAZON_HOME_CLASS = "com.amazon.tv.launcher.ui.HomeActivity_vNext";
    private static final long REDIRECT_DEBOUNCE_MS = 1_500L;
    private static final long MASK_TIMEOUT_MS = 8_000L;
    @SuppressLint("StaticFieldLeak")
    private static volatile HomeRedirectService connectedInstance;

    private final Handler handler = new Handler();
    private long lastRedirectAt;
    private WindowManager windowManager;
    private FrameLayout mask;
    private final Runnable hardMaskTimeout = this::removeMaskImmediately;

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        connectedInstance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Log.i(TAG, "Home redirect ready");
    }

    public static void notifyLauncherWindowReady() {
        HomeRedirectService service = connectedInstance;
        if (service != null) service.handler.postDelayed(service::fadeAndRemoveMask, 100L);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        String packageName = event.getPackageName() == null ? "" : event.getPackageName().toString();
        String className = event.getClassName() == null ? "" : event.getClassName().toString();
        if (getPackageName().equals(packageName)) return;
        if (!AMAZON_HOME_PACKAGE.equals(packageName) || !AMAZON_HOME_CLASS.equals(className)) return;

        long now = System.currentTimeMillis();
        if (now - lastRedirectAt < REDIRECT_DEBOUNCE_MS) return;
        lastRedirectAt = now;
        showMask();
        launchMinimalHome();
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
            removeMaskImmediately();
        }
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

        WindowManager.LayoutParams windowParams = new WindowManager.LayoutParams(
                -1, -1, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        windowParams.gravity = Gravity.TOP | Gravity.START;
        try {
            windowManager.addView(frame, windowParams);
            mask = frame;
            handler.removeCallbacks(hardMaskTimeout);
            handler.postDelayed(hardMaskTimeout, MASK_TIMEOUT_MS);
        } catch (RuntimeException error) {
            Log.w(TAG, "Could not show redirect mask", error);
            mask = null;
        }
    }

    private void fadeAndRemoveMask() {
        if (mask == null) return;
        final FrameLayout fading = mask;
        fading.animate().alpha(0f).setDuration(260L).withEndAction(() -> {
            if (mask == fading) removeMaskImmediately();
        }).start();
    }

    private void removeMaskImmediately() {
        handler.removeCallbacks(hardMaskTimeout);
        if (mask != null && windowManager != null) {
            try {
                windowManager.removeView(mask);
            } catch (RuntimeException ignored) {
                // Already detached by the system.
            }
        }
        mask = null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public void onInterrupt() {
        removeMaskImmediately();
    }

    @Override public boolean onUnbind(Intent intent) {
        if (connectedInstance == this) connectedInstance = null;
        removeMaskImmediately();
        return super.onUnbind(intent);
    }
}
