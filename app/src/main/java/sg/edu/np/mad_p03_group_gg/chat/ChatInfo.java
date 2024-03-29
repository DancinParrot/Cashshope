package sg.edu.np.mad_p03_group_gg.chat;

public class ChatInfo {
    // Values
    private String id;
    private String name;
    private String message;
    private String date;
    private String time;
    private boolean isImage;

    // Constructor
    public ChatInfo(String id, String name, String message, String date, String time, boolean isImage) {
        this.id = id;
        this.name = name;
        this.message = message;
        this.date = date;
        this.time = time;
        this.isImage = isImage;
    }

    public String getid() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public boolean isImage() {
        return isImage;
    }
}
