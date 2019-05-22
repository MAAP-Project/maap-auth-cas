package org.apereo.cas.maap.urs4;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.Response;
import lombok.val;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.HttpCommunicationException;
import org.pac4j.oauth.config.OAuth20Configuration;
import org.pac4j.oauth.credentials.OAuth20Credentials;
import org.pac4j.oauth.credentials.OAuthCredentials;
import org.pac4j.oauth.credentials.authenticator.OAuth20Authenticator;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class Urs4Authenticator extends OAuth20Authenticator {

    public Urs4Authenticator(final OAuth20Configuration configuration, final IndirectClient client) {
        super(configuration, client);
    }

    @Override
    protected void retrieveAccessToken(final WebContext context, final OAuthCredentials credentials) {
        OAuth20Credentials oAuth20Credentials = (OAuth20Credentials) credentials;
        // no request token saved in context and no token (OAuth v2.0)
        final String code = oAuth20Credentials.getCode();
        logger.debug("code: {}", code);
        final OAuth2AccessToken accessToken;
        final String profileUrl;

        try {
            val service = (Urs4Service) this.configuration.buildService(context, client, null);
            Response response = service.getTokenResponse(code);

            accessToken = service.getAccessToken(response);
            profileUrl = new URL(new URL(((Urs4Api) this.configuration.getApi()).getAccessTokenEndpoint()), service.getProfileUrlToken(response)).toString();
            ((Urs4ProfileDefinition) this.configuration.getProfileDefinition()).setProfileUrl(profileUrl);
        } catch (final IOException | InterruptedException | ExecutionException e) {
            throw new HttpCommunicationException("Error getting token:" + e.getMessage());
        }
        logger.debug("accessToken: {}", accessToken);
        oAuth20Credentials.setAccessToken(accessToken);
    }
}
