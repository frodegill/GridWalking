package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


public class HighscoreIntentService  extends IntentService {

    private static final String TAG = HighscoreIntentService.class.getSimpleName();

    private static final String GRIDWALKING_ENDPOINT = "https://gill-roxrud.dyndns.org:1416/gridwalking";

    public static final String PENDING_RESULT_EXTRA = "pending_result";
    public static final String RESPONSE_EXTRA       = "response";
    public static final String RESPONSE_MSG_EXTRA   = "msg";


    public HighscoreIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        PendingIntent reply = null;
        try {
            reply = intent.getParcelableExtra(PENDING_RESULT_EXTRA);

            StringBuilder sb = new StringBuilder();

            GridWalkingDBHelper db = GameState.getInstance().getDB();

            sb.append("?guid=");
            sb.append(URLEncoder.encode(db.GetStringProperty(GridWalkingDBHelper.PROPERTY_USER_GUID), "UTF-8"));
            sb.append("&name=");
            sb.append(URLEncoder.encode(db.GetStringProperty(GridWalkingDBHelper.PROPERTY_USER_NAME), "UTF-8"));
            byte i;
            for (i=0; i<Grid.LEVEL_COUNT; i++) {
                sb.append("&l");
                sb.append(Integer.toString(i));
                sb.append('=');
                sb.append(Integer.toString(db.GetLevelCount(i)));
            }
            sb.append("&crc=");
            //TODO

            Intent response = new Intent();
            response.putExtra(RESPONSE_EXTRA, "TODO");

            reply.send(this, GridWalkingApplication.NetworkResponseCode.OK.ordinal(), response);
        } catch (PendingIntent.CanceledException | UnsupportedEncodingException e) {
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
}
