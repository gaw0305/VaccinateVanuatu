package ogcio.vaccinatevanuatu;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Grace Whitmore
 *
 * Receives and handles broadcast intents
 *
 * Triggers notifications, and sets the vibrate and sound that the user will hear, using the system
 * defaults
 */
public class Alarm extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Calls the ScheduleNotifications service after the device reboots in order to reschedule
        // the notifications
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent service = new Intent(context, ScheduleNotifications.class);
            // Pass in extras so ScheduleNotifications knows where the command comes from
            service.putExtra("boot", "true");
            service.putExtra("name", "");
            service.putExtra("single", "false");
            context.startService(service);
        }
        // If Alarm is not called by rebooting the phone, it is called when the user creates a
        // new child. Then it goes into this part, which sets the information for each notification
        // The Notification Intent that is created in ScheduleNotifications is passed as a parcelable
        // extra, and used to notify the user
        else {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = intent.getParcelableExtra("notification");
            notification.defaults |= Notification.DEFAULT_SOUND;
            notification.defaults |= Notification.DEFAULT_VIBRATE;
            int id = intent.getIntExtra("id", 0);
            if (notificationManager != null) notificationManager.notify(id, notification);

        }
    }
}
