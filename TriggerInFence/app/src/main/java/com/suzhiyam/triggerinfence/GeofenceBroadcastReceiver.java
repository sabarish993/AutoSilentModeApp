package com.suzhiyam.triggerinfence;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = GeofenceBroadcastReceiver.class.getSimpleName();
    private AudioManager audioManager;

    public GeofenceBroadcastReceiver()
    {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "GeofenceBroadcastReceiver onReceive intent: " + intent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if(audioManager == null)
        {
            if(context instanceof MainActivity)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    audioManager = ((MainActivity) context).getAudioManager();
                }
                else
                {
                    audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                }
            }
            else
            {
                audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            }
        }
        else
        {
            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        Log.e(TAG, "GeofenceBroadcastReceiver onReceive audioManager: " + audioManager);

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent != null) {
            if (geofencingEvent.hasError()) {
                Toast.makeText(context, "Error occurred try again!", Toast.LENGTH_SHORT).show();

                Log.e(TAG, "GeofenceBroadcastReceiver onReceive Error occurred: " + geofencingEvent.getErrorCode());
                return;
            }

            int geofenceTransition = geofencingEvent.getGeofenceTransition();

            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                Toast.makeText(context, "Your mobile is in Silent Mode", Toast.LENGTH_SHORT).show();

                Log.e(TAG, "GeofenceBroadcastReceiver onReceive called GEOFENCE_TRANSITION_ENTER");

                if(audioManager != null)
                {
                    if (!notificationManager.isNotificationPolicyAccessGranted()) {
                        Intent notificationIntent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(notificationIntent);
                    } else {
                        audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    }
                }
            }
            else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                Toast.makeText(context, "Your mobile is in Normal Ring Mode", Toast.LENGTH_SHORT).show();

                Log.e(TAG, "GeofenceBroadcastReceiver onReceive called GEOFENCE_TRANSITION_EXIT");

                if(audioManager != null)
                {
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                }
            }
        } else {
            Toast.makeText(context, "geofencingEvent is null", Toast.LENGTH_SHORT).show();

            Log.e(TAG, "GeofenceBroadcastReceiver onReceive geofencingEvent is null");
        }
    }
}
