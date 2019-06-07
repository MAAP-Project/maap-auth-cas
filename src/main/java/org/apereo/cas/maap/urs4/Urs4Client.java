package org.apereo.cas.maap.urs4;

import com.github.scribejava.core.exceptions.OAuthException;
import lombok.extern.slf4j.Slf4j;
import org.pac4j.oauth.client.OAuth20Client;

@Slf4j
public class Urs4Client extends OAuth20Client<Urs4Profile> {

    private String url4_authurl;
    private String urs4_tokenurl;
    private String urs4_uid;

    public Urs4Client(String urs4_key, String urs4_secret, String urs4_authurl, String urs4_tokenurl, String urs4_uid) throws OAuthException {
        this.url4_authurl = urs4_authurl;
        this.urs4_tokenurl = urs4_tokenurl;
        this.urs4_uid = urs4_uid;

        setKey(urs4_key);
        setSecret(urs4_secret);
    }

    @Override
    protected void clientInit() {
        configuration.setApi(new Urs4Api(url4_authurl, urs4_tokenurl));
        configuration.setProfileDefinition(new Urs4ProfileDefinition());
        configuration.getCustomParams().put("urs4_uid", urs4_uid);
        configuration.getCustomParams().put("urs4_key", this.getKey());
 	super.clientInit();
        setAuthenticator(new Urs4Authenticator(configuration, this));
    }
}
