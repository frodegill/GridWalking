package org.dyndns.gill_roxrud.frodeg.gridwalking.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.dyndns.gill_roxrud.frodeg.gridwalking.BuildConfig;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GameState;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GridWalkingApplication;
import org.dyndns.gill_roxrud.frodeg.gridwalking.R;
import org.osmdroid.config.Configuration;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
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

        Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(this::onStartButtonClicked);

        Button restoreButton = findViewById(R.id.restore_button);
        restoreButton.setVisibility(Button.INVISIBLE);
        restoreButton.setOnClickListener(this::onRestoreButtonClicked);

        Button helpButton = findViewById(R.id.help_button);
        helpButton.setOnClickListener(this::onHelpButtonClicked);

        TextView createdByText = findViewById(R.id.created_by_text);
        createdByText.setOnClickListener(this::onCreatedByTextClicked);

        Configuration.getInstance().load(GridWalkingApplication.getContext(), PreferenceManager.getDefaultSharedPreferences(GridWalkingApplication.getContext()));
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
        Thread thread = new Thread(() -> {
            try {
                URL restoreFileUrl = new URL("https://gill-roxrud.dyndns.org/gridwalking.dat");
                URLConnection connection = restoreFileUrl.openConnection();
                connection.connect();

                try (InputStream is = new BufferedInputStream(restoreFileUrl.openStream());
                     ByteArrayOutputStream os = new ByteArrayOutputStream(connection.getContentLength())) {
                    byte[] data = new byte[10 * 1024];
                    int count;
                    while ((count = is.read(data)) != -1) {
                        os.write(data, 0, count);
                    }
                    os.flush();

                    GameState.getInstance().getDB().RestoreFromFileT(os.toByteArray());
                }
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Restore failed: "+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        thread.start();
    }

    public void onHelpButtonClicked(View v) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GridWalkingApplication.HELP_URL)));
    }

    public void onCreatedByTextClicked(View v) {
        if (++createdByTextClickedCount == 3) {
            showRestoreButton();
        }
    }

    private void showRestoreButton() {
        Button restoreButton = findViewById(R.id.restore_button);
        restoreButton.setVisibility(Button.VISIBLE);
    }

    // START PERMISSION CHECK
    final private int REQUEST_CODE_ASK_MULTIPLE_APP_PERMISSIONS = 124;

    private boolean needsAppPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissions = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (!permissions.isEmpty()) {
                String[] params = permissions.toArray(new String[0]);
                requestPermissions(params, REQUEST_CODE_ASK_MULTIPLE_APP_PERMISSIONS);
                return true;
            } // else: We already have permissions, so handle as normal
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        TextView toastView = findViewById(R.id.toast);
        if (toastView != null) {
            toastView.setText("");
        }

        if (requestCode == REQUEST_CODE_ASK_MULTIPLE_APP_PERMISSIONS) {
            Map<String, Integer> perms = new HashMap<>();
            // Initial
            perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);

            // Fill with results
            for (int i = 0; i < permissions.length; i++)
                perms.put(permissions[i], grantResults[i]);
            // Check for ACCESS_FINE_LOCATION
            if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Permission Denied
                final String msg = "Location permission is required to show the user's location on map.";
                if (toastView != null) {
                    toastView.setText(msg);
                } else {
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}
