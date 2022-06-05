package sg.edu.np.mad_p03_group_gg;
import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable{
    private String name;
    private String email;
    private String phonenumber;
    private int id;

    // Parcelable (To pass objects from activity to activity)
    protected User(Parcel in) {
        name = in.readString();
        email = in.readString();
        phonenumber = in.readString();
        id = in.readInt();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(email);
        parcel.writeString(phonenumber);
        parcel.writeInt(id);
    }

    // End


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    public User(){}

    public User(String n, String e, String p){
        setEmail(e);
        setName(n);
        setPhonenumber(p);
    }

}
