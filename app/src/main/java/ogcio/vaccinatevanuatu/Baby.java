package ogcio.vaccinatevanuatu;

import android.graphics.Bitmap;

/**
 * Created by Grace Whitmore
 * This class creates a new Baby Object; it stores all the information (name, birthday, and gender)
 * for each of the children in the application. It is used for populating the list of children
 * on the first page
 *
 * It also contains getters and setters for each of the pieces of information it stores
 */

class Baby {
    private String name;
    private String birthday;
    private String gender;
    private Bitmap photo;

    Baby(String name, String birthday, String gender, Bitmap photo) {
        this.name = name;
        this.birthday = birthday;
        this.gender = gender;
        this.photo = photo;
    }

    String getName() { return name; }

    void setName(String name) { this.name = name; }

    String getBirthday() { return birthday;}

    void setBirthday(String birthday) { this.birthday = birthday; }

    String getGender() { return gender; }

    void setGender(String gender) { this.gender = gender; }

    Bitmap getPhoto() { return photo; }

    void setPhoto(Bitmap photo) { this.photo = photo; }
}
