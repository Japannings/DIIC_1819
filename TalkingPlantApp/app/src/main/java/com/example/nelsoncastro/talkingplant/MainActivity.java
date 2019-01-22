package com.example.nelsoncastro.talkingplant;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;
import com.amazonaws.services.polly.model.Voice;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {

    private TextView plantText, choosePlantText, temperatureText, lightText, waterText, temperatureValueText, lightValueText, waterValueText;
    private ImageView plantImage, plantChoiceImage, seasonImage, timeOfDayImage, automaticWateringImage;
    private Spinner plantSpinner, plantPersonalitySpinner;
    private Button plantChoiceButton;
    private Switch automaticWateringSwitch;
    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private FirebaseDatabase realTimeDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference dbRealTimeRef;
    private DocumentReference dbInfoRef, dbPersonalitiesRef;

    private boolean plantChoice = false, automaticWatering = false;
    private boolean temperatureOK, lightOK, waterOK, plantOK;
    private boolean isSleeping, hasGreeted, hasThanked, canGreet, canThank, isGreeting, isThanking;
    private Timer timer, timerCanGreet;
    private TimerTask timerTask;
    private Handler handler;
    private boolean doRotations;
    private String plantName, plantPersonality;

    private boolean changeText, changeWhen2Or3Bad;
    private String currentTemperature, currentLight, currentWater;
    private Integer currentNumBad = null;

    private TextToSpeech tts;
    private float speed, pitch;
    private String phrase;

    private SeekBar temperatureValue, waterValue, lightValue;
    private MediaPlayer mediaPlayer;
    private AmazonPollyPresigningClient client;
    private List<Voice> voices;

    private Plant plant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbRealTimeRef = realTimeDatabase.getReference("plantStatus");

        plantText = findViewById(R.id.plantText);
        plantImage = findViewById(R.id.plantImage);
        temperatureText = findViewById(R.id.temperatureText);
        lightText = findViewById(R.id.lightText);
        waterText = findViewById(R.id.waterText);
        temperatureValue = findViewById(R.id.temperatureValue);
        lightValue = findViewById(R.id.lightValue);
        waterValue = findViewById(R.id.waterValue);
        temperatureValueText = findViewById(R.id.temperatureValueText);
        lightValueText = findViewById(R.id.lightValueText);
        waterValueText = findViewById(R.id.waterValueText);
        seasonImage = findViewById(R.id.seasonImage);
        timeOfDayImage = findViewById(R.id.timeOfDayImage);

        temperatureValue.setEnabled(false);
        lightValue.setEnabled(false);
        waterValue.setEnabled(false);

        automaticWateringSwitch = findViewById(R.id.automaticWateringSwitch);
        automaticWateringImage = findViewById(R.id.automaticWateringImage);

        automaticWateringSwitch.setOnCheckedChangeListener(this);

        plantChoiceImage = findViewById(R.id.plantChoiceImage);
        choosePlantText = findViewById(R.id.choosePlantText);
        plantSpinner = findViewById(R.id.plantSpinner);
        plantPersonalitySpinner = findViewById(R.id.plantPersonalitySpinner);
        plantChoiceButton = findViewById(R.id.plantChoiceButton);

        phrase = plantText.getText().toString();

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        plantChoice = prefs.getBoolean("plantChoice", false);
        plantName = prefs.getString("plantName", null);
        plantPersonality = prefs.getString("plantPersonality", null);

        currentWater = "";
        currentTemperature = "";
        currentLight = "";
        hasGreeted = false;
        hasThanked = false;
        isGreeting = false;
        isThanking = false;
        canThank = true;
        canGreet = true;
        doRotations = false;

        setupNewMediaPlayer();

        plantChoice = false;

        if (plantChoice) {
            makeChoosePlantComponentsInvisible();
            compareInfo();
        }

        if (!plantChoice) {
            makePlantComponentsInvisible();
            ArrayAdapter<CharSequence> adapterNames = ArrayAdapter.createFromResource(this, R.array.plantNames, android.R.layout.simple_spinner_item);
            adapterNames.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            plantSpinner.setAdapter(adapterNames);
            plantSpinner.setOnItemSelectedListener(this);

            ArrayAdapter<CharSequence> adapterPersonalities = ArrayAdapter.createFromResource(this, R.array.plantPersonalities, android.R.layout.simple_spinner_item);
            adapterPersonalities.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            plantPersonalitySpinner.setAdapter(adapterPersonalities);
            plantPersonalitySpinner.setOnItemSelectedListener(this);

        }
    }

    public void makePlantComponentsInvisible() {
        plantText.setVisibility(View.INVISIBLE);
        plantImage.setVisibility(View.INVISIBLE);

        temperatureText.setVisibility(View.INVISIBLE);
        temperatureValue.setVisibility(View.INVISIBLE);
        temperatureValueText.setVisibility(View.INVISIBLE);
        lightText.setVisibility(View.INVISIBLE);
        lightValue.setVisibility(View.INVISIBLE);
        lightValueText.setVisibility(View.INVISIBLE);
        waterText.setVisibility(View.INVISIBLE);
        waterValue.setVisibility(View.INVISIBLE);
        waterValueText.setVisibility(View.INVISIBLE);

        automaticWateringSwitch.setVisibility(View.INVISIBLE);
        automaticWateringImage.setVisibility(View.INVISIBLE);

        seasonImage.setVisibility(View.INVISIBLE);
        timeOfDayImage.setVisibility(View.INVISIBLE);
    }

    public void makePlantComponentsVisible() {
        plantText.setVisibility(View.VISIBLE);
        plantImage.setVisibility(View.VISIBLE);

        temperatureText.setVisibility(View.VISIBLE);
        temperatureValue.setVisibility(View.VISIBLE);
        temperatureValueText.setVisibility(View.VISIBLE);
        lightText.setVisibility(View.VISIBLE);
        lightValue.setVisibility(View.VISIBLE);
        lightValueText.setVisibility(View.VISIBLE);
        waterText.setVisibility(View.VISIBLE);
        waterValue.setVisibility(View.VISIBLE);
        waterValueText.setVisibility(View.VISIBLE);

        automaticWateringSwitch.setVisibility(View.VISIBLE);
        automaticWateringImage.setVisibility(View.VISIBLE);

        seasonImage.setVisibility(View.VISIBLE);
        timeOfDayImage.setVisibility(View.VISIBLE);
    }

    public void makeChoosePlantComponentsInvisible() {
        plantChoiceImage.setVisibility(View.INVISIBLE);
        choosePlantText.setVisibility(View.INVISIBLE);
        plantSpinner.setVisibility(View.INVISIBLE);
        plantPersonalitySpinner.setVisibility(View.INVISIBLE);
        plantChoiceButton.setVisibility(View.INVISIBLE);
    }

    public void compareInfo() {
        dbInfoRef = database.collection("plantInfo").document(plantName);
        dbPersonalitiesRef = database.collection("plantPersonalities").document(plantPersonality);

        dbInfoRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            plant = new Plant(plantName, Integer.parseInt(documentSnapshot.get("MinLight").toString()), Integer.parseInt(documentSnapshot.get("MaxLight").toString()),
                                    Integer.parseInt(documentSnapshot.get("MinWater").toString()), Integer.parseInt(documentSnapshot.get("MaxWater").toString()),
                                    Float.parseFloat(documentSnapshot.get("MinTemperature").toString()), Float.parseFloat(documentSnapshot.get("MaxTemperature").toString()), plantPersonality);

                            checkPlantStatus(plant);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    private void checkPlantStatus(final Plant plant) {
       /* dbStatusRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.exists()) {
                            int light = Integer.parseInt(documentSnapshot.get("Light").toString());
                            float temperature = Float.parseFloat(documentSnapshot.get("Temperature").toString());
                            int water = Integer.parseInt(documentSnapshot.get("Water").toString());
                            boolean greet = Boolean.parseBoolean(String.valueOf(documentSnapshot.getBoolean("Greet")));
                            boolean beingWatered = Boolean.parseBoolean(String.valueOf(documentSnapshot.getBoolean("BeingWatered")));

                            compareData(plant, light, temperature, water, greet, beingWatered);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
                */

       dbRealTimeRef.addListenerForSingleValueEvent(new ValueEventListener() {
           @Override
           public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
               PlantStatus plantStatus = dataSnapshot.getValue(PlantStatus.class);

               compareData(plant, plantStatus.getLight(), plantStatus.getTemperature(), plantStatus.getWater(), plantStatus.isGreet(), plantStatus.isBeingWatered());
           }

           @Override
           public void onCancelled(@NonNull DatabaseError databaseError) {

           }
       });
    }

    private void compareData(final Plant plant, final int light, final float temperature, final int water, final boolean greet, final boolean beingWatered) {

        int numBad = 0;
        changeText = false;
        changeWhen2Or3Bad = true;

        if (temperature > plant.getMaxTemperature()) {
            temperatureValueText.setText("Melting " + "(" + temperature + "ºC)");
        }
        if (temperature < plant.getMinTemperature()) {
            temperatureValueText.setText("Freezing " + "(" + temperature + "ºC)");
        }
        if (temperature <= plant.getMaxTemperature() && temperature >= plant.getMinTemperature()) {
            temperatureValueText.setText("Alright " + "(" + temperature + "ºC)");
        }
        temperatureValue.setProgress((int)temperature + 50);

        switch(light) {
            case 1:
                lightValue.setProgress(0);
                lightValueText.setText("Very Low");
                break;
            case 2:
                lightValue.setProgress(25);
                lightValueText.setText("Low");
                break;
            case 3:
                lightValue.setProgress(50);
                lightValueText.setText("Medium");
                break;
            case 4:
                lightValue.setProgress(75);
                lightValueText.setText("High");
                break;
            case 5:
                lightValue.setProgress(100);
                lightValueText.setText("Very High");
                break;
            default:
                lightValue.setProgress(0);
                lightValueText.setText("Unknown");
                break;
        }

        switch(water) {
            case 1:
                waterValue.setProgress(0);
                waterValueText.setText("Very Dry");
                break;
            case 2:
                waterValue.setProgress(25);
                waterValueText.setText("A Bit Dry");
                break;
            case 3:
                waterValue.setProgress(50);
                waterValueText.setText("Nice");
                break;
            case 4:
                waterValue.setProgress(75);
                waterValueText.setText("Fully Watered");
                break;
            case 5:
                waterValue.setProgress(100);
                waterValueText.setText("Soaked");
                break;
            default:
                waterValue.setProgress(0);
                waterValueText.setText("Unknown");
                break;
        }

        if (temperature > plant.getMaxTemperature()) {
            if (currentTemperature.equals("")) {
                currentTemperature = "HOT";
                changeText = true;
            }
            else {
                if (currentTemperature.equals("COLD") || currentTemperature.equals("OK")) {
                    changeText = true;
                    currentTemperature = "HOT";
                }
            }

            temperatureOK = false;
            numBad++;
        }
        if (temperature < plant.getMinTemperature()) {
            if (currentTemperature.equals("")) {
                currentTemperature = "COLD";
                changeText = true;
            }
            else {
                if (currentTemperature.equals("HOT") || currentTemperature.equals("OK")) {
                    changeText = true;
                    currentTemperature = "COLD";
                }
            }

            temperatureOK = false;
            numBad++;
        }

        if (temperature <= plant.getMaxTemperature() && temperature >= plant.getMinTemperature()) {
            if (currentTemperature.equals("")) {
                currentTemperature = "OK";
                changeText = true;
            }
            else {
                if (currentTemperature.equals("HOT") || currentTemperature.equals("COLD")) {
                    changeText = true;
                    currentTemperature = "OK";
                }
            }

            temperatureOK = true;
        }
        if (water > plant.getMaxWater()) {
            if (currentWater.equals("")) {
                if (water == 6)
                    currentWater = "UNKNOWN";
                else
                    currentWater = "SOAKED";

                changeText = true;
            }
            else {
                if (water != 6) {
                    if (currentWater.equals("OK") || currentWater.equals("DRY") || currentWater.equals("UNKNOWN")) {
                        changeText = true;
                        currentWater = "SOAKED";
                    }
                }
                if (water == 6) {
                    if (currentWater.equals("DRY") || currentWater.equals("SOAKED")) {
                        changeText = true;
                        currentWater = "UNKNOWN";
                    }
                }
            }

            if (water != 6) {
                numBad++;
                waterOK = false;
            } else {
                waterOK = true;
            }
        }
        if (water < plant.getMinWater()) {
            if (currentWater.equals("")) {
                currentWater = "DRY";
                changeText = true;
            }
            else {
                if (currentWater.equals("OK") || currentWater.equals("SOAKED") || currentWater.equals("UNKNOWN")) {
                    changeText = true;
                    currentWater = "DRY";
                }
            }

            numBad++;
            waterOK = false;

        }
        if (water <= plant.getMaxWater() && water >= plant.getMinWater()) {
            if (currentWater.equals("")) {
                currentWater = "OK";
                changeText = true;
            }
            else {
                if (currentWater.equals("DRY") || currentWater.equals("SOAKED")) {
                    changeText = true;
                    currentWater = "OK";
                }
            }

            waterOK = true;
        }
        if (light > plant.getMaxLight()) {
            if (currentLight.equals("")) {
                if (light == 6)
                    currentLight = "UNKNOWN";
                else
                    currentLight = "HIGH";

                changeText = true;
            }
            else {
                if (light != 6) {
                    if (currentLight.equals("MEDIUM") || currentLight.equals("LOW") || currentLight.equals("UNKNOWN")) {
                        changeText = true;
                        currentLight = "HIGH";
                    }
                }
                if (light == 6) {
                    if (currentLight.equals("LOW") || currentLight.equals("HIGH")) {
                        changeText = true;
                        currentLight = "UNKNOWN";
                    }
                }
            }

            if (light != 6) {
                numBad++;
                lightOK = false;
            } else {
                lightOK = true;
            }
        }
        if (light < plant.getMinLight()) {
            if (currentLight.equals("")) {
                currentLight = "LOW";
                changeText = true;
            }
            else {
                if (currentLight.equals("MEDIUM") || currentLight.equals("HIGH") || currentLight.equals("UNKNOWN")) {
                    changeText = true;
                    currentLight = "LOW";
                }
            }

            lightOK = false;
            numBad++;
        }
        if (light <= plant.getMaxLight() && light >= plant.getMinLight()) {
            if (currentLight.equals("")) {
                currentLight = "MEDIUM";
                changeText = true;
            }
            else {
                if (currentLight.equals("LOW") || currentLight.equals("HIGH")) {
                    changeText = true;
                    currentLight = "MEDIUM";
                }
            }

            lightOK = true;
        }

        if (currentNumBad == null)
            currentNumBad = numBad;
        else {
            if (currentNumBad == numBad) {
                changeWhen2Or3Bad = false;
            }

            currentNumBad = numBad;
        }

        final int finalNumBad = numBad;

        dbPersonalitiesRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.exists()) {

                            List<String> phraseNumBad3 = (List<String>) documentSnapshot.get("PhraseNumBad3");
                            List<String> phraseNumBad2 = (List<String>) documentSnapshot.get("PhraseNumBad2");
                            List<String> phraseNumBad0 = (List<String>) documentSnapshot.get("PhraseNumBad0");
                            List<String> phraseTemperatureHot = (List<String>) documentSnapshot.get("PhraseTemperatureHot");
                            List<String> phraseTemperatureCold = (List<String>) documentSnapshot.get("PhraseTemperatureCold");
                            List<String> phraseLightMore = (List<String>) documentSnapshot.get("PhraseLightMore");
                            List<String> phraseLightLess = (List<String>) documentSnapshot.get("PhraseLightLess");
                            List<String> phraseWaterMore = (List<String>) documentSnapshot.get("PhraseWaterMore");
                            List<String> phraseWaterLess = (List<String>) documentSnapshot.get("PhraseWaterLess");

                            updateValues(phraseNumBad3, phraseNumBad2, phraseNumBad0, phraseTemperatureHot,
                                    phraseTemperatureCold, phraseLightMore, phraseLightLess, phraseWaterMore, phraseWaterLess, finalNumBad, temperature, light, water, plant, greet, beingWatered);

                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    private void updateValues(List<String> phraseNumBad3, List<String> phraseNumBad2, final List<String> phraseNumBad0, List<String> phraseTemperatureHot, List<String> phraseTemperatureCold, List<String> phraseLightMore, List<String> phraseLightLess, List<String> phraseWaterMore, List<String> phraseWaterLess,
                              final int numBad, float temperature, int light, int water, Plant plant, boolean greet, boolean beingWatered) {

        phrase = "";

        Random random = new Random();
        final int randomPhraseNumBad3 = random.nextInt(phraseNumBad3.size());
        final int randomPhraseNumBad2 = random.nextInt(phraseNumBad2.size());
        final int randomPhraseNumBad0 = random.nextInt(phraseNumBad0.size());
        final int randomPhraseTemperatureHot = random.nextInt(phraseTemperatureHot.size());
        final int randomPhraseTemperatureCold = random.nextInt(phraseTemperatureCold.size());
        final int randomPhraseLightMore = random.nextInt(phraseLightMore.size());
        final int randomPhraseLightLess = random.nextInt(phraseLightLess.size());
        final int randomPhraseWaterMore = random.nextInt(phraseWaterMore.size());
        final int randomPhraseWaterLess = random.nextInt(phraseWaterLess.size());

        if ((changeText || hasGreeted || hasThanked) && !isGreeting && !isThanking) {

            if (changeWhen2Or3Bad || hasGreeted || hasThanked) {
                if (numBad == 3) {
                    doRotations = false;
                    stopTimer();
                    plantOK = false;
                    plantImage.setImageResource(R.drawable.nauseatedface);
                    plantText.setText(phraseNumBad3.get(randomPhraseNumBad3));
                    phrase = phraseNumBad3.get(randomPhraseNumBad3);
                }

                if (numBad == 2) {
                    doRotations = false;
                    stopTimer();
                    plantOK = false;
                    plantImage.setImageResource(R.drawable.facewithheadbandage);
                    plantText.setText(phraseNumBad2.get(randomPhraseNumBad2));
                    phrase = phraseNumBad2.get(randomPhraseNumBad2);
                }
            }

            if (numBad == 1) {
                doRotations = false;
                stopTimer();
                plantOK = false;
                if (!temperatureOK) {
                    if (temperature > plant.getMaxTemperature()) {
                        plantImage.setImageResource(R.drawable.overheatedface);
                        plantText.setText(phraseTemperatureHot.get(randomPhraseTemperatureHot));
                        phrase = phraseTemperatureHot.get(randomPhraseTemperatureHot);
                    }

                    if (temperature < plant.getMinTemperature()) {
                        plantImage.setImageResource(R.drawable.freezingface);
                        plantText.setText(phraseTemperatureCold.get(randomPhraseTemperatureCold));
                        phrase = phraseTemperatureCold.get(randomPhraseTemperatureCold);
                    }
                }

                if (!lightOK) {
                    if (light > plant.getMaxLight()) {
                        plantImage.setImageResource(R.drawable.shockedfacewithexplodinghead);
                        plantText.setText(phraseLightMore.get(randomPhraseLightMore));
                        phrase = phraseLightMore.get(randomPhraseLightMore);
                    }

                    if (light < plant.getMinLight()) {
                        plantImage.setImageResource(R.drawable.facescreaminginfear);
                        plantText.setText(phraseLightLess.get(randomPhraseLightLess));
                        phrase = phraseLightLess.get(randomPhraseLightLess);
                    }
                }

                if (!waterOK) {
                    if (water > plant.getMaxWater()) {
                        plantImage.setImageResource(R.drawable.dizzyface);
                        plantText.setText(phraseWaterMore.get(randomPhraseWaterMore));
                        phrase = phraseWaterMore.get(randomPhraseWaterMore);
                    }

                    if (water < plant.getMinWater()) {
                        plantImage.setImageResource(R.drawable.droolingface);
                        plantText.setText(phraseWaterLess.get(randomPhraseWaterLess));
                        phrase = phraseWaterLess.get(randomPhraseWaterLess);
                    }
                }
            }

            if (numBad == 0) {
                plantOK = true;
                doRotations = true;
                final int[] drawables = {R.drawable.smilingfacewithopenmouth, R.drawable.facesavouringdeliciousfood, R.drawable.smilingfacewithsunglasses};
                final int[] index = {0};
                handler = new Handler();
                timer = new Timer();
                final int interval = 10000; //milliseconds
                plantText.setText(phraseNumBad0.get(randomPhraseNumBad0));
                phrase = phraseNumBad0.get(randomPhraseNumBad0);
                timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (!isSleeping && doRotations) {
                                    plantImage.setImageResource(drawables[index[0]]);

                                    if (index[0] == 2)
                                        index[0] = 0;
                                    else
                                        index[0]++;
                                }
                            }
                        });
                    }
                };
                timer.schedule(timerTask, 0, interval);
            }

            checkTime();

            if (!hasGreeted && !hasThanked && !isSleeping) {
                readPhrase(phrase);
            }

            hasGreeted = false;
            hasThanked = false;

        }

        checkGreet(greet);
        checkBeingWatered(beingWatered);
    }

    private void checkBeingWatered(boolean beingWatered) {
        final Timer beingWateredTimer = new Timer();
        int interval = 10000;
        if (!isSleeping) {
            if (beingWatered && canThank && !isThanking) {
                if (isGreeting) {
                    isGreeting = false;
                    dbRealTimeRef.child("greet").setValue(false);
                    stopGreetTimer();
                }
                stopTimer();
                isThanking = true;
                plantImage.setImageResource(R.drawable.smilingfacewithheartshapedeyes);
                switch (plantPersonality) {
                    case "Grumpy":
                        phrase = "Better late than never!";
                        break;
                    case "Funny":
                        phrase = "Thanks for keeping me healthy!";
                        break;
                    case "Lover":
                        phrase = "Ooh! I love you!";
                        break;
                    case "Happy":
                        phrase = "Aaaw, thanks buddy!";
                        break;
                }
                plantText.setText(phrase);
                readPhrase(phrase);
                hasThanked = true;
                beingWateredTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        isThanking = false;
                        dbRealTimeRef.child("beingWatered").setValue(false);
                        startThankTimer();
                    }
                }, interval);
            }
        }
        else {
            dbRealTimeRef.child("beingWatered").setValue(false);
        }
    }

    private void checkGreet(boolean greet) {
        final Timer greetTimer = new Timer();
        int interval = 10000;
        if (!isSleeping && !isThanking) {
            if (greet && canGreet && !isGreeting) {
                stopTimer();
                isGreeting = true;
                if (plantOK) {
                    plantImage.setImageResource(R.drawable.winkingface);
                    switch (plantPersonality) {
                        case "Grumpy":
                            phrase = "Can’t a plant sleep in peace?!";
                            break;
                        case "Funny":
                            phrase = "Hello from the other side of the vase!";
                            break;
                        case "Lover":
                            phrase = "Hey handsome!";
                            break;
                        case "Happy":
                            phrase = "Hey there buddy!";
                            break;
                    }
                }
                if (!plantOK) {
                    plantImage.setImageResource(R.drawable.poutingface);
                    switch (plantPersonality) {
                        case "Grumpy":
                            phrase = "Hey you, can you help?";
                            break;
                        case "Funny":
                            phrase = "Hey hey! I'm here!";
                            break;
                        case "Lover":
                            phrase = "Are you not interested in me anymore?";
                            break;
                        case "Happy":
                            phrase = "Can i get some help please?";
                            break;
                    }
                }
                plantText.setText(phrase);
                readPhrase(phrase);
                hasGreeted = true;
                greetTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        isGreeting = false;
                        dbRealTimeRef.child("greet").setValue(false);
                        startGreetTimer();
                    }
                }, interval);
            }
        }
        else {
            dbRealTimeRef.child("greet").setValue(false);
        }
    }

    public void startGreetTimer() {
        int interval = 20000;
        timerCanGreet = new Timer();
        canGreet = false;
        timerCanGreet.schedule(new TimerTask() {
            @Override
            public void run() {
                canGreet = true;
                dbRealTimeRef.child("greet").setValue(false);
            }
        }, interval);
    }

    public void stopGreetTimer() {
        if (timerCanGreet != null) {
            timerCanGreet.cancel();
            timerCanGreet = null;
        }
    }

    public void startThankTimer() {
        int interval = 20000;
        Timer timerCanThank = new Timer();
        canThank = false;
        timerCanThank.schedule(new TimerTask() {
            @Override
            public void run() {
                canThank = true;
                dbRealTimeRef.child("beingWatered").setValue(false);
            }
        }, interval);
    }

    private void readPhrase(String phrase) {
        int id = 0;
        if (!phrase.equals("")) {
            switch (plantPersonality) {
                case "Grumpy":
                    id = 34;
                    break;
                case "Happy":
                    id = 39;
                    break;
                case "Lover":
                    id = 41;
                    break;
                case "Funny":
                    id = 40;
                    break;
            }

            // Create speech synthesis request.
            SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
                    new SynthesizeSpeechPresignRequest()
                            // Set the text to synthesize.
                            .withText(phrase)
                            // Select voice for synthesis.
                            .withVoiceId(voices.get(id).getId())
                            // Set format to MP3.
                            .withOutputFormat(OutputFormat.Mp3);

            // Get the presigned URL for synthesized speech audio stream.
            URL presignedSynthesizeSpeechUrl =
                    client.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest);

            // Use MediaPlayer: https://developer.android.com/guide/topics/media/mediaplayer.html
            if (mediaPlayer.isPlaying()) {
                setupNewMediaPlayer();
            }

            // Create a media player to play the synthesized audio stream.
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            try {
                // Set media player's data source to previously obtained URL.
                mediaPlayer.setDataSource(presignedSynthesizeSpeechUrl.toString());
            } catch (IOException e) {
                Log.e("TAG", "Unable to set data source for the media player! " + e.getMessage());
            }

            mediaPlayer.prepareAsync();

            /*tts.setPitch(pitch);
            tts.setSpeechRate(speed);
            tts.speak(phrase, TextToSpeech.QUEUE_FLUSH, null);*/

        }
    }

    public void setupNewMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
                setupNewMediaPlayer();
            }
        });
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });
    }

    public void checkTime() {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hours = calendar.get(Calendar.HOUR_OF_DAY);

        String season = "";
        String specialSeason = "";
        int month_day = month * 100 + day;

        switch (month_day) {
            case 1125:
                specialSeason = "Christmas";
                break;
            case 321:
                specialSeason = "Easter";
                break;
            case 217:
                specialSeason = "Saint Patrick";
                break;
            case 1131:
                specialSeason = "New Year's";
                break;
            case 114:
                specialSeason = "Saint Valentine";
                break;
            case 931:
                specialSeason = "Halloween";
                break;
            case 1023:
                specialSeason = "Thanksgiving";
                break;
        }

        if (month_day <= 219) {
            season = "Winter";
        }
        else if (month_day <= 520) {
            season = "Spring";
        }
        else if (month_day <= 822) {
            season = "Summer";
        }
        else if (month_day <= 1120) {
            season = "Autumn";
        }
        else {
            season = "Winter";
        }

        if (specialSeason.equals("")) {
            switch(season) {
                case "Winter":
                    seasonImage.setImageResource(R.drawable.winter);
                    break;
                case "Spring":
                    seasonImage.setImageResource(R.drawable.spring);
                    break;
                case "Summer":
                    seasonImage.setImageResource(R.drawable.summer);
                    break;
                case "Autumn":
                    seasonImage.setImageResource(R.drawable.autumn);
                    break;
            }
        } else {
            switch(specialSeason) {
                case "Christmas":
                    seasonImage.setImageResource(R.drawable.christmastree);
                    break;
                case "Easter":
                    seasonImage.setImageResource(R.drawable.easterbunny);
                    break;
                case "Saint Patrick":
                    seasonImage.setImageResource(R.drawable.saintpatricksday);
                    break;
                case "New Year's":
                    seasonImage.setImageResource(R.drawable.newyearseve);
                    break;
                case "Saint Valentine":
                    seasonImage.setImageResource(R.drawable.valentinesday);
                    break;
                case "Halloween":
                    seasonImage.setImageResource(R.drawable.halloweenjackolantern);
                    break;
                case "Thanksgiving":
                    seasonImage.setImageResource(R.drawable.thanksgiving);
                    break;
            }
        }

        if (hours >= 8 && hours < 20) {
            isSleeping = false;
            timeOfDayImage.setImageResource(R.drawable.sun);
        }
        else {
            timeOfDayImage.setImageResource(R.drawable.moon);
            if (plantOK) {
                isSleeping = true;
                plantImage.setImageResource(R.drawable.sleepingface);
                plantText.setText("Shhh... I am taking a nap");
            }
            else
                isSleeping = false;
        }
    }

    private void stopTextToSpeech() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    private void stopMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void confirmPlantChoice(View view) {
        if (!plantSpinner.getSelectedItem().equals(null) && !plantPersonalitySpinner.getSelectedItem().equals(null)) {
            plantChoice = true;
            makeChoosePlantComponentsInvisible();
            makePlantComponentsVisible();
            compareInfo();
        }

        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putBoolean("plantChoice", plantChoice);
        editor.putString("plantName", plantName);
        editor.putString("plantPersonality", plantPersonality);
        editor.apply();
        editor.commit();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.plantSpinner:
                plantName = parent.getItemAtPosition(position).toString();
                plantChoiceImage.setImageResource(getResources().getIdentifier(plantName.toLowerCase(), "drawable", getPackageName()));
                break;
            case R.id.plantPersonalitySpinner:
                plantPersonality = parent.getItemAtPosition(position).toString();
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (automaticWateringSwitch.isChecked())
                automaticWatering = true;
            else
                automaticWatering = false;

            dbRealTimeRef.child("autoWatering").setValue(automaticWatering);

            //compareInfo();
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopMediaPlayer();
        stopTextToSpeech();
        stopTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopMediaPlayer();
        stopTextToSpeech();
        stopTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopMediaPlayer();
        stopTimer();
        stopTextToSpeech();
    }

    @Override
    protected void onStart() {
        super.onStart();

        dbRealTimeRef.child("autoWatering").setValue(false);

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int language = tts.setLanguage(Locale.ENGLISH);

                    if (language == TextToSpeech.LANG_MISSING_DATA || language ==  TextToSpeech.LANG_NOT_SUPPORTED)
                        Log.e("TTS", "Language not supported");

                }
                else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        }, "com.google.android.tts");


        AWSMobileClient.getInstance().initialize(this, new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails result) {
                // Create a client that supports generation of presigned URLs.
                client = new AmazonPollyPresigningClient(AWSMobileClient.getInstance());
                Log.d("TAG", "onResult: Created polly pre-signing client");

                if (voices == null) {
                    // Create describe voices request.
                    DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();

                    try {
                        // Synchronously ask the Polly Service to describe available TTS voices.
                        DescribeVoicesResult describeVoicesResult = client.describeVoices(describeVoicesRequest);

                        // Get list of voices from the result.
                        voices = describeVoicesResult.getVoices();

                        // Log a message with a list of available TTS voices.
                        Log.i("TAG", "Available Polly voices: " + voices);
                    } catch (RuntimeException e) {
                        Log.e("TAG", "Unable to get available voices.", e);
                        return;
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("TAG", "onError: Initialization error", e);
            }
        });

        /*
        dbStatusRef.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.d("ERROR", e.toString());
                    return;
                }

                if(documentSnapshot.exists()) {
                    int light = Integer.parseInt(documentSnapshot.get("Light").toString());
                    float temperature = Float.parseFloat(documentSnapshot.get("Temperature").toString());
                    int water = Integer.parseInt(documentSnapshot.get("Water").toString());

                    boolean greet = Boolean.parseBoolean(String.valueOf(documentSnapshot.getBoolean("Greet")));
                    boolean beingWatered = Boolean.parseBoolean(String.valueOf(documentSnapshot.getBoolean("BeingWatered")));

                    if (plant != null)
                        compareData(plant, light, temperature, water, greet, beingWatered);
                }
            }
        });*/

        dbRealTimeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                PlantStatus plantStatus = dataSnapshot.getValue(PlantStatus.class);

                if (plant != null)
                    compareData(plant, plantStatus.getLight(), plantStatus.getTemperature(), plantStatus.getWater(), plantStatus.isGreet(), plantStatus.isBeingWatered());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}