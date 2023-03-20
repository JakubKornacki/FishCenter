package com.example.fishcenter;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import java.util.ArrayList;

public class PostRecyclerViewAdapter extends RecyclerView.Adapter<PostRecyclerViewAdapter.PostViewHolder> {
    private Context con;
    private ArrayList<PostModel> posts;
    private OnClickListener listener;
    private Drawable playVideoIcon;


    public PostRecyclerViewAdapter(Context con, ArrayList<PostModel> posts, OnClickListener listener) {
        this.con = con;
        this.posts = posts;
        this.listener = listener;
        playVideoIcon = con.getDrawable(R.drawable.img_play_video_foreground);
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(con);
        View view = inflater.inflate(R.layout.layout_post, parent, false);
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
                if(MediaUtilities.supportedImageMimeTypes.contains(mimeType)) {
                    Glide.with(con).load(uri).transform(new RoundedCorners(30)).into(holder.postImageAndGif);
                    holder.postImageAndGif.setVisibility(View.VISIBLE);
                    holder.postVideoThumbnail.setVisibility(View.GONE);
                } else if (MediaUtilities.supportedVideoMimeTypes.contains(mimeType)) {
                    Glide.with(con).load(uri).transform(new RoundedCorners(30)).into(holder.postVideoThumbnail);
                    holder.postVideoThumbnail.setForeground(playVideoIcon);
                    holder.postVideoThumbnail.setVisibility(View.VISIBLE);
                    holder.postImageAndGif.setVisibility(View.GONE);
                }
            }
        }
        holder.postBody.setText(posts.get(position).getBody());
        holder.likesButton.setImageResource(R.drawable.ic_baseline_thumb_up_24_white);
        holder.dislikesButton.setImageResource(R.drawable.ic_baseline_thumb_down_24_white);
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
        private ImageView dislikesButton;
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
            dislikesButton = itemView.findViewById(R.id.dislikesButton);
            // reference to the MainPageActivity class in which the on-clicks will be handled
            this.listener = listener;
            // listen for on-click the video thumbnail and likes button
            postVideoThumbnail.setOnClickListener(this);
            likesButton.setOnClickListener(this);
            dislikesButton.setOnClickListener(this);
        }
        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            // if the view clicked is an image
            if(view.getId() == R.id.postVideoThumbnail) {
                listener.onClickVideoThumbnail(position);
            } else if(view.getId() == R.id.likesButton || view.getId() == R.id.dislikesButton) {
                listener.onClickEitherLikeButton(position, view.getId());
            }
        }

    }

}
