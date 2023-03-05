package com.example.fishcenter;


import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;


public class PostRecyclerViewAdapter extends RecyclerView.Adapter<PostRecyclerViewAdapter.PostViewHolder> {
    private Context con;
    private ArrayList<PostModel> posts;
    private OnClickListener listener;

    private HashSet<String> imageMimeTypes = new HashSet<>(Arrays.asList("image/jpg","image/jpeg","image/png","image/gif"));
    private HashSet<String> videoMimeTypes = new HashSet<>(Arrays.asList("video/3gp","video/mov","video/avi","video/wmv","video/mp4","video/mpeg"));
    public PostRecyclerViewAdapter(Context con, ArrayList<PostModel> posts, OnClickListener listener) {
        this.con = con;
        this.posts = posts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(con);
        View view = inflater.inflate(R.layout.post, parent, false);
        return new PostRecyclerViewAdapter.PostViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        byte[] photoBytes = posts.get(position).getProfilePhoto();
        Glide.with(con).asBitmap().load(photoBytes).placeholder(R.drawable.ic_baseline_person_128_white).transform(new RoundedCorners(10)).into(holder.userProfilePicture);
        holder.userNickname.setText(posts.get(position).getNickname());
        holder.datePosted.setText(posts.get(position).getDatePosted());
        holder.postTitle.setText(posts.get(position).getTitle());
        if(posts.get(position).getMedia() != null) {
            Uri uri = Uri.parse(posts.get(position).getMedia());
            String mimeType = posts.get(position).getMimeType();
            if(uri != null) {
                if(imageMimeTypes.contains(mimeType)) {
                    Glide.with(con).load(uri).transform(new RoundedCorners(30)).into(holder.postImageAndGif);
                    holder.postImageAndGif.setVisibility(View.VISIBLE);
                    holder.postVideoThumbnail.setVisibility(View.GONE);
                } else if (videoMimeTypes.contains(mimeType)) {
                    Glide.with(con).load(uri).transform(new RoundedCorners(30)).into(holder.postVideoThumbnail);
                    holder.postVideoThumbnail.setVisibility(View.VISIBLE);
                    holder.postImageAndGif.setVisibility(View.GONE);
                }
            }
        }
        holder.likesButton.setImageResource(R.drawable.ic_baseline_thumb_up_24_white);
        holder.postBody.setText(posts.get(position).getBody());
        holder.postLikes.setText(posts.get(position).getNumLikes());
    }



    @Override
    public int getItemCount() {
        return posts.size();
    }




    public class PostViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView userProfilePicture;
        private TextView userNickname;
        private TextView datePosted;
        private TextView postTitle;
        private ImageView postImageAndGif;
        private ImageView postVideoThumbnail;
        private TextView postBody;
        private ImageView likesButton;
        private TextView postLikes;
        private OnClickListener listener;


        public PostViewHolder(@NonNull View itemView, OnClickListener listener) {
            super(itemView);
            userProfilePicture = itemView.findViewById(R.id.userProfilePicture);
            userNickname = itemView.findViewById(R.id.userNickname);
            datePosted = itemView.findViewById(R.id.datePosted);
            postTitle = itemView.findViewById(R.id.postTitle);
            postImageAndGif = itemView.findViewById(R.id.imageAndGifView);
            postVideoThumbnail = itemView.findViewById(R.id.postVideoThumbnail);
            postBody = itemView.findViewById(R.id.postBody);
            likesButton = itemView.findViewById(R.id.likesButton);
            postLikes = itemView.findViewById(R.id.postLikes);
            // reference to the MainPageActivity class in which the on-clicks will be handled
            this.listener = listener;
            // listen for on-click the video thumbnail and likes button
            postVideoThumbnail.setOnClickListener(this);
            likesButton.setOnClickListener(this);
        }
        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            // if the view clicked is an image
            if(view.getId() == R.id.postVideoThumbnail) {
                listener.onClickVideoThumbnail(position);
            } else if(view.getId() == R.id.likesButton) {
                listener.onClickLikeButton(position);
            }
        }

    }

}
