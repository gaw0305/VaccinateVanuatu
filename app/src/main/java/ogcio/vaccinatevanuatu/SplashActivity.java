package ogcio.vaccinatevanuatu;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Grace Whitmore
 *
 * Shows the splash screen briefly, while the app is loading. Then starts the Startup Activity
 */

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, StartupPage.class);
        startActivity(intent);
        finish();
    }
}
