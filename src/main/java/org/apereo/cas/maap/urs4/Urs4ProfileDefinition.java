package org.apereo.cas.maap.urs4;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.scribejava.core.model.OAuth2AccessToken;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.pac4j.core.profile.AttributeLocation;
import org.pac4j.core.profile.ProfileHelper;
import org.pac4j.core.profile.converter.Converters;
import org.pac4j.oauth.config.OAuth20Configuration;
import org.pac4j.oauth.profile.JsonHelper;
import org.pac4j.oauth.profile.definition.OAuth20ProfileDefinition;
import org.pac4j.oidc.profile.OidcProfileDefinition;

import java.util.Arrays;

@Slf4j
public class Urs4ProfileDefinition extends OAuth20ProfileDefinition<Urs4Profile, OAuth20Configuration> {
    public static final String UID = "uid";
    public static final String EMAIL_ADDRESS = "email_address";
    public static final String FIRST_NAME = "first_name";
    public static final String LAST_NAME = "last_name";
    public static final String STUDY_AREA = "study_area";
    public static final String ORGANIZATION = "organization";
    public static final String AFFILIATION = "affiliation";

    @Setter
    private String profileUrl;

    public Urs4ProfileDefinition() {
        super(x -> new Urs4Profile());
        Arrays.stream(new String[]{UID, EMAIL_ADDRESS, FIRST_NAME, LAST_NAME}).forEach(a -> primary(a, Converters.STRING));
        setProfileId(EMAIL_ADDRESS);
    }

    @Override
    public String getProfileUrl(OAuth2AccessToken oAuth2AccessToken, OAuth20Configuration oAuth20Configuration) {
        return profileUrl;
    }

    @Override
    public Urs4Profile extractUserProfile(final String body) {
        final Urs4Profile profile = newProfile();
        final JsonNode json = JsonHelper.getFirstNode(body);
        if (json != null) {
            profile.setId(ProfileHelper.sanitizeIdentifier(profile, JsonHelper.getElement(json, EMAIL_ADDRESS)));
//            for (final String attribute : getPrimaryAttributes()) {
//                convertAndAdd(profile, AttributeLocation.PROFILE_ATTRIBUTE, attribute, JsonHelper.getElement(json, attribute));
//            }

            // OIDC Attributes
            convertAndAdd(profile, AttributeLocation.PROFILE_ATTRIBUTE, OidcProfileDefinition.PREFERRED_USERNAME, JsonHelper.getElement(json, UID));
            convertAndAdd(profile, AttributeLocation.PROFILE_ATTRIBUTE, OidcProfileDefinition.GIVEN_NAME, JsonHelper.getElement(json, FIRST_NAME));
            convertAndAdd(profile, AttributeLocation.PROFILE_ATTRIBUTE, OidcProfileDefinition.FAMILY_NAME, JsonHelper.getElement(json, LAST_NAME));
            convertAndAdd(profile, AttributeLocation.PROFILE_ATTRIBUTE, OidcProfileDefinition.NAME, JsonHelper.getElement(json, FIRST_NAME) +  " " + JsonHelper.getElement(json, LAST_NAME));
            convertAndAdd(profile, AttributeLocation.PROFILE_ATTRIBUTE, OidcProfileDefinition.EMAIL, JsonHelper.getElement(json, EMAIL_ADDRESS));
            convertAndAdd(profile, AttributeLocation.PROFILE_ATTRIBUTE, STUDY_AREA, JsonHelper.getElement(json, STUDY_AREA));
            convertAndAdd(profile, AttributeLocation.PROFILE_ATTRIBUTE, ORGANIZATION, JsonHelper.getElement(json, ORGANIZATION));
            convertAndAdd(profile, AttributeLocation.PROFILE_ATTRIBUTE, AFFILIATION, JsonHelper.getElement(json, AFFILIATION));

        } else {
            raiseProfileExtractionJsonError(body);
        }
        return profile;
    }
}