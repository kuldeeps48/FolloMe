package com.theeralabs.follome.view.peopleList;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.theeralabs.follome.R;
import com.theeralabs.follome.adapter.PeopleAdapter;
import com.theeralabs.follome.model.directionMatrix.user.User;
import com.theeralabs.follome.util.ItemClickListener;
import com.theeralabs.follome.util.OnSwipeTouchListener;
import com.theeralabs.follome.view.map.MapActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class PeopleListFragment extends Fragment {


    public PeopleListFragment() {
        // Required empty public constructor
    }


    RecyclerView recyclerView;
    ProgressBar progressBar;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_people_list, container, false);
        progressBar = v.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        recyclerView = v.findViewById(R.id.people_recyclerview);

        getPeopleList();
        return v;
    }

    private void showPersonOnMap(User listPerson) {
        MapActivity.addMarker(listPerson);
        getActivity().getSupportFragmentManager().popBackStack();
    }

    List<User> userList;
    private void getPeopleList() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("users");
        ref.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //Get map of users in datasnapshot
                        progressBar.setVisibility(View.GONE);
                        userList = new ArrayList<>();
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            userList.add(ds.getValue(User.class));
                        }

                        //Set recyclerView
                        recyclerView.addOnItemTouchListener(new ItemClickListener(getContext(), new ItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                User listPerson = userList.get(position);
                                Toast.makeText(getContext(), "Clicked!", Toast.LENGTH_SHORT).show();
                                showPersonOnMap(listPerson);
                            }
                        }));
                        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
                        recyclerView.setAdapter(new PeopleAdapter(userList, getContext()));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //handle databaseError
                        Toast.makeText(getContext(), "Database error", Toast.LENGTH_SHORT).show();
                    }
                });
    }



}
