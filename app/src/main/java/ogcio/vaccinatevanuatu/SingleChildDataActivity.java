package ogcio.vaccinatevanuatu;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Grace Whitmore
 *
 * Shows the information for the individual child that was clicked, including how many days until
 * the next shot is due, and the due dates for every shot, as well as what shots have been given
 * or not. Also allows the user to get information about the individual shots and update whether
 * or not they have been given.
 */
public class SingleChildDataActivity extends AppCompatActivity {

    String name;
    String birthday;
    String day;
    String month;
    String year;
    ArrayList<Integer> months = new ArrayList<>(Arrays.asList(31, 28, 31, 30, 31, 30, 31, 31,
            30, 31, 30, 31));
    ArrayList<Integer> leapYears = new ArrayList<>(Arrays.asList(2012, 2016, 2020, 2024,
            2028, 2032));
    ArrayList<Shot> shots;
    ArrayList<Shot> shotsGiven;
    ArrayList<String> shotDates;
    CustomShotAdapter shotAdapter;
    CustomShotAdapter shotGivenAdapter;
    ListView shotList;
    ListView shotGivenList;
    ArrayList<String> shotNames;
    ArrayList<String> dbShotNames;
    AlertDialog alertDialog;
    String shotNameForDisplay;
    String shotNameForDB;
    int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Gets the name that was clicked in Kids List
        // If the user clicked a notification to get here, it will also have a shot name,
        // and "notification" will be true
        name = getIntent().getStringExtra("name");
        String shotName = getIntent().getStringExtra("shot");
        String notification = getIntent().getStringExtra("notification");

        // Gets the information for the user from the database
        DBHandler myDB = new DBHandler(this);
        birthday = myDB.getUserBirthday(name);
        day = birthday.split("/")[0];
        month = birthday.split("/")[1];
        year = birthday.split("/")[2];
        shotDates = myDB.getUpdatedShotDates(name);

        // Sets up the two lists, one with the shots that need to be given, and one with those
        // that have already been given
        shots = new ArrayList<>();
        shotsGiven = new ArrayList<>();
        shotNames = new Helper(this).getShotNames();
        dbShotNames = new Helper(this).getDBShotNames();
        for (int i = 0; i < shotNames.size(); i++) {
            ArrayList<String> shotInfoFromDB = new Helper(this).getDBNameAndNum(dbShotNames.get(i));
            if (myDB.shotGiven(shotInfoFromDB.get(0), shotInfoFromDB.get(1), name))
                shotsGiven.add(new Shot(shotNames.get(i), shotDates.get(i), true));
            else shots.add(new Shot(shotNames.get(i), shotDates.get(i), false));
        }
        shotAdapter = new CustomShotAdapter(SingleChildDataActivity.this, R.layout.shot_layout, shots);
        shotGivenAdapter = new CustomShotAdapter(SingleChildDataActivity.this, R.layout.shot_layout, shotsGiven);

        // If the intent was started from a notification, opens the appropriate shot dialog
        if (notification.equals("true")) {
            for (int i = 0; i < shots.size(); i++) {
                if (shots.get(i).getShotTitle().equals(shotName)) {
                    String shotDate = shots.get(i).getShotDue();
                    buildDialog(shotName, new Helper(this).formatBirthday(shotDate).split(",")[0], i, false);
                    break;
                }
            }
            String language = myDB.getLanguage();
            Locale locale = new Locale(language);
            Locale.setDefault(locale);

            Configuration config = new Configuration();
            config.locale = locale;

            getBaseContext().getResources().updateConfiguration(
                    config,getBaseContext().getResources().getDisplayMetrics());
        }

        setContentView(R.layout.activity_single_child_data);

        ((TextView) findViewById(R.id.baby_name)).setText(name);

        ((TextView) findViewById(R.id.baby_age)).setText(new Helper(this).formatBirthday(birthday).split(",")[0]);

