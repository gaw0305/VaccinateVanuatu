package ogcio.vaccinatevanuatu;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Grace Whitmore on 2/05/2018.
 *
 * A service that creates notifications
 */

public class ScheduleNotifications extends Service {

    ArrayList<String> shots;
    ArrayList<String> dbShots;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    // When the service starts, schedule the notifications
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DBHandler myDB = new DBHandler(this);

        // Get the locale, to make sure the notifications are scheduled in the correct language
        String language = myDB.getLanguage();

        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.locale = locale;

        getBaseContext().getResources().updateConfiguration(
                config,getBaseContext().getResources().getDisplayMetrics());

        // If the data table is empty, there is no need to schedule notifications
        if (!myDB.isEmpty("DATA")) {
            if (intent != null && intent.hasExtra("boot")) {
                // Checks to see if the service is being called after the phone reboots, in which
                // case it will reschedule all notifications, since they are automatically cancelled
                // when the phone turns off
                if (intent.getStringExtra("boot").equals("true")) {
                    ArrayList<String> names = new ArrayList<>();
                    try {
                        names = new ArrayList<>(Arrays.asList(myDB.getNames().split(",")));
                    } catch (NullPointerException e) { e.printStackTrace(); }
                    for (String curName : names) scheduleShotNotifications(curName);
                }
                // Otherwise, we know that it is being called by the app itself, so it will only
                // schedule notifications for the given child
                else {
                    String name = intent.getStringExtra("name");
                    // If the date for penta or polio has changed, the subsequent penta and polio shot
                    // notifications will also be individually updated
                    if (intent.getStringExtra("single").equals("true")) {
                        String shot = intent.getStringExtra("shot");
                        String shotDate = intent.getStringExtra("shotDate");
                        String i = intent.getStringExtra("idNum");
                        scheduleSingleNotification(name, shot, shotDate, Integer.parseInt(i));
                    }
                    // Otherwise, it will reschedule all the notifications
                    else scheduleShotNotifications(name);
                }
            }
        }
        myDB.close();
        return super.onStartCommand(intent, flags, startId);
    }

    // Gets the shot names in the current language, and those for access the database, then
    // schedules each individual notification with the updated shot dates it should have
    public void scheduleShotNotifications(String name) {
        shots = new Helper(this).getShotNames();
        dbShots = new Helper(this).getDBShotNames();
        DBHandler myDB = new DBHandler(this);
        ArrayList<String> shotDates = myDB.getUpdatedShotDates(name);
        for (int i = 0; i < shotDates.size(); i++) {
            scheduleSingleNotification(name, shots.get(i), shotDates.get(i), i);
        }
        myDB.close();
    }

    // Schedules a single notification for the given name, with the given shot name, at the given
    // shot date, and using i, AKA it's position in the shot date list, as a multiplier to get the correct id
    public void scheduleSingleNotification(String name, String shotName, String shotDate, int i) {
        shots = new Helper(this).getShotNames();
        dbShots = new Helper(this).getDBShotNames();
        // Gets the current date and uses the time of 9AM to see if the notification should be shown now
        Calendar calendar = Calendar.getInstance();
        String day = Integer.toString(calendar.get(Calendar.DATE));
        String month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
        String year = Integer.toString(calendar.get(Calendar.YEAR));
        String time = "09:00:00";

        DBHandler myDB = new DBHandler(this);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        // Gets the shot name and if it's first, second, or third (for penta and polio)
        ArrayList<String> shotInfoFromDB = new Helper(this).getDBNameAndNum(dbShots.get(shots.indexOf(shotName)));
        // If the shot has been given, the notification will not be scheduled
        if (!myDB.shotGiven(shotInfoFromDB.get(0), shotInfoFromDB.get(1), name)) {
            long difference = 0;
            // Checks to see if the date of the shot has already passed
            try {
                Date date1 = simpleDateFormat.parse(day + "/" + month + "/" + year + " " + time);
                Date date2 = simpleDateFormat.parse(shotDate + " 09:00:00");
                long time1 = date1.getTime();
                long time2 = date2.getTime();
                difference = time2 - time1;
            } catch (ParseException e) { e.printStackTrace(); }
            // Sets the information for the notification, including creating a new Single Child
            // Data Activity intent with the extra "true" for notifications so that when the user
            // clicks on the notification, the app will start in the Single Child Data Activity and
            // the correct shot information dialog will be open
            String ticker = getResources().getString(R.string.shot_due) + name + ": \n\t" + shotName;
            String title = getResources().getString(R.string.shot_due) + name;
            Intent newIntent = new Intent(this, SingleChildDataActivity.class);
            newIntent.putExtra("name", name);
            newIntent.putExtra("shot", shotName);
            newIntent.putExtra("notification", "true");
            newIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            Intent notificationIntent = new Intent(this, Alarm.class);
            notificationIntent.putExtra("name", name);
            int id = i + (10 * (myDB.getIdNum(name) - 1));
            // PendingIntents allow schedule notifications to execute code from the app even when
            // the app isn't running
            PendingIntent pendingIntent = PendingIntent.getActivity(this, id, newIntent, 0);
            // Creates the notification and sets its parameters
            Notification notification = new Notification.Builder(this)
                    .setTicker(ticker)
                    .setContentTitle(title)
                    .setContentText(shotName)
                    .setSmallIcon(R.drawable.syringe_icon)
                    .setContentIntent(pendingIntent).getNotification();
            notification.flags = Notification.FLAG_ONGOING_EVENT;
            // Parcels the notification with its pending intent to be sent to the alarm manager,
            // which will then notify the user
            notificationIntent.putExtra("notification", notification);
            notificationIntent.putExtra("id", id);
            PendingIntent pendingAlarmIntent = PendingIntent.getBroadcast(this, id, notificationIntent, 0);
            AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

            // If the app is due after today, the app will schedule the notification for 9AM of the
            // correct day
            if (difference > 0) {
                if (alarmManager != null) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + difference, pendingAlarmIntent);
                }
            }
            // If the shot was due before the current date, the app will notify the user now
            else {
                if (alarmManager != null) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, pendingAlarmIntent);
                }
            }
        }
        myDB.close();
    }
}
