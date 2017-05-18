package org.dyndns.gill_roxrud.frodeg.gridwalking.intents;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;

import org.dyndns.gill_roxrud.frodeg.gridwalking.GameState;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GridWalkingApplication;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GridWalkingDBHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;


public class SyncGridsIntentService extends IntentService {

    private static final String TAG = SyncGridsIntentService.class.getSimpleName();

    //private static final String GRIDWALKING_ENDPOINT = "https://gill-roxrud.dyndns.org:1416";
    private static final String GRIDWALKING_ENDPOINT = "http://10.0.2.2:1416";
    private static final String SYNC_GRIDS_REST_PATH = "/gridwalking/grids/";

    private static final boolean USE_SECURE_CONNECTION = GRIDWALKING_ENDPOINT.startsWith("https://");

    public static final String PARAM_GUID           = "param_guid";
    public static final String PENDING_RESULT_EXTRA = "pending_result";
    public static final String RESPONSE_MSG_EXTRA   = "msg";


    public SyncGridsIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        PendingIntent reply = null;

        boolean failed = false;
        try {
            reply = intent.getParcelableExtra(PENDING_RESULT_EXTRA);
            String guid = intent.getStringExtra(PARAM_GUID);

            GameState gameState = GameState.getInstance();
            GridWalkingDBHelper db = gameState.getDB();

            String pathParams = generatePathParamString(guid);

            String msg = null;
            HttpURLConnection httpConnection = null;
            try {
                URL url = new URL(GRIDWALKING_ENDPOINT+pathParams);

                httpConnection = (HttpURLConnection) url.openConnection();
                if (USE_SECURE_CONNECTION) {
                    HttpsURLConnection httpsConnection = (HttpsURLConnection) httpConnection;

                    SSLContext sc;
                    sc = SSLContext.getInstance("TLS");
                    sc.init(null, null, new java.security.SecureRandom());
                    httpsConnection.setSSLSocketFactory(sc.getSocketFactory());
                }

                httpConnection.setReadTimeout(7000*100);
                httpConnection.setConnectTimeout(7000*100);
                httpConnection.setRequestMethod("GET");
                httpConnection.setDoOutput(false);
                httpConnection.setDoInput(true);

                httpConnection.connect();

                int status = httpConnection.getResponseCode();
                if (status >= 400) {
                    InputStream is = httpConnection.getErrorStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader in = new BufferedReader(isr);
                    String inputLine;
                    StringBuilder sb = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        sb.append(inputLine);
                    }
                    failed = true;
                    throw new IOException("HTTP "+Integer.toString(status)+": "+sb.toString());
                }

                db.SyncExternalGrids(httpConnection.getInputStream());

            } catch (IOException|NoSuchAlgorithmException|KeyManagementException e) {
                failed = true;
                msg = e.getMessage();
            } finally {
                if (httpConnection != null) {
                    httpConnection.disconnect();
                }
            }

            Intent response = new Intent();
            if (msg != null) {
                response.putExtra(RESPONSE_MSG_EXTRA, msg);
            }

            reply.send(this,
                    failed ? GridWalkingApplication.NetworkResponseCode.ERROR.ordinal() : GridWalkingApplication.NetworkResponseCode.OK.ordinal(),
                    response);

        } catch (PendingIntent.CanceledException e) {
            reportError(reply, GridWalkingApplication.NetworkResponseCode.ERROR.ordinal(), e.getMessage());
        }
    }

    private void reportError(PendingIntent reply, int errorCode, final String msg) {
        if (reply != null) {
            try {
                Intent response = new Intent();
                response.putExtra(RESPONSE_MSG_EXTRA, msg);
                reply.send(this, errorCode, response);
            } catch (PendingIntent.CanceledException e) {
            }
        }
    }

    private String generatePathParamString(final String guid) {
        return SYNC_GRIDS_REST_PATH+guid;
    }
}
