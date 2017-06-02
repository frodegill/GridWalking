package org.dyndns.gill_roxrud.frodeg.gridwalking.intents;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import org.dyndns.gill_roxrud.frodeg.gridwalking.GameState;
import org.dyndns.gill_roxrud.frodeg.gridwalking.Grid;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GridWalkingApplication;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GridWalkingDBHelper;
import org.dyndns.gill_roxrud.frodeg.gridwalking.HighscoreItem;
import org.dyndns.gill_roxrud.frodeg.gridwalking.HighscoreList;
import org.dyndns.gill_roxrud.frodeg.gridwalking.networking.Networking;
import org.dyndns.gill_roxrud.frodeg.gridwalking.Secrets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
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
            HttpURLConnection httpConnection = null;
            try {
                db.GetModifiedGrids(dbInTransaction, deletedGrids, newGrids);
                boolean syncGrids = 100L<=gameState.getGrid().getScore();

                String urlString = GRIDWALKING_ENDPOINT+pathParams+URLEncoder.encode(nameParam, "UTF-8").replaceAll("\\+", "%20")
                        +"?crc="+Crc((pathParams+nameParam).getBytes("UTF-8"));

                httpConnection = Networking.prepareConnection(urlString, "POST", syncGrids, true);

                if (syncGrids) {
                    OutputStream outputStream = httpConnection.getOutputStream();
                    generateBody(outputStream, deletedGrids, newGrids);
                    outputStream.close();
                }

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
                    in.close();
                    failed = true;
                    throw new IOException("HTTP "+Integer.toString(status)+": "+sb.toString());
                }

                highscoreList = new HighscoreList();
                InputStream is = httpConnection.getInputStream();
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
                in.close();
            } catch (Exception e) {
                failed = true;
                msg = e.getMessage();
            } finally {
                if (httpConnection != null) {
                    httpConnection.disconnect();
                }
            }

            Intent response = new Intent();
            response.putExtra(RESPONSE_EXTRA, highscoreList);
            if (msg != null) {
                response.putExtra(RESPONSE_MSG_EXTRA, msg);
            }

            reply.send(this,
                    failed ? GridWalkingApplication.NetworkResponseCode.ERROR.ordinal() : GridWalkingApplication.NetworkResponseCode.OK.ordinal(),
                    response);

            db.CommitModifiedGrids(dbInTransaction, deletedGrids, newGrids);

        } catch (Exception e) {
            reportError(reply, GridWalkingApplication.NetworkResponseCode.ERROR.ordinal(), e.getMessage());
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

    private int Crc(final byte[] s) {
        int sum1 = Secrets.CRC_SEED1;
        int sum2 = Secrets.CRC_SEED2;
        int i;
        for (i = 0; i < s.length; i++) { /* https://en.wikipedia.org/wiki/Fletcher's_checksum */
            sum1 = (sum1 + (s[i]&0xFF)) % 255;
            sum2 = (sum2 + sum1) % 255;
        }
        return (sum2 << 8) | sum1;
    }
}
