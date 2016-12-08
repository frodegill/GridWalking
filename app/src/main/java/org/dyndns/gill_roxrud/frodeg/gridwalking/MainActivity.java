package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Toast;

import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OpenStreetMapTileProviderConstants.setUserAgentValue(BuildConfig.APPLICATION_ID);

        onStartButtonClicked(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_map, menu);
        return true;
    }

    public void onStartButtonClicked(View v) {
        // Request permissions to support Android Marshmallow and above devices
        if (!needsPermissions()) {
            ContextCompat.startActivity(this, new Intent(this, MapActivity.class), null);
        }
    }

    // START PERMISSION CHECK
    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    private boolean needsPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissions = new ArrayList<>();
            String message = "Requested permissions:";
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
                message += "\nLocation to show user location.";
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                message += "\nStorage access to store map tiles.";
            }
            if (!permissions.isEmpty()) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                String[] params = permissions.toArray(new String[permissions.size()]);
                requestPermissions(params, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                return true;
            } // else: We already have permissions, so handle as normal
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION and WRITE_EXTERNAL_STORAGE
                Boolean location = perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                Boolean storage = perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                if (location && storage) {
                    // All Permissions Granted
                    Toast.makeText(MainActivity.this, "All permissions granted", Toast.LENGTH_SHORT).show();
                } else if (location) {
                    Toast.makeText(MainActivity.this, "Storage permission is required to store map tiles to reduce data usage and for offline usage.", Toast.LENGTH_LONG).show();
                } else if (storage) {
                    Toast.makeText(MainActivity.this, "Location permission is required to show the user's location on map.", Toast.LENGTH_LONG).show();
                } else { // !location && !storage case
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "Storage permission is required to store map tiles to reduce data usage and for offline usage." +
                            "\nLocation permission is required to show the user's location on map.", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}
