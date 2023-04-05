package com.example.fishcenter;

import static java.lang.Thread.currentThread;

import android.content.Context;

import androidx.room.Room;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class FishingLocationsController {

    private final FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private FishingLocationDatabase fishingLocationDatabase;
    private FishingLocationDao fishingLocationDao;
    private Context context;
    private FishingLocationsCallback fishingLocationsCallback;

    public FishingLocationsController(Context context, FishingLocationsCallback fishingLocationsCallback) {
        this.context = context;
        this.fishingLocationsCallback = fishingLocationsCallback;
        fishingLocationDatabase = Room.databaseBuilder(context, FishingLocationDatabase.class, "fishLocationDatabase").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        fishingLocationDao =  fishingLocationDatabase.fishingLocationDao();
    }

    public void getCurrentFishingLocation(LatLng locationPosition) {
     FishingLocation currentFishingLocation = fishingLocationDao.findFishingLocationByLatLng(locationPosition.latitude, locationPosition.longitude);
     fishingLocationsCallback.currentFishingLocationReady(currentFishingLocation);
    }

    public void updateFishingLocationRatingInRoomDatabase(FishingLocation fishingLocation, double newUserRating) {
        // save the entry in ROOM and update the corresponding object
        double overallRating = 0, averageRating;
        double currentUserRating = fishingLocation.getUserRating();
        ArrayList<Double> ratings = fishingLocation.getRatingsList();
        if(ratings == null) {
            ratings = new ArrayList<>();
        }
        // if the user has not rated the location before add the new rating
        if(currentUserRating == 0) {
            ratings.add(newUserRating);
        } else {
            // if the user rated the location before replace the rating
            ratings.remove(currentUserRating);
            ratings.add(newUserRating);
        }
        // recalculate the total rating
        for(int j = 0; j < ratings.size(); j++) {
            overallRating += ratings.get(j);
        }
        // get the average rating
        averageRating = overallRating / ratings.size();
        // update the entry in the ROOM database
        fishingLocation.setUserRating(newUserRating);
        fishingLocation.setOverallRating(averageRating);
        fishingLocationDao.updateFishingLocation(fishingLocation);
        // return the position of the updated location back to the map page
        LatLng position = fishingLocation.getPosition();
        fishingLocationsCallback.fishingLocationUpdated(position);
    }


    public void saveNewMarkerInFirestore(String locationName, LatLng position) {
        // proceed to save a valid location in firestore
        Map<String, Object> fishingLocationMap = new HashMap<>();
        fishingLocationMap.put("name", locationName);
        fishingLocationMap.put("latitude", position.latitude);
        fishingLocationMap.put("longitude", position.longitude);
        firebaseFirestore.collection("fishingLocations").add(fishingLocationMap);
    }

    public void saveNewMarkerInRoomDatabase(String locationName, LatLng position) {
        // proceed to save a valid location in Room
        // snippet is left out intentionally, it needs to be defines because
        // of the interface but the snippet will be defined dynamically
        FishingLocation newFishingLocation = new FishingLocation(locationName, position, 0, 0, null);
        fishingLocationDao.addFishingLocation(newFishingLocation);
    }


    public void updateFishingLocationRatingInFirestore(FishingLocation currentFishingLocation, User currentUser, double newUserRating){
        double latitude = currentFishingLocation.getPosition().latitude;
        double longitude = currentFishingLocation.getPosition().longitude;
        String userId = currentUser.getUserUid();
        CollectionReference fishingLocations = firebaseFirestore.collection("fishingLocations");
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
                        boolean previouslyUpdated;
                        if(userRating != null) {
                            // there should only be one entry in the list per user
                            String ratingFirestoreRef = userRating.getId();
                            fishingLocations.document(fishingLocationFirestoreRef).collection("ratings").document(ratingFirestoreRef).update(ratingMap);
                            previouslyUpdated = true;
                        } else {
                            // the user did not like this location yet so create a new entry
                            fishingLocations.document(fishingLocationFirestoreRef).collection("ratings").add(ratingMap);
                            previouslyUpdated = false;
                        }
                        fishingLocationsCallback.fishingLocationRatingUpdated(previouslyUpdated, newUserRating);
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

    public void synchronizeRoomDatabaseWithFirestore(User currentUser, int numLocationsExisting) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                System.out.println("Thread in synchronizeRoomDatabaseWithFirestore" + Thread.currentThread());

                String userId = currentUser.getUserUid();
                // if true then the database needs to be setup first
                CollectionReference fishingLocations = firebaseFirestore.collection("fishingLocations");
                fishingLocations.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        ArrayList<FishingLocation> loadedFishingLocations = new ArrayList<>();
                        int numLocationsToLoad = queryDocumentSnapshots.size();
                        // check if sync is necessary
                        if(numLocationsToLoad == numLocationsExisting) {
                            fishingLocationsCallback.isSynchronisationNecessary(false);
                            return;
                        } else {
                            fishingLocationsCallback.isSynchronisationNecessary(true);
                        }
                        for(DocumentSnapshot fishingLocation : queryDocumentSnapshots) {
                            String locationName = fishingLocation.getString("name");
                            double lat = fishingLocation.getDouble("latitude");
                            double lng = fishingLocation.getDouble("longitude");
                            fishingLocations.document(fishingLocation.getId()).collection("ratings").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    double overallRating = 0, userRating = 0, averageRating = 0;
                                    int noOfRatings =  queryDocumentSnapshots.getDocuments().size();
                                    ArrayList<Double> ratings = new ArrayList<>();
                                    // loop through all ratings and accumulate the total rating
                                    for(DocumentSnapshot ratingFromFirestore : queryDocumentSnapshots) {
                                        overallRating += ratingFromFirestore.getDouble("rating");
                                        // if the current user rated this location extract his rating
                                        if(ratingFromFirestore.get("userId").equals(userId)) {
                                            userRating = ratingFromFirestore.getDouble("rating");
                                        }
                                        ratings.add(ratingFromFirestore.getDouble("rating"));
                                    }
                                    // divide the total rating by the number of ratings to get the average
                                    if(overallRating != 0) {
                                        averageRating = overallRating / noOfRatings;
                                    }
                                    loadedFishingLocations.add(new FishingLocation(locationName, new LatLng(lat, lng), averageRating, userRating, ratings));
                                    // update Room database on a separate thread
                                    if(loadedFishingLocations.size() == numLocationsToLoad) {
                                        synchronizeRoomDatabaseDuringSync(loadedFishingLocations);
                                    }
                                }
                            });
                        }
                    }
                });
            }
        }.start();
    }

    private void synchronizeRoomDatabaseDuringSync(ArrayList<FishingLocation> fishingLocations) {
        for(FishingLocation fishingLocation : fishingLocations) {
            double lat  = fishingLocation.getPosition().latitude;
            double lng = fishingLocation.getPosition().longitude;
            if(fishingLocationDao.findFishingLocationByLatLng(lat, lng) == null) {
                // if this location does not exist in the database add it
                fishingLocationDao.addFishingLocation(fishingLocation);
            } else {
                // otherwise update it
                FishingLocation fishingLocationInRoom = fishingLocationDao.findFishingLocationByLatLng(lat, lng);
                fishingLocationInRoom.setOverallRating(fishingLocation.getOverallRating());
                fishingLocationInRoom.setUserRating(fishingLocation.getUserRating());
                fishingLocationInRoom.setRatingsList(fishingLocation.getRatingsList());
                fishingLocationDao.updateFishingLocation(fishingLocationInRoom);
            }
        }
        // return updated list to the map page
        getFishingLocationsFromRoomDatabase();
    }

    public void getFishingLocationsFromRoomDatabase() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                ArrayList<FishingLocation> fishingLocations = convertFishingLocationListToArrayList(fishingLocationDao.getAllFishingLocations());
                fishingLocationsCallback.fishingLocationsReady(fishingLocations);
            }
        }.start();
    }

    private ArrayList<FishingLocation> convertFishingLocationListToArrayList(List<FishingLocation> fishingLocationList) {
        return new ArrayList<>(fishingLocationList);
    }


}
