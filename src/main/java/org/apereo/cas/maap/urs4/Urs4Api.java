package org.apereo.cas.maap.urs4;

import com.github.scribejava.core.httpclient.HttpClient;
import com.github.scribejava.core.httpclient.HttpClientConfig;
import org.pac4j.scribe.builder.api.GenericApi20;

import java.io.OutputStream;

public class Urs4Api extends GenericApi20 {
    public Urs4Api(final String authUrl, final String tokenUrl) {
        super(authUrl, tokenUrl);
    }

    @Override
    public Urs4Service createService(String apiKey, String apiSecret, String callback, String scope,
                                     OutputStream debugStream, String state, String responseType, String userAgent,
                                     HttpClientConfig httpClientConfig, HttpClient httpClient) {
        return new Urs4Service(this, apiKey, apiSecret, callback, scope, state, responseType, userAgent, httpClientConfig, httpClient);
    }
}
