package com.suzhiyam.triggerinfence;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.suzhiyam.triggerinfence.databinding.ActivityMainBinding;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSION_REQUEST_CODE = 125;
    private ActivityMainBinding binding;
    private final Context context = MainActivity.this;
    private final Activity activity = MainActivity.this;

    private RelativeLayout rl_status_container;
    private TextView tv_set_lat_long,
            tv_refresh;
    private Spinner sp_fence_distance;
    private SwitchCompat sw_locker;

    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private static final int PERMISSION_REQUEST_CODE = 124;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private final String[] fenceDistanceSpinnerItems = {
            "100",
            "200",
            "300",
            "400",
            "500",
            "1000"
    };

    double fenceLatitude = 8.174114959993265; // Fence latitude.
    double fenceLongitude = 77.4349498879206; // Fence longitude.
    float fenceRadius = Float.parseFloat(fenceDistanceSpinnerItems[0]);
    double userLatitude = fenceLatitude; // User latitude.
    double userLongitude = fenceLongitude; // User longitude.

    Geofence geofence;
    GeofencingRequest geofencingRequest;
    GeofencingClient geofencingClient;
    PendingIntent pendingIntent;

    public AudioManager audioManager;

    private boolean isFineLocationEnabled = false;
    private boolean isCoarseLocationEnabled = false;
    private boolean isBackgroundLocationEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewInitializations();

        initialWorks();

        setGeofenceProperties();

        listenersContainer();
    }

    private void viewInitializations() {
        rl_status_container = findViewById(R.id.rl_status_container);
        sw_locker = findViewById(R.id.sw_locker);
        tv_set_lat_long = findViewById(R.id.tv_set_lat_long);
        tv_refresh = findViewById(R.id.tv_refresh);
        sp_fence_distance = findViewById(R.id.sp_fence_distance);
    }

    private void initialWorks() {
        sw_locker.setChecked(true);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000)// 5 seconds Interval
                .build();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, fenceDistanceSpinnerItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sp_fence_distance.setAdapter(adapter);

        rl_status_container.setBackgroundColor(getColor(R.color.red));
    }

    private void listenersContainer() {
        tv_set_lat_long.setOnClickListener(view -> {
            if(sw_locker.isChecked())
            {
                Toast.makeText(context, "Unlock switch to set location", Toast.LENGTH_SHORT).show();
            }
            else
            {
                fenceLatitude = userLatitude;
                fenceLongitude = userLongitude;

                tv_refresh.performClick();

                Toast.makeText(context, "Your fence location got set", Toast.LENGTH_SHORT).show();

                sw_locker.setChecked(true);
            }
        });

        tv_refresh.setOnClickListener(view -> {
            boolean allPermissionsGranted = isFineLocationEnabled && isCoarseLocationEnabled && isBackgroundLocationEnabled;

            if(allPermissionsGranted)
            {
                rl_status_container.setBackgroundColor(getColor(R.color.green));
            }
            else
            {
                rl_status_container.setBackgroundColor(getColor(R.color.red));
            }

            if (geofencingClient != null) {
                geofencingClient.removeGeofences(pendingIntent);

                checkRuntimeAudioPermission();

                checkRuntimeLocationPermission();
            }
        });

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();

                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    Log.e(TAG, "onLocationChanged latitude: " + latitude);
                    Log.e(TAG, "onLocationChanged longitude: " + longitude);

                    Log.e(TAG, "\n*********************************\n");

                    userLatitude = latitude;
                    userLongitude = longitude;
                } else {
                    Log.e(TAG, "onLocationChanged location is null");
                }
            }
        };

        sp_fence_distance.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);

                fenceRadius = Float.parseFloat(selectedItem);

                Toast.makeText(getApplicationContext(), selectedItem + " meters selected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing.
            }
        });
    }

    private void setGeofenceProperties() {
        geofence = new Geofence.Builder()
                .setRequestId("Trigger Geofence")
                .setCircularRegion(fenceLatitude, fenceLongitude, fenceRadius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);

        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        geofencingClient = LocationServices.getGeofencingClient(this);

        Log.e(TAG, "setGeofenceProperties is now set");
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    private void checkRuntimeAudioPermission() {
        if (!Settings.System.canWrite(context)) {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    private void checkRuntimeLocationPermission() {
        isFineLocationEnabled = (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        isCoarseLocationEnabled = (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        isBackgroundLocationEnabled = (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED);

        Log.e(TAG, "checkRuntimeLocationPermission isFineLocationEnabled: " + isFineLocationEnabled);
        Log.e(TAG, "checkRuntimeLocationPermission isCoarseLocationEnabled: " + isCoarseLocationEnabled);
        Log.e(TAG, "checkRuntimeLocationPermission isBackgroundLocationEnabled: " + isBackgroundLocationEnabled);

        if (isFineLocationEnabled) {
            if (isCoarseLocationEnabled) {
                if (isBackgroundLocationEnabled) {
                    Log.e(TAG, "checkRuntimeLocationPermission going to add geo fences");

                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

                    geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                            .addOnSuccessListener(this, aVoid -> {
                                Log.d(TAG, "Geo fences added successfully");

                                rl_status_container.setBackgroundColor(getColor(R.color.green));

                                Toast.makeText(context, "SetUp completed successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error adding Geo fences: " + e.getMessage());

                                Toast.makeText(context, "Error occurred try again!", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Log.e(TAG, "checkRuntimeLocationPermission requestPermissions");

                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PERMISSION_REQUEST_CODE);
                }
            } else {
                Log.e(TAG, "checkRuntimeLocationPermission requestPermissions");

                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
            }
        } else {
            Log.e(TAG, "checkRuntimeLocationPermission requestPermissions");

            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.e(TAG, "onActivityResult called");

        if (Settings.System.canWrite(context)) {
            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        } else {
            Toast.makeText(context, "Audio System Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;

            Log.e(TAG, "onRequestPermissionsResult grantResults.length: " + grantResults.length);

            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    Log.e(TAG, "onRequestPermissionsResult grantResult: " + grantResult);

                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = isFineLocationEnabled && isCoarseLocationEnabled && isBackgroundLocationEnabled;
                        break;
                    }
                }
            }

            if (allPermissionsGranted) {
                Log.e(TAG, "onRequestPermissionsResult now all permissions granted");
            } else {
                Toast.makeText(context, "Location permission denied, Manually enable location service", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);

            fusedLocationProviderClient.flushLocations();
        }
    }
}