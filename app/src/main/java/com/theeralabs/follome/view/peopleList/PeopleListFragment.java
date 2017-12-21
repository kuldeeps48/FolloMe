package com.theeralabs.follome.view.peopleList;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.theeralabs.follome.R;
import com.theeralabs.follome.util.OnSwipeTouchListener;

/**
 * A simple {@link Fragment} subclass.
 */
public class PeopleListFragment extends Fragment {


    public PeopleListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_people_list, container, false);
        RecyclerView recyclerView = v.findViewById(R.id.people_recyclerview);
        recyclerView.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();
                onDestroy();
            }
        });
        return v;
    }

}
