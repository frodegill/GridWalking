package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;


public class HighscoreIntentService  extends IntentService {

    private static final String TAG = HighscoreIntentService.class.getSimpleName();

    private static final String GRIDWALKING_ENDPOINT = "https://gill-roxrud.dyndns.org:1416";
    private static final String HIGHSCORE_REST_PATH = "/gridwalking/highscore/";

    public static final String PENDING_RESULT_EXTRA = "pending_result";
    public static final String RESPONSE_EXTRA       = "response";
    public static final String RESPONSE_MSG_EXTRA   = "msg";


    public HighscoreIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        PendingIntent reply = null;
        HighscoreList highscoreList = null;
        try {
            reply = intent.getParcelableExtra(PENDING_RESULT_EXTRA);

            GameState gameState = GameState.getInstance();
            GridWalkingDBHelper db = gameState.getDB();

            StringBuilder sb = new StringBuilder();
            sb.append(HIGHSCORE_REST_PATH);
            sb.append(db.GetStringProperty(GridWalkingDBHelper.PROPERTY_USER_GUID));
            byte i;
            for (i=Grid.LEVEL_COUNT-1; i>=0; i--) {
                sb.append('/');
                sb.append(Integer.toString(db.GetLevelCount(i)));
            }
            sb.append('/');
            String pathParams = sb.toString();
            String nameParam = gameState.getHighscoreNickname();

            boolean failed = false;
            HttpsURLConnection httpsConnection = null;
            try {
                URL url = new URL(GRIDWALKING_ENDPOINT+pathParams+URLEncoder.encode(nameParam, "UTF-8").replaceAll("\\+", "%20")
                                 +"?crc="+Crc((pathParams+nameParam).getBytes("UTF-8")));

                httpsConnection = (HttpsURLConnection) url.openConnection();

                SSLContext sc;
                sc = SSLContext.getInstance("TLS");
                sc.init(null, null, new java.security.SecureRandom());
                httpsConnection.setSSLSocketFactory(sc.getSocketFactory());

                httpsConnection.setReadTimeout(7000);
                httpsConnection.setConnectTimeout(7000);
                httpsConnection.setRequestMethod("POST");
                httpsConnection.setDoInput(true);

                httpsConnection.connect();

                int status = httpsConnection.getResponseCode();
                if (status >= 400) {
                    InputStream is = httpsConnection.getErrorStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader in = new BufferedReader(isr);
                    String inputLine;
                    sb = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        sb.append(inputLine);
                    }
                    throw new IOException("HTTP 406: "+sb.toString());
                }

                highscoreList = new HighscoreList();
                InputStream is = httpsConnection.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader in = new BufferedReader(isr);
                boolean first = true;
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (first) {
                        highscoreList.setPosition(inputLine);
                        first = false;
                    } else {
                        HighscoreItem highscoreItem = new HighscoreItem();
                        highscoreItem.setItem(inputLine);
                        highscoreList.getHighscoreItemList().add(highscoreItem);
                    }
                }
            } catch (IOException|NoSuchAlgorithmException|KeyManagementException e) {
                failed = true;
            } finally {
                if (httpsConnection != null) {
                    httpsConnection.disconnect();
                }
            }

            Intent response = new Intent();
            response.putExtra(RESPONSE_EXTRA, highscoreList);

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

    private int Crc(final byte[] s) {
        int sum1 = Secrets.CRC_SEED1;
        int sum2 = Secrets.CRC_SEED2;
        int i;
        for (i = 0; i < s.length; i++) { /* https://en.wikipedia.org/wiki/Fletcher's_checksum */
            sum1 = (sum1 + s[i]) % 255;
            sum2 = (sum2 + sum1) % 255;
        }
        return (sum2 << 8) | sum1;
    }
}
