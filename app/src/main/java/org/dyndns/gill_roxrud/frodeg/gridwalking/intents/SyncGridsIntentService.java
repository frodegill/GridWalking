package org.dyndns.gill_roxrud.frodeg.gridwalking.intents;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.dyndns.gill_roxrud.frodeg.gridwalking.BuildConfig;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GameState;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GridWalkingApplication;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GridWalkingDBHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class SyncGridsIntentService extends IntentService {

    private static final String TAG = SyncGridsIntentService.class.getSimpleName();

    private static final String GRIDWALKING_ENDPOINT = "https://gill-roxrud.dyndns.org:1416";
    //private static final String GRIDWALKING_ENDPOINT = "http://10.0.2.2:1416";
    private static final String SYNC_GRIDS_REST_PATH = "/gridwalking/grids/";

    public static final String PARAM_GUID           = "param_guid";
    public static final String PENDING_RESULT_EXTRA = "pending_result";
    public static final String RESPONSE_EXTRA       = "org.dyndns.gill_roxrud.frodeg.gridwalking.response";
    public static final String RESPONSE_MSG_EXTRA   = "org.dyndns.gill_roxrud.frodeg.gridwalking.msg";


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

            Integer aGrid = null;
            String msg = null;
            try {
                String urlString = GRIDWALKING_ENDPOINT+pathParams;

                HttpClient httpClient = new DefaultHttpClient();
                httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Grid Walking/"+ BuildConfig.VERSION_NAME);
                HttpGet httpGet = new HttpGet(urlString);

                HttpResponse response = httpClient.execute(httpGet);
                HttpEntity resEntity = response.getEntity();
                InputStream is = resEntity.getContent();

                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 400) {
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader in = new BufferedReader(isr);
                    String inputLine;
                    StringBuilder sb = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        sb.append(inputLine);
                    }
                    in.close();
                    throw new IOException("HTTP "+Integer.toString(statusCode)+": "+sb.toString());
                }

                aGrid = db.SyncExternalGridsT(is);
                is.close();
            } catch (Exception e) {
                failed = true;
                msg = e.getMessage();
            }

            Intent response = new Intent();
            if (aGrid != null) {
                response.putExtra(RESPONSE_EXTRA, aGrid.intValue());
            }
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
