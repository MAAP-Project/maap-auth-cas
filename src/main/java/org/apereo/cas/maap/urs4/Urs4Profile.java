package org.apereo.cas.maap.urs4;

import org.pac4j.oauth.profile.OAuth20Profile;

import java.net.URI;

public class Urs4Profile extends OAuth20Profile {
    @Override
    public String getEmail() {
        return (String)getAttribute(Urs4ProfileDefinition.EMAIL_ADDRESS);
    }

    @Override
    public String getFirstName() {
        return (String)getAttribute(Urs4ProfileDefinition.FIRST_NAME);
    }

    @Override
    public String getFamilyName() {
        return (String)getAttribute(Urs4ProfileDefinition.LAST_NAME);
    }

    @Override
    public String getUsername() {
        return (String)getAttribute(Urs4ProfileDefinition.UID);
    }

    @Override
    public URI getProfileUrl() {
        return super.getProfileUrl();
    }
}
