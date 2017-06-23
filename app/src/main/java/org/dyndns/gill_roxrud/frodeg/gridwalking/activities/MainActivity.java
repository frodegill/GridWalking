package org.dyndns.gill_roxrud.frodeg.gridwalking.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.dyndns.gill_roxrud.frodeg.gridwalking.BuildConfig;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GameState;
import org.dyndns.gill_roxrud.frodeg.gridwalking.R;
import org.osmdroid.config.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private int createdByTextClickedCount = 0;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button restoreButton = (Button) findViewById(R.id.restore_button);
        restoreButton.setVisibility(Button.INVISIBLE);

        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

        onStartButtonClicked(null);
    }

    public void onStartButtonClicked(View v) {
        // Request permissions to support Android Marshmallow and above devices
        if (!needsAppPermissions()) {
            ContextCompat.startActivity(this, new Intent(this, MapActivity.class), null);
        }
    }

    public void onRestoreButtonClicked(View v) {
        File restoreFile = getRestoreFile();
        if (null == restoreFile) {
            return;
        }

        try {
            GameState.getInstance().getDB().RestoreFromFileT(restoreFile);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Resore failed: "+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void onCreatedByTextClicked(View v) {
        if (++createdByTextClickedCount == 3) {
            if (!needsRestorePermissions()) {
                showRestoreButton();
            }
        }
    }

    private void showRestoreButton() {
        File restoreFile = getRestoreFile();
        if (null == restoreFile) {
            return;
        }

        Button restoreButton = (Button) findViewById(R.id.restore_button);
        restoreButton.setVisibility(Button.VISIBLE);
    }

    private File getRestoreFile() {
        final String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state) && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return null;
        }

        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File restoreFile = new File(downloadDir, "gridwalking.dat");
        return restoreFile.exists() ? restoreFile : null;
    }

    // START PERMISSION CHECK
    final private int REQUEST_CODE_ASK_MULTIPLE_APP_PERMISSIONS = 124;
    final private int REQUEST_CODE_ASK_MULTIPLE_RESTORE_PERMISSIONS = 125;

    private boolean needsAppPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissions = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (!permissions.isEmpty()) {
                String[] params = permissions.toArray(new String[permissions.size()]);
                requestPermissions(params, REQUEST_CODE_ASK_MULTIPLE_APP_PERMISSIONS);
                return true;
            } // else: We already have permissions, so handle as normal
        }
        return false;
    }

    private boolean needsRestorePermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissions = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (!permissions.isEmpty()) {
                String[] params = permissions.toArray(new String[permissions.size()]);
                requestPermissions(params, REQUEST_CODE_ASK_MULTIPLE_RESTORE_PERMISSIONS);
                return true;
            } // else: We already have permissions, so handle as normal
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_APP_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "Location permission is required to show the user's location on map.", Toast.LENGTH_SHORT).show();
                }
                break;
            }

            case REQUEST_CODE_ASK_MULTIPLE_RESTORE_PERMISSIONS: {
                boolean permissionGranted;
                if (Build.VERSION.SDK_INT < 16) {
                    permissionGranted = true;
                } else {
                    Map<String, Integer> perms = new HashMap<>();
                    // Initial
                    perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                    // Fill with results
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    // Check for READ_EXTERNAL_STORAGE
                    permissionGranted = perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                }

                if (!permissionGranted) {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "External Storage permission is required to restore from file.", Toast.LENGTH_SHORT).show();
                } else {
                    showRestoreButton();
                }
                break;
            }

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}
