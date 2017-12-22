package com.theeralabs.follome.view.login;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theeralabs.follome.R;
import com.theeralabs.follome.model.directionMatrix.user.User;
import com.theeralabs.follome.view.map.MapActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class AddPhotoFragment extends Fragment {
    private static final int TAKEN_FROM_GALLERY = 1;
    private static final int TAKEN_FROM_CAMERA = 2;
    private static final int REQ_PERM = 3;

    private User registeredUser;

    public AddPhotoFragment() {
        // Required empty public constructor
    }

    public static AddPhotoFragment newInstance(User user) {
        AddPhotoFragment fragment = new AddPhotoFragment();
        Bundle args = new Bundle();
        args.putParcelable("userObject", user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            registeredUser = getArguments().getParcelable("userObject");
        }
    }


    private ImageView imgPhoto;
    private Button btnSetPhoto;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_add_photo, container, false);
        btnSetPhoto = v.findViewById(R.id.btn_set_photo);
        imgPhoto = v.findViewById(R.id.img_photo);
        progressBar = v.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        btnSetPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, REQ_PERM);
                } else {
                    selectImage();
                }
            }
        });

        return v;
    }

    private FirebaseStorage storage;
    private DatabaseReference myRef;
    private FirebaseDatabase database;

    private void uploadPhoto() {
        if (TextUtils.isEmpty(imageURI.toString())) {
            Toast.makeText(getActivity(), "Add an image", Toast.LENGTH_SHORT).show();
            return;
        }
        btnSetPhoto.setEnabled(false);

        //Create small image
        InputStream image_stream = null;
        try {
            image_stream = getActivity().getContentResolver().openInputStream(imageURI);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Bitmap b = BitmapFactory.decodeStream(image_stream);
        Bitmap scaled = Bitmap.createScaledBitmap(b,
                (int) (b.getWidth() * 0.07),
                (int) (b.getHeight() * 0.07), true);
        File file = createImageFile();
        if (file != null) {
            FileOutputStream fout;
            try {
                fout = new FileOutputStream(file);
                scaled.compress(Bitmap.CompressFormat.JPEG, 70, fout);
                fout.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Uri compressedImageUri = Uri.fromFile(file);

        //Upload to Firebase storage
        storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference photoRef = storageRef.child("images/" + registeredUser.getId() + ".jpg");
        UploadTask uploadTask = photoRef.putFile(compressedImageUri);

        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(getContext(), "Uploading...", Toast.LENGTH_SHORT).show();
        uploadTask
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        btnSetPhoto.setEnabled(true);
                        Toast.makeText(getContext(), "Upload Failed. Try again", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //Get firebase image uri
                        Log.d("AddPhotoFrag", "onSuccess: Upload Success");
                        Uri downloadUrl = taskSnapshot.getMetadata().getDownloadUrl();
                        registeredUser.setPhotoUri(downloadUrl.toString());

                        //Store in firebase database for future reference
                        database = FirebaseDatabase.getInstance();
                        myRef = database.getReference("users");
                        myRef.child(registeredUser.getId()).setValue(registeredUser);

                        //Start Map activity
                        try {
                            Intent intent = new Intent(getContext(), MapActivity.class);
                            intent.putExtra("userObject", registeredUser);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);

                            //Finish LoginActivity
                            getActivity().finish();
                        } catch (Exception e) {
                            e.getStackTrace();
                        }
                    }

                });

    }

    public File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File mFileTemp = null;
        String root = getActivity().getDir("my_sub_dir", Context.MODE_PRIVATE).getAbsolutePath();
        File myDir = new File(root + "/Img");
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        try {
            mFileTemp = File.createTempFile(imageFileName, ".jpg", myDir.getAbsoluteFile());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return mFileTemp;
    }

    private void selectImage() {
        CharSequence[] options = new CharSequence[]{"Camera", "Gallery", "Cancel"};

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle("Select Image Source");
        alertDialog.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: {
                        cameraCapture();
                        break;
                    }
                    case 1: {
                        takeImageFromGallery();
                        break;
                    }
                    case 2: {
                        dialog.dismiss();
                        break;
                    }
                }
            }
        });
        alertDialog.show();
    }


    Uri destinationUri;

    private void cameraCapture() {
        ContentValues values = new ContentValues(1);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
        destinationUri = getActivity().getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, destinationUri);
        startActivityForResult(intent, TAKEN_FROM_CAMERA);
    }

    private void takeImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, TAKEN_FROM_GALLERY);
    }

    private Uri imageURI = Uri.parse("");

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKEN_FROM_GALLERY) {
            if (resultCode == Activity.RESULT_OK) {
                Uri selectedImage = data.getData();
                assert selectedImage != null;
                imageURI = selectedImage;
                Glide.with(getContext()).load(selectedImage)
                        .into(imgPhoto);
                uploadPhoto();
            }
        }

        if (requestCode == TAKEN_FROM_CAMERA) {
            if (resultCode == Activity.RESULT_OK) {
                imageURI = destinationUri;
                Glide.with(getContext()).load(imageURI)
                        .into(imgPhoto);
                uploadPhoto();
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQ_PERM) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage();
            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, REQ_PERM);
            }
        }
    }
}
