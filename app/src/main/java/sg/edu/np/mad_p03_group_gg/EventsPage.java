package sg.edu.np.mad_p03_group_gg;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import sg.edu.np.mad_p03_group_gg.view.ui.fragments.HomepageFragment;

public class EventsPage extends AppCompatActivity {
    RecyclerView eventRV;
    EventsRecyclerViewAdapter eventsRecyclerViewAdapter;
    private static TextView eventTV;
    private ImageView backBtn;
    private String name, location, time, date, desc, userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.events_page);
        // Check and request for Calendar permissions if denied
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR},
                    23);
        }
        // Initialise views
        eventTV = findViewById(R.id.eventTV);
        eventRV = findViewById(R.id.eventRecyclerView);
        initRecyclerView();
        noOfEvent(Event.eventsList);
        backBtn = findViewById(R.id.backBtn);
        // Closes activity when back button is clicked
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // Initialise eventList
        if (Event.eventsList.size() == 0) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            userId = user.getUid();
            readFromFireBase(userId);
        }
        initRecyclerView();
        // Count number of events created
        noOfEvent(Event.eventsList);
        // Sort events according to date
        Collections.sort(Event.eventsList, new Comparator<Event>() {
            @Override
            public int compare(Event event1, Event event2) {
                int sComp = event1.getDate().compareTo(event2.getDate());
                // If two events have different dates, compare date only
                if (sComp != 0){
                    return sComp;
                }
                // If two events have same dates, compare time as well
                try {
                    // Format time string
                    return new SimpleDateFormat("hh:mm a").parse(event1.getTime()).compareTo(new SimpleDateFormat("hh:mm a").parse(event2.getTime()));
                } catch (ParseException e) {
                    return 0;
                }
            }
        });
    }

    // Initialise recycler view
    private void initRecyclerView(){
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        eventsRecyclerViewAdapter = new EventsRecyclerViewAdapter(Event.eventsList, this);
        eventRV.setLayoutManager(linearLayoutManager);
        eventRV.setAdapter(eventsRecyclerViewAdapter);
        eventsRecyclerViewAdapter.notifyDataSetChanged();
    }

    // Count number of events created by user
    public static void noOfEvent(ArrayList<Event> eventsList){
        String eventNumberText;
        int count = eventsList.size();
        // Set text based on number of events created
        if (count == 0){
            eventNumberText = "No Event planned currently";
        }
        else if (count == 1){
            eventNumberText = "1 Event planned by you";
        }
        else{
            eventNumberText = count + " Events planned by you";
        }
        eventTV.setText(eventNumberText);
    }

    // Create new event when clicked
    public void createEvent(View view){
        // Goes to EventDetails
        Intent newEvent = new Intent(EventsPage.this, EventDetails.class);
        // Send data for EventDetails page to know that a new event is being created
        newEvent.putExtra("NewEvent", true);
        startActivity(newEvent);
    }
    // Temporary only
    public void calendar(View view){
        Intent test = new Intent(EventsPage.this, WeekViewActivity.class);
        startActivity(test);
    }

    /**
     * Read event details of user from Firebase
     * @param userId
     */
    public void readFromFireBase(String userId){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://cashoppe-179d4-default-rtdb.asia-southeast1.firebasedatabase.app/");
        DatabaseReference myRef = database.getReference("Planner");
        myRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    LocalDate dt;
                    int eventId = Integer.parseInt(snapshot.getKey());
                    name = snapshot.child("name").getValue(String.class);
                    location = snapshot.child("location").getValue(String.class);
                    time = snapshot.child("time").getValue(String.class);
                    date = snapshot.child("date").getValue(String.class);
                    try{
                        dt = LocalDate.parse(date, dtf);
                    }catch (DateTimeParseException e){
                        DateTimeFormatter dtfv = DateTimeFormatter.ofPattern("yyyy-MM-d");
                        dt = LocalDate.parse(date, dtfv);
                    }
                    desc = snapshot.child("description").getValue(String.class);
                    // Only display current events
                    if (dt.isAfter(LocalDate.now()) || dt.isEqual(LocalDate.now())){
                        Event event = new Event(eventId, name, location, dt, time, desc);
                        Event.eventsList.add(event);
                    }
                    else {
                        continue;
                    }

                    Collections.sort(Event.eventsList, new Comparator<Event>() {
                        @Override
                        public int compare(Event event1, Event event2) {
                            if (event1.getDate() == event2.getDate()){
                                Log.e("Same event Date", "Event date same");
                                try {
                                    return new SimpleDateFormat("hh:mm a").parse(event1.getTime()).compareTo(new SimpleDateFormat("hh:mm a").parse(event2.getTime()));
                                } catch (ParseException e) {
                                    return 0;
                                }
                            }
                            return event1.getDate().compareTo(event2.getDate());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("TAG", "Failed to read value.", error.toException());
            }
        });
    }
}