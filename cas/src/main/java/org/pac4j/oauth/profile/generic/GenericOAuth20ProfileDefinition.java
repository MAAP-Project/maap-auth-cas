package org.pac4j.oauth.profile.generic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.model.Verb;

import static org.pac4j.core.profile.AttributeLocation.PROFILE_ATTRIBUTE;

import java.util.HashMap;
import java.util.Map;

import org.pac4j.core.profile.ProfileHelper;
import org.pac4j.core.profile.converter.AttributeConverter;
import org.pac4j.core.profile.converter.StringConverter;
import org.pac4j.oauth.config.OAuthConfiguration;
import org.pac4j.oauth.profile.JsonHelper;
import org.pac4j.oauth.profile.OAuth20Profile;
import org.pac4j.oauth.profile.definition.OAuthProfileDefinition;

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
import org.json.JSONTokener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Arrays;

/**
 * <p>This class is the user profile for generic OAuth2 with appropriate getters.</p>
 * <p>The map of <code>profileAttributes</code> is intended to replace the primary/secondary attributes where
 * the key is the name of the attribute and the value is the path to obtain that attribute from the
 * json response starting from <code>firstNodePath</code></p>
 *
 * @author Julio Arrebola
 */
public class GenericOAuth20ProfileDefinition extends OAuthProfileDefinition {
    private final ObjectMapper jacksonObjectMapper = new ObjectMapper().findAndRegisterModules();

    public static final String UID = "uid";
    public static final String EMAIL_ADDRESS = "email_address";
    public static final String FIRST_NAME = "first_name";
    public static final String LAST_NAME = "last_name";
    public static final String STUDY_AREA = "study_area";
    public static final String ORGANIZATION = "organization";
    public static final String AFFILIATION = "affiliation";
    public static final String STATUS = "status";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_SUSPENDED = "suspended";
    public static final String CLIENT_ESA = "ESA";    
    public static final String CLIENT_NASA = "maapauth";    
    public static final String CUSTOM_SYNCOPE_EMAIL_WHITELIST = "syncope_email_whitelist";
    public static final String CUSTOM_CAS_KEY = "cas_key";
    public static final String CUSTOM_MAAP_API_URL = "maap_api_url";
    public static final String CUSTOM_GITLAB_PASSWORD = "gitlab_password";
    public static final String CUSTOM_GITLAB_URL = "gitlab_url";
    
    private String syncope_email_whitelist;
    private String cas_key;
    private String maap_api_url;
    private String gitlab_password;
    private String gitlab_url;

    private final Map<String,String> profileAttributes = new HashMap<>();

    private String profileUrl = null;
    private Verb profileVerb = null;
    private String firstNodePath = null;

    public void setProfileVerb(final Verb value) {
        this.profileVerb = value;
    }

    @Override
    public Verb getProfileVerb() {
        if (profileVerb != null) {
            return this.profileVerb;
        } else {
            return super.getProfileVerb();
        }
    }

    public void setProfileUrl(final String profileUrl) {
        this.profileUrl = profileUrl;
    }

    @Override
    public String getProfileUrl(final Token accessToken, final OAuthConfiguration configuration) {    	
        return profileUrl;
    }

