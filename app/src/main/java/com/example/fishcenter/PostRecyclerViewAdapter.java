package com.example.fishcenter;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import pl.droidsonroids.gif.GifImageView;

public class PostRecyclerViewAdapter extends RecyclerView.Adapter<PostRecyclerViewAdapter.PostViewHolder> {
    private ContentResolver contResolver;
    private Context con;
    private ArrayList<PostModel> posts;
    private HashSet<String> imageMimeTypes = new HashSet<>(Arrays.asList("image/jpg","image/jpeg","image/png"));
    private HashSet<String> videoMimeTypes = new HashSet<>(Arrays.asList("video/3gp","video/mov","video/avi","video/wmv","video/mp4","video/mpeg"));
    public PostRecyclerViewAdapter(Context con, ArrayList<PostModel> posts) {
        this.con = con;
        this.posts = posts;
        this.contResolver = contResolver;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(con);
        View view = inflater.inflate(R.layout.post, parent, false);
        return new PostRecyclerViewAdapter.PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        System.out.println();
        byte[] photoBytes = posts.get(position).getProfilePhoto();
        Glide.with(con).asBitmap().load(photoBytes).placeholder(R.drawable.ic_baseline_person_128_white).transform(new RoundedCorners(10)).into(holder.userProfilePicture);
        holder.userNickname.setText(posts.get(position).getNickname());
        holder.datePosted.setText(posts.get(position).getDatePosted());
        holder.postTitle.setText(posts.get(position).getTitle());
        Uri uri = posts.get(position).getMedia();

        if(uri != null) {

               //Glide.with(con).load(uri).transform(new RoundedCorners(10)).into(holder.postImage);

           Glide.with(con).load(uri).thumbnail().transform(new RoundedCorners(10)).into(holder.postImageAndGif);


           // Glide.with(con).asGif().load(uri).transform(new RoundedCorners(10)).into(holder.postGif);

        }
        holder.postBody.setText(posts.get(position).getBody());
       // String likes = "Likes: " + posts.get(position).getNumLikes();
        //holder.postLikes.setText(likes);
    }



    @Override
    public int getItemCount() {
        return posts.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        private ImageView userProfilePicture;
        private TextView userNickname;
        private TextView datePosted;
        private TextView postTitle;
        private ImageView postImageAndGif;
        private GifImageView postGif;
        private ImageView postVideoThumbnail;
        private TextView postBody;
        private ImageButton likesButton;
        private TextView postLikes;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            userProfilePicture = itemView.findViewById(R.id.userProfilePicture);
            userNickname = itemView.findViewById(R.id.userNickname);
            datePosted = itemView.findViewById(R.id.datePosted);
            postTitle = itemView.findViewById(R.id.postTitle);
            postGif = itemView.findViewById(R.id.postGif);
            postImageAndGif = itemView.findViewById(R.id.imageAndGifView);
            postBody = itemView.findViewById(R.id.postBody);
            //likesButton = itemView.findViewById(R.id.likesButton);
            //postLikes = itemView.findViewById(R.id.postLikes);
        }
    }

}
