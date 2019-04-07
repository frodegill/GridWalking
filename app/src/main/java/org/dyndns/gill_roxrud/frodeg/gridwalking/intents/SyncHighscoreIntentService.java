package org.dyndns.gill_roxrud.frodeg.gridwalking.intents;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import org.dyndns.gill_roxrud.frodeg.gridwalking.GameState;
import org.dyndns.gill_roxrud.frodeg.gridwalking.Grid;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GridWalkingDBHelper;
import org.dyndns.gill_roxrud.frodeg.gridwalking.HighscoreItem;
import org.dyndns.gill_roxrud.frodeg.gridwalking.HighscoreList;
import org.dyndns.gill_roxrud.frodeg.gridwalking.Secrets;
import org.dyndns.gill_roxrud.frodeg.gridwalking.network.HttpsClient;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class SyncHighscoreIntentService extends IntentService {

    private static final String TAG = SyncHighscoreIntentService.class.getSimpleName();

    private static final String GRIDWALKING_ENDPOINT = "https://gill-roxrud.dyndns.org:1416";
    //private static final String GRIDWALKING_ENDPOINT = "http://10.0.2.2:1416";
    private static final String SYNC_HIGHSCORE_REST_PATH = "/gridwalking/sync/";

    public static final String PENDING_RESULT_EXTRA = "pending_result";
    public static final String RESPONSE_EXTRA       = "org.dyndns.gill_roxrud.frodeg.gridwalking.response";
    public static final String RESPONSE_MSG_EXTRA   = "org.dyndns.gill_roxrud.frodeg.gridwalking.msg";


    public SyncHighscoreIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        PendingIntent reply = null;
        HighscoreList highscoreList = null;

        GridWalkingDBHelper db = null;
        SQLiteDatabase dbInTransaction = null;
        boolean failed = false;
        try {
            reply = intent.getParcelableExtra(PENDING_RESULT_EXTRA);

            GameState gameState = GameState.getInstance();
            db = gameState.getDB();
            dbInTransaction = db.StartTransaction();

            String pathParams = generatePathParamString(db);
            String nameParam = gameState.getHighscoreNickname();

            Set<Integer> deletedGrids = new TreeSet<>();
            ArrayList<Set<Integer>> newGrids = new ArrayList<>();
            byte level;
            for (level=0; level<Grid.LEVEL_COUNT; level++) {
                newGrids.add(new TreeSet<Integer>());
            }

            String msg = null;
            HttpsClient httpsClient = null;
            Map<String,Object> result = null;
            BufferedReader in = null;
            try {
                db.GetModifiedGrids(dbInTransaction, deletedGrids, newGrids);
                boolean syncGrids = 10L<=gameState.getGrid().getScore();

                Secrets secrets = new Secrets();
                secrets.Append((pathParams+nameParam).getBytes(StandardCharsets.UTF_8));
                String urlString = GRIDWALKING_ENDPOINT+pathParams+URLEncoder.encode(nameParam, "UTF-8").replaceAll("\\+", "%20")
                        +"?crc="+Integer.toString(secrets.Crc16());

                httpsClient = GameState.getInstance().getHttpsClient();
                byte[] body = null;
                if (syncGrids) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    generateBody(baos, deletedGrids, newGrids);
                    body = baos.toByteArray();
                }
                result = httpsClient.httpPost(urlString, body);
                int statusCode = (int)result.get(HttpsClient.STATUS_INT);
                InputStream is = (InputStream)result.get(HttpsClient.RESPONSE_INPUTSTREAM);
                if (statusCode >= 400) {
                    InputStreamReader isr = new InputStreamReader(is);
                    in = new BufferedReader(isr);
                    String inputLine;
                    StringBuilder sb = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        sb.append(inputLine);
                    }
                    httpsClient.disconnect(result.get(HttpsClient.CONNECTION_OBJECT));
                    failed = true;
                    throw new IOException("HTTP "+Integer.toString(statusCode)+": "+sb.toString());
                } else {
                    highscoreList = new HighscoreList();
                    InputStreamReader isr = new InputStreamReader(is);
                    in = new BufferedReader(isr);
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
                }
            } catch (Exception e) {
                failed = true;
                msg = e.getMessage();
            } finally {
                if (in != null) {
                    in.close();
                }
                if (httpsClient!=null && result!= null && result.containsKey(HttpsClient.CONNECTION_OBJECT))
                {
                    httpsClient.disconnect(result.get(HttpsClient.CONNECTION_OBJECT));
                }
            }

            Intent response = new Intent();
            response.putExtra(RESPONSE_EXTRA, highscoreList);
            if (msg != null) {
                response.putExtra(RESPONSE_MSG_EXTRA, msg);
            }

            reply.send(this,
                    failed ? HttpsClient.NetworkResponseCode.ERROR.ordinal() : HttpsClient.NetworkResponseCode.OK.ordinal(),
                    response);

            db.CommitModifiedGrids(dbInTransaction, deletedGrids, newGrids);

        } catch (Exception e) {
            reportError(reply, HttpsClient.NetworkResponseCode.ERROR.ordinal(), e.getMessage());
        } finally {
            if (db!=null && dbInTransaction!=null) {
                db.EndTransaction(dbInTransaction, !failed);
            }
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

    private String generatePathParamString(final GridWalkingDBHelper db) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYNC_HIGHSCORE_REST_PATH);
        sb.append(db.GetStringProperty(GridWalkingDBHelper.PROPERTY_USER_GUID));
        sb.append('/');
        sb.append(Integer.toString(db.GetUnusedBonusCount()));
        byte i;
        for (i=Grid.LEVEL_COUNT-1; i>=0; i--) {
            sb.append('/');
            sb.append(Integer.toString(db.GetLevelCount(i)));
        }
        sb.append('/');
        return sb.toString();
    }

    private void generateBody(final OutputStream outputStream, final Set<Integer> deletedGrids, final ArrayList<Set<Integer>> newGrids) throws IOException {
        appendList(outputStream, deletedGrids);

        byte level;
        for (level=0; level<Grid.LEVEL_COUNT; level++) {
            appendList(outputStream, newGrids.get(level));
        }
    }

    private void appendList(final OutputStream outputStream, Set<Integer> list) throws IOException {
        for (Integer listItem : list) {
            appendInt32(outputStream, listItem);
        }
        appendInt32(outputStream, 0xFFFFFFFF);
    }

    private void appendInt32(final OutputStream outputStream, int value) throws IOException {
        outputStream.write((value&0xFF000000)>>24);
        outputStream.write((value&0x00FF0000)>>16);
        outputStream.write((value&0x0000FF00)>>8);
        outputStream.write(value&0x000000FF);
    }

}
