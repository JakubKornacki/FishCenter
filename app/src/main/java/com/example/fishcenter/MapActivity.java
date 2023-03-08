package com.example.fishcenter;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.room.Room;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.clustering.ClusterManager;

import java.util.HashMap;
import java.util.Map;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private ImageButton goBackImageButton;
    private ImageButton logoutImageButton;

    private LinearLayout linearLayoutIndeterminateProgressBarToMaps;
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private Toolbar toolbar;
    //private ImageView getWeatherStations;
    private FishingLocationDatabase fishingLocationDatabase;
    private ClusterManager<FishingLocation> fishingLocationClusterManager;
    private FishingLocationDao fishingLocationDao;
   // private HashMap<LatLng, FishingLocation> referenceHashMap = new HashMap<>();
    private boolean syncComplete = false;
    private SupportMapFragment mapFragment;
    private CustomWeatherStationInfoWindowAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        goBackImageButton = findViewById(R.id.goBackImageButton);
        logoutImageButton = findViewById(R.id.logoutImageButton);
        linearLayoutIndeterminateProgressBarToMaps = findViewById(R.id.linearLayoutIndeterminateProgressBarToMaps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
        //getWeatherStations = findViewById(R.id.getWeatherStations);
        // need this as google maps overwrite the view and make the toolbar dis
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        fishingLocationDatabase = Room.databaseBuilder(getApplicationContext(), FishingLocationDatabase.class, "fishLocationDatabase") .fallbackToDestructiveMigration().allowMainThreadQueries().build();
        fishingLocationDao =  fishingLocationDatabase.fishingLocationDao();
        syncDatabases();
        goBackImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        logoutImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseAuth.signOut();
                Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(loginActivity);
                finish();
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


    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        adapter = new CustomWeatherStationInfoWindowAdapter(getApplicationContext());
       // fishingLocationClusterManager = new ClusterManager<>(getApplicationContext(), googleMap);
        googleMap.setInfoWindowAdapter(adapter);
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                adapter.setLocationName(marker.getTitle());
                adapter.setLocationData(marker.getSnippet());
                marker.showInfoWindow();
                return true;
            }
        });
        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng latLng) {
                setupLocation(latLng);
            }
        });

        /*
        weatherStationClusterManager = new ClusterManager<>(getApplicationContext(), googleMap);
        googleMap.setOnCameraIdleListener(weatherStationClusterManager);
        googleMap.setOnMarkerClickListener(weatherStationClusterManager);
        // need to set a new info window adapter for the collection of markers that are found under it
        // the CustomWeatherStationInfoWindowAdapter will inflate the layout layout_mapmarker.xml and
        // set it as the default info window to be displayed when the marker is clicked
      //  weatherStationClusterManager.getMarkerCollection().setInfoWindowAdapter(new CustomWeatherStationInfoWindowAdapter(getApplicationContext()));
        weatherStationClusterManager.getMarkerCollection().setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(@NonNull Marker marker) {
                if(marker.isInfoWindowShown()) {
                    marker.hideInfoWindow();
                } else {
                    marker.showInfoWindow();
                }
            }
        });

        weatherStationClusterManager.getMarkerCollection().setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                double lat = marker.getPosition().latitude;
                double lng = marker.getPosition().longitude;
                //JSONObject tideExtremesData = fetchWorldTidesTidesData(lat,lng);
                JSONObject tideExtremesData = null;
                updateMarkerAndRedraw(marker, tideExtremesData);
                return false;
            }
        });


        waitForMapReady(); */
    }

    private void setupLocation(LatLng position) {
        // start a thread and wait for it to finish
        OpenMeteoTidesData tidesDataThread = new OpenMeteoTidesData(getApplicationContext(), position.latitude, position.longitude);
        tidesDataThread.start();
        try {
            tidesDataThread.join();
            String data = tidesDataThread.getTidesData();
            AlertDialog.Builder locationNameDialogBuilder = new AlertDialog.Builder(MapActivity.this);
            locationNameDialogBuilder.setTitle("How would you like to call this location?");
            EditText nameLocationText = new EditText(getApplicationContext());
            locationNameDialogBuilder.setView(nameLocationText);
            locationNameDialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String userInput = nameLocationText.getText().toString();
                    if(userInput.length() == 0 ) {
                        // overwrite the location name
                        userInput = "Location with no name!";
                        googleMap.addMarker(new MarkerOptions().position(position).title(userInput).snippet(data));
                    } else {
                        googleMap.addMarker(new MarkerOptions().position(position).title(userInput).snippet(data));
                    }
                    if(!data.equals("No data is available for this location!")) {
                       // proceed to save a valid location in firestore
                        Map<String, Object> fishingLocationMap = new HashMap<>();
                        fishingLocationMap.put("name", userInput);
                        fishingLocationMap.put("latitude", position.latitude);
                        fishingLocationMap.put("longitude", position.longitude);
                        firebaseFirestore.collection("fishingLocations").add(fishingLocationMap);

                        // proceed to save a valid location in ROOM
                        // snippet is left out intentionally, it needs to be defines because
                        // of the interface but the snippet will be defined dynamically
                        FishingLocation newFishingLocation =  new FishingLocation(userInput, null, position);
                        fishingLocationDao.addFishingLocation(newFishingLocation);
                        fishingLocationClusterManager.addItem(newFishingLocation);
                        fishingLocationClusterManager.cluster();
                    }
                }
            });
            locationNameDialogBuilder.show();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void syncDatabases(){
        // if true then the database needs to be setup first
        waitForSyncComplete();
        firebaseFirestore.collection("fishingLocations").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for(DocumentSnapshot fishingLocation : queryDocumentSnapshots) {
                    String name = fishingLocation.getString("name");
                    double lat = fishingLocation.getDouble("latitude");
                    double lng = fishingLocation.getDouble("longitude");
                    if(fishingLocationDao.findWeatherStationByLatLng(lat, lng) != null) {
                        fishingLocationDao.addFishingLocation(new FishingLocation(name, null, lat, lng));
                    }
                }
                syncComplete = true;
            }
        });
    }



    private void setupExistingMarkers() {
        for(FishingLocation fishingLocation : fishingLocationDao.getAllFishingLocations()) {
            try {
                // launch a thread to get fishing location data an launch it
                OpenMeteoTidesData getTideData = new OpenMeteoTidesData(MapActivity.this, fishingLocation.lat, fishingLocation.lng);
                getTideData.start();
                // wait for thread to finish and get the data fetched
                getTideData.join();
                String data = getTideData.getTidesData();
                // need to run the UI for creating markers and assigning them to cluster supdate on the main thread

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        googleMap.addMarker(new MarkerOptions().title(fishingLocation.title).snippet(data).position(fishingLocation.getPosition()));
                       // FishingLocation newFishingLocation =  new FishingLocation(fishingLocation.title, fishingLocation.snippet, fishingLocation.getPosition());
                        //fishingLocationClusterManager.addItem(newFishingLocation);
                    }
                });
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            //fishingLocationClusterManager.cluster();
        }

        //fishingLocationClusterManager.cluster();

    }
    private void waitForSyncComplete () {
        new Thread() {
            @Override
            public void run() {
                super.run();
                while(!syncComplete) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                // setup the markers that are in the ROOM database after the sync with firebase
                setupExistingMarkers();
                // hide the spinner
                linearLayoutIndeterminateProgressBarToMaps.post(new Runnable() {
                    @Override
                    public void run() {
                        // hide the spinner and force a refresh of the cluster manager
                        linearLayoutIndeterminateProgressBarToMaps.setVisibility(View.GONE);
                    }
                });
            }
        }.start();
    }

}
