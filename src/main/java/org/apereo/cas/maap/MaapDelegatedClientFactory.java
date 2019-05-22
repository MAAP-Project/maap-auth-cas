package org.apereo.cas.maap;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apereo.cas.configuration.model.support.custom.CasCustomProperties;
import org.apereo.cas.configuration.model.support.pac4j.Pac4jDelegatedAuthenticationProperties;
import org.apereo.cas.maap.urs4.Urs4Client;
import org.apereo.cas.support.pac4j.authentication.DelegatedClientFactory;
import org.pac4j.core.client.BaseClient;

import java.util.Collection;
import java.util.Set;

@Slf4j
public class MaapDelegatedClientFactory extends DelegatedClientFactory {
    private CasCustomProperties customProperties;

    public MaapDelegatedClientFactory(Pac4jDelegatedAuthenticationProperties pac4jProperties, CasCustomProperties customProperties) {
        super(pac4jProperties);
        this.customProperties = customProperties;
    }

    protected void configureUrs4Client(final Collection<BaseClient> properties) {
        String urs4_key = customProperties.getProperties().get("urs4_key");
        String urs4_secret = customProperties.getProperties().get("urs4_secret");
        String urs4_authurl = customProperties.getProperties().get("urs4_authurl");
        String urs4_tokenurl = customProperties.getProperties().get("urs4_tokenurl");
        String urs4_autoredirect = customProperties.getProperties().get("urs4_autoredirect");

        if (urs4_key != null && urs4_secret != null && urs4_authurl != null && urs4_tokenurl != null) {
            val client = new Urs4Client(urs4_key, urs4_secret, urs4_authurl, urs4_tokenurl);
            client.setName("URS");
            //LOGGER.debug("Created client [{}] with identifier [{}]", client.getName(), client.getKey());
            properties.add(client);
            if (urs4_autoredirect != null) {
                client.getCustomProperties().put("autoRedirect", Boolean.parseBoolean(urs4_autoredirect));
            }
        } else {
            //LOGGER.error("URS4 properties incomplete, not enabling URS4 authentication");
        }

    }

    /**
     * Build set of clients configured.
     *
     * @return the set
     */
    public Set<BaseClient> build() {
        val clients = super.build();
        configureUrs4Client(clients);
        return clients;
    }
}
