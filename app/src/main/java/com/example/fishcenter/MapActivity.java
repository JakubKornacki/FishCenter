package com.example.fishcenter;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.room.Room;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private ImageButton goBackImageButton;
    private ImageButton logoutImageButton;

    private LinearLayout progressSpinnerLayout;
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private final String userId = firebaseAuth.getCurrentUser().getUid();
    private Toolbar toolbar;
    private FishingLocationDatabase fishingLocationDatabase;
    private FishingLocationDao fishingLocationDao;
    private SupportMapFragment mapFragment;
    private CustomFishingLocationInfoWindowAdapter adapter;
    private ImageView syncRoomWithFirestoreButton;
    private ArrayList<Marker> refMapMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        progressSpinnerLayout = findViewById(R.id.progressSpinnerLayout);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);

        // need this as google maps overwrite the view and make the toolbar dis
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        fishingLocationDatabase = Room.databaseBuilder(getApplicationContext(), FishingLocationDatabase.class, "fishLocationDatabase").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        fishingLocationDao =  fishingLocationDatabase.fishingLocationDao();

        goBackImageButton = findViewById(R.id.goBackImageButton);
        goBackImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        logoutImageButton = findViewById(R.id.logoutImageButton);
        logoutImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertUtilities.createLogoutDialog(MapActivity.this, firebaseAuth);
            }
        });

        syncRoomWithFirestoreButton = findViewById(R.id.syncDatabasesButton);
        syncRoomWithFirestoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSpinnerAndDisableComponents(true);
                synchronizeRoomDatabaseWithFirestore();
                setupMapWithMarkersFromRoomDatabase();
            }
        });
    }
    private void showSpinnerAndDisableComponents(boolean flag) {
        syncRoomWithFirestoreButton.setClickable(!flag);
        goBackImageButton.setClickable(!flag);
        if(flag) {
            progressSpinnerLayout.setVisibility(View.VISIBLE);
            syncRoomWithFirestoreButton.setBackground(null);
            goBackImageButton.setBackground(null);
        } else {
            progressSpinnerLayout.setVisibility(View.INVISIBLE);
            syncRoomWithFirestoreButton.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_5_gray_opacity_25_to_transparent));
            goBackImageButton.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_5_gray_opacity_25_to_transparent));
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        adapter = new CustomFishingLocationInfoWindowAdapter(getApplicationContext());
        googleMap.setInfoWindowAdapter(adapter);
        // setup markers that are in the user ROOM database
        setupMapWithMarkersFromRoomDatabase();
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                LatLng markerPosition = marker.getPosition();
                String tidesData = null;
                try {
                    tidesData = getTidesDataForMarker(markerPosition);
                } catch (InvalidLocationException e) {
                    // this should not get triggered as the location should already
                    // contain valid coordinates if a marker was created for it
                    throw new RuntimeException(e);
                }
                String ratingData = getRatingDataForMarker(markerPosition);
                // need to concatenate both for display purposes as marker
                // only contains 1 snippet and all data to be displayed
                // needs to fit within it
                marker.setSnippet(tidesData + ratingData);
                adapter.setLocationName(marker.getTitle());
                adapter.setLocationData(tidesData + ratingData);
                marker.showInfoWindow();
                focusCameraOnMarker(marker, googleMap);
                return true;
            }
        });
        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng latLng) {
                createNewFishingLocationAlertDialog(latLng);
            }
        });
        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(@NonNull Marker marker) {
                if(marker.isInfoWindowShown()) {
                    marker.hideInfoWindow();
                }
            }
        });
        googleMap.setOnInfoWindowLongClickListener(new GoogleMap.OnInfoWindowLongClickListener() {
            @Override
            public void onInfoWindowLongClick(@NonNull Marker marker) {
                createReviewFishingLocationAlertDialog(marker.getPosition());
            }
        });
    }

    private void focusCameraOnMarker(Marker marker, GoogleMap map) {
        // update the camera to move to the center of the marker
        float currentZoom = map.getCameraPosition().zoom;
        float currentTilt = map.getCameraPosition().tilt;
        float currentBearing = map.getCameraPosition().bearing;
        // define the new camera position and animate the camera to the new position
        CameraPosition newCameraPosition = new CameraPosition(marker.getPosition(),currentZoom, currentTilt, currentBearing);
        map.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition));
    }
    public void setupMapWithMarkersFromRoomDatabase() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                for(FishingLocation fishingLocation : fishingLocationDao.getAllFishingLocations()) {
                    LatLng position = fishingLocation.getPosition();
                    String locationName = fishingLocation.getLocationName();
                    // both operation below need to be run on the UI thread as only it can modify the views
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(findMarkerByLatLng(position) == null) {
                                createMarkerFromRoomDatabase(locationName, position);
                            }
                            showSpinnerAndDisableComponents(false);
                        }
                    });
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showSpinnerAndDisableComponents(false);
                    }
                });
            }
        }.start();
    }
    private Marker findMarkerByLatLng(LatLng position) {
        for(int i = 0; i < refMapMarkers.size(); i++) {
            if(refMapMarkers.get(i).getPosition().equals(position)){
                return refMapMarkers.get(i);
            }
        }
        return null;
    }
    private String getTidesDataForMarker(LatLng position) throws InvalidLocationException {
        double longitude = position.longitude;
        double latitude = position.latitude;
        OpenMeteoGetTidesData getTidesData = new OpenMeteoGetTidesData(MapActivity.this, latitude, longitude);
        getTidesData.start();
        try {
            getTidesData.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if(getTidesData.getResponseCode() != 200) {
            throw new InvalidLocationException("Attempt to create a location on land!");
        }
        return getTidesData.getTidesData();
    }

    private String getRatingDataForMarker(LatLng position) {
        double longitude = position.longitude;
        double latitude = position.latitude;
        FishingLocation fishingLocation = fishingLocationDao.findFishingLocationByLatLng(latitude, longitude);
        if(fishingLocation == null) {
            return "\nThis location is rated: 0.0 \n\nPress on the window to rate the location!\n";
        } else {
            return "\nThis location is rated: " + fishingLocation.getOverallRating() + "\n\nPress on the window to rate the location!\n";
        }
    }

    private void createReviewFishingLocationAlertDialog(LatLng position) {
        AlertDialog.Builder reviewLocationDialogBuilder = new AlertDialog.Builder(MapActivity.this);
        reviewLocationDialogBuilder.setTitle("Give a rating of this location!");
        // define the rating bar
        RatingBar ratingBar = new RatingBar(MapActivity.this);
        ratingBar.setNumStars(5);
        // linear layout to hold the rating bar that allows to define params to wrap content so rating bar does not overflow and stays at 5 stars
        LinearLayout ratingBarLinearLayout = new LinearLayout(MapActivity.this);
        LinearLayout.LayoutParams ratingBarLinearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        // move the rating bar a bit down
        ratingBarLinearLayoutParams.setMargins(0,15,0,0);
        // set the layout params to the rating bar
        ratingBar.setLayoutParams(ratingBarLinearLayoutParams);
        // put the rating bar inside of the linear layout center the layout and add it into the dialog box
        ratingBarLinearLayout.addView(ratingBar);
        ratingBarLinearLayout.setGravity(Gravity.CENTER);
        reviewLocationDialogBuilder.setView(ratingBarLinearLayout);
        // get the rating of the location in question which will show the user his previous rating of this location
        double fishingLocationRating = fishingLocationDao.findFishingLocationByLatLng(position.latitude, position.longitude).getUserRating();
        ratingBar.setRating((float) fishingLocationRating);
        reviewLocationDialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                double newUserRating = ratingBar.getRating();
                // if the user has actually rated an location
                if(newUserRating != 0.0) {
                    updateFishingLocationRatingInFirestore(position, newUserRating);
                    updateFishingLocationRatingInRoomDatabase(position, newUserRating);
                } else {
                    Toast.makeText(MapActivity.this, "Your rating was not saved! Please select a value!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        reviewLocationDialogBuilder.show();
    }


    private void updateFishingLocationRatingInRoomDatabase(LatLng position, double newUserRating) {
        // save the entry in ROOM and update the corresponding object
        FishingLocation fishingLocation = fishingLocationDao.findFishingLocationByLatLng(position.latitude, position.longitude);
        double overallRating = 0;
        double currentUserRating = fishingLocation.getUserRating();
        ArrayList<Double> ratingsList = fishingLocation.getRatingsList();
        if(ratingsList == null) {
            ratingsList = new ArrayList<>();
        }
        // if the user has not rated the location before add the new rating
        if(currentUserRating == 0) {
            ratingsList.add(newUserRating);
        } else {
            // if the user rated the location before replace the rating
            ratingsList.remove(currentUserRating);
            ratingsList.add(newUserRating);
        }
        // recalculate the total rating
        for(int j = 0; j < ratingsList.size(); j++) {
            overallRating += ratingsList.get(j);
        }
        overallRating /= ratingsList.size();
        // update the entry in the ROOM database
        fishingLocation.setUserRating(newUserRating);
        fishingLocation.setOverallRating(overallRating);
        fishingLocationDao.updateFishingLocation(fishingLocation);
        // update the marker on the map
        Marker markerToUpdate = findMarkerByLatLng(position);
        // should not happen as the refMapMarkers list should contain this marker
        if (markerToUpdate != null) {
            updateMarkerOnMapWithNewOverallRating(markerToUpdate);
        }
    }
    private void updateMarkerOnMapWithNewOverallRating(Marker marker) {
        try {
            LatLng markerPosition = marker.getPosition();
            String tidesData = getTidesDataForMarker(markerPosition);
            String ratingData = getRatingDataForMarker(markerPosition);
            marker.setSnippet(tidesData + ratingData);
            adapter.setLocationName(marker.getTitle());
            adapter.setLocationData(marker.getSnippet());
            marker.showInfoWindow();
        } catch (InvalidLocationException e) {
            // should not be thrown as the marker should have an valid location at this point
            throw new RuntimeException(e);
        }
    }
    private void updateFishingLocationRatingInFirestore(LatLng position, double newUserRating){
        // call to firebase to save the rating in the backend
        CollectionReference fishingLocations = firebaseFirestore.collection("fishingLocations");
        double latitude = position.latitude;
        double longitude = position.longitude;
        // find the fishing location based on latitude and longitude
        fishingLocations.whereEqualTo("latitude", latitude).whereEqualTo("longitude", longitude).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                // get the fishing location returned there should only be one instance of it
                String fishingLocationFirestoreRef = queryDocumentSnapshots.getDocuments().get(0).getId();
                fishingLocations.document(fishingLocationFirestoreRef).collection("ratings").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        Map<String, Object> ratingMap = new HashMap<>();
                        ratingMap.put("userId", userId);
                        ratingMap.put("rating", newUserRating);
                        List<DocumentSnapshot> userRatings = queryDocumentSnapshots.getDocuments();
                        // use the stream filter to find if the user has already liked this rating and collect the document reference into a list of document snapshots
                        DocumentSnapshot userRating = getUserPreviousRating(userRatings, userId);
                        // if the entry for this user exists update it
                        if(userRating != null) {
                            // there should only be one entry in the list per user
                            String ratingFirestoreRef = userRating.getId();
                            fishingLocations.document(fishingLocationFirestoreRef).collection("ratings").document(ratingFirestoreRef).update(ratingMap);
                            Toast.makeText(MapActivity.this, "We have updated your rating with " + newUserRating + "!", Toast.LENGTH_SHORT).show();
                        } else {
                            // the user did not like this location yet so create a new entry
                            fishingLocations.document(fishingLocationFirestoreRef).collection("ratings").add(ratingMap);
                            Toast.makeText(MapActivity.this, "We have received your rating of " + newUserRating + "!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
    private DocumentSnapshot getUserPreviousRating(List<DocumentSnapshot> fishingLocationRatings, String userId) {
        for(DocumentSnapshot rating : fishingLocationRatings) {
            if(rating.get("userId").equals(userId)){
                return rating;
            }
        }
        return null;
    }
    private void createNewFishingLocationAlertDialog(LatLng position) {
        AlertDialog.Builder newLocationBuilder = new AlertDialog.Builder(MapActivity.this);
        newLocationBuilder.setTitle("How would you like to call this location?");
        EditText nameLocationText = new EditText(getApplicationContext());
        newLocationBuilder.setView(nameLocationText);
        // capitalise first letter of the sentence
        nameLocationText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        newLocationBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String locationName = nameLocationText.getText().toString();
                if(locationName.isEmpty()) {
                    // overwrite the location name
                    locationName = "Location with no name!";
                }
                createNewMarker(locationName, position);
            }
        });
        newLocationBuilder.show();
    }

    private void createMarkerFromRoomDatabase(String locationName, LatLng position) {
        try {
            // this may throw an invalid location exception
            // if this is the case then non of the below code will execute
            String tidesData = getTidesDataForMarker(position);
            String ratingData = getRatingDataForMarker(position);
            MarkerOptions markerOption = new MarkerOptions().position(position).title(locationName).snippet(tidesData + ratingData);
            Marker marker = googleMap.addMarker(markerOption);
            // add marker to reference ArrayList
            refMapMarkers.add(marker);
        } catch (InvalidLocationException e) {
            // do nothing a this is a valid marker and should its position should not throw an exception
        }
    }
    private void createNewMarker(String locationName, LatLng position) {
        try {
            // this may throw an invalid location exception
            // if this is the case then non of the below code will execute
            String tidesData = getTidesDataForMarker(position);
            String ratingData = getRatingDataForMarker(position);
            MarkerOptions markerOption = new MarkerOptions().position(position).title(locationName).snippet(tidesData + ratingData);
            Marker marker = googleMap.addMarker(markerOption);
            // add marker to reference ArrayList
            refMapMarkers.add(marker);
            saveNewMarkerInFirestore(locationName, position);
            saveNewMarkerInRoomDatabase(locationName, position);
        } catch (InvalidLocationException e) {
            Toast.makeText(MapActivity.this, "Cannot place a marker on land!", Toast.LENGTH_SHORT).show();
        }
    }
    private void saveNewMarkerInRoomDatabase(String locationName, LatLng position) {
        // proceed to save a valid location in ROOM
        // snippet is left out intentionally, it needs to be defines because
        // of the interface but the snippet will be defined dynamically
        FishingLocation newFishingLocation = new FishingLocation(locationName, null, position, 0, 0, null);
        fishingLocationDao.addFishingLocation(newFishingLocation);
    }
    private void saveNewMarkerInFirestore(String locationName, LatLng position) {
        // proceed to save a valid location in firestore
        Map<String, Object> fishingLocationMap = new HashMap<>();
        fishingLocationMap.put("name", locationName);
        fishingLocationMap.put("latitude", position.latitude);
        fishingLocationMap.put("longitude", position.longitude);
        firebaseFirestore.collection("fishingLocations").add(fishingLocationMap);
    }
    private void synchronizeRoomDatabaseWithFirestore() {
        // if true then the database needs to be setup first
        CollectionReference fishingLocations = firebaseFirestore.collection("fishingLocations");
        fishingLocations.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for(DocumentSnapshot fishingLocation : queryDocumentSnapshots) {
                    String name = fishingLocation.getString("name");
                    double lat = fishingLocation.getDouble("latitude");
                    double lng = fishingLocation.getDouble("longitude");
                    fishingLocations.document(fishingLocation.getId()).collection("ratings").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            double overallRating = 0, userRating = 0;
                            int noOfRatings =  queryDocumentSnapshots.getDocuments().size();
                            ArrayList<Double> ratingsList = new ArrayList<>();
                            // loop through all ratings and accumulate the total rating
                            for(DocumentSnapshot rating : queryDocumentSnapshots) {
                                overallRating += rating.getDouble("rating");
                                // if the current user rated this location extract his rating
                                if(rating.get("userId").equals(userId)) {
                                    userRating = rating.getDouble("rating");
                                }
                                ratingsList.add(rating.getDouble("rating"));
                            }
                            // divide the total rating by the number of ratings to get the average
                            if(overallRating != 0) {
                                overallRating /= noOfRatings;
                            }
                            // update the room database
                            if(fishingLocationDao.findFishingLocationByLatLng(lat, lng) == null) {
                                fishingLocationDao.addFishingLocation(new FishingLocation(name, null, lat, lng, overallRating, userRating, ratingsList));
                            } else {
                                FishingLocation fishingLocationInRoom = fishingLocationDao.findFishingLocationByLatLng(lat, lng);
                                fishingLocationInRoom.setOverallRating(overallRating);
                                fishingLocationInRoom.setUserRating(userRating);
                                fishingLocationInRoom.setRatingsList(ratingsList);
                                fishingLocationDao.updateFishingLocation(fishingLocationInRoom);
                            }
                        }
                    });
                }
            }
        });
    }
}