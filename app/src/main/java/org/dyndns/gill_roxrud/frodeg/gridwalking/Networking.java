package org.dyndns.gill_roxrud.frodeg.gridwalking;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;



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
