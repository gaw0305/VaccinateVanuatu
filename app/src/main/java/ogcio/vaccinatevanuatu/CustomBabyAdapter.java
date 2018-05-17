package ogcio.vaccinatevanuatu;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Grace Whitmore
 *
 * Takes a baby object, and translates its information into a listview item for use on the
 * front page in the list of children
 */

public class CustomBabyAdapter extends ArrayAdapter<ogcio.vaccinatevanuatu.Baby> {

    private ArrayList<Baby> babyList = new ArrayList<>();

    CustomBabyAdapter(Context context, int textViewResourceId, ArrayList<Baby> objects) {
        super(context, textViewResourceId, objects);
        babyList = objects;
    }

    @Override
    public int getCount() {
        return super.getCount();
    }

    /**
     * With each baby in the list, it inflates the child_layout file and populates it with
     * the baby's name, birthday, and a picture representing its gender
     *
     * @param position: position in list
     * @param convertView: the view to be converted, in this case to be inflated with child_Layout
     * @param parent: the parent viewgroup, in this case the listview
     * @return the populated view
     */
    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null)
            convertView = View.inflate(getContext(), R.layout.child_layout, null);

        // Gets instances of the xml resources so they can be set with the correct information
        TextView name = convertView.findViewById(R.id.name);
        TextView birthday = convertView.findViewById(R.id.birthday);
        ImageView imageView = convertView.findViewById(R.id.genderImage);

        // Sets the name, birthday, and gender image for the baby
        name.setText(babyList.get(position).getName());
        String birthdayString = parent.getResources().getString(R.string.birthday) + ": "
                + babyList.get(position).getBirthday();
        birthday.setText(birthdayString);
        if (babyList.get(position).getPhoto() != null)
            imageView.setImageBitmap(babyList.get(position).getPhoto());
        else if (babyList.get(position).getGender().equals("Male"))
            imageView.setImageResource(R.drawable.boy);
        else imageView.setImageResource(R.drawable.girl);
        imageView.setTag(babyList.get(position).getGender());
        return convertView;
    }
}
