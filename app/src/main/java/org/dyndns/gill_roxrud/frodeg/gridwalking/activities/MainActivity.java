package org.dyndns.gill_roxrud.frodeg.gridwalking.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
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
import org.osmdroid.tileprovider.util.StorageUtils;

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
        if (!StorageUtils.isWritable()) {
            Configuration.getInstance().setOsmdroidBasePath(StorageUtils.getStorage());
            Configuration.getInstance().setOsmdroidTileCache(StorageUtils.getStorage());
            if (!StorageUtils.isWritable()) {
                TextView toastView = findViewById(R.id.toast);
                if (toastView != null) {
                    toastView.setText("Storage "+ Configuration.getInstance().getOsmdroidTileCache().getAbsolutePath() + " is NOT writable!");
                }
            }
        }

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

    public void onHelpButtonClicked(View v) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GridWalkingApplication.HELP_URL)));
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

        Button restoreButton = findViewById(R.id.restore_button);
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (!permissions.isEmpty()) {
                String[] params = permissions.toArray(new String[0]);
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
                String[] params = permissions.toArray(new String[0]);
                requestPermissions(params, REQUEST_CODE_ASK_MULTIPLE_RESTORE_PERMISSIONS);
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

        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_APP_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
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
                if (perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // Permission Denied
                    final String msg = "Write External Storage permission is required to store map tile cache.";
                    if (toastView != null) {
                        toastView.setText(msg);
                    } else {
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            }

            case REQUEST_CODE_ASK_MULTIPLE_RESTORE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                // Initial
                perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for READ_EXTERNAL_STORAGE
                boolean permissionGranted = perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

                if (!permissionGranted) {
                    // Permission Denied
                    final String msg = "External Storage permission is required to restore from file.";
                    if (toastView != null) {
                        toastView.setText(msg);
                    } else {
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
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
