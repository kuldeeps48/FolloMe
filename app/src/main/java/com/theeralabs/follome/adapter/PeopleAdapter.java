package com.theeralabs.follome.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.theeralabs.follome.R;
import com.theeralabs.follome.model.directionMatrix.user.User;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kuldeep on 21/12/17.
 */

public class PeopleAdapter extends RecyclerView.Adapter<PeopleAdapter.ViewHolder> {
    private View view;
    private List<User> mResults;
    public static List<String> imgPaths;
    private Context mContext;

    public PeopleAdapter(List<User> resultList, Context context) {
        mResults = resultList;
        mContext = context;
        imgPaths = new ArrayList<>(mResults.size());
    }

    @Override
    public PeopleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        view = LayoutInflater.from(mContext).inflate(R.layout.item_people, parent, false);
        return new PeopleAdapter.ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(PeopleAdapter.ViewHolder holder, int position) {
        holder.txtName.setText(mResults.get(position).getName());
        holder.txtEmail.setText(mResults.get(position).getEmail());
        Glide.with(mContext)
                .load(mResults.get(position).getPhotoUri())
                .into(holder.img_photo);

        //Store image into disk
        Glide.with(mContext)
                .downloadOnly()
                .apply(new RequestOptions().override(50, 50))
                .load(mResults.get(position).getPhotoUri())
                .into(new SimpleTarget<File>() {
                    @Override
                    public void onResourceReady(File resource, Transition<? super File> transition) {
                        imgPaths.add(resource.getAbsolutePath());
                    }
                });


    }

    @Override
    public int getItemCount() {
        return mResults.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtEmail;
        ImageView img_photo;

        public ViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txt_name);
            txtEmail = itemView.findViewById(R.id.txt_email);
            img_photo = itemView.findViewById(R.id.img_photo);
        }
    }
}

