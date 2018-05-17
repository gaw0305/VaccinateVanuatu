package ogcio.vaccinatevanuatu;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Grace Whitmore
 *
 * Takes a shot object, and translates its information into a listview item for use on the
 * SingleChildDataActivity page in the list of shots
 */

public class CustomShotAdapter extends ArrayAdapter<ogcio.vaccinatevanuatu.Shot> {

    private ArrayList<Shot> shotList = new ArrayList<>();

    CustomShotAdapter(Context context, int textViewResourceId, ArrayList<Shot> objects) {
        super(context, textViewResourceId, objects);
        shotList = objects;
    }

    @Override
    public int getCount() {
        return super.getCount();
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null)
            convertView = View.inflate(getContext(), R.layout.shot_layout, null);
        TextView shotTitle = (TextView) convertView.findViewById(R.id.shot);
        TextView shotDate = (TextView) convertView.findViewById(R.id.date);
        String shotDateString = shotList.get(position).getShotDue();
        boolean shotGiven = shotList.get(position).getShotGiven();
        shotTitle.setText(shotList.get(position).getShotTitle());
        shotDate.setText(new Helper(getContext()).formatBirthday(shotDateString));

        // Sets color of shotdate text based on if the shot has been given or has been missed
        if (shotGiven)  shotDate.setTextColor(Color.parseColor("#009900"));
        else if (shotMissed(shotDateString))
            shotDate.setTextColor(Color.parseColor("#990000"));
        else shotDate.setTextColor(Color.parseColor("#000000"));

        return convertView;
    }

    // Returns whether or not the date the shot should have been given has already passed or not
    private boolean shotMissed(String dateOfShot) {
        Calendar calendar = Calendar.getInstance();
        int curDay = calendar.get(Calendar.DATE);
        int curMonth = calendar.get(Calendar.MONTH) + 1;
        int curYear = calendar.get(Calendar.YEAR);

        int shotDay = Integer.parseInt(dateOfShot.split("/")[0]);
        int shotMonth = Integer.parseInt(dateOfShot.split("/")[1]);
        int shotYear = Integer.parseInt(dateOfShot.split("/")[2]);

        if (curYear > shotYear) return true;
        else if (curYear == shotYear && curMonth > shotMonth) return true;
        else if (curYear == shotYear && curMonth == shotMonth && curDay > shotDay) return true;
        return false;
    }
}
