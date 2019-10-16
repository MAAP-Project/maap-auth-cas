package org.apereo.cas.maap.urs4;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.scribejava.core.model.OAuth2AccessToken;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.pac4j.core.profile.AttributeLocation;
import org.pac4j.core.profile.ProfileHelper;
import org.pac4j.core.profile.converter.Converters;
import org.pac4j.oauth.config.OAuth20Configuration;
import org.pac4j.oauth.profile.JsonHelper;
import org.pac4j.oauth.profile.definition.OAuth20ProfileDefinition;
import org.pac4j.oidc.profile.OidcProfileDefinition;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Arrays;
import java.io.*;

@Slf4j
public class Urs4ProfileDefinition extends OAuth20ProfileDefinition<Urs4Profile, OAuth20Configuration> {
    private final ObjectMapper jacksonObjectMapper = new ObjectMapper().findAndRegisterModules();
    private final String DEFAULT_PASSWORD = "HELLO123.";

    public static final String UID = "uid";
    public static final String EMAIL_ADDRESS = "email_address";
    public static final String FIRST_NAME = "first_name";
    public static final String LAST_NAME = "last_name";
    public static final String STUDY_AREA = "study_area";
    public static final String ORGANIZATION = "organization";
    public static final String AFFILIATION = "affiliation";

    @Setter
    public String profileUrl;

    private String syncopeEmailWhitelist;
    private String syncopeUser;
    private String syncopePassword;
    private String syncopeUrl;

    public String getSyncopeUser() {
        return syncopeUser;
    }

    public void setSyncopeUser(String s) {
        this.syncopeUser = s;
    }

    public String getSyncopePassword() {
        return syncopePassword;
    }

    public void setSyncopePassword(String s) {
        this.syncopePassword = s;
    }

    public String getSyncopeUrl() {
        return syncopeUrl;
    }

    public void setSyncopeUrl(String s) {
        this.syncopeUrl = s;
    }

    public String getSyncopeEmailWhiteList() {
        return syncopeEmailWhitelist;
    }

    public void setSyncopeEmailWhitelist(String s) {
        this.syncopeEmailWhitelist = s;
    }

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

            try {
                syncWithSyncope("" + JsonHelper.getElement(json, EMAIL_ADDRESS), "" + JsonHelper.getElement(json, FIRST_NAME), "" + JsonHelper.getElement(json, LAST_NAME));
            } catch (final Exception e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }

        } else {
            raiseProfileExtractionJsonError(body);
        }

        return profile;
    }

    private void syncWithSyncope(String email, String firstName, String lastName) throws URISyntaxException, IOException  {
        final HttpClientBuilder builder = HttpClientBuilder.create();
        final HttpClient client = builder.build();

        String encoding = java.util.Base64.getEncoder().encodeToString((syncopeUser + ":" + syncopePassword).getBytes());
        URIBuilder uriBuilder = new URIBuilder(syncopeUrl + "/users/" + email);
        URI uri = uriBuilder.build();
        HttpUriRequest request = new HttpGet(uri);
        request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
        HttpResponse response = client.execute(request);
        Map attributes = jacksonObjectMapper.readValue(response.getEntity().getContent(), Map.class);

        //If no user was found, add the user now to Syncope
        if(attributes.get("status") != null && attributes.get("status").toString().equals("404")) {
            URIBuilder uriBuilderPost = new URIBuilder(syncopeUrl + "/users");
            URI uriPost = uriBuilderPost.build();

            HttpUriRequest requestPost = new HttpPost(uriPost);
            requestPost.addHeader("Accept", "application/json");
            requestPost.addHeader("Content-Type", "application/json");
            requestPost.addHeader("X-Syncope-Domain", "Master");
            requestPost.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);


            ((HttpPost) requestPost).setEntity(new StringEntity(
                    "{ "
                            + "\"@class\": \"org.apache.syncope.common.lib.to.UserTO\", "
                            + "\"username\" : \"" + email + "\", "
                            + "\"password\" : \"" + DEFAULT_PASSWORD + "\", "
                            + "\"realm\" : \"/\", "
                            + "\"status\" : \"" + getInitialStatus(email, syncopeEmailWhitelist) + "\", "
                            + "\"auxClasses\" : [], "
                            + "\"plainAttrs\" : [\n" +
                            "    {\n" +
                            "      \"schema\": \"family_name\",\n" +
                            "      \"values\": [\n" +
                            "        \"" + lastName + "\"\n" +
                            "      ]\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"schema\": \"given_name\",\n" +
                            "      \"values\": [\n" +
                            "        \"" + firstName + "\"\n" +
                            "      ]\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"schema\": \"display_name\",\n" +
                            "      \"values\": [\n" +
                            "        \"" + firstName + " " + lastName + "\"\n" +
                            "      ]\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"schema\": \"email\",\n" +
                            "      \"values\": [\n" +
                            "        \"" + email + "\"\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ], "
                            + "\"derAttrs\" : [], "
                            + "\"virAttrs\" : [], "
                            + "\"resources\" : []"
                            + "}"));

            client.execute(requestPost);
        }
    }

    private String getInitialStatus(String email, String whitelist) {
        return emailWhitelisted(email, whitelist) ?
                "Active" :
                "Suspended";
    }

    private boolean emailWhitelisted(String email, String whitelist) {

        if(whitelist.equals("*"))
            return true;
        else {
            List<String> domainSegments = Arrays.asList(whitelist.split("\\s*,\\s*"));

            for(String ds : domainSegments) {
                if(email.endsWith(ds))
                    return true;
            }

            return false;
        }
    }
}
