package com.example.fishcenter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class FishRecognisedActivity extends AppCompatActivity {
    private HashSet<Fish> fishes;
    private LinearLayout linearLayoutInsideScrollView;
    private LinearLayout backToMainMenuButtonLayout;
    private ImageButton goBackImageButton;
    private ImageButton logoutImageButton;
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fish_recognised);
        goBackImageButton = findViewById(R.id.goBackImageButton);
        logoutImageButton = findViewById(R.id.logoutImageButton);
        goBackImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        logoutImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createLogoutDialog(firebaseAuth);
            }
        });

        // way of passing objects between activities using Intent with the help of serializable interface and Bundle
        // more references in the FishialAPIFetchFishData.java class
        // https://stackoverflow.com/questions/13601883/how-to-pass-arraylist-of-objects-from-one-to-another-activity-using-intent-in-an
        Bundle packagedBundle = getIntent().getBundleExtra("bundle");
        // extract fishes ArrayList out from the bundle
        fishes = (HashSet<Fish>) packagedBundle.getSerializable("fishes");
        linearLayoutInsideScrollView = findViewById(R.id.linearLayoutInsideScrollView);

        // iterate through the returned fishes and append non-duplicates to linear layout
        Iterator<Fish> fishesIterator = fishes.iterator();
        while(fishesIterator.hasNext()) {
            Fish fish = fishesIterator.next();
            LinearLayout table = drawResultsTable(fish);
            linearLayoutInsideScrollView.addView(table);
        }

        // iterate through the returned fishes and append non-duplicates to linear layout
        backToMainMenuButtonLayout = createBackToMainMenuLayout(linearLayoutInsideScrollView);
        linearLayoutInsideScrollView.addView(backToMainMenuButtonLayout);

        backToMainMenuButtonLayout.getChildAt(0).setOnClickListener(view ->  {
            Intent mainMenuActivity = new Intent(getApplicationContext(), MainPageActivity.class);
            // if a main activity already exists in the activity stack then bring back that activity
            // with all its existing data to the user and clear also remove all activities from the stack
            // between the main activity and this activity inclusive, with the activity between being the
            // fish recognition activity this avoids reloading of the main activity to fetch new posts
            // need to chain flags like this since each call to set flags overwrites the previous flag
            mainMenuActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(mainMenuActivity);
        });
    }

    public void createLogoutDialog(FirebaseAuth firebaseAuthInstance) {
        AlertDialog.Builder logoutDialog = new AlertDialog.Builder(FishRecognisedActivity.this);
        logoutDialog.setMessage("Are you sure you want to sign out?");
        logoutDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                firebaseAuthInstance.signOut();
                Intent goBackToLogin = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(goBackToLogin);
                finish();
            }
        });

        logoutDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        logoutDialog.show();
    }

    private LinearLayout createBackToMainMenuLayout(View parentView) {
        Context con = getApplicationContext();
        LinearLayout linearLayout = new LinearLayout(con);
        linearLayout.setLayoutParams(parentView.getLayoutParams());
        linearLayout.setBackgroundColor(getColor(R.color.ordinaryButtonColor));
        Button backToMainMenuButton = new Button(con);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        backToMainMenuButton.setLayoutParams(layoutParams);
        // https://stackoverflow.com/a/32202256 getColor() method is deprecated as of API 23
        // another way to get color from resources is to use ContextCompat.getColor(context, R.color.color_name)
        // by passing in the application context along with the resource id of the wanted colour
        backToMainMenuButton.setBackground(getDrawable(R.drawable.background_rounded_corners_toggle_5_gray_opacity_30_to_transparent));
        backToMainMenuButton.setMinHeight(48);
        backToMainMenuButton.setText(getText(R.string.backToMainMenu));
        backToMainMenuButton.setTextColor(getColor(R.color.white));
        backToMainMenuButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        linearLayout.addView(backToMainMenuButton);
        return linearLayout;
    }


    // the below method creates a tables that is inserted into a scroll view dynamically which is inserted into a linear layout
    // it also uses predefined strings with HTML markup which are converted into SpannableString objects
    // and inserted into the table row along with the fish data
    // https://developer.android.com/guide/topics/resources/string-resource
    private LinearLayout drawResultsTable(Fish fish) {
        // reference data
        Context con = getApplicationContext();
        // child should inherits its parent layout params
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        TableLayout.LayoutParams tabLayoutParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
        TableRow.LayoutParams tableRowParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1);

        // linear layout to store the table
        LinearLayout linearLayout = new LinearLayout(con);
        linearLayout.setLayoutParams(linearLayoutInsideScrollView.getLayoutParams());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setBackground(getDrawable(R.drawable.rounded_corners_white_15_opacity_55));
        // create the tablelayout
        TableLayout tableLayout = new TableLayout(con);
        tableLayout.setLayoutParams(linearLayoutParams);
        linearLayout.addView(tableLayout);

        // create the header row
        TableRow headerRow = new TableRow(con);
        headerRow.setLayoutParams(tabLayoutParams);
        headerRow.setPadding(0,20,0,20);
        tableLayout.addView(headerRow);
        // create the fish name text view
        TextView headerText = new TextView(con);
        headerText.setLayoutParams(tableRowParams);
        headerText.setText(fish.getFishName());
        headerText.setGravity(View.TEXT_ALIGNMENT_GRAVITY);
        headerText.setTextColor(ContextCompat.getColor(con, R.color.black));
        headerText.setTextSize(26);
        headerRow.addView(headerText);

        // create the image row
        TableRow imageRow = new TableRow(con);
        imageRow.setLayoutParams(tabLayoutParams);
        imageRow.setGravity(Gravity.CENTER);
        imageRow.setPadding(0,5,0,5);
        tableLayout.addView(imageRow);
        // create the image view with the fish image
        ImageView imageView = new ImageView(con);
        imageView.setLayoutParams(tableRowParams);
        // https://stackoverflow.com/questions/3681714/bad-bitmap-error-when-setting-uri
        // use Glide to load the image into the image view with its uri and set the image rounded corners to
        // https://bumptech.github.io/glide/doc/transformations.html
        // have radius of 30 pixels use the Glide override method to keep the image its original size as otherwise it is displayed stretched
        Glide.with(con).load(fish.getMediaUri()).transform(new RoundedCorners(30)).override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).into(imageView);
       // imageView.setBackground(getDrawable(R.drawable.image_round_corners_10));
        imageView.setClipToOutline(true);
        imageRow.addView(imageView);

        // create the latin name row
        TableRow latinNameRow = new TableRow(con);
        latinNameRow.setLayoutParams(tabLayoutParams);
        latinNameRow.setPadding(20,10,0,15);
        tableLayout.addView(latinNameRow);
        // create the latin name text view
        TextView latinNameText = new TextView(con);
        latinNameText.setLayoutParams(tableRowParams);
        SpannableStringBuilder latinSpanString = createSpannableString(R.string.latinName, fish.getLatinName());
        latinNameText.setText(latinSpanString);
        latinNameText.setTextColor(ContextCompat.getColor(con, R.color.black));
        latinNameText.setTextSize(14);
        latinNameRow.addView(latinNameText);

        // create the accuracy row
        TableRow accuracyRow = new TableRow(con);
        accuracyRow.setLayoutParams(tabLayoutParams);
        accuracyRow.setPadding(20,10,0,15);
        tableLayout.addView(accuracyRow);
        // create the accuracy text view
        TextView accuracyText = new TextView(con);
        accuracyText.setLayoutParams(tableRowParams);
        SpannableStringBuilder predAccurSpanString = createSpannableString(R.string.predictionAccuracy, predAccurString(fish.getPredictionAccuracy()));
        accuracyText.setText(predAccurSpanString);
        accuracyText.setTextColor(ContextCompat.getColor(con, R.color.black));
        accuracyText.setTextSize(14);
        accuracyRow.addView(accuracyText);

        // create the common names row
        TableRow commonNamesRow = new TableRow(con);
        commonNamesRow.setLayoutParams(tabLayoutParams);
        commonNamesRow.setPadding(20,10,0,15);
        tableLayout.addView(commonNamesRow);
        // create the common names text view
        TextView commonNamesText = new TextView(con);
        commonNamesText.setLayoutParams(tableRowParams);
        SpannableStringBuilder commonNamesSpanString = createSpannableString(R.string.commonNames, fish.getCommonNames());
        commonNamesText.setText(commonNamesSpanString);
        commonNamesText.setTextColor(ContextCompat.getColor(con, R.color.black));
        commonNamesText.setTextSize(14);
        commonNamesRow.addView(commonNamesText);

        // create the distribution row
        TableRow distributionRow = new TableRow(con);
        distributionRow.setLayoutParams(tabLayoutParams);
        distributionRow.setPadding(20,10,0,15);
        tableLayout.addView(distributionRow);
        // create the distribution text view
        TextView distributionText = new TextView(con);
        distributionText.setLayoutParams(tableRowParams);
        SpannableStringBuilder distrSpanString = createSpannableString(R.string.distribution, fish.getDistribution());
        distributionText.setText(distrSpanString);
        distributionText.setTextSize(14);
        distributionText.setTextColor(ContextCompat.getColor(con, R.color.black));
        distributionRow.addView(distributionText);

        // create the scales row
        TableRow scalesRow = new TableRow(con);
        scalesRow.setLayoutParams(tabLayoutParams);
        scalesRow.setPadding(20,10,0,15);
        tableLayout.addView(scalesRow);
        // create the scales text view
        TextView scalesText = new TextView(con);
        scalesText.setLayoutParams(tableRowParams);
        Spanned scalesSpanString = HtmlCompat.fromHtml(getString(R.string.scales), HtmlCompat.FROM_HTML_MODE_LEGACY);
        scalesText.setText(scalesSpanString);
        scalesText.setTextSize(14);
        scalesText.setTextColor(ContextCompat.getColor(con, R.color.black));
        scalesRow.addView(scalesText);
        // create the scales checkbox
        CheckBox scalesCheckBox = getCheckBox(con, fish.hasScales());
        scalesRow.addView(scalesCheckBox);

        // create the saltwater row
        TableRow saltWater = new TableRow(con);
        saltWater.setLayoutParams(tabLayoutParams);
        saltWater.setPadding(20,10,0,15);
        tableLayout.addView(saltWater);
        // create the salt water text view
        TextView saltWaterText = new TextView(con);
        saltWaterText.setLayoutParams(tableRowParams);
        Spanned saltWaterSpanString = HtmlCompat.fromHtml(getString(R.string.saltWater), HtmlCompat.FROM_HTML_MODE_LEGACY);
        saltWaterText.setText(saltWaterSpanString);
        saltWaterText.setTextSize(14);
        saltWaterText.setTextColor(ContextCompat.getColor(con, R.color.black));
        saltWater.addView(saltWaterText);
        // create the salt water checkbox
        CheckBox saltWaterCheckBox = getCheckBox(con, fish.isSaltWater());
        saltWater.addView(saltWaterCheckBox);

        // create the freshwater row
        TableRow freshWater = new TableRow(con);
        freshWater.setLayoutParams(tabLayoutParams);
        freshWater.setPadding(20,10,0,15);
        tableLayout.addView(freshWater);
        // create the fresh water text view
        TextView freshWaterText = new TextView(con);
        freshWaterText.setLayoutParams(tableRowParams);
        Spanned freshWaterSpanString = HtmlCompat.fromHtml(getString(R.string.freshWater), HtmlCompat.FROM_HTML_MODE_LEGACY);
        freshWaterText.setText(freshWaterSpanString);
        freshWaterText.setTextColor(ContextCompat.getColor(con, R.color.black));
        freshWaterText.setTextSize(14);
        freshWater.addView(freshWaterText);
        // create the fresh water checkbox
        CheckBox freshWaterCheckBox = getCheckBox(con, fish.isFreshWater());
        freshWater.addView(freshWaterCheckBox);

        // create coloration row
        TableRow colorationRow = new TableRow(con);
        colorationRow.setLayoutParams(tabLayoutParams);
        colorationRow.setPadding(20,10,0,15);
        tableLayout.addView(colorationRow);
        // create the coloration text view
        TextView colorationText = new TextView(con);
        colorationText.setLayoutParams(tableRowParams);
        SpannableStringBuilder colorationSpanString = createSpannableString(R.string.coloration, fish.getColoration());
        colorationText.setText(colorationSpanString);
        colorationText.setTextColor(ContextCompat.getColor(con, R.color.black));
        colorationText.setTextSize(14);
        colorationRow.addView(colorationText);

        // create the feeding behaviour row
        TableRow feedingBehaviourRow = new TableRow(con);
        feedingBehaviourRow.setLayoutParams(tabLayoutParams);
        feedingBehaviourRow.setPadding(20,10,0,15);
        tableLayout.addView(feedingBehaviourRow);
        // create the feeding behaviour text view
        TextView feedingBehaviourText = new TextView(con);
        feedingBehaviourText.setLayoutParams(tableRowParams);
        SpannableStringBuilder feedingBehavSpanString = createSpannableString(R.string.feedingBehaviour, fish.getFeedingBehaviour());
        feedingBehaviourText.setText(feedingBehavSpanString);
        feedingBehaviourText.setTextColor(ContextCompat.getColor(con, R.color.black));
        feedingBehaviourText.setTextSize(14);
        feedingBehaviourRow.addView(feedingBehaviourText);

        // create the health warning row
        TableRow healthWarningsRow = new TableRow(con);
        healthWarningsRow.setLayoutParams(tabLayoutParams);
        healthWarningsRow.setPadding(20,10,0,15);
        tableLayout.addView(healthWarningsRow);
        // create the health warning text view
        TextView healthWarningsText = new TextView(con);
        healthWarningsText.setLayoutParams(tableRowParams);
        SpannableStringBuilder healthWarningsSpanString = createSpannableString(R.string.healthWarnings, fish.getHealthWarnings());
        healthWarningsText.setText(healthWarningsSpanString);
        healthWarningsText.setTextSize(14);
        healthWarningsText.setTextColor(ContextCompat.getColor(con, R.color.black));
        healthWarningsRow.addView(healthWarningsText);

        // create the food value row
        TableRow foodValueRow = new TableRow(con);
        foodValueRow.setLayoutParams(tabLayoutParams);
        foodValueRow.setPadding(20,10,0,15);
        tableLayout.addView(foodValueRow);
        // create the food value text view
        TextView foodValueText = new TextView(con);
        foodValueText.setLayoutParams(tableRowParams);
        SpannableStringBuilder foodValueSpanString = createSpannableString(R.string.foodValue, fish.getFoodValue());
        foodValueText.setText(foodValueSpanString);
        foodValueText.setTextSize(14);
        foodValueText.setTextColor(ContextCompat.getColor(con, R.color.black));
        foodValueRow.addView(foodValueText);


        TableRow similarSpeciesRow = new TableRow(con);
        similarSpeciesRow.setLayoutParams(tabLayoutParams);
        similarSpeciesRow.setPadding(20,10,0,15);
        tableLayout.addView(similarSpeciesRow);
        // create the fish name text view
        TextView similarSpeciesText = new TextView(con);
        similarSpeciesText.setLayoutParams(tableRowParams);
        SpannableStringBuilder simSpeciesSpanString = createSpannableString(R.string.similarSpecies, fish.getSimilarSpecies());
        similarSpeciesText.setText(simSpeciesSpanString);
        similarSpeciesText.setTextSize(14);
        similarSpeciesText.setTextColor(ContextCompat.getColor(con, R.color.black));
        similarSpeciesRow.addView(similarSpeciesText);


        TableRow environmentalDetailRow = new TableRow(con);
        environmentalDetailRow.setLayoutParams(tabLayoutParams);
        environmentalDetailRow.setPadding(20,10,0,50);
        tableLayout.addView(environmentalDetailRow);
        // create the fish name text view
        TextView environmentalsDetailText = new TextView(con);
        environmentalsDetailText.setLayoutParams(tableRowParams);
        SpannableStringBuilder environmentalDetSpanString = createSpannableString(R.string.environmentalDetails, fish.getEnvironmentDetail());
        environmentalsDetailText.setText(environmentalDetSpanString);
        environmentalsDetailText.setTextSize(14);
        environmentalsDetailText.setTextColor(ContextCompat.getColor(con, R.color.black));
        environmentalDetailRow.addView(environmentalsDetailText);

        return linearLayout;
    }

    private String predAccurString(float predAccuracy) {
        return predAccuracy * 100 + " %";
    }


    // according to https://stackoverflow.com/a/29556731, there is very little difference between using a
    // SpannableString and SpannableStringBuilder directly in the text view since the SpannableStringBuilder
    // is just a mutable version of the SpannableString, similarly to an ordinary String and StringBuilder
    private SpannableStringBuilder createSpannableString(int resID, String text) {
        // get the prepared string as span form the string resources.xml
        Spanned span = HtmlCompat.fromHtml(getString(resID), HtmlCompat.FROM_HTML_MODE_LEGACY);
        // requires a spannable string builder to append normal fish text to the bold span
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(span);
        spannableStringBuilder.append(" " + text);
        return spannableStringBuilder;
    }

    private SpannableStringBuilder createSpannableString(int resID, String[] texts) {
        // get the prepared string as span form the string resources.xml
        Spanned span = HtmlCompat.fromHtml(getString(resID), HtmlCompat.FROM_HTML_MODE_LEGACY);
        // requires a spannable string builder to append normal fish text to the bold span
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(span);
        for(int i = 0; i < texts.length; i++) {
            if(i != texts.length) {
                spannableStringBuilder.append(" " + texts[i] + ", ");
            } else {
                spannableStringBuilder.append(" " + texts[i] + ", ");
            }
        }
        return spannableStringBuilder;
    }

    private CheckBox getCheckBox(Context con, boolean ticked) {
        CheckBox checkBox = new CheckBox(con);
        checkBox.setClickable(false);
        checkBox.setChecked(ticked);
        return checkBox;
    }

}
