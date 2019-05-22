package org.apereo.cas.maap.urs4;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.exceptions.OAuthException;
import com.github.scribejava.core.httpclient.HttpClient;
import com.github.scribejava.core.httpclient.HttpClientConfig;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.utils.Preconditions;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Urs4Service extends OAuth20Service {
    private static final Pattern PROFILE_URL_TOKEN_REGEX_PATTERN = Pattern.compile("\"endpoint\"\\s*:\\s*\"(\\S*?)\"");


    public Urs4Service(DefaultApi20 api, String apiKey, String apiSecret, String callback, String scope,
                       String state, String responseType, String userAgent, HttpClientConfig httpClientConfig,
                       HttpClient httpClient) {
        super(api, apiKey, apiSecret, callback, scope, state, responseType, userAgent, httpClientConfig, httpClient);
    }

    public Response getTokenResponse(String code) throws IOException, InterruptedException, ExecutionException {
        return execute(createAccessTokenRequest(code));
    }

    public OAuth2AccessToken getAccessToken(Response response) throws IOException {
        return getApi().getAccessTokenExtractor().extract(response);
    }

    public String getProfileUrlToken(Response response) throws IOException, OAuthException {
        return extractProfileUrlFromResponse(response);
    }

    private String extractProfileUrlFromResponse(Response response) throws IOException, OAuthException {
        final String body = response.getBody();
        Preconditions.checkEmptyString(body, "Response body is incorrect. Can't extract a token from an empty string");
        final Matcher matcher = PROFILE_URL_TOKEN_REGEX_PATTERN.matcher(body);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new OAuthException("Cant' find Profile URL from response:" + response, null);
        }
    }
}
