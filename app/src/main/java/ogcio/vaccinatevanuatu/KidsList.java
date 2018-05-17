package ogcio.vaccinatevanuatu;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Grace Whitmore on 18/04/2018.
 *
 * Fragment contained in a tabhost, containing a list of children created by the parent
 */

public class KidsList extends android.support.v4.app.Fragment {

    ArrayList<Baby> childList;
    ArrayAdapter<Baby> childListAdapter;
    ListView listView;
    AlertDialog newChildDialog;
    AlertDialog languageDialog;
    AlertDialog startupDialog;
    String name;
    String birthday;
    ArrayList<Integer> months = new ArrayList<>(Arrays.asList(31, 28, 31, 30, 31, 30, 31, 31,
            30, 31, 30, 31));
    ArrayList<Integer> leapYears = new ArrayList<>(Arrays.asList(2012, 2016, 2020, 2024,
            2028, 2032));
    Button dateButton;
    EditText nameEditText;
    RadioButton maleRadioButton;
    RadioButton femaleRadioButton;
    int prevID;
    int showInformation;
    AlertDialog.Builder builder;
    String gender;
    Bitmap imageBitmap = null;
    Uri picUri;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.kids_list_fragment, container, false);

        setupListView(view);

        view.findViewById(R.id.addChildButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNewChild("", "", "");
            }
        });

        if (savedInstanceState != null) {
            // If the new child dialog was showing when the app was rotated or restarted,
            // shows that dialog again, with any information that the parent already entered
            if (savedInstanceState.getString("newChild").equals("true")) {
                String name = savedInstanceState.getString("name");
                String gender = savedInstanceState.getString("gender");
                String birthday = savedInstanceState.getString("birthday");
                createNewChild(name, gender, birthday);
            }
            // If the language dialog was showing when the app was rotated or restarted,
            // show the language dialog
            else if (savedInstanceState.getString("language").equals("true"))
                showLanguageDialog();
            // If the startup dialog was showing when the app was rotated or restarted,
            // show the language dialog
            else if (savedInstanceState.getString("startup").equals("true")) {
                if (savedInstanceState.getString("information").equals("true"))
                    showInformationDialog();
                else showStartupDialog();
            }
        }
        else {
            DBHandler myDB = new DBHandler(getContext());
            // If the settings table is empty in the database, show the language dialog, because
            // it means that it is the first time the user uses the app
            if (myDB.isEmpty("SETTINGS")) showLanguageDialog();
            else if (myDB.showStartupDialog()) showStartupDialog();
            // If it is not the first time using the app, but there are no children in the list,
            // the app will show the create new child dialog
            else if (myDB.isEmpty("DATA")) {
                prevID = 0;
                createNewChild("", "", "");
            }
            // Otherwise, it gets a new id num in preparation for another child being created, just
            // in case
            else prevID = myDB.generateIdNum();
            myDB.close();
        }

        return view;
    }

    // Sets up the list of children listview, including an on click listener which sends the user
    // to the individual child information page
    private void setupListView(View view) {
        listView = (ListView) view.findViewById(R.id.listView);
        registerForContextMenu(listView);
        childList = getInfoFromDB();
        childListAdapter = new CustomBabyAdapter(getActivity(), R.layout.child_layout, childList);
        listView.setAdapter(childListAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                name = ((TextView) view.findViewById(R.id.name)).getText().toString();
                Intent intent = new Intent(getActivity(), SingleChildDataActivity.class);
                intent.putExtra("name", name);
                intent.putExtra("notification", "false");
                startActivity(intent);
            }
        });
    }

    // Shows the startup dialog, which includes information about who the app is meant for and
    // why vaccines are important
    private void showStartupDialog() {
        showInformation = 0;
        builder = new AlertDialog.Builder(getContext());
        View view = View.inflate(getContext(), R.layout.welcome_layout, null);
        TextView title = ((TextView) view.findViewById(R.id.title));
        title.setText(getResources().getString(R.string.startup_title));
        final TextView message = ((TextView) view.findViewById(R.id.welcome_message));
        message.setText(getResources().getString(R.string.welcome_message));
        builder.setView(view);
        builder.setPositiveButton(getResources().getString(R.string.next), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });
        startupDialog = builder.create();
        startupDialog.show();

        startupDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (showInformation == 0) {
                    message.setText(getResources().getString(R.string.information_welcome));
                    startupDialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(getResources().getString(R.string.startup_yes));
                    showInformation++;
                }
                else {
                    startupDialog.cancel();
                    DBHandler myDB = new DBHandler(getContext());
                    myDB.updateStartupInformation();
                    if (myDB.isEmpty("DATA")) {
                        prevID = 0;
                        createNewChild("", "", "");
                    } else prevID = myDB.generateIdNum();
                    myDB.close();
                }
            }
        });
    }

    // Shows just the information dialog, which says why vaccines are important, if needed
    public void showInformationDialog() {
        builder = new AlertDialog.Builder(getContext());
        View view = View.inflate(getContext(), R.layout.welcome_layout, null);
        TextView title = ((TextView) view.findViewById(R.id.title));
        title.setText(getResources().getString(R.string.startup_title));
        final TextView message = ((TextView) view.findViewById(R.id.welcome_message));
        message.setText(getResources().getString(R.string.information_welcome));
//        ((TextView) view.findViewById(R.id.information)).setText(getResources().getString(R.string.information_welcome));
        builder.setView(view);
        builder.setPositiveButton(getResources().getString(R.string.startup_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startupDialog.cancel();
                DBHandler myDB = new DBHandler(getContext());
                myDB.updateStartupInformation();
                if (myDB.isEmpty("DATA")) {
                    prevID = 0;
                    createNewChild("", "", "");
                } else prevID = myDB.generateIdNum();
                myDB.close();
            }
        });
        startupDialog = builder.create();
        startupDialog.show();
    }

    // Shows the choose language dialog
    private void showLanguageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = View.inflate(getContext(), R.layout.choose_language_layout, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        languageDialog = builder.create();
        languageDialog.show();
    }

    // Creates a context menu item for each list view item that allows the user to delete a child
    // they created
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId()==R.id.listView) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.menu_child_options, menu);
        }
    }

    // Handles what happens when a user clicks on the delete child item in the context menu (and
    // any other context item options if they are added later)
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final int index = info.position;
        final String editName = childList.get(index).getName();
        switch(item.getItemId()) {
            case R.id.delete_child:
                // First, cancel the notifications for the child
                cancelNotifications(editName);
                // Then, remove it from the list and notify the adapter of the change
                childList.remove(index);
                childListAdapter.notifyDataSetChanged();
                // Now delete it from the database
                DBHandler myDB = new DBHandler(getActivity());
                myDB.deleteUserData(editName);
                myDB.close();
                // If it was the only child on the list, automatically open the add new child dialog
                if (childList.size() == 0)  {
                    childListAdapter = new CustomBabyAdapter(getActivity(), R.layout.child_layout, childList);
                    listView.setAdapter(childListAdapter);
                    createNewChild("", "", "");
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    // Cancels any alert dialogs that are showing when the app stops, to prevent it from leaking
    // windows
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (newChildDialog != null) newChildDialog.cancel();
        if (languageDialog != null) languageDialog.cancel();
        if (startupDialog != null) startupDialog.cancel();
    }

    // Gets all the information from the data table of the database, AKA all the information about
    // all the children that have been entered
    public ArrayList<Baby> getInfoFromDB() {
        DBHandler myDB = new DBHandler(getActivity());
        Cursor cursor = myDB.getAllData();
        ArrayList<Baby> userInformation = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                String birthday = cursor.getString(1);
                String gender = cursor.getString(2);
                Bitmap imageBitmap = getImageBitmap(cursor.getBlob(3));
                userInformation.add(new Baby(name, new Helper(getContext()).formatBirthday(birthday), gender,
                        imageBitmap));
            }while(cursor.moveToNext());
        }
        cursor.close();
        myDB.close();
        return userInformation;
    }

    public byte[] getBytes(Bitmap bitmap) {
        if (bitmap == null) return null;
        else {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
            return stream.toByteArray();
        }
    }

    public Bitmap getImageBitmap(byte[] photo) {
        if (photo == null) return null;
        return BitmapFactory.decodeByteArray(photo, 0, photo.length);
    }

    // Creates the new child dialog and populates it with information if the dialog is being recreated
    // after being rotated or restarted
    public void createNewChild(String curName, String curGender, String curBirthday) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View dialogView = View.inflate(getContext(), R.layout.add_new_child_layout, null);
        builder.setView(dialogView);
        nameEditText = dialogView.findViewById(R.id.nameText);
        if (!curName.equals("")) nameEditText.setText(curName);
        maleRadioButton = dialogView.findViewById(R.id.male);
        femaleRadioButton = dialogView.findViewById(R.id.female);
        if (curGender.equals("male")) maleRadioButton.setChecked(true);
        else if (curGender.equals("female")) femaleRadioButton.setChecked(true);
        dateButton = dialogView.findViewById(R.id.birthday);
        if (!curBirthday.equals("")) {
            dateButton.setText(curBirthday);
            dateButton.setClickable(false);
        }
        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dateButton.setError(null);
                birthdayClicked();
            }
        });
        // This is empty, because if the user clicks save but there are errors in the form, the dialog
        // should not close yet
        builder.setPositiveButton(getResources().getString(R.string.save), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        builder.setNeutralButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                newChildDialog.dismiss();
            }
        });

        // If there was an error in the form, we want the error to be removed once the user inserts
        // the information
        nameEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!((EditText) dialogView.findViewById(R.id.nameText)).getText().toString().equals("")) {
                    ((EditText) dialogView.findViewById(R.id.nameText)).setError(null);
                }
            }
        });

        maleRadioButton.findViewById(R.id.male).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((RadioButton) dialogView.findViewById(R.id.female)).setError(null);
            }
        });

        femaleRadioButton.findViewById(R.id.female).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((RadioButton) dialogView.findViewById(R.id.female)).setError(null);
            }
        });

        newChildDialog = builder.create();
        newChildDialog.show();

        // Here is where we deal with the save button being clicked
        newChildDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Gets the child's information from the dialog
                name = ((EditText) dialogView.findViewById(R.id.nameText)).getText().toString();
                birthday =  dateButton.getText().toString();
                int radioButtonId = ((RadioGroup) dialogView.findViewById(R.id.genderRadioGroup)).getCheckedRadioButtonId();
                if (radioButtonId != -1)
                    gender = ((RadioButton) dialogView.findViewById(radioButtonId)).getText().toString();
                DBHandler myDB = new DBHandler(getActivity());
                // Deals with any errors in the form and sets appropriate error messages
                if (name.equals("")) ((EditText) dialogView.findViewById((R.id.nameText))).setError(getResources().getString(R.string.name_error));
                else if (myDB.nameExists(name)) ((EditText) dialogView.findViewById(R.id.nameText)).setError(getResources().getString(R.string.name_exists_error));
                else if (radioButtonId == -1) ((RadioButton) dialogView.findViewById(R.id.female)).setError("");
                else if (birthday.equals("")) ((Button) dialogView.findViewById(R.id.birthday)).setError(getResources().getString(R.string.birthday_error));
                // If there are no errors, adds the child's information to the database, and updates
                // the list of children on the page
                else {
                    PackageManager packageManager = getContext().getPackageManager();
                    if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                            && Camera.getNumberOfCameras() != 0) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle(R.string.take_photo_title);
                        builder.setMessage(R.string.take_photo_message);
                        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                    if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null)
                                        startActivityForResult(takePictureIntent, 1);
                                    else {
                                        imageBitmap = null;
                                        saveChild();
                                    }
                                }
                                catch(ActivityNotFoundException e) {
                                    Toast.makeText(getContext(), R.string.camera_not_found, Toast.LENGTH_SHORT).show();
                                    imageBitmap = null;
                                    saveChild();
                                }
                            }
                        });
                        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                imageBitmap = null;
                                saveChild();
                            }
                        });
                        builder.create().show();
                    }
                    else saveChild();
                }
                myDB.close();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == getActivity().RESULT_OK) {
            imageBitmap = (Bitmap) data.getExtras().get("data");
            picUri = data.getData();
            performCrop();
        }
        else if (requestCode == 2 && resultCode == getActivity().RESULT_CANCELED) {
            imageBitmap = null;
            saveChild();
        }
        else if (requestCode == 2 && resultCode == getActivity().RESULT_OK) {
            imageBitmap = (Bitmap) data.getExtras().get("data");
            saveChild();
        }
        else {
            imageBitmap = null;
            saveChild();
        }
    }

    public void performCrop() {
        try {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            //indicate image type and Uri
            cropIntent.setDataAndType(picUri, "image/*");
            //set crop properties
            cropIntent.putExtra("crop", "true");
            //indicate aspect of desired crop
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            //indicate output X and Y
            cropIntent.putExtra("outputX", 256);
            cropIntent.putExtra("outputY", 256);
            //retrieve data on return
            cropIntent.putExtra("return-data", true);
            //start the activity - we handle returning in onActivityResult
            startActivityForResult(cropIntent, 2);
        }
        catch(ActivityNotFoundException e) {
            e.printStackTrace();
            saveChild();
        }
    }

    public void saveChild() {
        prevID++;
        int idNum;
        if (prevID == 0) idNum = 1;
        else idNum = prevID;
        ArrayList<String> shotDates = shotDates();
        StringBuilder shotDatesString = new StringBuilder(shotDates.get(0));
        for (int i = 1; i < shotDates.size(); i++)
            shotDatesString.append(",").append(shotDates.get(i));
        DBHandler myDB = new DBHandler(getContext());
        myDB.insertNameAndBirthdayData(name, birthday, gender, idNum, shotDatesString.toString(), getBytes(imageBitmap));
        myDB.close();
        childList.add(new Baby(name, new Helper(getContext()).formatBirthday(birthday), gender, imageBitmap));
        childListAdapter.notifyDataSetChanged();
        scheduleNotifications();
        newChildDialog.dismiss();
    }

    // Cancels notifications for a specific child, to be used when deleting the child
    // In order to do so, you need to create the same pending intent as you did to create the
    // notification
    public void cancelNotifications(String name) {
        Intent intent = new Intent(getActivity(), Alarm.class);
        DBHandler myDB = new DBHandler(getActivity());

        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        for (int i = 0; i < 10; i++) {
            // Gets the id number for the individual shot; the first childs' will be 1-10, the second
            // 11-20, etc
            int id = i + (10 * (myDB.getIdNum(name) - 1));
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), id, intent, 0);
            if (alarmManager != null) alarmManager.cancel(pendingIntent);
            if (notificationManager != null) notificationManager.cancel(id);
        }
        myDB.close();
    }

    // If the app restarts or changes orientation or pauses in some way, it saves certain variables
    // so that the user can restart where they left off
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // If the new child dialog is showing, saves the information the user has already entered
        // to reload when the app starts again
        if (newChildDialog != null && newChildDialog.isShowing()) {
            savedInstanceState.putString("newChild", "true");
            savedInstanceState.putString("language", "false");
            savedInstanceState.putString("startup", "false");
            String name = nameEditText.getText().toString();
            String gender = "";
            if (maleRadioButton.isChecked()) gender = "male";
            else if (femaleRadioButton.isChecked()) gender = "female";
            String birthday = dateButton.getText().toString();
            if (birthday.equals(getResources().getString(R.string.choose_birthday)))
                birthday = "";
            savedInstanceState.putString("name", name);
            savedInstanceState.putString("gender", gender);
            savedInstanceState.putString("birthday", birthday);
        }
        // If the language dialog is showing, puts the string "language" as true
        else if (languageDialog != null && languageDialog.isShowing()) {
            savedInstanceState.putString("newChild", "false");
            savedInstanceState.putString("language", "true");
            savedInstanceState.putString("startup", "false");
        }
        // If the startup dialog is showing, saves it as true so it will show again
        else if (startupDialog != null && startupDialog.isShowing()) {
            savedInstanceState.putString("newChild", "false");
            savedInstanceState.putString("language", "false");
            savedInstanceState.putString("startup", "true");
            if (showInformation == 0)
                savedInstanceState.putString("information", "false");
            else savedInstanceState.putString("information", "true");
        }
        // Otherwise, all the dialog variables will be false
        else {
            savedInstanceState.putString("newChild", "false");
            savedInstanceState.putString("language", "false");
            savedInstanceState.putString("startup", "false");
            savedInstanceState.putString("name", "");
            savedInstanceState.putString("gender", "");
            savedInstanceState.putString("birthday", "");
        }
    }

    // Starts the Schedule Notifications Service to schedule the notifications
    public void scheduleNotifications() {
        Intent service = new Intent(getContext(), ScheduleNotifications.class);
        service.putExtra("boot", "false");
        service.putExtra("name", name);
        service.putExtra("single", "false");
        getContext().startService(service);
    }

    // Takes the just added child's birthday and returns the dates for all the shots they should have
    public ArrayList<String> shotDates() {
        int day = Integer.parseInt(birthday.split("/")[0]);
        int month = Integer.parseInt(birthday.split("/")[1]);
        int year = Integer.parseInt(birthday.split("/")[2]);
        String shotOneDate = Integer.toString(day) + "/" + Integer.toString(month) + "/"
                + Integer.toString(year);
        String shotTwoDate = getNextShotDate(42);
        String shotThreeDate = getNextShotDate(70);
        String shotFourDate = getNextShotDate(98);
        String shotFiveDate = day + "/" + month + "/" + (year + 1);
        return new ArrayList<>(Arrays.asList(shotOneDate, shotOneDate, shotTwoDate, shotTwoDate,
                shotThreeDate, shotThreeDate, shotFourDate, shotFourDate, shotFourDate, shotFiveDate));
    }

    // Returns the date for the next shot, calculating the given number of days after the birthday
    public String getNextShotDate(int numDays) {
        int counter = 0;
        int day = Integer.parseInt(birthday.split("/")[0]);
        int month = Integer.parseInt(birthday.split("/")[1]);
        int year = Integer.parseInt(birthday.split("/")[2]);
        if (leapYears.contains(year)) months.set(1, 29);
        else months.set(1, 28);
        // Math to make sure months and days are correct
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

    // Handles the birthday button in the new child dialog being clicked
    public void birthdayClicked() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DBHandler myDB = new DBHandler(getContext());
        // If the language is Bislama, resets the locale to English just for the time when the
        // language dialog is showing, because android's default settings for vanuatu are incorrect
        // in the date picker dialog, showing January as M01, which would be confusing for many
        if (myDB.getLanguage().equals("bi")) {
            Locale locale = new Locale("en");
            Locale.setDefault(locale);

            Configuration config = new Configuration();
            config.locale = locale;

            getActivity().getBaseContext().getResources().updateConfiguration(
                    config, getActivity().getBaseContext().getResources().getDisplayMetrics());
        }
        myDB.close();

        // Creates the date picker dialog and sets the currently selected date to today
        // Also only allows parents to choose a birthday within the last year
        final DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), datePickerListener,
                year, month, day);
        calendar.set(year, month, day, 23, 59, 59);
        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        calendar.set(year - 1, month, day, 0, 0, 0);
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());

        // If the date picker is cancelled, reset the locale to the user chosen locale
        datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_NEGATIVE) {
                    DBHandler myDB = new DBHandler(getContext());
                    String language = myDB.getLanguage();
                    Locale locale = new Locale(language);
                    Locale.setDefault(locale);

                    Configuration config = new Configuration();
                    config.locale = locale;

                    getActivity().getBaseContext().getResources().updateConfiguration(
                            config, getActivity().getBaseContext().getResources().getDisplayMetrics());
                    myDB.close();
                    datePickerDialog.cancel();
                }
            }
        });

        // Disallows the user from clicking outside of the date picker dialog
        datePickerDialog.setCancelable(false);

        datePickerDialog.show();
    }

    // Handles what happens when the user picks the date for their child's birthday
    private DatePickerDialog.OnDateSetListener datePickerListener
            = new DatePickerDialog.OnDateSetListener() {

        public void onDateSet(DatePicker view, int selectedYear,
                              int selectedMonth, int selectedDay) {

            // Gets the selected date and sets the text of the choose date button to that date
            // Then disallows the user from changing the date, in order to avoid having to redo
            // the notifications right away
            String dateString = selectedDay + "/" + (selectedMonth+1) + "/" + selectedYear;

            dateButton.setText(dateString);
            dateButton.setClickable(false);

            // Resets the locale to the user chosen locale
            DBHandler myDB = new DBHandler(getContext());
            String language = myDB.getLanguage();
            Locale locale = new Locale(language);
            Locale.setDefault(locale);

            Configuration config = new Configuration();
            config.locale = locale;

            getActivity().getBaseContext().getResources().updateConfiguration(
                    config,getActivity().getBaseContext().getResources().getDisplayMetrics());
            myDB.close();
        }
    };
}
