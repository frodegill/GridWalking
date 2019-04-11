package org.dyndns.gill_roxrud.frodeg.gridwalking.network;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.dyndns.gill_roxrud.frodeg.gridwalking.BuildConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

public class HttpsClientCompat6 implements HttpsClient {

    @Override
    public Map<String,Object> httpGet(String urlString) throws IOException {
        HttpGet httpGet = new HttpGet(urlString);
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Grid Walking/"+ BuildConfig.VERSION_NAME);
        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity resEntity = response.getEntity();

        Map<String,Object> result = new HashMap<>();
        result.put(STATUS_INT, response.getStatusLine().getStatusCode());
        result.put(RESPONSE_INPUTSTREAM, resEntity.getContent());
        return result;
    }

    @Override
    public Map<String,Object> httpPost(String urlString, byte[] body) throws IOException {
        HttpPost httpPost = new HttpPost(urlString);
        if (body != null) {
            httpPost.setEntity(new ByteArrayEntity(body));
        }

        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Grid Walking/"+ BuildConfig.VERSION_NAME);
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity resEntity = response.getEntity();

        Map<String,Object> result = new HashMap<>();
        result.put(CONNECTION_OBJECT, httpClient);
        result.put(STATUS_INT, response.getStatusLine().getStatusCode());
        result.put(RESPONSE_INPUTSTREAM, resEntity.getContent());
        return result;
    }

    @Override
    public void disconnect(final Map<String,Object> details) throws IOException {
        InputStream is = (InputStream)details.get(HttpsClient.RESPONSE_INPUTSTREAM);
        if (is != null) {
            is.close();
        }
        OutputStream os = (OutputStream)details.get(HttpsClient.RESPONSE_OUTPUTSTREAM);
        if (os != null) {
            os.close();
        }
    }

}
