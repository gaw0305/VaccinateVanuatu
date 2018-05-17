package ogcio.vaccinatevanuatu;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Written by Grace Whitmore
 *
 * Handles the Database, which contains two table.
 * The first, TABLE_DATA has information on each of the babies and what shots they have received
 * The second, TABLE_SETTINGS stores whether or not to show the welcome message and what language
 * the app should be run in
 */
public class DBHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Immune.db";
    private static final String TABLE_DATA = "DATA";
    private static final String KEY_NAME = "NAME";
    private static final String KEY_BIRTHDAY = "BIRTHDAY";
    private static final String KEY_GENDER = "GENDER";
    private static final String KEY_PHOTO = "PHOTO";
    private static final String KEY_ID_NUM = "ID_NUM";
    private static final String KEY_BCG = "BCG";
    private static final String KEY_HEP_B = "HEP_B";
    private static final String KEY_PENTA = "PENTA";
    private static final String KEY_POLIO = "POLIO";
    private static final String KEY_IPV = "IPV";
    private static final String KEY_MEASLES_RUBELLA = "MEASLES_RUBELLA";
    private static final String KEY_SHOT_DATES = "SHOT_DATES";
    private static final String KEY_UPDATED_SHOT_DATES = "UPDATED_SHOT_DATES";

    private static final String TABLE_SETTINGS = "SETTINGS";
    private static final String KEY_SHOW_STARTUP = "SHOW_STARTUP";
    private static final String KEY_LANGUAGE = "LANGUAGE";

    DBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_DATA_TABLE = "CREATE TABLE " + TABLE_DATA + "("
                + KEY_NAME + " TEXT,"
                + KEY_BIRTHDAY + " TEXT,"
                + KEY_GENDER + " TEXT,"
                + KEY_PHOTO + " BLOB,"
                + KEY_ID_NUM + " INT,"
                + KEY_BCG + " INT,"
                + KEY_HEP_B + " INT,"
                + KEY_PENTA + " INT,"
                + KEY_POLIO + " INT,"
                + KEY_IPV + " INT,"
                + KEY_MEASLES_RUBELLA + " INT,"
                + KEY_SHOT_DATES + " TEXT,"
                + KEY_UPDATED_SHOT_DATES + " TEXT)";
        db.execSQL(CREATE_DATA_TABLE);
        String CREATE_SETTINGS_TABLE = "CREATE TABLE " + TABLE_SETTINGS + "("
                + KEY_SHOW_STARTUP + " INT,"
                + KEY_LANGUAGE + " TEXT)";
        db.execSQL(CREATE_SETTINGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }

    // Inserts information into the database about what language to use in the app
    void insertLanguageInformation(String language) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_SHOW_STARTUP, 1);
        contentValues.put(KEY_LANGUAGE, language);
        db.insert(TABLE_SETTINGS, null, contentValues);
        db.close();
    }

    // Returns the current language chosen by the user
    String getLanguage() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_LANGUAGE + " from " + TABLE_SETTINGS, null);
        cursor.moveToFirst();
        String language = cursor.getString(0);
        cursor.close();
        db.close();
        return language;
    }

    // Updates the language if the user decides to change it
    void updateLanguageInformation(String language) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_SETTINGS + " SET " + KEY_LANGUAGE + "='" + language + "'");
        db.close();
    }

    // Updates the db so that the user will no longer see the startup information
    void updateStartupInformation() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_SETTINGS + " SET " + KEY_SHOW_STARTUP + "=0");
        db.close();
    }

    // Returns whether or not to show the startup dialog
    boolean showStartupDialog() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_SHOW_STARTUP + " from "
                + TABLE_SETTINGS, null);
        cursor.moveToFirst();
        int answer = cursor.getInt(0);
        cursor.close();
        db.close();
        return (answer == 1);
    }

    // Inserts a new child's information into the db when a new child is added to the list
    // idNum allows for scheduling and accessing unique notifications for each child
    void insertNameAndBirthdayData(String name, String birthday, String gender, int idNum, String shotDates,
                                   byte[] photo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_NAME, name);
        contentValues.put(KEY_BIRTHDAY, birthday);
        contentValues.put(KEY_GENDER, gender);
        contentValues.put(KEY_PHOTO, photo);
        contentValues.put(KEY_ID_NUM, idNum);
        contentValues.put(KEY_BCG, 0);
        contentValues.put(KEY_HEP_B, 0);
        contentValues.put(KEY_PENTA, 0);
        contentValues.put(KEY_POLIO, 0);
        contentValues.put(KEY_IPV, 0);
        contentValues.put(KEY_MEASLES_RUBELLA, 0);
        contentValues.put(KEY_SHOT_DATES, shotDates);
        contentValues.put(KEY_UPDATED_SHOT_DATES, shotDates);
        db.insert(TABLE_DATA, null, contentValues);
    }

    // Returns all the information stored in the data table, AKA all information about all the children
    // registered in the app
    Cursor getAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("SELECT * from " + TABLE_DATA, null);
    }

    // Returns the list of updated shot dates, AKA the shots that the user has gotten on a different
    // date than they should have, or that have been automatically updated by those shots
    ArrayList<String> getUpdatedShotDates(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_UPDATED_SHOT_DATES + " from "
                + TABLE_DATA + " WHERE " + KEY_NAME + "='" + name + "'", null);
        cursor.moveToFirst();
        ArrayList<String> shotDates = new ArrayList<>(Arrays.asList(cursor.getString(0).split(",")));
        db.close();
        cursor.close();
        return shotDates;
    }

    // Updates the id number of a given child, used for scheduling different notifications
    void updateIDNum(String name, int idNum) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_DATA + " SET " + KEY_ID_NUM + "=" + idNum + " WHERE "
                + KEY_NAME + "='" + name + "'");
        db.close();
    }

    // Returns the list of shot dates that are scheduled based on the child's date of birth
    ArrayList<String> getShotDates(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_SHOT_DATES + " from "
                + TABLE_DATA + " WHERE " + KEY_NAME + "='" + name + "'", null);
        cursor.moveToFirst();
        ArrayList<String> shotDates = new ArrayList<>(Arrays.asList(cursor.getString(0).split(",")));
        db.close();
        cursor.close();
        return shotDates;
    }

    // Returns whether or not a given table is empty
    boolean isEmpty(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * from '" + tableName + "'", null);
        int count = cursor.getCount();
        cursor.close();
        db.close();
        return count == 0;
    }

    // Returns whether or not a name already exists in the database so that parents cannot add
    // a given name twice
    boolean nameExists(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_NAME + " from " + TABLE_DATA, null);
        cursor.moveToFirst();
        if (cursor.getCount() == 0) return false;
        do {
            if (name.equals(cursor.getString(0))) return true;
        } while (cursor.moveToNext());
        db.close();
        cursor.close();
        return false;
    }

    // Returns the idNum for the requested child
    int getIdNum(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_ID_NUM + " from " + TABLE_DATA + " WHERE "
                + KEY_NAME + "='" + name + "'", null);
        cursor.moveToFirst();
        int num = cursor.getInt(0);
        cursor.close();
        db.close();
        return num;
    }

    // Returns the birthday for the requested child
    String getUserBirthday(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_BIRTHDAY + " from " + TABLE_DATA
                + " WHERE " + KEY_NAME + "='" + name + "'", null);
        cursor.moveToFirst();
        String birthday = cursor.getString(0);
        cursor.close();
        db.close();
        return birthday;
    }

    // Returns a new id number for a child
    int generateIdNum() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_ID_NUM + " from " + TABLE_DATA, null);
        cursor.moveToFirst();
        int num = 0;
        if (cursor.getCount() > 0) {
            do {
                if (cursor.getInt(0) > num) num = cursor.getInt(0);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return num;
    }

    // Deletes the data for a child
    void deleteUserData(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE From " + TABLE_DATA + " WHERE " + KEY_NAME + "='" + name + "'");
        db.close();
    }

    // Updates the information for a shot, if it is 0, the shot has not been given. If 1, it has
    void updateShotData(String name, String shotName, int num) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + shotName + " from " + TABLE_DATA
                + " WHERE " + KEY_NAME + "='" + name + "'", null);
        cursor.moveToFirst();
        int data = cursor.getInt(0) + num;
        cursor.close();
        db.execSQL("UPDATE " + TABLE_DATA + " SET " + shotName + "=" + data + " WHERE "
                + KEY_NAME + "='" + name + "'");
        db.close();
    }

    // Updates the shot date in the updated shots list, meaning that a user has entered information
    // about a shot
    void updateShotDate(String name, int index, String shotDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_UPDATED_SHOT_DATES + " from " + TABLE_DATA
                + " WHERE " + KEY_NAME + "='" + name + "'", null);
        cursor.moveToFirst();
        ArrayList<String> shotDates = new ArrayList<>(Arrays.asList(cursor.getString(0).split(",")));
        StringBuffer updatedShotDates;
        if (index == 0) updatedShotDates = new StringBuffer(shotDate);
        else updatedShotDates = new StringBuffer(shotDates.get(0));
        for (int i = 1; i < shotDates.size(); i++) {
            if (i == index) updatedShotDates.append(",").append(shotDate);
            else updatedShotDates.append(",").append(shotDates.get(i));
        }
        cursor.close();
        db.execSQL("UPDATE " + TABLE_DATA + " SET " + KEY_UPDATED_SHOT_DATES + "='" + updatedShotDates
                + "' WHERE " + KEY_NAME + "='" + name + "'");
        db.close();
    }

    // Returns whether or not a shot has been given
    boolean shotGiven(String shotNameForDB, String shotNum, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + shotNameForDB + " from " + TABLE_DATA + " WHERE " + KEY_NAME + "='"
            + name + "'", null);
        cursor.moveToFirst();
        int shotData = cursor.getInt(0);
        db.close();
        cursor.close();
        // Since Penta and Polio require three shots, 0 means none given, 1 means first given, etc
        switch(shotNum) {
            case "FIRST":
                return shotData != 0;
            case "SECOND":
                return !(shotData <= 1);
            case "THIRD":
                return !(shotData <= 2);
            default:
                return shotData != 0;
        }
    }

    // Returns a list of all the children's names that have been registered
    String getNames() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_NAME + " from " + TABLE_DATA, null);
        cursor.moveToFirst();
        StringBuilder names = new StringBuilder(cursor.getString(0));
        while(cursor.moveToNext()) {
            names.append(",").append(cursor.getString(0));
        }
        cursor.close();
        db.close();
        return names.toString();
    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }
}
