package dev.umid.firetvlauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                && HomeMonitorService.hasUsageAccess(context)) {
            context.startService(new Intent(context, HomeMonitorService.class));
        }
    }
}
