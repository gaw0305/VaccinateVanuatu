package ogcio.vaccinatevanuatu;

import android.content.Intent;
import android.content.res.Configuration;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.View;

import java.util.Locale;

/**
 * Written by Grace Whitmore
 *
 * The main Window for the application, which contains two tabs; one which displays an overall
 * schedule of shots, the other which allows the user to add children to the list
 */
public class StartupPage extends AppCompatActivity {

    /**
     * Sets up the main page, creates the layout for the tabs
     * @param savedInstanceState loads the savedInstanceState, if exists
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DBHandler myDB = new DBHandler(this);
        if (!myDB.isEmpty("SETTINGS")) {
            String language = myDB.getLanguage();
            Locale locale = new Locale(language);
            Locale.setDefault(locale);

            Configuration config = new Configuration();
            config.locale = locale;

            getBaseContext().getResources().updateConfiguration(
                    config,getBaseContext().getResources().getDisplayMetrics());
        }
        myDB.close();

        setContentView(R.layout.activity_startup_page);

        ViewPager viewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(viewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    // Restarts the activity when it's returned to from the Single Child Data Activity, in case
    // the user changed the language while in that activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                this.recreate();
            }
        }
    }

    /**
     * Adds the list of kids and the schedule to the tabs
     * @param viewPager animates screen slides automatically
     */
    private void setupViewPager(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());
        adapter.addFragment(new KidsList(), getResources().getString(R.string.kids_list_title));
        adapter.addFragment(new ShotSchedule(), getResources().getString(R.string.shot_schedule_title));
        viewPager.setAdapter(adapter);
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

        Locale locale = new Locale(translateLanguage);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.locale = locale;

        getBaseContext().getResources().updateConfiguration(
                config,getBaseContext().getResources().getDisplayMetrics());

        DBHandler myDB = new DBHandler(StartupPage.this);
        myDB.updateLanguageInformation(translateLanguage);
        myDB.close();

        Intent intent = new Intent(StartupPage.this, StartupPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        new Helper(this).rescheduleNotifications();
    }

    // Changes the locale of the app, and adds the current language to the dictionary
    public void initialFlagClicked(View view) {
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

        Locale locale = new Locale(translateLanguage);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.locale = locale;

        getBaseContext().getResources().updateConfiguration(
                config,getBaseContext().getResources().getDisplayMetrics());

        DBHandler myDB = new DBHandler(StartupPage.this);
        myDB.insertLanguageInformation(translateLanguage);
        myDB.close();

        Intent intent = new Intent(StartupPage.this, StartupPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
