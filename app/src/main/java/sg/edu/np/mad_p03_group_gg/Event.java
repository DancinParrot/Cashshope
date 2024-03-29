package sg.edu.np.mad_p03_group_gg;

import java.time.LocalDate;
import java.util.ArrayList;

public class Event
{
    private int id;
    private String name;
    private String location;
    private LocalDate date;
    private String time;
    private String desc;
    public static ArrayList<Event> eventsList = new ArrayList<>();

    // Get events for each date
    public static ArrayList<Event> eventsForDate(LocalDate date)
    {
        ArrayList<Event> events = new ArrayList<>();
        for(Event event : eventsList)
        {
            if(event.getDate().equals(date))
                events.add(event);
        }
        return events;
    }

    public Event(int id, String name, String location, LocalDate date, String time, String desc)
    {
        this.id = id;
        this.name = name;
        this.location = location;
        this.date = date;
        this.time = time;
        this.desc = desc;
    }

    // Get selected event when editing an event
    public static Event getEventForID(int passedEventID) {
        for (Event event : eventsList){
            if (event.getID() == passedEventID){
                return event;
            }
        }
        return null;
    }

    public int getID(){return id;}

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getLocation(){return location;}

    public void setLocation(String location){
        this.location = location;
    }

    public LocalDate getDate()
    {
        return date;
    }

    public void setDate(LocalDate date)
    {
        this.date = date;
    }

    public String getTime()
    {
        return time;
    }

    public void setTime(String time)
    {
        this.time = time;
    }

    public String getDesc(){return desc;}

    public void setDesc(String desc) {this.desc = desc;}
}