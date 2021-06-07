package com.example.fypandroidapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Firebase onDataChange.";
    private static final String LOCAL_PATH = "/Local/";
    private static final String CHANNEL_ID = "1234";
    private static final String FACEMASK_ALERT = "Facemask Alert!";
    private static final String DISTANCE_ALERT = "Social Distancing Alert!";

    private final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private TextView distanceText, facemaskText;
    private int distanceViolationsCount, facemaskViolationsCount,
            previousDistanceViolationsCount, previousFacemaskViolationsCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        distanceText = findViewById(R.id.distanceText);
        facemaskText = findViewById(R.id.facemaskText);

        createNotificationChannel();

        onDataChange();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void onDataChange() {
        databaseReference.child(LOCAL_PATH).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String distanceViolations = (String) dataSnapshot.child("distance_violations").getValue();
                String facemaskViolations = (String) dataSnapshot.child("facemask_violations").getValue();

                distanceText.setText(String.format("Distance Violations: %s", distanceViolations));
                facemaskText.setText(String.format("Facemask Violations: %s", facemaskViolations));

                distanceViolationsCount = Integer.parseInt(distanceViolations);
                facemaskViolationsCount = Integer.parseInt(facemaskViolations);

                checkViolations();

                previousDistanceViolationsCount = distanceViolationsCount;
                previousFacemaskViolationsCount = facemaskViolationsCount;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        });
    }

    private void checkViolations() {
        if (distanceViolationsCount > previousDistanceViolationsCount) {
            Toast toast = Toast.makeText(getApplicationContext(),
                    DISTANCE_ALERT,
                    Toast.LENGTH_SHORT);
            toast.show();
            pushNotification(DISTANCE_ALERT);
        }
        if (facemaskViolationsCount > previousFacemaskViolationsCount) {
            Toast toast = Toast.makeText(getApplicationContext(),
                    FACEMASK_ALERT,
                    Toast.LENGTH_LONG);
            toast.show();
            pushNotification(FACEMASK_ALERT);
        }
    }

    private void pushNotification(String alert) {
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.splash)
                .setContentTitle(alert)
                .setContentText("New violation was detected in your area. " +
                        "Please ensure you wear a facemask and adhere to social distancing.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("New violation was detected in your area. " +
                                "Please ensure you wear a facemask and adhere to social distancing."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)// For Android 7.1 and lower
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(0, builder.build());
    }
}