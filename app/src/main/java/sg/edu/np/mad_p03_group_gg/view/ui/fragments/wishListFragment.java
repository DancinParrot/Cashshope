package sg.edu.np.mad_p03_group_gg.view.ui.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.Time;
import java.util.ArrayList;

import sg.edu.np.mad_p03_group_gg.R;
import sg.edu.np.mad_p03_group_gg.listingObject;
import sg.edu.np.mad_p03_group_gg.listing_adapter;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link wishListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class wishListFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public wishListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment wishListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static wishListFragment newInstance(String param1, String param2) {
        wishListFragment fragment = new wishListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_wish_list, container, false);
        ArrayList<listingObject> data = new ArrayList<>();

        DatabaseReference connectedRef = FirebaseDatabase.getInstance("https://cashoppe-179d4-default-rtdb.asia-southeast1.firebasedatabase.app").getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (!connected) {
                    Toast.makeText(getActivity(), "No internet connection.", Toast.LENGTH_SHORT).show();
                }

                else {

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), "Failed to retrieve information.", Toast.LENGTH_SHORT).show();
            }
        });

        initialCheckSharedPreferences();
        retrieveFromFirebase(view, data); //Starts main downloading task
        viewChanger(view, data); //Does check for view mode
        return view;
    }

    private void initialCheckSharedPreferences() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Cashopee", MODE_PRIVATE);

        String mode = sharedPreferences.getString("view", "");

        if (mode == "") {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("view", "card");
            editor.commit();
        }
    }

    private void viewChanger(View view, ArrayList<listingObject> data) { //Changes view for listing
        ToggleButton viewMode = view.findViewById(R.id.view_mode);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Cashopee", MODE_PRIVATE);

        String mode = sharedPreferences.getString("view", "");

        switch (mode) {
            case "card":
                viewMode.setText("Card view");
                break;

            case "grid":
                viewMode.setText("Grid view");
                break;
        }

        viewMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Cashopee", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();

                if (b == false) {
                    editor.putString("view", "card");
                }

                else {
                    editor.putString("view", "grid");
                }

                editor.commit();
                recyclerViewStarter(view, data);
            }
        });
    }

    private void retrieveFromFirebase(View view, ArrayList<listingObject> data) { //Retrieves data from Firebase
        String uID = null;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // User is signed in
            uID = String.valueOf(user.getUid());
        } else {
            // No user is signed in
        }

        String dblink = "https://cashoppe-179d4-default-rtdb.asia-southeast1.firebasedatabase.app";
        DatabaseReference db = FirebaseDatabase.getInstance(dblink).getReference().child("users").child(uID).child("liked");
        DatabaseReference db2 = FirebaseDatabase.getInstance(dblink).getReference().child("individual-listing");
        listing_adapter adapter = recyclerViewStarter(view, data);
        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot datasnap : snapshot.getChildren()) {
                    String listingid = datasnap.getKey();
                    DatabaseReference individualListing = db2.child(listingid);
                    individualListing.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                            DataSnapshot result = task.getResult();

                            if (result.getValue() != null) {
                                String titles = String.valueOf(result.child("title").getValue(String.class));
                                long thumbnailurlsize = result.child("tURLs").getChildrenCount();
                                ArrayList<String> tURLs = new ArrayList<>();
                                for (int i = 0; i < thumbnailurlsize; i++) {
                                    tURLs.add(result.child("tURLs").child(String.valueOf(i)).getValue(String.class));
                                }
                                String sellerid = String.valueOf(result.child("sid").getValue(String.class));
                                String itemcondition = String.valueOf(result.child("iC").getValue(String.class));
                                String price = String.valueOf(result.child("price").getValue(String.class));
                                Boolean reserved = result.child("reserved").getValue(Boolean.class);
                                String timeStamp = result.child("timeStamp").getValue(String.class);


                                listingObject listing = new listingObject(listingid, titles, tURLs, sellerid, itemcondition, price, reserved, timeStamp);

                                data.add(listing);
                                adapter.notifyDataSetChanged();
                            }

                            else {
                                db.child(listingid).removeValue();
                            }

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), "Failed to retrieve information.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private listing_adapter recyclerViewStarter(View view, ArrayList<listingObject> data) { //Starts recyclerview
        RecyclerView listingRecycler = view.findViewById(R.id.listing_recycler);
        listing_adapter adapter = new listing_adapter(data);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Cashopee", MODE_PRIVATE);

        String mode = sharedPreferences.getString("view", "");

        switch (mode) {
            case "card":
                LinearLayoutManager cardLayoutManager = new LinearLayoutManager(view.getContext());
                listingRecycler.setLayoutManager(cardLayoutManager);
                listingRecycler.setItemAnimator(new DefaultItemAnimator());
                listingRecycler.setAdapter(adapter);
                break;

            case "grid":
                GridLayoutManager gridLayoutManager = new GridLayoutManager(view.getContext(), 2);
                listingRecycler.setLayoutManager(gridLayoutManager);
                listingRecycler.setItemAnimator(new DefaultItemAnimator());
                listingRecycler.setAdapter(adapter);
                break;
        }

        return adapter;
    }
}