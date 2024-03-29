package org.dyndns.gill_roxrud.frodeg.gridwalking.network;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public interface HttpsClient {

    enum RequestCode {
        SYNC_HIGHSCORE
    }

    enum NetworkResponseCode {
        OK,
        ERROR
    }

    String STATUS_INT            = "status";
    String RESPONSE_INPUTSTREAM  = "input";
    String RESPONSE_OUTPUTSTREAM = "output";
    String CONNECTION_OBJECT     = "connection";

    Map<String,Object> httpGet(final String urlString) throws IOException, NoSuchAlgorithmException, KeyManagementException;
    Map<String,Object> httpPost(final String urlString, final byte[] body) throws IOException, NoSuchAlgorithmException, KeyManagementException;
    void disconnect(final Map<String,Object> details) throws IOException;

}
