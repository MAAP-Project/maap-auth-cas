package org.apereo.cas.maap.urs4;

import com.github.scribejava.core.exceptions.OAuthException;
import lombok.extern.slf4j.Slf4j;
import org.pac4j.oauth.client.OAuth20Client;

@Slf4j
public class Urs4Client extends OAuth20Client<Urs4Profile> {

    private String url4_authurl;
    private String urs4_tokenurl;

    public Urs4Client(String urs4_key, String urs4_secret, String urs4_authurl, String urs4_tokenurl) throws OAuthException {
        this.url4_authurl = urs4_authurl;
        this.urs4_tokenurl = urs4_tokenurl;

        setKey(urs4_key);
        setSecret(urs4_secret);
    }

    @Override
    protected void clientInit() {
        configuration.setApi(new Urs4Api(url4_authurl, urs4_tokenurl));
        configuration.setProfileDefinition(new Urs4ProfileDefinition());
        super.clientInit();
        setAuthenticator(new Urs4Authenticator(configuration, this));
    }
}