        // Sets the two shot lists to their various list views and adapters
        shotList = (ListView) findViewById(R.id.shotListView);
        shotGivenList = (ListView) findViewById(R.id.shotGivenListView);
        shotList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String shotName = ((TextView) view.findViewById(R.id.shot)).getText().toString();
                String shotDate = ((TextView) view.findViewById(R.id.date)).getText().toString().split(",")[0];
                buildDialog(shotName, shotDate, position, false);
            }
        });

        shotGivenList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String shotName = ((TextView) view.findViewById(R.id.shot)).getText().toString();
                String shotDate = ((TextView) view.findViewById(R.id.date)).getText().toString().split(",")[0];
                buildDialog(shotName, shotDate, position, true);
            }
        });

        shotList.setAdapter(shotAdapter);
        shotGivenList.setAdapter(shotGivenAdapter);

        showNextShot();
    }

    @Override
    public void onNewIntent(Intent intent) {
        name = intent.getStringExtra("name");
    }

    /**
     * Changes the locale of the app to switch between English, Bislama, and French
     * Restarts the Activity to reflect the changes
     * @param view the flag item that was clicked in the xml
     */
    public void flagClicked(View view) {
        String translateLanguage = "";
        switch(view.getId()) {
            case R.id.britain:
                translateLanguage = "en";
                break;
            case R.id.vanuatu:
                translateLanguage = "bi";
                break;
            case R.id.france:
                translateLanguage = "fr";
                break;
        }

        Locale locale2 = new Locale(translateLanguage);
        Locale.setDefault(locale2);

        Configuration config2 = new Configuration();
        config2.locale = locale2;

        getBaseContext().getResources().updateConfiguration(
                config2,getBaseContext().getResources().getDisplayMetrics());

        DBHandler myDB = new DBHandler(SingleChildDataActivity.this);
        myDB.updateLanguageInformation(translateLanguage);
        myDB.close();

        new Helper(this).rescheduleNotifications();

        Intent intent = new Intent(SingleChildDataActivity.this, SingleChildDataActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        intent.putExtra("name", name);
        intent.putExtra("shot", "");
        intent.putExtra("notification", "false");

        startActivity(intent);
    }

    // Cancels a single notification, used for when the date for the penta/polio shots needs to be
    // changed. The old notification needs to be cancelled before the new one can be scheduled
    public void cancelNotification() {
        Intent intent = new Intent(SingleChildDataActivity.this, Alarm.class);
        DBHandler myDB = new DBHandler(SingleChildDataActivity.this);

        AlarmManager alarmManager = (AlarmManager) SingleChildDataActivity.this.getSystemService(Context.ALARM_SERVICE);
        NotificationManager notificationManager = (NotificationManager) SingleChildDataActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);

        int id = shotNames.indexOf(shotNameForDisplay) + (10 * (myDB.getIdNum(name) - 1));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(SingleChildDataActivity.this, id, intent, 0);
        if (alarmManager != null) alarmManager.cancel(pendingIntent);
        if (notificationManager != null) notificationManager.cancel(id);
    }

    // Schedules a single notification, used for when the date for the penta/polio shots needs to
    // be changed. Starts the schedule notifications service
    public void scheduleNotification(String shot, String shotDate) {
        Intent service = new Intent(this, ScheduleNotifications.class);
        service.putExtra("boot", "false");
        service.putExtra("name", name);
        service.putExtra("single", "true");
        service.putExtra("shot", shot);
        service.putExtra("shotDate", shotDate);
        service.putExtra("idNum", Integer.toString(shotNames.indexOf(shot)));
        this.startService(service);
    }

    // Shows how many days until the next shot is due
    public void showNextShot() {
        if (shots.size() == 0) {
            findViewById(R.id.today).setVisibility(View.INVISIBLE);
            findViewById(R.id.counterToday).setVisibility(View.INVISIBLE);
            findViewById(R.id.counter).setVisibility(View.VISIBLE);
            findViewById(R.id.main_day_hint).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.main_day_hint)).setText(R.string.shot_complete);
            ((TextView) findViewById(R.id.counter)).setText(R.string.congratulations);
            ((TextView) findViewById(R.id.counter)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 35);
        }
        else {
            long curDifference = -1;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Calendar calendar = Calendar.getInstance();
            String day = Integer.toString(calendar.get(Calendar.DATE));
            String month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
            String year = Integer.toString(calendar.get(Calendar.YEAR));
            String daysToNextShot = "0";
            // Goes through the list of shots and finds the smallest difference that is larger than 0
            for (int i = 0; i < shots.size(); i++) {
                try {
                    Date date1 = simpleDateFormat.parse(day + "/" + month + "/" + year);
                    Date date2 = simpleDateFormat.parse(shots.get(i).getShotDue());
                    long difference = date2.getTime() - date1.getTime();
                    if (difference >= 0 && (curDifference == -1 || difference < curDifference)) {
                        curDifference = difference;
                        daysToNextShot = Long.toString(difference / (1000 * 60 * 60 * 24));
                    }
                } catch (ParseException e) {
                    Log.d("", "");
                }
            }

            // If there are 0 or 1 days until the next shots, change the format slightly in the xml,
            // so that it will say "get your next shot today" rather than "0 days until your next shot"
            if (daysToNextShot.equals("0") || daysToNextShot.equals("1")) {
                findViewById(R.id.today).setVisibility(View.VISIBLE);
                findViewById(R.id.counterToday).setVisibility(View.VISIBLE);
                findViewById(R.id.counter).setVisibility(View.INVISIBLE);
                findViewById(R.id.main_day_hint).setVisibility(View.INVISIBLE);
                if (daysToNextShot.equals("0"))
                    ((TextView) findViewById(R.id.counterToday)).setText(getResources().getString(R.string.today));
                else
                    ((TextView) findViewById(R.id.counterToday)).setText(getResources().getString(R.string.tomorrow));
            }
            // Otherwise updates the counter so it says "45 days until your next shot"
            else {
                findViewById(R.id.today).setVisibility(View.INVISIBLE);
                findViewById(R.id.counterToday).setVisibility(View.INVISIBLE);
                findViewById(R.id.counter).setVisibility(View.VISIBLE);
                findViewById(R.id.main_day_hint).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.main_day_hint)).setText(R.string.days_until_shot);
                ((TextView) findViewById(R.id.counter)).setText(daysToNextShot);
                ((TextView) findViewById(R.id.counter)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 140);
            }
        }
    }

    // Builds the information dialog, using the name of the shot, the date, and whether or not
    // it has already been given
    // Position is for updating the item in the list
    public void buildDialog(final String shotName, String shotDate, int position, boolean shotGiven) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SingleChildDataActivity.this);
        LayoutInflater inflater = SingleChildDataActivity.this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.shot_detail_layout, null);
        builder.setView(dialogView);

        DBHandler myDB = new DBHandler(SingleChildDataActivity.this);
        shotNameForDisplay = shotName;
        shotNameForDB = dbShotNames.get(shotNames.indexOf(shotNameForDisplay));
        this.position = position;
        ((TextView) dialogView.findViewById(R.id.vaccineName)).setText(shotNameForDisplay);
        ((TextView) dialogView.findViewById(R.id.vaccineDateText)).setText(shotDate);
        ((CheckBox) dialogView.findViewById(R.id.viewVaccineGiven)).setChecked(shotGiven);

        String dbShotName = new Helper(this).getDBNameAndNum(shotNameForDB).get(0);

        if (shotGiven) {
            if (shotNameForDB.contains("First") && myDB.shotGiven(dbShotName, "SECOND", name))
                dialogView.findViewById(R.id.viewVaccineGiven).setEnabled(false);
            else if (shotNameForDB.contains("Second") && myDB.shotGiven(dbShotName, "THIRD", name))
                dialogView.findViewById(R.id.viewVaccineGiven).setEnabled(false);
            else dialogView.findViewById(R.id.viewVaccineGiven).setEnabled(true);
        }
        else {
            if (shotNameForDB.contains("Second") && !myDB.shotGiven(dbShotName, "FIRST", name))
                dialogView.findViewById(R.id.viewVaccineGiven).setEnabled(false);
            else if (shotNameForDB.contains("Third") && !myDB.shotGiven(dbShotName, "SECOND", name))
                dialogView.findViewById(R.id.viewVaccineGiven).setEnabled(false);
            else dialogView.findViewById(R.id.viewVaccineGiven).setEnabled(true);
        }

        final CheckBox vaccineGiven = (CheckBox) dialogView.findViewById(R.id.viewVaccineGiven);
        vaccineGiven.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (vaccineGiven.isChecked()) {
                    Calendar calendar = Calendar.getInstance();
                    final int year = calendar.get(Calendar.YEAR);
                    final int month = calendar.get(Calendar.MONTH);
                    final int day = calendar.get(Calendar.DAY_OF_MONTH);

                    DBHandler myDB = new DBHandler(SingleChildDataActivity.this);
                    if (myDB.getLanguage().equals("bi")) {
                        Locale locale = new Locale("en");
                        Locale.setDefault(locale);

                        Configuration config = new Configuration();
                        config.locale = locale;

                        getBaseContext().getResources().updateConfiguration(
                                config, getBaseContext().getResources().getDisplayMetrics());
                    }
                    myDB.close();

                    final DatePickerDialog datePickerDialog = new DatePickerDialog(
                            SingleChildDataActivity.this, datePickerListener, year, month, day);
                    datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_NEGATIVE) {
                                ((CheckBox) dialogView.findViewById(R.id.viewVaccineGiven)).setChecked(false);
                                DBHandler myDB = new DBHandler(SingleChildDataActivity.this);
                                String language = myDB.getLanguage();
                                Locale locale = new Locale(language);
                                Locale.setDefault(locale);

                                Configuration config = new Configuration();
                                config.locale = locale;

                                getBaseContext().getResources().updateConfiguration(
                                        config, getBaseContext().getResources().getDisplayMetrics());
                                myDB.close();
                                datePickerDialog.cancel();
                            }
                        }
                    });

                    datePickerDialog.setCancelable(false);

                    datePickerDialog.show();
                }
                else {
                    DBHandler myDB = new DBHandler(SingleChildDataActivity.this);
                    String dbShotName = new Helper(SingleChildDataActivity.this).getDBNameAndNum(shotNameForDB).get(0);

                    String date;

                    boolean updatedDate = false;
                    if (shotNameForDisplay.equals(getResources().getString(R.string.first_penta_name)))
                        resetTwoDates("Second Penta", "Third Penta");
                    else if (shotNameForDisplay.equals(getResources().getString(R.string.second_penta_name))) {
                        date = myDB.getUpdatedShotDates(name).get(dbShotNames.indexOf("First Penta"));
                        updateTwoDates("Second Penta", "Third Penta",
                                date.split("/")[0], date.split("/")[1],
                                date.split("/")[2]);
                        updatedDate = true;
                    } else if (shotNameForDisplay.equals(getResources().getString(R.string.third_penta_name))) {
                        date = myDB.getUpdatedShotDates(name).get(dbShotNames.indexOf("Second Penta"));
                        updateOneDate("Third Penta", date.split("/")[0],
                                date.split("/")[1], date.split("/")[2]);
                        updatedDate = true;
                    }
                    else if (shotNameForDisplay.equals(getResources().getString(R.string.first_polio_name)))
                        resetTwoDates("Second Polio", "Third Polio");
                    else if (shotNameForDisplay.equals(getResources().getString(R.string.second_polio_name))) {
                        date = myDB.getUpdatedShotDates(name).get(dbShotNames.indexOf("First Polio"));
                        updateTwoDates("Second Polio", "Third Polio",
                            date.split("/")[0], date.split("/")[1],
                            date.split("/")[2]);
                        updatedDate = true;
                    }
                    else if (shotNameForDisplay.equals(getResources().getString(R.string.third_polio_name))) {
                        date = myDB.getUpdatedShotDates(name).get(dbShotNames.indexOf("Second Polio"));
                        updateOneDate("Third Polio", date.split("/")[0],
                                date.split("/")[1], date.split("/")[2]);
                        updatedDate = true;
                    }
                    String shotDate;
                    if (updatedDate)
                        shotDate = myDB.getUpdatedShotDates(name).get(shotNames.indexOf(shotNameForDisplay));
                    else
                        shotDate = myDB.getShotDates(name).get(shotNames.indexOf(shotNameForDisplay));

                    myDB.updateShotDate(name, shotNames.indexOf(shotNameForDisplay), shotDate);
                    myDB.updateShotData(name, dbShotName, -1);
                    myDB.close();

                    shotsGiven.remove(SingleChildDataActivity.this.position);
                    int index = shotIndex(shotDate, shots);
                    shots.add(index, new Shot(shotNameForDisplay, shotDate, false));
                    shotAdapter.notifyDataSetChanged();
                    shotGivenAdapter.notifyDataSetChanged();
                    showNextShot();
                }
            }
        });

        Button viewVaccineDetails = (Button) dialogView.findViewById(R.id.viewVaccineDetails);
        viewVaccineDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SingleChildDataActivity.this);
                LayoutInflater inflater = SingleChildDataActivity.this.getLayoutInflater();
                final View dialogViewThree = inflater.inflate(R.layout.content_information, null);
                builder.setView(dialogViewThree);

                setInformation(shotNameForDB, dialogViewThree);

                builder.setNegativeButton(getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.cancel();
                        alertDialog.show();
                    }
                });

                alertDialog = builder.create();
                alertDialog.show();
            }
        });

        alertDialog = builder.create();
        alertDialog.show();
    }

    // Returns the index of the shot date in the currently showing list of shot dates, so it can
    // be updated
    public int shotIndex(String shotDate, ArrayList<Shot> shotDates) {
        int day = Integer.parseInt(shotDate.split("/")[0]);
        int month = Integer.parseInt(shotDate.split("/")[1]);
        int year = Integer.parseInt(shotDate.split("/")[2]);
        for (int i = 0; i < shotDates.size(); i++) {
            String checkShotDate = shotDates.get(i).getShotDue();
            int checkShotDay = Integer.parseInt(checkShotDate.split("/")[0]);
            int checkShotMonth = Integer.parseInt(checkShotDate.split("/")[1]);
            int checkShotYear = Integer.parseInt(checkShotDate.split("/")[2]);
            if (checkShotYear > year
                    || (checkShotYear == year && checkShotMonth > month)
                    || (checkShotYear == year && checkShotMonth == month && checkShotDay > day))
                return i;
        }
        return shotDates.size();
    }

    // Allows the user to toggle between seeing the shots their child needs, and those the child
    // has already been given
    public void shotButtonClicked(View view) {
        Button button = (Button) findViewById(R.id.button);
        if (button.getText().toString().equals(getResources().getString(R.string.view_completed_shots))) {
            button.setText(getResources().getString(R.string.view_needed_shots));
            findViewById(R.id.shotListView).setVisibility(View.INVISIBLE);
            findViewById(R.id.shotGivenListView).setVisibility(View.VISIBLE);
        }
        else {
            button.setText(getResources().getString(R.string.view_completed_shots));
            findViewById(R.id.shotListView).setVisibility(View.VISIBLE);
            findViewById(R.id.shotGivenListView).setVisibility(View.INVISIBLE);
        }
    }

    // Handles what happens when the user chooses the date for when a shot is due
    private DatePickerDialog.OnDateSetListener datePickerListener
            = new DatePickerDialog.OnDateSetListener() {

        public void onDateSet(DatePicker view, int selectedYear,
                              int selectedMonth, int selectedDay) {
            year = Integer.toString(selectedYear);
            month = Integer.toString(selectedMonth+1);
            day = Integer.toString(selectedDay);

            DBHandler myDB = new DBHandler(SingleChildDataActivity.this);
            String dbShotName = new Helper(SingleChildDataActivity.this).getDBNameAndNum(shotNameForDB).get(0);
            myDB.updateShotDate(name, shotNames.indexOf(shotNameForDisplay), day + "/" + month + "/" + year);
            myDB.updateShotData(name, dbShotName, 1);

            // Resets the locale to the user chosen locale
            String language = myDB.getLanguage();
            Locale locale = new Locale(language);
            Locale.setDefault(locale);

            Configuration config = new Configuration();
            config.locale = locale;

            getBaseContext().getResources().updateConfiguration(
                    config, getBaseContext().getResources().getDisplayMetrics());
            myDB.close();

            // Removes the item from the shots needed list, and adds it to the shots given list
            shots.remove(position);
            shotsGiven.add(new Shot(shotNameForDisplay, day + "/" + month + "/" + year, true));

            if (shotNameForDisplay.equals(getResources().getString(R.string.first_penta_name)))
                updateTwoDates("Second Penta", "Third Penta", day,
                        month, year);
            else if (shotNameForDisplay.equals(getResources().getString(R.string.second_penta_name)))
                updateOneDate("Third Penta", day, month, year);
            else if (shotNameForDisplay.equals(getResources().getString(R.string.first_polio_name)))
                updateTwoDates("Second Polio", "Third Polio", day,
                        month, year);
            else if (shotNameForDisplay.equals(getResources().getString(R.string.second_polio_name)))
                updateOneDate("Third Polio", day, month, year);

            shotAdapter.notifyDataSetChanged();
            shotGivenAdapter.notifyDataSetChanged();
            showNextShot();
            cancelNotification();
        }
    };

    // Resets Penta 2/3 or Polio 2/3 if the user previously said the child got the shot on a different
    // day than it was required, then removes it from the shots given and reschedules the notification
    // for the new date
    public void resetTwoDates(String shotStringOne, String shotStringTwo) {
        DBHandler myDB = new DBHandler(SingleChildDataActivity.this);
        String firstShotDate = myDB.getShotDates(name).get(dbShotNames.indexOf(shotStringOne));
        myDB.updateShotDate(name, dbShotNames.indexOf(shotStringOne), firstShotDate);
        String secondShotDate = myDB.getShotDates(name).get(dbShotNames.indexOf(shotStringTwo));
        myDB.updateShotDate(name, dbShotNames.indexOf(shotStringTwo), secondShotDate);
        for (int i = 0; i < shots.size(); i++) {
            if (shots.get(i).getShotTitle().equals(shotNames.get(dbShotNames.indexOf(shotStringOne)))) {
                Shot shot = shots.remove(i);
                shot.setShotDue(firstShotDate);
                int index = shotIndex(firstShotDate, shots);
                shots.add(index, shot);
            }
            else if (shots.get(i).getShotTitle().equals(shotNames.get(dbShotNames.indexOf(shotStringTwo)))) {
                Shot shot = shots.remove(i);
                shot.setShotDue(secondShotDate);
                int index = shotIndex(secondShotDate, shots);
                shots.add(index, shot);
            }
        }
        scheduleNotification(shotNames.get(dbShotNames.indexOf(shotStringOne)), firstShotDate);
        scheduleNotification(shotNames.get(dbShotNames.indexOf(shotStringTwo)), secondShotDate);
    }

    // Updates Penta 3 or Polio 3 if Penta 2 or Polio 2 is given on a different day than it was
    // scheduled and reschedules the notification for the new date
    public void updateOneDate(String shotString, String day, String month, String year) {
        String shotDate = getNextShotDate(Integer.parseInt(day), Integer.parseInt(month),
                Integer.parseInt(year), 28);
        DBHandler myDB = new DBHandler(SingleChildDataActivity.this);
        myDB.updateShotDate(name, dbShotNames.indexOf(shotString), shotDate);
        for (int i = 0; i < shots.size(); i++) {
            if (shots.get(i).getShotTitle().equals(shotNames.get(dbShotNames.indexOf(shotString)))) {
                Shot shot = shots.remove(i);
                shot.setShotDue(shotDate);
                int index = shotIndex(shotDate, shots);
                shots.add(index, shot);
            }
        }
        myDB.close();
        scheduleNotification(shotNames.get(dbShotNames.indexOf(shotString)), shotDate);
    }

    // Intentionally sends information that back was pressed to the previous activity, so that if
    // language is changed here, it will update the previous activity too
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivityForResult(new Intent(this, StartupPage.class), 1);
    }

    // Updates Penta 1/2 or Polio 1/2 to new dates if Penta 1 or Polio 1 was given on a day other
    // than the day it was scheduled, and reschedules the notifications
    public void updateTwoDates(String shotOneString, String shotTwoString, String day, String month, String year) {
        String firstShotDate = getNextShotDate(Integer.parseInt(day), Integer.parseInt(month),
                Integer.parseInt(year), 28);
        DBHandler myDB = new DBHandler(SingleChildDataActivity.this);
        myDB.updateShotDate(name, dbShotNames.indexOf(shotOneString), firstShotDate);
        String secondShotDate = getNextShotDate(Integer.parseInt(day), Integer.parseInt(month),
                Integer.parseInt(year), 56);
        myDB.updateShotDate(name, dbShotNames.indexOf(shotTwoString), secondShotDate);
        for (int i = 0; i < shots.size(); i++) {
            if (shots.get(i).getShotTitle().equals(shotNames.get(dbShotNames.indexOf(shotOneString)))) {
                Shot shot = shots.remove(i);
                shot.setShotDue(firstShotDate);
                int index = shotIndex(firstShotDate, shots);
                shots.add(index, shot);
            }
            else if (shots.get(i).getShotTitle().equals(shotNames.get(dbShotNames.indexOf(shotTwoString)))) {
                Shot shot = shots.remove(i);
                shot.setShotDue(secondShotDate);
                int index = shotIndex(secondShotDate, shots);
                shots.add(index, shot);
            }
        }

        scheduleNotification(shotNames.get(dbShotNames.indexOf(shotOneString)), firstShotDate);
        scheduleNotification(shotNames.get(dbShotNames.indexOf(shotTwoString)), secondShotDate);
    }

    // Gets the date for a shot, based on the number of days from a previous date
    public String getNextShotDate(int day, int month, int year, int numDays) {
        int counter = 0;
        if (leapYears.contains(year)) months.set(1, 29);
        else months.set(1, 28);
        while (counter < numDays) {
            if (day < months.get(month-1))
                day++;
            else if (month + 1 < 12) {
                month++;
                day = 1;
            }
            else {
                year++;
                month=1;
                day=1;
            }
            counter++;
        }
        return Integer.toString(day) + "/" + Integer.toString(month)
                + "/" + Integer.toString(year);
    }

    // Sets the information for a given vaccine when the information button is clicked
    public void setInformation(String vaccine, View dialogView) {
        if (vaccine.contains("First") || vaccine.contains("Second") || vaccine.contains("Third"))
            vaccine = vaccine.split(" ")[1];
        switch(vaccine) {
            case "BCG":
            ((TextView) dialogView.findViewById(R.id.title)).setText(R.string.BCG_title);
            ((TextView) dialogView.findViewById(R.id.target)).setText(R.string.BCG_target);
            ((TextView) dialogView.findViewById(R.id.information)).setText(R.string.BCG_information);
            break;
            case "Hep B":
                ((TextView) dialogView.findViewById(R.id.title)).setText(R.string.hep_b_title);
                ((TextView) dialogView.findViewById(R.id.target)).setText(R.string.hep_b_target);
                ((TextView) dialogView.findViewById(R.id.information)).setText(R.string.hep_b_information);
                break;
            case "Penta":
                ((TextView) dialogView.findViewById(R.id.title)).setText(R.string.penta_title);
                ((TextView) dialogView.findViewById(R.id.target)).setText(R.string.penta_target);
                ((TextView) dialogView.findViewById(R.id.information)).setText(R.string.penta_information);
                break;
            case "Polio":
                ((TextView) dialogView.findViewById(R.id.title)).setText(R.string.polio_title);
                ((TextView) dialogView.findViewById(R.id.target)).setText(R.string.polio_target);
                ((TextView) dialogView.findViewById(R.id.information)).setText(R.string.polio_information);
                break;
            case "IPV":
                ((TextView) dialogView.findViewById(R.id.title)).setText(R.string.ipv_title);
                ((TextView) dialogView.findViewById(R.id.target)).setText(R.string.polio_target);
                ((TextView) dialogView.findViewById(R.id.information)).setText(R.string.polio_information);
                break;
            case "Measles/Rubella":
                ((TextView) dialogView.findViewById(R.id.title)).setText(R.string.measles_rubella);
                ((TextView) dialogView.findViewById(R.id.target)).setText(R.string.measles_rubella_target);
                ((TextView) dialogView.findViewById(R.id.information)).setText(R.string.measles_rubella_information);
                break;
        }
    }
}