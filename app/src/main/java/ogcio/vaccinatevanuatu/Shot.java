package ogcio.vaccinatevanuatu;

/**
 * Created by Grace Whitmore
 *
 * This class creates a new Shot Object; it stores all the information (title, due date, and
 * whether or not it's been given) for each of the children in the application. It is used
 * for population the shot lists in Single Child Data Activity
 *
 * It also contains getters and setters for each of the pieces of information it stores
 */

public class Shot {
    String shotTitle;
    String shotDue;
    boolean shotGiven;
    public Shot(String shotTitle, String shotDue, boolean shotGiven) {
        this.shotTitle = shotTitle;
        this.shotDue = shotDue;
        this.shotGiven = shotGiven;
    }

    public String getShotTitle() { return shotTitle; }

    public void setShotTitle(String shotTitle) { this.shotTitle = shotTitle; }

    public String getShotDue() { return shotDue;}

    public void setShotDue(String shotDue) { this.shotDue = shotDue; }

    public boolean getShotGiven() { return shotGiven; }

    public void setShotGiven(boolean shotGiven) { this.shotGiven = shotGiven; }
}
