package org.dyndns.gill_roxrud.frodeg.gridwalking.network;

import org.dyndns.gill_roxrud.frodeg.gridwalking.BuildConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class HttpsClientCompat7 implements HttpsClient {

    @Override
    public Map<String,Object> httpGet(String urlString) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        URL url = new URL(urlString);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        if (urlString.startsWith("https://")) {
            HttpsURLConnection httpsConnection = (HttpsURLConnection) httpConnection;

            SSLContext sc;
            sc = SSLContext.getInstance("TLS");
            sc.init(null, null, new java.security.SecureRandom());
            httpsConnection.setSSLSocketFactory(sc.getSocketFactory());
        }

        httpConnection.setReadTimeout(30*1000);
        httpConnection.setConnectTimeout(30*1000);
        httpConnection.setRequestMethod("GET");
        httpConnection.setRequestProperty("Connection", "close");
        httpConnection.setUseCaches(false);
        httpConnection.addRequestProperty("User-Agent", "Grid Walking/"+ BuildConfig.VERSION_NAME);
        httpConnection.setDoOutput(false);
        httpConnection.setDoInput(true);

        Map<String,Object> result = new HashMap<>();
        result.put(CONNECTION_OBJECT, httpConnection);
        result.put(STATUS_INT, httpConnection.getResponseCode());
        result.put(RESPONSE_INPUTSTREAM, httpConnection.getResponseCode() >= 400 ? httpConnection.getErrorStream() : httpConnection.getInputStream());

        return result;
    }

    @Override
    public Map<String,Object> httpPost(String urlString, byte[] body) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        URL url = new URL(urlString);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        if (urlString.startsWith("https://")) {
            HttpsURLConnection httpsConnection = (HttpsURLConnection) httpConnection;

            SSLContext sc;
            sc = SSLContext.getInstance("TLS");
            sc.init(null, null, new java.security.SecureRandom());
            httpsConnection.setSSLSocketFactory(sc.getSocketFactory());
        }

        httpConnection.setReadTimeout(7*1000);
        httpConnection.setConnectTimeout(7*1000);
        httpConnection.setRequestMethod("POST");
        httpConnection.setRequestProperty("Connection", "close");
        httpConnection.setUseCaches(false);
        httpConnection.addRequestProperty("User-Agent", "Grid Walking/"+ BuildConfig.VERSION_NAME);
        httpConnection.setDoInput(true);

        OutputStream outputStream = null;
        if (body != null) {
            httpConnection.setDoOutput(true);
            outputStream = httpConnection.getOutputStream();
            outputStream.write(body);
            outputStream.flush();
        }

        Map<String,Object> result = new HashMap<>();
        result.put(CONNECTION_OBJECT, httpConnection);
        result.put(STATUS_INT, httpConnection.getResponseCode());
        result.put(RESPONSE_OUTPUTSTREAM, outputStream);
        result.put(RESPONSE_INPUTSTREAM, httpConnection.getResponseCode() >= 400 ? httpConnection.getErrorStream() : httpConnection.getInputStream());

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
        HttpURLConnection connection = (HttpURLConnection)details.get(HttpsClient.CONNECTION_OBJECT);
        if (connection != null) {
            connection.disconnect();
        }
    }

}
