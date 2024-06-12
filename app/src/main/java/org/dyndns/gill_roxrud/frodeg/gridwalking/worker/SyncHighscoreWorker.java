package org.dyndns.gill_roxrud.frodeg.gridwalking.worker;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.dyndns.gill_roxrud.frodeg.gridwalking.GameState;
import org.dyndns.gill_roxrud.frodeg.gridwalking.Grid;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GridWalkingApplication;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GridWalkingDBHelper;
import org.dyndns.gill_roxrud.frodeg.gridwalking.HighscoreItem;
import org.dyndns.gill_roxrud.frodeg.gridwalking.HighscoreList;
import org.dyndns.gill_roxrud.frodeg.gridwalking.Secrets;
import org.dyndns.gill_roxrud.frodeg.gridwalking.activities.HighscoreActivity;
import org.dyndns.gill_roxrud.frodeg.gridwalking.activities.MapActivity;
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

public class SyncHighscoreWorker extends Worker {

    private static final String GRIDWALKING_ENDPOINT = "https://gill-roxrud.dyndns.org:1416";
    private static final String SYNC_HIGHSCORE_REST_PATH = "/gridwalking/sync/";

    public SyncHighscoreWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        final HighscoreList highscoreList = new HighscoreList();
        GridWalkingDBHelper db = null;
        SQLiteDatabase dbInTransaction = null;
        boolean failed = false;
        try {
            GameState gameState = GameState.getInstance();
            db = gameState.getDB();
            dbInTransaction = db.StartTransaction();

            String pathParams = generatePathParamString(db);
            String nameParam = gameState.getHighscoreNickname();

            Set<Integer> deletedGrids = new TreeSet<>();
            ArrayList<Set<Integer>> newGrids = new ArrayList<>();
            byte level;
            for (level=0; level< Grid.LEVEL_COUNT; level++) {
                newGrids.add(new TreeSet<>());
            }

            String errorMsg = null;
            HttpsClient httpsClient = null;
            Map<String,Object> result = null;
            BufferedReader in = null;
            try {
                db.GetModifiedGrids(dbInTransaction, deletedGrids, newGrids);
                boolean syncGrids = 10L<=gameState.getGrid().getScore();

                Secrets secrets = new Secrets();
                secrets.Append((pathParams+nameParam).getBytes(StandardCharsets.UTF_8));
                String urlString = GRIDWALKING_ENDPOINT+pathParams+ URLEncoder.encode(nameParam, "UTF-8").replaceAll("\\+", "%20")
                        +"?crc="+ secrets.Crc16();

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
                InputStreamReader isr = new InputStreamReader(is);
                in = new BufferedReader(isr);
                if (statusCode >= 400) {
                    String inputLine;
                    StringBuilder sb = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        sb.append(inputLine);
                    }
                    failed = true;
                    throw new IOException("HTTP "+ statusCode +": "+sb);
                } else {
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
                errorMsg = e.getMessage();
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (httpsClient != null && result != null) {
                        httpsClient.disconnect(result);
                    }
                } catch (Exception e) {
                }
            }

            final String finalErrorMsg = errorMsg;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (finalErrorMsg == null) {
                    Intent intent = new Intent(GridWalkingApplication.getContext(), HighscoreActivity.class);
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(HighscoreActivity.HIGHSCORE_LIST, highscoreList);
                    GridWalkingApplication.getContext().startActivity(intent);
                } else {
                    Toast.makeText(GridWalkingApplication.getContext(), "Syncing highscore failed: " + finalErrorMsg, Toast.LENGTH_LONG).show();
                }
            });

            db.CommitModifiedGrids(dbInTransaction, deletedGrids, newGrids);

        } catch (Exception e) {
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(GridWalkingApplication.getContext(), "Syncing highscore failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        } finally {
            if (db!=null && dbInTransaction!=null) {
                db.EndTransaction(dbInTransaction, !failed);
            }
        }

        return Result.success();
   }

    private String generatePathParamString(final GridWalkingDBHelper db) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYNC_HIGHSCORE_REST_PATH);
        sb.append(db.GetStringProperty(GridWalkingDBHelper.PROPERTY_USER_GUID));
        sb.append('/');
        sb.append(db.GetUnusedBonusCount());
        byte i;
        for (i=Grid.LEVEL_COUNT-1; i>=0; i--) {
            sb.append('/');
            sb.append(db.GetLevelCount(i));
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
