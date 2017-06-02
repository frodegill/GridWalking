package org.dyndns.gill_roxrud.frodeg.gridwalking.networking;

import org.dyndns.gill_roxrud.frodeg.gridwalking.BuildConfig;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GameState;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;


public class Networking {

    public static HttpURLConnection prepareConnection(final String urlString, final String requestMethod, final boolean doOutput, final boolean doInput) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        boolean useSecureConnection = urlString.startsWith("https://");
        URL url = new URL(urlString);

        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        if (useSecureConnection) {
            HttpsURLConnection httpsConnection = (HttpsURLConnection) httpConnection;
            httpsConnection.setSSLSocketFactory(new TLSSocketFactory());
        }

        httpConnection.setReadTimeout(30*1000);
        httpConnection.setConnectTimeout(1*1000);
        httpConnection.setRequestMethod(requestMethod);
        httpConnection.setRequestProperty("Connection", "close");
        httpConnection.setRequestProperty("User-Agent", "Grid Walking "+ BuildConfig.VERSION_NAME);
        httpConnection.setDoOutput(doOutput);
        httpConnection.setDoInput(doInput);

        return httpConnection;
    }

}
