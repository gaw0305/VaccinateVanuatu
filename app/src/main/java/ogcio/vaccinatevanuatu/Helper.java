package ogcio.vaccinatevanuatu;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Grace Whitmore
 *
 * A class that holds functions that are used across multiple activities
 */

class Helper {

    private Context context;

    Helper(Context context) {
        this.context = context;
    }

    // Returns an arraylist of the month names as stored in the string resource files, eg if English,
    // the list will be in English
    private ArrayList<String> getMonthNames() {
        return new ArrayList<>(Arrays.asList(context.getResources().getString(R.string.january),
                context.getResources().getString(R.string.february),
                context.getResources().getString(R.string.march),
                context.getResources().getString(R.string.april),
                context.getResources().getString(R.string.may),
                context.getResources().getString(R.string.june),
                context.getResources().getString(R.string.july),
                context.getResources().getString(R.string.august),
                context.getResources().getString(R.string.september),
                context.getResources().getString(R.string.october),
                context.getResources().getString(R.string.november),
                context.getResources().getString(R.string.december)));
    }

    // Returns an arraylist of the names of the shots as stored in the string resource files, in the
    // correct language
    ArrayList<String> getShotNames() {
        return new ArrayList<>(Arrays.asList(context.getResources().getString(R.string.bcg_name),
                context.getResources().getString(R.string.hep_b_name),
                context.getResources().getString(R.string.first_penta_name),
                context.getResources().getString(R.string.first_polio_name),
                context.getResources().getString(R.string.second_penta_name),
                context.getResources().getString(R.string.second_polio_name),
                context.getResources().getString(R.string.third_penta_name),
                context.getResources().getString(R.string.third_polio_name),
                context.getResources().getString(R.string.ipv_name),
                context.getResources().getString(R.string.measles_rubella_name)));
    }

    // Returns an arraylist of the names of the shots as stored in the string resource files, this
    // will always be in english, and is used for accessing the database
    ArrayList<String> getDBShotNames() {
        return new ArrayList<>(Arrays.asList(context.getResources().getString(R.string.bcg_name_db),
                context.getResources().getString(R.string.hep_b_name_db),
                context.getResources().getString(R.string.first_penta_name_db),
                context.getResources().getString(R.string.first_polio_name_db),
                context.getResources().getString(R.string.second_penta_name_db),
                context.getResources().getString(R.string.second_polio_name_db),
                context.getResources().getString(R.string.third_penta_name_db),
                context.getResources().getString(R.string.third_polio_name_db),
                context.getResources().getString(R.string.ipv_name_db),
                context.getResources().getString(R.string.measles_rubella_name_db)));
    }

    // Returns the birthday as 1 Jan, 2018 format, rather than 1/1/2018 format
    String formatBirthday(String date) {
        ArrayList<String> monthNames = getMonthNames();
        String month = monthNames.get(Integer.parseInt(date.split("/")[1]) - 1);
        String year = Integer.toString(Integer.parseInt(date.split("/")[2]));
        return date.split("/")[0] + " " + month + ", " + year;
    }

    // Returns an arraylist of two items, the first the shot name formatted for the database,
    // the second for if the shot is penta or polio, will return the number of the shot, eg
    // first penta will return (PENTA, FIRST)
    ArrayList<String> getDBNameAndNum(String shot) {
        String shotNameForDB = shot.replace("/", "_").replace(" ", "_").toUpperCase();
        String shotNum = "";
        if (shotNameForDB.split("_")[0].equals("FIRST") || shotNameForDB.split("_")[0].equals("SECOND")
                || shotNameForDB.split("_")[0].equals("THIRD")) {
            shotNum = shotNameForDB.split("_")[0];
            shotNameForDB = shotNameForDB.split("_")[1];
        }
        return new ArrayList<>(Arrays.asList(shotNameForDB, shotNum));
    }

    // Cancels and reschedules the notifications when the language is changed, otherwise they would
    // all come up in the previous language still
    void rescheduleNotifications() {
        Intent intent = new Intent(context, Alarm.class);
        DBHandler myDB = new DBHandler(context);

        int idNum = myDB.generateIdNum();

        ArrayList<String> names = new ArrayList<>(Arrays.asList(myDB.getNames().split(",")));
        for (String name : names) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            for (int i = 0; i < 10; i++) {
                int id = i + (10 * (myDB.getIdNum(name) - 1));
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id, intent, 0);
                if (alarmManager != null) alarmManager.cancel(pendingIntent);
                if (notificationManager != null) notificationManager.cancel(id);
                myDB.updateIDNum(name, idNum);
                idNum++;
            }
        }
        myDB.close();

        Intent service = new Intent(context, ScheduleNotifications.class);
        service.putExtra("boot", "true");
        service.putExtra("name", "");
        service.putExtra("single", "false");
        context.startService(service);
    }
}
