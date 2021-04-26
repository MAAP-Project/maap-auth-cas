package org.apereo.cas.maap.urs4;

import com.github.scribejava.core.exceptions.OAuthException;
import lombok.extern.slf4j.Slf4j;
import org.pac4j.oauth.client.OAuth20Client;

@Slf4j
public class Urs4Client extends OAuth20Client<Urs4Profile> {

    private String url4_authurl;
    private String urs4_tokenurl;
    private String urs4_uid;
    private String syncope_user;
    private String syncope_password;
    private String syncope_url;
    private String syncope_email_whitelist;
    private String gitlab_user;
    private String gitlab_password;
    private String gitlab_url;

    public Urs4Client(
            String urs4_key,
            String urs4_secret,
            String urs4_authurl,
            String urs4_tokenurl,
            String urs4_uid,
            String syncope_user,
            String syncope_password,
            String syncope_url,
            String syncope_email_whitelist,
            String gitlab_user,
            String gitlab_password,
            String gitlab_url) throws OAuthException {
        this.url4_authurl = urs4_authurl;
        this.urs4_tokenurl = urs4_tokenurl;
        this.urs4_uid = urs4_uid;
        this.syncope_user = syncope_user;
        this.syncope_password = syncope_password;
        this.syncope_url = syncope_url;
        this.syncope_email_whitelist = syncope_email_whitelist;
        this.gitlab_user = gitlab_user;
        this.gitlab_password = gitlab_password;
        this.gitlab_url = gitlab_url;

        setKey(urs4_key);
        setSecret(urs4_secret);
    }

    @Override
    protected void clientInit() {
        configuration.setApi(new Urs4Api(url4_authurl, urs4_tokenurl));
        Urs4ProfileDefinition profileDefinition = new Urs4ProfileDefinition();
        profileDefinition.setSyncopeEmailWhitelist(syncope_email_whitelist);
        profileDefinition.setSyncopeUser(syncope_user);
        profileDefinition.setSyncopePassword(syncope_password);
        profileDefinition.setSyncopeUrl(syncope_url);
        profileDefinition.setGitLabUser(gitlab_user);
        profileDefinition.setGitLabPassword(gitlab_password);
        profileDefinition.setGitLabUrl(gitlab_url);
        configuration.setProfileDefinition(profileDefinition);
        configuration.getCustomParams().put("urs4_uid", urs4_uid);
        configuration.getCustomParams().put("urs4_key", this.getKey());
 	super.clientInit();
        setAuthenticator(new Urs4Authenticator(configuration, this));
    }
}
