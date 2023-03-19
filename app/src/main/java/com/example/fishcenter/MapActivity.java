package com.example.fishcenter;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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
    private CustomWeatherStationInfoWindowAdapter adapter;
    private ImageView syncRoomWithFirestoreButton;
    private ArrayList<Marker> refMapMarkers = new ArrayList<>();
    //private ImageView getWeatherStations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        progressSpinnerLayout = findViewById(R.id.progressSpinnerLayout);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
        //getWeatherStations = findViewById(R.id.getWeatherStations);

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
                firebaseAuth.signOut();
                Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(loginActivity);
                finish();
            }
        });

        syncRoomWithFirestoreButton = findViewById(R.id.syncDatabasesButton);
        syncRoomWithFirestoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setMakersVisible(false);
                syncRoomWithFirestore();
                setupMarkersFromRoom();
            }
        });

        /*
        getWeatherStations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OneOffSaveOfWeatherStationsToFirestore getWeatherStationsOnceOff = new OneOffSaveOfWeatherStationsToFirestore(this);
                getWeatherStationsOnceOff.start();
                System.out.println("Stations are getting downloaded");
            }
        }); */

    }

    private void showSpinnerAndDisableComponents(boolean flag) {
        syncRoomWithFirestoreButton.setClickable(!flag);
        logoutImageButton.setClickable(!flag);
        goBackImageButton.setClickable(!flag);
        if(flag) {
            progressSpinnerLayout.setVisibility(View.VISIBLE);
            syncRoomWithFirestoreButton.setBackground(null);
            goBackImageButton.setBackground(null);
            logoutImageButton.setBackground(null);
        } else {
            progressSpinnerLayout.setVisibility(View.INVISIBLE);
            syncRoomWithFirestoreButton.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_5_gray_opacity_25_to_transparent));
            goBackImageButton.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_5_gray_opacity_25_to_transparent));
            logoutImageButton.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_5_gray_opacity_25_to_transparent));
        }
    }


    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        adapter = new CustomWeatherStationInfoWindowAdapter(getApplicationContext());
        googleMap.setInfoWindowAdapter(adapter);
        // setup markers that are in the user ROOM database
        setupMarkersFromRoom();
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                adapter.setLocationName(marker.getTitle());
                adapter.setLocationData(marker.getSnippet());
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
                reviewLocation(marker.getPosition());
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

    public void setMakersVisible(boolean flag) {
        if(flag) {
            for(Marker marker : refMapMarkers) {
                marker.setVisible(true);
            }
        } else {
            for(Marker marker : refMapMarkers) {
                marker.setVisible(false);
            }
        }
    }

    public void setupMarkersFromRoom() {
        showSpinnerAndDisableComponents(true);
        new Thread() {
            @Override
            public void run() {
                super.run();
                ArrayList<MarkerOptions> markers = new ArrayList<>();
                for(FishingLocation fishingLocation : fishingLocationDao.getAllFishingLocations()) {
                    try {
                        // launch a thread to get fishing location data an launch it
                        OpenMeteoGetTidesData getTideData = new OpenMeteoGetTidesData(MapActivity.this, fishingLocation.lat, fishingLocation.lng);
                        getTideData.start();
                        // wait for thread to finish and get the data fetched
                        getTideData.join();
                        String data = getTideData.getTidesData();
                        data += "\nThis location is rated: " + fishingLocation.overallRating + "\n\nPress on the window to rate the location!\n";
                        markers.add(new MarkerOptions().title(fishingLocation.title).snippet(data).position(fishingLocation.getPosition()));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                createNewMarkersFromRoom(markers);
            }
        }.start();
    }

    private void createNewMarkersFromRoom(ArrayList<MarkerOptions> markerOptions) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // update the ui and hide the spinner
                for(int i = 0; i < markerOptions.size(); i++) {
                    MarkerOptions markerToAdd = markerOptions.get(i);
                    // if there is no marker with this position add the marker to the collection as this is a new marker
                    // fetched after the database sync and it needs to be added into the list of markers that are displayed
                    if(refMapMarkers.stream().noneMatch(marker -> marker.getPosition().equals(markerToAdd.getPosition()))) {
                        Marker marker = googleMap.addMarker(markerOptions.get(i));
                        refMapMarkers.add(marker);
                    }
                }
                showSpinnerAndDisableComponents(false);
                setMakersVisible(true);
            }
        });
    }
    

    private void reviewLocation(LatLng position) {
        AlertDialog.Builder reviewLocationDialogBuilder = new AlertDialog.Builder(MapActivity.this);
        reviewLocationDialogBuilder.setTitle("Give a rating of this location!");
        // define the rating bar
        RatingBar ratingBar = new RatingBar(getApplicationContext());
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
        double fishLocRating = fishingLocationDao.findFishingLocationByLatLng(position.latitude, position.longitude).getUserRating();
        ratingBar.setRating((float) fishLocRating);
        reviewLocationDialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                float newUserRating = ratingBar.getRating();
                // if the user has actually rated an location
                if(newUserRating != 0.0) {
                    updateRatingInFirestore(position, newUserRating);
                    updateRatingInRoom(position, newUserRating);
                } else {
                    Toast.makeText(MapActivity.this, "Your rating was not saved! Please select a value!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        reviewLocationDialogBuilder.show();
    }


    private void updateRatingInRoom(LatLng position, float newUserRating) {
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
            ratingsList.add((double) newUserRating);
        } else {
            // if the user rated the location before replace the rating
            ratingsList.remove(currentUserRating);
            ratingsList.add((double) newUserRating);
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
        updateMarkerOnTheMap(position);
    }

    private void updateRatingInFirestore(LatLng position, float newUserRating){
        // call to firebase to save the rating in the backend
        CollectionReference fishingLocations = firebaseFirestore.collection("fishingLocations");
        // find the fishing location based on latitude and longitude
        fishingLocations.whereEqualTo("latitude", position.latitude).whereEqualTo("longitude", position.longitude).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                String fishingLocationId = queryDocumentSnapshots.getDocuments().get(0).getId();
                fishingLocations.document(fishingLocationId).collection("ratings").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        Map<String, Object> ratingMap = new HashMap<>();
                        ratingMap.put("userId", userId);
                        ratingMap.put("rating", newUserRating);
                        // use the stream filter to find if the user has already liked this post and collect the document reference into a list of document snapshots
                        List<DocumentSnapshot> ratings = queryDocumentSnapshots.getDocuments().stream().filter(rating -> rating.get("userId").equals(userId)).collect(Collectors.toList());
                        // if the entry for this user exists update it
                        if(ratings.size() != 0) {
                            // there should only be one entry in the list per user
                            String ratingRef = ratings.get(0).getId();
                            fishingLocations.document(fishingLocationId).collection("ratings").document(ratingRef).update(ratingMap);
                            Toast.makeText(MapActivity.this, "We have updated your rating with " + newUserRating + "!", Toast.LENGTH_SHORT).show();
                        } else {
                            // the user did not like this location yet so create a new entry
                            fishingLocations.document(fishingLocationId).collection("ratings").add(ratingMap);
                            Toast.makeText(MapActivity.this, "We have received your rating of " + newUserRating + "!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void updateMarkerOnTheMap(LatLng position) {
        // get the reference to the marker in the list of markers
        List<Marker> markerList = refMapMarkers.stream().filter(markerOptions -> markerOptions.getPosition().equals(position)).collect(Collectors.toList());
        Marker markerToUpdate = markerList.get(0);
        // load the marker entry from ROOM
        FishingLocation fishingLocationOfMarker = fishingLocationDao.findFishingLocationByLatLng(position.latitude, position.longitude);
        OpenMeteoGetTidesData tidesDataThread = new OpenMeteoGetTidesData(getApplicationContext(), position.latitude, position.longitude);
        tidesDataThread.start();
        try {
            tidesDataThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        String data = tidesDataThread.getTidesData();
        data += "\nThis location is rated: " + fishingLocationOfMarker.getOverallRating() + "\n\nPress on the window to rate the location!\n";
        markerToUpdate.setSnippet(data);
        adapter.setLocationName(markerToUpdate.getTitle());
        adapter.setLocationData(markerToUpdate.getSnippet());
        markerToUpdate.showInfoWindow();
    }

    private void createNewFishingLocationAlertDialog(LatLng position) {
        AlertDialog.Builder newLocationBuilder = new AlertDialog.Builder(MapActivity.this);
        newLocationBuilder.setTitle("How would you like to call this location?");
        EditText nameLocationText = new EditText(getApplicationContext());
        newLocationBuilder.setView(nameLocationText);
        newLocationBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // start a thread and wait for it to finish
                OpenMeteoGetTidesData tidesDataThread = new OpenMeteoGetTidesData(getApplicationContext(), position.latitude, position.longitude);
                tidesDataThread.start();
                try {
                    tidesDataThread.join();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                // if the marker is not on land then save it in firestore
                if(tidesDataThread.getResponseCode() == 200) {
                    String locationName = nameLocationText.getText().toString();
                    if(locationName.isEmpty()) {
                        // overwrite the location name
                        locationName = "Location with no name!";
                    }
                    String data = tidesDataThread.getTidesData();
                    data += "\nThis location is rated: 0.0\n\nPress on the window to rate the location!\n";
                    addMarkerToList(locationName, position, data);
                    saveNewMarkerInFirestore(locationName, position);
                    saveNewMarkerInRoomDatabase(locationName, position);
                } else {
                    Toast.makeText(MapActivity.this, "Can't place a marker on land!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        newLocationBuilder.show();
    }

    private void addMarkerToList(String locationName, LatLng position, String data) {
        MarkerOptions markerOption = new MarkerOptions().position(position).title(locationName).snippet(data);
        Marker marker = googleMap.addMarker(markerOption);
        // add marker to reference ArrayList
        refMapMarkers.add(marker);
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

    private void syncRoomWithFirestore() {
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
                            double overallRating = 0;
                            int noOfRatings =  queryDocumentSnapshots.getDocuments().size();
                            double userRating = 0;
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
                                FishingLocation fishingLocationROOM = fishingLocationDao.findFishingLocationByLatLng(lat, lng);
                                fishingLocationROOM.setOverallRating(overallRating);
                                fishingLocationROOM.setUserRating(userRating);
                                fishingLocationROOM.setRatingsList(ratingsList);
                                fishingLocationDao.updateFishingLocation(fishingLocationROOM);
                            }
                        }
                    });
                }
            }
        });
    }
}
