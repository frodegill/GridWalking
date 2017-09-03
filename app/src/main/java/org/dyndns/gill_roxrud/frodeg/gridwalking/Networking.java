package org.dyndns.gill_roxrud.frodeg.gridwalking;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import javax.net.ssl.SSLContext;

import static org.apache.http.conn.ssl.SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;


public class Networking {

    private static Networking instance = null;


    public static Networking getInstance() {
        if (instance == null) {
            instance = new Networking();
        }
        return instance;
    }

    public HttpClient createHttpClient() {

        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Grid Walking/"+ BuildConfig.VERSION_NAME);
        return httpClient;
    }
}