    @Override
    public OAuth20Profile extractUserProfile(final String body) {
        final var profile = new OAuth20Profile();
        final var json = JsonHelper.getFirstNode(body, getFirstNodePath());
        if (json != null) {
            profile.setId(ProfileHelper.sanitizeIdentifier(JsonHelper.getElement(json, getProfileId())));
            for (final var attribute : getPrimaryAttributes()) {
                convertAndAdd(profile, PROFILE_ATTRIBUTE, attribute, JsonHelper.getElement(json, attribute));
            }
            for (final var attribute : getSecondaryAttributes()) {
                convertAndAdd(profile, PROFILE_ATTRIBUTE, attribute, JsonHelper.getElement(json, attribute));
            }
            for (final var entry : getProfileAttributes().entrySet()) {
                final var key = entry.getKey();
                final var value = entry.getValue();
                assignCustomAttribute(key, value);
                convertAndAdd(profile, PROFILE_ATTRIBUTE, key, JsonHelper.getElement(json, value));
            }

        } else {
            raiseProfileExtractionJsonError(body);
        }  
        
        try {
            Boolean maap_user = getMaapUser(
                (String)profile.getAttribute("preferred_username"), 
                profile.getUsername(), 
                profile.getFirstName(), 
                profile.getFamilyName(),
                (String)profile.getAttribute("organization"));

            convertAndAdd(profile, PROFILE_ATTRIBUTE, STATUS, maap_user ? STATUS_ACTIVE : STATUS_SUSPENDED);

//             if(isActive) {
//                 syncWithGitLab(syncope_uid, (String)profile.getAttribute("preferred_username"), profile.getUsername(), profile.getFirstName(), profile.getFamilyName());
//             }
            
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return profile;
    }
    
    // private void applyMaapAttributes(OAuth20Profile profile) {        
    // 	String clientName = profile.getClientName() != null ? profile.getClientName() : "";
    	
    // 	if(clientName.equals(CLIENT_NASA)) {
    //         try {
    // 			syncWithSyncope((String)profile.getAttribute("preferred_username"), profile.getUsername(), profile.getFirstName(), profile.getFamilyName());
    // 		} catch (URISyntaxException e) {
    // 			e.printStackTrace();
    // 		} catch (IOException e) {
    // 			e.printStackTrace();
    // 		}
    // 	} else if (clientName.equals(CLIENT_ESA)) {
    // 		//Add 'status=active' attribute to profile. ESA users are assumed to be pre-approved.
    //         convertAndAdd(profile, PROFILE_ATTRIBUTE, STATUS, STATUS_ACTIVE);
    // 	} else {
    // 		 raiseProfileExtractionJsonError("No valid client name found.");
    // 	}
    // }
    
    private void assignCustomAttribute(String key, String value) {
    	switch(key) {
    	  case CUSTOM_GITLAB_PASSWORD:
    		gitlab_password = value;
    	    break;
    	  case CUSTOM_GITLAB_URL:
      	    gitlab_url = value;
      	    break;
    	  case CUSTOM_SYNCOPE_EMAIL_WHITELIST:
      	    syncope_email_whitelist = value;
      	    break;
    	  case CUSTOM_CAS_KEY:
      	    cas_key = value;
      	    break;
    	  case CUSTOM_MAAP_API_URL:
      	    maap_api_url = value;
      	    break;
    	  default:
    	    break;
    	}
    }

    public Map<String, String> getProfileAttributes() {
        return this.profileAttributes;
    }

     /**
     * Add an attribute as a primary one and its converter.
     *
     * @param name name of the attribute
     * @param converter converter
     */
    public void profileAttribute(final String name, final AttributeConverter converter) {
        profileAttribute(name, name, converter);
    }

     /**
     * Add an attribute as a primary one and its converter.
     *
     * @param name name of the attribute
     * @param tag json reference
     * @param converter converter
     */
    public void profileAttribute(final String name, String tag, final AttributeConverter converter) {
        profileAttributes.put(name, tag);
        if (converter != null) {
            getConverters().put(name, converter);
        } else {
            getConverters().put(name, new StringConverter());
        }
    }

    public String getFirstNodePath() {
        return firstNodePath;
    }

    public void setFirstNodePath(final String firstNodePath) {
        this.firstNodePath = firstNodePath;
    }

    public Boolean getMaapUser(
        final String username, 
        String email, 
        String firstName, 
        String lastName,
        String org) throws URISyntaxException, IOException {

        if(emailWhitelisted(username, syncope_email_whitelist)) {
            return true;
        }

        final HttpClientBuilder builder = HttpClientBuilder.create();
        final HttpClient client = builder.build();
   
        URIBuilder uriBuilder = new URIBuilder(maap_api_url + "/members/" + username);
        URI uri = uriBuilder.build();
        HttpUriRequest request = new HttpGet(uri);
        request.setHeader("cas-authorization", cas_key);
        HttpResponse response = client.execute(request);

        Boolean activeUser = false; 
        Boolean userExists = response.getStatusLine().getStatusCode() == 200;

        if(userExists) {
            Map attributes = jacksonObjectMapper.readValue(response.getEntity().getContent(), Map.class);    
            activeUser = attributesContainActiveStatus(attributes); 

            if(activeUser) {
                syncWithGitLab(username, email, firstName, lastName);
            }
        } else {
            request = new HttpPost(uri);
            request.setHeader("cas-authorization", cas_key);
            ((HttpPost) request).setEntity(new StringEntity(
                "{ "
                + "\"first_name\" : \"" + firstName + "\", "
                + "\"last_name\" : \"" + lastName + "\", "
                + "\"organization\" : \"" + org + "\", "
                + "\"email\" : \"" + email + "\""
                + "}"));
            response = client.execute(request);
        }
        
        return activeUser;
    }
    
    private Boolean attributesContainActiveStatus(Map attributes) {
    	return attributes.get("status") != null && attributes.get("status").equals(STATUS_ACTIVE);
    }
    
    //Add user to GitLab, and update MAAP with the GitLab access token and Id.
    private void syncWithGitLab(String username, String email, String firstName, String lastName) throws URISyntaxException, IOException  {
        final HttpClientBuilder builder = HttpClientBuilder.create();
        final CloseableHttpClient client = builder.build();  
        JSONArray jArray = new JSONArray();

        //Lookup user by EDL username
        jArray = findGitLabUser("/users?username=" + username);

        //If no match found, try searching by username (email)
        if(jArray.length() == 0) 
            jArray = findGitLabUser("/users?search=" + email);
        
        //If user doesn't exist in GitLab, create an account
        if(jArray.length() == 0) {
            URIBuilder uriBuilderPost = new URIBuilder(gitlab_url + "/users");
            URI uriPost = uriBuilderPost.build();
       
            HttpUriRequest requestPost = new HttpPost(uriPost);
            requestPost.addHeader("Accept", "application/json");
            requestPost.addHeader("Content-Type", "application/json");
            requestPost.addHeader("PRIVATE-TOKEN", gitlab_password);
       
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
            
            // Now that the user's GitLab is created, add the cas3 identity to the user's profile.
            createGitLabIdentity(strGitLabId, email);
       
            // Next, create a MAAP impersonation token for the ADE to use.
            String gitLabAccessToken = createGitLabImpersonationToken(strGitLabId);
       
            // Lastly, update the user's MAAP info so we can pass on the attributes during login.
            URIBuilder uriBuilder = new URIBuilder(maap_api_url + "/members/" + username);
            URI uri = uriBuilder.build();
            HttpUriRequest request = new HttpPut(uri);
            request.setHeader("cas-authorization", cas_key);
            ((HttpPut) request).setEntity(new StringEntity(
                "{ "
                + "\"gitlab_id\" : \"" + strGitLabId + "\", "
                + "\"gitlab_username\" : \"" + username + "\", "
                + "\"gitlab_token\" : \"" + gitLabAccessToken + "\""
                + "}"));
             client.execute(request);
        }  
    }

    private JSONArray findGitLabUser(String query) throws URISyntaxException, IOException {
        final HttpClientBuilder builder = HttpClientBuilder.create();
        final CloseableHttpClient client = builder.build();        
        
        URIBuilder uriBuilderGet = new URIBuilder(gitlab_url + query);
        URI uriGet = uriBuilderGet.build();
   
        HttpUriRequest requestGet = new HttpGet(uriGet);
        requestGet.addHeader("Accept", "application/json");
        requestGet.addHeader("Content-Type", "application/json");
        requestGet.addHeader("PRIVATE-TOKEN", gitlab_password);
   
        final CloseableHttpResponse getResponse = client.execute(requestGet);
        final HttpEntity getResponseEntity = getResponse.getEntity();
        
        byte[] byteResult = EntityUtils.toByteArray(getResponseEntity);
        String result = new String(byteResult, "ISO-8859-2");
        responseClose(getResponse);
        JSONArray jArray = (JSONArray) new JSONTokener(result).nextValue();

        return jArray;
    }
   
    private String createGitLabImpersonationToken(String uid) throws URISyntaxException, ClientProtocolException, IOException {
        String result = "";
        final HttpClientBuilder builder = HttpClientBuilder.create();
        final CloseableHttpClient client = builder.build();
   
        String impersonationTokenUrl = gitlab_url + "/users/" + uid + "/impersonation_tokens";
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
        requestPost.addHeader("PRIVATE-TOKEN", gitlab_password);
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
   
        String userIdentityUrl = gitlab_url + "/users/" + uid ;
        String userIdentityBody = "{ "
            + "\"provider\" : \"cas3\", "
            + "\"extern_uid\" : \"" + email + "\""
            + "}";
   
        URIBuilder uriBuilder = new URIBuilder(userIdentityUrl);
        URI uri = uriBuilder.build();
   
        HttpUriRequest requestPut = new HttpPut(uri);
        requestPut.addHeader("Accept", "application/json");
        requestPut.addHeader("Content-Type", "application/json");
        requestPut.addHeader("PRIVATE-TOKEN", gitlab_password);
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