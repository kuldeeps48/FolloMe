package com.theeralabs.follome.view.login;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.theeralabs.follome.R;
import com.theeralabs.follome.model.directionMatrix.user.User;

/**
 * A simple {@link Fragment} subclass.
 */
public class RegisterFragment extends Fragment {
    private FirebaseAuth auth;
    private DatabaseReference myRef;
    private FirebaseDatabase database;

    public RegisterFragment() {
        // Required empty public constructor
    }

    EditText edtName, edtEmail, edtPass;
    Button btnSetPhoto, btnRegister;
    ProgressBar progressBar;
    ImageView imgPhoto;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_register, container, false);
        edtEmail = v.findViewById(R.id.edt_email);
        edtPass = v.findViewById(R.id.edt_pass);
        edtName = v.findViewById(R.id.edt_name);
        btnRegister = v.findViewById(R.id.btn_register);
        btnSetPhoto = v.findViewById(R.id.btn_set_photo);
        imgPhoto = v.findViewById(R.id.img_photo);

        progressBar = v.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        setupListeners();
        return v;
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();

                String name = edtName.getText().toString().trim();
                String email = edtEmail.getText().toString().trim();
                String password = edtPass.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(getActivity(), "Name Field Empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getActivity(), "Enter email address", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(getActivity(), "Enter password", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                registerUser(email, password, name);
            }
        });

    }

    private void registerUser(final String email, final String password, final String name) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            String id = task.getResult().getUser().getUid();
                            User user = new User(id, name, email, "", 0.0, 0.0);

                            myRef = database.getReference("users");
                            myRef.child(id).setValue(user);

                            Toast.makeText(getActivity(), "Registered Successfully", Toast.LENGTH_SHORT).show();
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .setCustomAnimations(R.anim.fadein, R.anim.fadeout)
                                    .add(R.id.login_activity_frame, AddPhotoFragment.newInstance(user))
                                    .commit();
                        } else {
                            //Login failed
                            Toast.makeText(getActivity(),
                                    task.getException().getMessage(), Toast.LENGTH_LONG).show();
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
