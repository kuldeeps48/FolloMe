package com.theeralabs.follome.view.login;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.theeralabs.follome.R;
import com.theeralabs.follome.model.directionMatrix.user.User;
import com.theeralabs.follome.view.map.MapActivity;

/**
 * A simple {@link Fragment} subclass.
 */
public class LoginFragment extends Fragment {
    private FirebaseAuth auth;

    @Override
    public void onStart() {
        super.onStart();
        //FirebaseUser currentUser = auth.getCurrentUser();

    }

    public LoginFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);
        init(v);
        return v;
    }

    EditText edtEmail, edtPass;
    Button btnLogin, btnRegister;
    ProgressBar progressBar;

    private void init(View v) {
        edtEmail = v.findViewById(R.id.edt_email);
        edtPass = v.findViewById(R.id.edt_pass);
        btnLogin = v.findViewById(R.id.btn_login);
        btnRegister = v.findViewById(R.id.btn_register);
        progressBar = v.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        auth = FirebaseAuth.getInstance();
        setupListeners();
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();

                String email = edtEmail.getText().toString().trim();
                final String password = edtPass.getText().toString().trim();
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getContext(), "Enter email address", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(getContext(), "Enter password", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                loginWithEmailPassword(email, password);
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.setCustomAnimations(R.anim.fadein, R.anim.fadeout);
                transaction.replace(R.id.login_activity_frame, new RegisterFragment(), "RegisterFrag")
                        .addToBackStack("RegisterFragment")
                        .commit();

            }
        });
    }

    private void loginWithEmailPassword(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //Logged in
                            //Get user info from firebase db
                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            DatabaseReference myRef = database.getReference("users")
                                    .child(task.getResult().getUser().getUid());

                            myRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    progressBar.setVisibility(View.GONE);

                                    User u = dataSnapshot.getValue(User.class);
                                    Intent intent = new Intent(getActivity(), MapActivity.class);
                                    intent.putExtra("userObject", u);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);

                                    getActivity().finish();
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    progressBar.setVisibility(View.GONE);
                                }
                            });

                        } else {
                            //Login failed
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getActivity(),
                                    "    Authentication Failed. \nCheck credentials or Register", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }



    private void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            assert imm != null;
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}
