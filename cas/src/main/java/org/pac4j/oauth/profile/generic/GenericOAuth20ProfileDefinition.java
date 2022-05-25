package org.pac4j.oauth.profile.generic;

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
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.javatuples.Quartet;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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
    public static final String GITLAB_ID = "gitlab_id";
    public static final String GITLAB_TOKEN = "gitlab_token";
    public static final String PUBLIC_SSH_KEY = "public_ssh_key";
    public static final String CLIENT_ESA = "ESA";    
    public static final String CLIENT_NASA = "maapauth";    
    public static final String CUSTOM_CAS_KEY = "cas_key";
    public static final String CUSTOM_MAAP_API_URL = "maap_api_url";
    
    private String cas_key;
    private String maap_api_url;

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
        	Quartet<Boolean, String, String, String> maap_user_attributes = getMaapUser(
                (String)profile.getAttribute("preferred_username"), 
                profile.getUsername(), 
                profile.getFirstName(), 
                profile.getFamilyName(),
                (String)profile.getAttribute("organization"));

            convertAndAdd(profile, PROFILE_ATTRIBUTE, STATUS, maap_user_attributes.getValue0() ? STATUS_ACTIVE : STATUS_SUSPENDED);
            convertAndAdd(profile, PROFILE_ATTRIBUTE, GITLAB_ID, maap_user_attributes.getValue1());
            convertAndAdd(profile, PROFILE_ATTRIBUTE, GITLAB_TOKEN, maap_user_attributes.getValue2());
            convertAndAdd(profile, PROFILE_ATTRIBUTE, PUBLIC_SSH_KEY, maap_user_attributes.getValue3());
            
            
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return profile;
    }
    
    private void assignCustomAttribute(String key, String value) {
    	switch(key) {
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

    public Quartet<Boolean, String, String, String> getMaapUser(
        final String username, 
        String email, 
        String firstName, 
        String lastName,
        String org) throws URISyntaxException, IOException {

        Boolean activeUser = false; 
        String gitlabId = null;
        String gitlabToken = null;
        String publicSshKey = null;

        final HttpClientBuilder builder = HttpClientBuilder.create();
        final HttpClient client = builder.build();
   
        URIBuilder uriBuilder = new URIBuilder(maap_api_url + "/members/" + username);
        URI uri = uriBuilder.build();
        HttpUriRequest request = new HttpGet(uri);
        request.setHeader("cas-authorization", cas_key);
        HttpResponse response = client.execute(request);

        Boolean userExists = response.getStatusLine().getStatusCode() == 200;

        if(userExists) {
        	
            Map attributes = jacksonObjectMapper.readValue(response.getEntity().getContent(), Map.class);    
            activeUser = attributesContainActiveStatus(attributes); 
            gitlabId = getAttributeValue(attributes, GITLAB_ID);
            gitlabToken = getAttributeValue(attributes, GITLAB_TOKEN);
            publicSshKey = getAttributeValue(attributes, PUBLIC_SSH_KEY);
            
        } else {
        	
            URIBuilder uriBuilderPost = new URIBuilder(maap_api_url + "/members/" + username);
            URI uriPost = uriBuilderPost.build();
        	HttpUriRequest req_post = new HttpPost(uriPost);
        	req_post.addHeader("cas-authorization", cas_key);
        	req_post.addHeader("Accept", "application/json");
        	req_post.addHeader("Content-Type", "application/json");
        	String body = "{ "
                    + "\"first_name\" : \"" + firstName + "\", "
                    + "\"last_name\" : \"" + lastName + "\", "
                    + "\"organization\" : \"" + (org == null ? "" : org) + "\", "
                    + "\"email\" : \"" + email + "\""
                    + "}";
        	
            ((HttpPost) req_post).setEntity(new StringEntity(body));
            client.execute(req_post);
        }
        
        return Quartet.with(activeUser, gitlabId, gitlabToken, publicSshKey);
    }
    
    private Boolean attributesContainActiveStatus(Map attributes) {
    	return attributes.get("status") != null && attributes.get("status").equals(STATUS_ACTIVE);
    }
    
    private String getAttributeValue(Map attributes, String attributeName) {
    	return attributes.get(attributeName) == null ? null : (String) attributes.get(attributeName);
    }
}