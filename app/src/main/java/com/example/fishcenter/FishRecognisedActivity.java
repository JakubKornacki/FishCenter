package com.example.fishcenter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import java.util.ArrayList;
import java.util.HashSet;

public class FishRecognisedActivity extends AppCompatActivity {
    private ArrayList<Fish> fishes;
    private LinearLayout linearLayoutInsideScrollView;
    private Button backToMainMenuButton;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fish_recognised);

        // way of passing objects between activities using Intent with the help of serializable interface and Bundle
        // more references in the FishialAPIFetchFishData.java class
        // https://stackoverflow.com/questions/13601883/how-to-pass-arraylist-of-objects-from-one-to-another-activity-using-intent-in-an
        Bundle packagedBundle = getIntent().getBundleExtra("bundle");
        // extract fishes ArrayList out from the bundle
        fishes = (ArrayList<Fish>) packagedBundle.getSerializable("fishes");
        linearLayoutInsideScrollView = findViewById(R.id.linearLayoutInsideScrollView);

        // store unique fishes only
        HashSet<Fish> fishesDisplayed = new HashSet<>();
        // iterate through the returned fishes and append non-duplicates to linear layout
        for(int i = 0; i < fishes.size(); i++) {
            Fish fish = fishes.get(i);
            if(fishesDisplayed.add(fish)) {
                LinearLayout table = drawTable(fishes.get(i));
                linearLayoutInsideScrollView.addView(table);
            }
        }
        backToMainMenuButton = createBackToMainMenuButton();
        linearLayoutInsideScrollView.addView(backToMainMenuButton);

        backToMainMenuButton.setOnClickListener(view ->  {
            Intent mainMenuActivity = new Intent(getApplicationContext(), MainPageActivity.class);
            startActivity(mainMenuActivity);
        });

    }

    private Button createBackToMainMenuButton() {
        Context con = getApplicationContext();
        Button backToMainMenuButton = new Button(con);
        backToMainMenuButton.setLayoutParams(linearLayoutInsideScrollView.getLayoutParams());
        // https://stackoverflow.com/a/32202256 getColor() method is deprecated as of API 23
        // another way to get color from resources is to use ContextCompat.getColor(context, R.color.color_name)
        // by passing in the application context along with the resource id of the wanted colour
        backToMainMenuButton.setBackgroundColor(ContextCompat.getColor(con, R.color.ordinaryButtonColor));
        backToMainMenuButton.setMinHeight(48);
        backToMainMenuButton.setText(getText(R.string.backToMainMenu));
        backToMainMenuButton.setTextColor(getColor(R.color.white));
        backToMainMenuButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        return backToMainMenuButton;
    }





    // the below method creates a tables that is inserted into a scroll view dynamically which is inserted into a linear layout
    // it also uses predefined strings with HTML markup which are converted into SpannableString objects
    // and inserted into the table row along with the fish data
    // https://developer.android.com/guide/topics/resources/string-resource
    private LinearLayout drawTable(Fish fish) {
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
        linearLayout.setBackground(getDrawable(R.drawable.layout_rounded_corners_white_15));
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
        //https://stackoverflow.com/questions/3681714/bad-bitmap-error-when-setting-uri
        Glide.with(con).load(fish.getMediaUri()).override(Target.SIZE_ORIGINAL).into(imageView);
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
        freshWaterText.setTextSize(14);
        freshWater.addView(freshWaterText);
        // create the fresh water checkbox
        CheckBox freshWaterCheckBox = getCheckBox(con, fish.isFreshWater());
        freshWater.addView(freshWaterCheckBox);


        TableRow colorationRow = new TableRow(con);
        colorationRow.setLayoutParams(tabLayoutParams);
        colorationRow.setPadding(20,10,0,15);
        tableLayout.addView(colorationRow);
        // create the fish name text view
        TextView colorationText = new TextView(con);
        colorationText.setLayoutParams(tableRowParams);
        SpannableStringBuilder colorationSpanString = createSpannableString(R.string.coloration, fish.getColoration());
        colorationText.setText(colorationSpanString);
        colorationText.setTextSize(14);
        colorationRow.addView(colorationText);


        TableRow feedingBehaviourRow = new TableRow(con);
        feedingBehaviourRow.setLayoutParams(tabLayoutParams);
        feedingBehaviourRow.setPadding(20,10,0,15);
        tableLayout.addView(feedingBehaviourRow);
        // create the fish name text view
        TextView feedingBehaviourText = new TextView(con);
        feedingBehaviourText.setLayoutParams(tableRowParams);
        SpannableStringBuilder feedingBehavSpanString = createSpannableString(R.string.feedingBehaviour, fish.getFeedingBehaviour());
        feedingBehaviourText.setText(feedingBehavSpanString);
        feedingBehaviourText.setTextSize(14);
        feedingBehaviourRow.addView(feedingBehaviourText);

        TableRow healthWarningsRow = new TableRow(con);
        healthWarningsRow.setLayoutParams(tabLayoutParams);
        healthWarningsRow.setPadding(20,10,0,15);
        tableLayout.addView(healthWarningsRow);
        // create the fish name text view
        TextView healthWarningsText = new TextView(con);
        healthWarningsText.setLayoutParams(tableRowParams);
        SpannableStringBuilder healthWarningsSpanString = createSpannableString(R.string.healthWarnings, fish.getFeedingBehaviour());
        healthWarningsText.setText(healthWarningsSpanString);
        healthWarningsText.setTextSize(14);
        healthWarningsRow.addView(healthWarningsText);


        TableRow foodValueRow = new TableRow(con);
        foodValueRow.setLayoutParams(tabLayoutParams);
        foodValueRow.setPadding(20,10,0,15);
        tableLayout.addView(foodValueRow);
        // create the fish name text view
        TextView foodValueText = new TextView(con);
        foodValueText.setLayoutParams(tableRowParams);
        SpannableStringBuilder foodValueSpanString = createSpannableString(R.string.foodValue, fish.getFoodValue());
        foodValueText.setText(foodValueSpanString);
        foodValueText.setTextSize(14);
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
        environmentalDetailRow.addView(environmentalsDetailText);

        return linearLayout;
    }

    private String predAccurString(float predAccuracy) {
        return Float.toString(predAccuracy * 100) + " %";
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
