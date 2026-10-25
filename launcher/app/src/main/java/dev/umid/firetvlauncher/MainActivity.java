package dev.umid.firetvlauncher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final long WALLPAPER_INTERVAL_MS = 90_000L;
    private static final int GRID_COLUMNS = 5;

    // Alphabetical by the label shown on screen.
    private static final AppEntry[] APPS = {
            AppEntry.appstore("Amazon Appstore", "com.amazon.venezia"),
            AppEntry.app("Apple TV", "com.apple.atve.amazon.appletv"),
            AppEntry.app("Disney+", "com.disney.disneyplus"),
            AppEntry.app("Downloader", "com.esaba.downloader"),
            AppEntry.app("Hulu", "com.hulu.plus"),
            AppEntry.app("Max", "com.hbo.hbonow"),
            AppEntry.app("Netflix", "com.netflix.ninja"),
            AppEntry.app("Paramount+", "com.cbs.ott"),
            AppEntry.app("Peacock", "com.peacock.peacockfiretv"),
            AppEntry.app("Prime Video", "com.amazon.firebat"),
            AppEntry.settings("Settings", "com.amazon.tv.settings.v2"),
            AppEntry.app("Tubi", "com.tubitv.ott"),
            AppEntry.app("YouTube", "com.amazon.firetv.youtube")
    };

    private static final int[] WALLPAPERS = {
            R.drawable.wallpaper_mountains_public,
            R.drawable.wallpaper_lake_public,
            R.drawable.wallpaper_coast_public
    };

    private final Handler handler = new Handler();
    private ImageView background;
    private TextView clock;
    private int wallpaperIndex;

    private final Runnable rotateWallpaper = new Runnable() {
        @Override public void run() {
            wallpaperIndex = (wallpaperIndex + 1) % WALLPAPERS.length;
            Drawable current = background.getDrawable();
            Drawable next = getResources().getDrawable(WALLPAPERS[wallpaperIndex]);
            TransitionDrawable transition = new TransitionDrawable(new Drawable[]{current, next});
            transition.setCrossFadeEnabled(true);
            background.setImageDrawable(transition);
            transition.startTransition(1400);
            handler.postDelayed(this, WALLPAPER_INTERVAL_MS);
        }
    };

    private final Runnable updateClock = new Runnable() {
        @Override public void run() {
            if (clock != null) {
                clock.setText(new SimpleDateFormat("h:mm", Locale.getDefault()).format(new Date()));
            }
            handler.postDelayed(this, 15_000L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUi();
        if (HomeMonitorService.hasUsageAccess(this)) {
            startService(new Intent(this, HomeMonitorService.class));
        }
        buildUi();
    }

    @Override protected void onResume() {
        super.onResume();
        hideSystemUi();
        handler.removeCallbacks(rotateWallpaper);
        handler.removeCallbacks(updateClock);
        handler.postDelayed(rotateWallpaper, WALLPAPER_INTERVAL_MS);
        handler.post(updateClock);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            HomeRedirectService.notifyLauncherWindowReady();
            HomeMonitorService.notifyLauncherWindowReady();
        }
    }

    @Override protected void onPause() {
        handler.removeCallbacks(rotateWallpaper);
        handler.removeCallbacks(updateClock);
        super.onPause();
    }

    @Override public void onBackPressed() {
        // A home screen has nowhere to navigate back to.
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            launchSettings();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void buildUi() {
        final FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(8, 11, 16));

        wallpaperIndex = (int) ((System.currentTimeMillis() / WALLPAPER_INTERVAL_MS) % WALLPAPERS.length);
        background = new ImageView(this);
        background.setScaleType(ImageView.ScaleType.CENTER_CROP);
        background.setImageResource(WALLPAPERS[wallpaperIndex]);
        root.addView(background, matchParent());

        View shade = new View(this);
        GradientDrawable shadeDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x52000000, 0x65000000, 0xD6000000});
        shade.setBackground(shadeDrawable);
        root.addView(shade, matchParent());

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(Typeface.create("sans", Typeface.BOLD));
        title.setLetterSpacing(0.04f);
        FrameLayout.LayoutParams titleParams = wrapContent();
        titleParams.gravity = Gravity.TOP | Gravity.START;
        titleParams.leftMargin = dp(48);
        titleParams.topMargin = dp(25);
        root.addView(title, titleParams);

        clock = new TextView(this);
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(27);
        clock.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        FrameLayout.LayoutParams clockParams = wrapContent();
        clockParams.gravity = Gravity.TOP | Gravity.END;
        clockParams.rightMargin = dp(49);
        clockParams.topMargin = dp(20);
        root.addView(clock, clockParams);

        final ScrollView scroller = new ScrollView(this);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setFillViewport(true);
        scroller.setClipChildren(false);
        scroller.setClipToPadding(false);
        scroller.setPadding(dp(17), dp(10), dp(17), dp(18));
        scroller.setBackground(rounded(0x6611151D, 26, 0, 0));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(GRID_COLUMNS);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        grid.setPadding(dp(10), dp(8), dp(10), dp(14));
        scroller.addView(grid, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View first = null;
        for (int index = 0; index < APPS.length; index++) {
            AppEntry app = APPS[index];
            View tile = createAppTile(app, scroller);
            GridLayout.LayoutParams tileParams = new GridLayout.LayoutParams(
                    GridLayout.spec(index / GRID_COLUMNS),
                    GridLayout.spec(index % GRID_COLUMNS, 1f));
            tileParams.width = 0;
            tileParams.height = dp(124);
            tileParams.setMargins(dp(6), dp(6), dp(6), dp(6));
            grid.addView(tile, tileParams);
            if (first == null) first = tile;
        }

        FrameLayout.LayoutParams gridParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        gridParams.gravity = Gravity.TOP;
        gridParams.leftMargin = dp(40);
        gridParams.rightMargin = dp(40);
        gridParams.topMargin = dp(70);
        gridParams.bottomMargin = dp(28);
        root.addView(scroller, gridParams);

        TextView hint = new TextView(this);
        hint.setText(R.string.menu_settings);
        hint.setTextColor(0xBFFFFFFF);
        hint.setTextSize(12);
        FrameLayout.LayoutParams hintParams = wrapContent();
        hintParams.gravity = Gravity.BOTTOM | Gravity.END;
        hintParams.rightMargin = dp(49);
        hintParams.bottomMargin = dp(8);
        root.addView(hint, hintParams);

        setContentView(root);
        if (first != null) first.requestFocus();
    }

    private View createAppTile(final AppEntry app, final ScrollView scroller) {
        final LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setPadding(dp(8), dp(9), dp(8), dp(7));
        tile.setBackground(rounded(0x5AFFFFFF, 18, 0, 0));

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        try {
            icon.setImageDrawable(getPackageManager().getApplicationIcon(app.iconPackage));
        } catch (PackageManager.NameNotFoundException ignored) {
            icon.setImageResource(R.drawable.ic_launcher);
            tile.setAlpha(0.58f);
        }
        tile.addView(icon, new LinearLayout.LayoutParams(dp(72), dp(72)));

        TextView label = new TextView(this);
        label.setText(app.label);
        label.setTextColor(Color.WHITE);
        label.setTextSize(13);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34));
        labelParams.topMargin = dp(2);
        tile.addView(label, labelParams);

        tile.setOnFocusChangeListener((view, focused) -> {
            view.animate().scaleX(focused ? 1.07f : 1f).scaleY(focused ? 1.07f : 1f)
                    .setDuration(130).start();
            view.setBackground(focused
                    ? rounded(0xF0FFFFFF, 18, 0xFFFFFFFF, 2)
                    : rounded(0x5AFFFFFF, 18, 0, 0));
            label.setTextColor(focused ? 0xFF11151D : Color.WHITE);
            if (focused) {
                scroller.post(() -> scroller.smoothScrollTo(
                        0, Math.max(0, view.getTop() - scroller.getHeight() / 2 + view.getHeight() / 2)));
            }
        });
        tile.setOnClickListener(view -> launch(app));
        return tile;
    }

    private void launch(AppEntry app) {
        if (app.kind == AppEntry.KIND_SETTINGS) {
            launchSettings();
            return;
        }

        Intent intent = null;
        if (app.kind == AppEntry.KIND_APPSTORE) {
            intent = new Intent("com.amazon.venezia.ade.FEATURED_LAUNCH");
            intent.setPackage(app.iconPackage);
        } else {
            PackageManager pm = getPackageManager();
            intent = pm.getLeanbackLaunchIntentForPackage(app.iconPackage);
            if (intent == null) intent = pm.getLaunchIntentForPackage(app.iconPackage);
        }

        if (intent == null) {
            Toast.makeText(this, app.label + " is not available", Toast.LENGTH_SHORT).show();
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException error) {
            Toast.makeText(this, app.label + " could not be opened", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchSettings() {
        try {
            Intent intent = new Intent("amazon.intent.action.MAINSETTINGS");
            intent.addCategory("android.intent.category.MAINSETTINGS");
            intent.setComponent(new ComponentName(
                    "com.amazon.tv.launcher", "com.amazon.tv.launcher.ui.MainSettingsActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException primaryError) {
            try {
                Intent fallback = new Intent(Settings.ACTION_SETTINGS);
                fallback.setComponent(new ComponentName(
                        "com.amazon.tv.launcher", "com.amazon.tv.launcher.ui.SettingsActivity"));
                startActivity(fallback);
            } catch (ActivityNotFoundException | SecurityException ignored) {
                Toast.makeText(this, "Settings unavailable", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private GradientDrawable rounded(int color, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private static FrameLayout.LayoutParams wrapContent() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static final class AppEntry {
        static final int KIND_APP = 0;
        static final int KIND_APPSTORE = 1;
        static final int KIND_SETTINGS = 2;
        final String label;
        final String iconPackage;
        final int kind;

        private AppEntry(String label, String iconPackage, int kind) {
            this.label = label;
            this.iconPackage = iconPackage;
            this.kind = kind;
        }

        static AppEntry app(String label, String packageName) {
            return new AppEntry(label, packageName, KIND_APP);
        }

        static AppEntry appstore(String label, String packageName) {
            return new AppEntry(label, packageName, KIND_APPSTORE);
        }

        static AppEntry settings(String label, String packageName) {
            return new AppEntry(label, packageName, KIND_SETTINGS);
        }
    }
}
