package com.example.fishcenter;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class FishScoreController {

    private final FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();

    public FishScoreController(){}

    public void createFishWithRandomScore(Fish fish) {
        new MyThread(fish).start();
    }

    private class MyThread extends Thread {
        private final Fish fish;

        public MyThread(Fish fish) {
            this.fish = fish;
        }

        @Override
        public void run() {
            super.run();
            String fishLatinName = fish.getLatinName();
            CollectionReference fishDocument = firebaseFirestore.collection("fishes");
            fishDocument.whereEqualTo("latinName", fishLatinName).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    // only if the fish does not already exist save it in firestore
                    if(task.getResult().isEmpty()) {
                        HashMap<String, Object> fishMap = new HashMap<>();
                        fishMap.put("latinName", fish.getLatinName());
                        fishMap.put("predictionAccuracy", fish.getPredictionAccuracy());
                        fishMap.put("fishName", fish.getFishName());
                        fishMap.put("mediaUri", fish.getMediaUri());
                        fishMap.put("commonNames", new ArrayList<>(Arrays.asList(fish.getCommonNames())));
                        fishMap.put("distribution", fish.getDistribution());
                        fishMap.put("scales", fish.hasScales());
                        fishMap.put("saltWater", fish.isSaltWater());
                        fishMap.put("freshWater", fish.isFreshWater());
                        fishMap.put("coloration", fish.getColoration());
                        fishMap.put("feedingBehaviour", fish.getFeedingBehaviour());
                        fishMap.put("healthWarnings", fish.getHealthWarnings());
                        fishMap.put("foodValue", fish.getFoodValue());
                        fishMap.put("similarSpecies", new ArrayList<>(Arrays.asList(fish.getSimilarSpecies())));
                        fishMap.put("environmentDetail", fish.getEnvironmentDetail());
                        int randomScore = new Random().nextInt(100);
                        fishMap.put("randomScore", randomScore);
                        fishDocument.add(fishMap);
                    }
                }
            });
        }
    }
}
