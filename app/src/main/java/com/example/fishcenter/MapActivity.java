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
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, FishingLocationsCallback {
    private GoogleMap googleMap;
    private ImageButton goBackImageButton;
    private ImageButton logoutImageButton;
    private LinearLayout progressSpinnerLayout;
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private Toolbar toolbar;
    private SupportMapFragment mapFragment;
    private CustomFishingLocationInfoWindowAdapter adapter;
    private ImageView syncRoomWithFirestoreButton;
    private ArrayList<Marker> refMapMarkers = new ArrayList<>();
    private FishingLocation currentFishingLocation;
    private FishingLocationsController fishingLocationsController;
    private User currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        progressSpinnerLayout = findViewById(R.id.progressSpinnerLayout);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);

        currentUser = (User) getIntent().getExtras().getSerializable("currentUser");

        // need this as google maps overwrite the view and make the toolbar dis
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        fishingLocationsController = new FishingLocationsController(MapActivity.this, this);


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
                int numExistingLocations = refMapMarkers.size();
                fishingLocationsController.synchronizeRoomDatabaseWithFirestore(currentUser, numExistingLocations);
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
        fishingLocationsController.getFishingLocationsFromRoomDatabase();
        // snackBar to instruct the user how to use the map
        createMapInstructionsSnackBar();
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                LatLng markerPosition = marker.getPosition();
                fishingLocationsController.getCurrentFishingLocation(markerPosition);
                String tidesData;
                try {
                    tidesData = getTidesDataForMarker(markerPosition);
                } catch (InvalidLocationException e) {
                    // this should not get triggered as the location should already
                    // contain valid coordinates if a marker was created for it
                    throw new RuntimeException(e);
                }
                String ratingData = getRatingDataForCurrentlySelectedFishingLocation();
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
                double currentUserRating = currentFishingLocation.getUserRating();
                createReviewFishingLocationAlertDialog(currentUserRating);
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

    private void createMapInstructionsSnackBar() {
        View mapParentView = mapFragment.getView();
        int snackBarBackgroundColorId = ContextCompat.getColor(MapActivity.this, R.color.ordinaryButtonColor);
        int snackBarTextColorId = ContextCompat.getColor(MapActivity.this, R.color.white);
        final Snackbar instructionSnackBar = Snackbar.make(mapParentView, "Press on the map to create a marker", Snackbar.LENGTH_INDEFINITE);
        instructionSnackBar.setBackgroundTint(snackBarBackgroundColorId);
        instructionSnackBar.setTextColor(snackBarTextColorId);
        instructionSnackBar.setActionTextColor(snackBarTextColorId);
        instructionSnackBar.setAction("Close", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                instructionSnackBar.dismiss();
            }
        });
        instructionSnackBar.show();
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

    private String getRatingDataForCurrentlySelectedFishingLocation() {
        if(currentFishingLocation == null) {
            return "\nThis location is rated: 0.0 \n\nPress on the window to rate the location!\n";
        } else {
            return "\nThis location is rated: " + currentFishingLocation.getOverallRating() + "\n\nPress on the window to rate the location!\n";
        }
    }

    private void createReviewFishingLocationAlertDialog(double currentUserRating) {
        AlertDialog.Builder reviewLocationDialogBuilder = new AlertDialog.Builder(MapActivity.this);
        reviewLocationDialogBuilder.setTitle("Give a rating of this location!");
        // define the rating bar
        RatingBar ratingBar = new RatingBar(MapActivity.this);
        ratingBar.setNumStars(5);
        // linear layout to hold the rating bar that allows to define params to wrap content so rating bar does not overflow and stays at 5 stars
        LinearLayout ratingBarLinearLayout = new LinearLayout(MapActivity.this);
        LinearLayout.LayoutParams ratingBarLinearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ratingBarLinearLayoutParams.setMargins(0,15,0,0);
        // set the layout params to the rating bar
        ratingBar.setLayoutParams(ratingBarLinearLayoutParams);
        // put the rating bar inside of the linear layout center the layout and add it into the dialog box
        ratingBarLinearLayout.addView(ratingBar);
        ratingBarLinearLayout.setGravity(Gravity.CENTER);
        ratingBar.setRating((float) currentUserRating);
        reviewLocationDialogBuilder.setView(ratingBarLinearLayout);
        reviewLocationDialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                double newUserRating = ratingBar.getRating();
                // if the user has actually rated an location
                if(newUserRating != 0.0) {
                    fishingLocationsController.updateFishingLocationRatingInFirestore(currentFishingLocation, currentUser, newUserRating);
                    fishingLocationsController.updateFishingLocationRatingInRoomDatabase(currentFishingLocation, newUserRating);
                } else {
                    Toast.makeText(MapActivity.this, "Your rating was not saved! Please select a value!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        reviewLocationDialogBuilder.show();
    }

    private void updateMarkerOnMapWithNewOverallRating(Marker marker) {
        try {
            LatLng markerPosition = marker.getPosition();
            String tidesData = getTidesDataForMarker(markerPosition);
            String ratingData = getRatingDataForCurrentlySelectedFishingLocation();
            marker.setSnippet(tidesData + ratingData);
            adapter.setLocationName(marker.getTitle());
            adapter.setLocationData(marker.getSnippet());
            marker.showInfoWindow();
        } catch (InvalidLocationException e) {
            // should not be thrown as the marker should have an valid location at this point
            throw new RuntimeException(e);
        }
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
            String ratingData = getRatingDataForCurrentlySelectedFishingLocation();
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
            String ratingData = getRatingDataForCurrentlySelectedFishingLocation();
            MarkerOptions markerOption = new MarkerOptions().position(position).title(locationName).snippet(tidesData + ratingData);
            Marker marker = googleMap.addMarker(markerOption);
            // add marker to reference ArrayList
            refMapMarkers.add(marker);
            fishingLocationsController.saveNewMarkerInFirestore(locationName, position);
            fishingLocationsController.saveNewMarkerInRoomDatabase(locationName, position);
        } catch (InvalidLocationException e) {
            Toast.makeText(MapActivity.this, "Cannot place a marker on land!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void currentFishingLocationReady(FishingLocation currentFishingLocation) {
        this.currentFishingLocation = currentFishingLocation;
    }

    @Override
    public void fishingLocationUpdated(LatLng position) {
        Marker markerToUpdate = findMarkerByLatLng(position);
        // should not happen as the refMapMarkers list should contain this marker
        if (markerToUpdate != null) {
            updateMarkerOnMapWithNewOverallRating(markerToUpdate);
        }
    }

    @Override
    public void fishingLocationRatingUpdated(boolean previouslyUpdated, double newUserRating) {
        if(previouslyUpdated) {
            Toast.makeText(MapActivity.this, "We have updated your rating with " + newUserRating + "!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MapActivity.this, "We have received your rating of " + newUserRating + "!", Toast.LENGTH_SHORT).show();
        }
    }

    private Marker findMarkerByLatLng(LatLng position) {
        for(int i = 0; i < refMapMarkers.size(); i++) {
            if(refMapMarkers.get(i).getPosition().equals(position)){
                return refMapMarkers.get(i);
            }
        }
        return null;
    }

    @Override
    public void fishingLocationsReady(ArrayList<FishingLocation> fishingLocations) {
        for(FishingLocation fishingLocation : fishingLocations) {
            LatLng position = fishingLocation.getPosition();
            String locationName = fishingLocation.getLocationName();
            // only create markers which do not already exist
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(findMarkerByLatLng(position) == null) {
                        createMarkerFromRoomDatabase(locationName, position);
                    }
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

    @Override
    public void isSynchronisationNecessary(boolean isNecessary) {
        if(isNecessary) {
            showSpinnerAndDisableComponents(true);
        } else {
            Toast.makeText(MapActivity.this, "Your map is up to date!", Toast.LENGTH_SHORT).show();
        }
    }
}