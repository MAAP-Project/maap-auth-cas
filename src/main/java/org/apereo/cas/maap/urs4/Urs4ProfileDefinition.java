package org.apereo.cas.maap.urs4;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.scribejava.core.model.OAuth2AccessToken;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.pac4j.core.profile.AttributeLocation;
import org.pac4j.core.profile.ProfileHelper;
import org.pac4j.core.profile.converter.Converters;
import org.pac4j.oauth.config.OAuth20Configuration;
import org.pac4j.oauth.profile.JsonHelper;
import org.pac4j.oauth.profile.definition.OAuth20ProfileDefinition;
import org.pac4j.oidc.profile.OidcProfileDefinition;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Arrays;

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
    private String gitLabUser;
    private String gitLabPassword;
    private String gitLabUrl;

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

    public String getGitLabUser() {
        return gitLabUser;
    }

    public void setGitLabUser(String s) {
        this.gitLabUser = s;
    }

    public String getGitLabPassword() {
        return gitLabPassword;
    }

    public void setGitLabPassword(String s) {
        this.gitLabPassword = s;
    }

    public String getGitLabUrl() {
        return gitLabUrl;
    }

    public void setGitLabUrl(String s) {
        this.gitLabUrl = s;
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
                syncWithSyncope("" + JsonHelper.getElement(json, UID), "" + JsonHelper.getElement(json, EMAIL_ADDRESS), "" + JsonHelper.getElement(json, FIRST_NAME), "" + JsonHelper.getElement(json, LAST_NAME));
            } catch (final Exception e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }

        } else {
            raiseProfileExtractionJsonError(body);
        }

        return profile;
    }

    private void syncWithSyncope(String username, String email, String firstName, String lastName) throws URISyntaxException, IOException  {
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
                            + "\"status\" : \"active\", "
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
            String uid = getUidForUser(client, syncopeUrl, encoding, email);
            syncWithGitLab(uid, username, email, firstName, lastName);

            if(!emailWhitelisted(email, syncopeEmailWhitelist)) {
                suspendUser(client, uid, syncopeUrl, encoding);
            }
        }
    }

    //Add user to GitLab, and update Syncope with the GitLab access token and Id.
    private void syncWithGitLab(String uid, String username, String email, String firstName, String lastName) throws URISyntaxException, IOException  {
        final HttpClientBuilder builder = HttpClientBuilder.create();
        final CloseableHttpClient client = builder.build();
        URIBuilder uriBuilderPost = new URIBuilder(gitLabUrl + "/users");
        URI uriPost = uriBuilderPost.build();

        HttpUriRequest requestPost = new HttpPost(uriPost);
        requestPost.addHeader("Accept", "application/json");
        requestPost.addHeader("Content-Type", "application/json");
        requestPost.addHeader("PRIVATE-TOKEN", gitLabPassword);

        ((HttpPost) requestPost).setEntity(new StringEntity(
                "{ "
                + "\"username\" : \"" + username + "\", "
                + "\"password\" : \"" + Double.toString(Math.random()) + "\", "
                + "\"name\" : \"" + firstName + " " + lastName + "\", "
                + "\"email\" : \"" + email + "\", "
                + "\"skip_confirmation\" : true"
                + "}"));

        final CloseableHttpResponse response = client.execute(requestPost);
        final HttpEntity responseEntity = response.getEntity();

        byte[] byteResult = EntityUtils.toByteArray(responseEntity);
        String result = new String(byteResult, "ISO-8859-2");
        responseClose(response);
        JSONObject jsonOutput = new JSONObject(result);
        int gitLabId = jsonOutput.getInt("id");
        String strGitLabId = String.valueOf(gitLabId);
        
        // Now that the user's GitLab is created, at the cas3 identity to the user's profile.
        createGitLabIdentity(strGitLabId, email);

        // Next, create a MAAP impersonation token for the ADE to use.
        String gitLabAccessToken = createGitLabImpersonationToken(strGitLabId);

        // Lastly, update Syncope with the user's Gitlab info so we can pass on the attributes during login.
        String syncopePatchUrl = syncopeUrl + "/users/" + uid;
        String syncopePatchBody = "{ "
            + "\"@class\" : \"org.apache.syncope.common.lib.patch.UserPatch\", " +
            "  \"key\" : \"" + uid + "\", " +
            "  \"plainAttrs\" : [\n" +
            "    {\n" +
            "      \"operation\": \"ADD_REPLACE\",\n" +
            "      \"attrTO\" :\n" +
            "        {\n" +
            "          \"schema\": \"gitlab_access_token\",\n" +
            "          \"values\": [\n" +
            "            \"" + gitLabAccessToken + "\"\n" +
            "          ]\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"operation\": \"ADD_REPLACE\",\n" +
            "      \"attrTO\" :\n" +
            "        {\n" +
            "          \"schema\": \"gitlab_uid\",\n" +
            "          \"values\": [\n" +
            "            \"" + strGitLabId + "\"\n" +
            "          ]\n" +
            "      }\n" +
            "    }\n" +
            "  ]"
            + "}";

        uriBuilderPost = new URIBuilder(syncopePatchUrl);
        uriPost = uriBuilderPost.build();
        String encoding = java.util.Base64.getEncoder().encodeToString((syncopeUser + ":" + syncopePassword).getBytes());

        HttpUriRequest requestPatch = new HttpPatch(uriPost);
        requestPatch.addHeader("Accept", "application/json");
        requestPatch.addHeader("Content-Type", "application/json");
        requestPatch.addHeader("X-Syncope-Domain", "Master");
        requestPatch.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
        ((HttpPatch) requestPatch).setEntity(new StringEntity(syncopePatchBody));
        client.execute(requestPatch);
    }

    private String createGitLabImpersonationToken(String uid) throws URISyntaxException, ClientProtocolException, IOException {
        String result = "";
        final HttpClientBuilder builder = HttpClientBuilder.create();
        final CloseableHttpClient client = builder.build();

        String impersonationTokenUrl = gitLabUrl + "/users/" + uid + "/impersonation_tokens";
        String impersonationTokenBody = "{ "
            + "\"name\" : \"MAAP\", "
            + "\"expires_at\" : \"2038-01-19\", "
            + "\"scopes\" :[\"api\"]"
            + "}";

        URIBuilder uriBuilderPost = new URIBuilder(impersonationTokenUrl);
        URI uriPost = uriBuilderPost.build();

        HttpUriRequest requestPost = new HttpPost(uriPost);
        requestPost.addHeader("Accept", "application/json");
        requestPost.addHeader("Content-Type", "application/json");
        requestPost.addHeader("PRIVATE-TOKEN", gitLabPassword);
        ((HttpPost) requestPost).setEntity(new StringEntity(impersonationTokenBody));

        final CloseableHttpResponse response = client.execute(requestPost);

        String json = EntityUtils.toString(response.getEntity());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        result = node.get("token").asText();

        responseClose(response);

        return result;
    }

    private void createGitLabIdentity(String uid, String email) throws URISyntaxException, ClientProtocolException, IOException {
        final HttpClientBuilder builder = HttpClientBuilder.create();
        final CloseableHttpClient client = builder.build();

        String userIdentityUrl = gitLabUrl + "/users/" + uid ;
        String userIdentityBody = "{ "
            + "\"provider\" : \"cas3\", "
            + "\"extern_uid\" : \"" + email + "\""
            + "}";

        URIBuilder uriBuilder = new URIBuilder(userIdentityUrl);
        URI uri = uriBuilder.build();

        HttpUriRequest requestPut = new HttpPut(uri);
        requestPut.addHeader("Accept", "application/json");
        requestPut.addHeader("Content-Type", "application/json");
        requestPut.addHeader("PRIVATE-TOKEN", gitLabPassword);
        ((HttpPut) requestPut).setEntity(new StringEntity(userIdentityBody));

        final CloseableHttpResponse response = client.execute(requestPut);

        String json = EntityUtils.toString(response.getEntity());

        responseClose(response);
    }

    private void responseClose(CloseableHttpResponse response) {
        try {
            response.close();
        } catch (IOException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Failed close response: ").append(response);
            logger.info("Error closing response: " + sb.toString());
        }
    }

    private String getUidForUser(
            final HttpClient client,
            final String syncopeUrl,
            final String encoding,
            final String email) throws URISyntaxException, IOException {

        final URIBuilder uriBuilder = new URIBuilder(syncopeUrl + "/users/" + email);
        final URI uri = uriBuilder.build();
        final HttpUriRequest request = new HttpGet(uri);
        request.addHeader("Accept", "application/json");
        request.addHeader("Content-Type", "application/json");
        request.addHeader("X-Syncope-Domain", "Master");
        request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
        final HttpResponse response = client.execute(request);

        final Map attributes = jacksonObjectMapper.readValue(response.getEntity().getContent(), Map.class);

        attributes.values().removeAll(Collections.singleton(null));
        attributes.remove("@class");
        final List plainAttributes = (List) attributes.remove("plainAttrs");
        plainAttributes.forEach(plainAttribute -> {
            attributes.put(((Map) plainAttribute).get("schema"), ((Map) plainAttribute).get("values"));
        });

        if (attributes.get("key") != null) {
            return attributes.get("key").toString();
        }

        return null;
    }

    private void suspendUser(
            final HttpClient client,
            final String uid,
            final String syncopeUrl,
            final String encoding) throws URISyntaxException, IOException {

        URIBuilder uriBuilderStatus = new URIBuilder(syncopeUrl + "/users/" + uid + "/status");
        URI uriStatus = uriBuilderStatus.build();

        HttpUriRequest requestStatus = new HttpPost(uriStatus);
        requestStatus.addHeader("Accept", "application/json");
        requestStatus.addHeader("Content-Type", "application/json");
        requestStatus.addHeader("X-Syncope-Domain", "Master");
        requestStatus.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);

        ((HttpPost) requestStatus).setEntity(new StringEntity("{ "
                + "\"operation\": \"ADD_REPLACE\", "
                + "\"key\" : \"" + uid + "\", "
                + "\"type\" : \"SUSPEND\", "
                + "\"resources\" : [] "
                + "}"));

        client.execute(requestStatus);
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
