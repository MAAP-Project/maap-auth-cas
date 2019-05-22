package org.apereo.services.persondir.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apereo.services.persondir.IPersonAttributes;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class SyncopeRestfulPersonAttributeDao extends RestfulPersonAttributeDao {
    private final ObjectMapper jacksonObjectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public IPersonAttributes getPerson(final String uid) {
        try {
            final HttpClientBuilder builder = HttpClientBuilder.create();

            if (StringUtils.isNotBlank(getBasicAuthUsername()) && StringUtils.isNotBlank(getBasicAuthPassword())) {
                final CredentialsProvider provider = new BasicCredentialsProvider();
                final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(getBasicAuthUsername(), getBasicAuthPassword());
                provider.setCredentials(AuthScope.ANY, credentials);
                builder.setDefaultCredentialsProvider(provider);
            }

            final HttpClient client = builder.build();

            final URIBuilder uriBuilder = new URIBuilder(getUrl() + "/users/" + uid);
//            uriBuilder.addParameter("username", uid);
            final URI uri = uriBuilder.build();
            final HttpUriRequest request = getMethod().equalsIgnoreCase(HttpMethod.GET.name()) ? new HttpGet(uri) : new HttpPost(uri);
            final HttpResponse response = client.execute(request);
            final Map attributes = jacksonObjectMapper.readValue(response.getEntity().getContent(), Map.class);

            attributes.values().removeAll(Collections.singleton(null));
            attributes.remove("@class");
            final List plainAttributes = (List) attributes.remove("plainAttrs");
            plainAttributes.forEach(plainAttribute -> {
                attributes.put(((Map) plainAttribute).get("schema"), ((Map) plainAttribute).get("values"));
            });

            if (attributes.get("key") != null) {
                attributes.put("syncopeRestUrl", getUrl());

                String syncopeToken = getAccessTokenForUser(client, uid);

                if (syncopeToken == null) {
                    String tempPassword = setRandomPasswordForUser(client, uid, attributes.get("key").toString());
                    syncopeToken = createAccessTokenForUserPass(uid, tempPassword);
                }

                attributes.put("syncopeToken", syncopeToken);
            }

            if (isCaseInsensitiveUsername()) {
                return new CaseInsensitiveNamedPersonImpl(uid, stuffAttributesIntoListValues(attributes));
            }
            return new NamedPersonImpl(uid, stuffAttributesIntoListValues(attributes));

        } catch (final Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private String getAccessTokenForUser(final HttpClient client, final String uid) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(getUrl() + "/accessTokens");
        URI uri = uriBuilder.build();
        HttpUriRequest request = new HttpGet(uri);
        HttpResponse response = client.execute(request);
        Map attributes = jacksonObjectMapper.readValue(response.getEntity().getContent(), Map.class);

        final List<Map> accessTokens = (List<Map>) attributes.get("result");
        String userAccessToken = null;
        for (Map accessToken : accessTokens) {
            if (accessToken.get("owner").equals(uid)) {
                userAccessToken = accessToken.get("body").toString();
            }
        }
        return userAccessToken;
    }

    private String setRandomPasswordForUser(final HttpClient client, final String uid, final String key) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(getUrl() + "/users/" + uid);
        URI uri = uriBuilder.build();
        HttpUriRequest request = new HttpPatch(uri);
        request.addHeader("Accept", "application/json");
        request.addHeader("Content-Type", "application/json");
        request.addHeader("X-Syncope-Domain", "Master");

        String password = "HELLO123.";

        ((HttpPatch) request).setEntity(new StringEntity(
                "{ \"@class\": \"org.apache.syncope.common.lib.patch.UserPatch\", \"key\" : \""
                        + key
                        + "\", \"password\": { \"operation\": \"ADD_REPLACE\", \"value\": \"" + password + "\" } }"));

        client.execute(request);
        return password;
    }

    private String createAccessTokenForUserPass(final String uid, final String password) throws URISyntaxException, IOException {
        final HttpClientBuilder builder = HttpClientBuilder.create();
        final CredentialsProvider provider = new BasicCredentialsProvider();
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(uid, password);
        provider.setCredentials(AuthScope.ANY, credentials);
        builder.setDefaultCredentialsProvider(provider);
        final HttpClient client = builder.build();

        URIBuilder uriBuilder = new URIBuilder(getUrl() + "/accessTokens/login");
        URI uri = uriBuilder.build();
        HttpUriRequest request = new HttpPost(uri);
        HttpResponse response = client.execute(request);

        return response.getFirstHeader("X-Syncope-Token").getValue();
    }

    private static Map<String, List<Object>> stuffAttributesIntoListValues(final Map<String, ?> personAttributesMap) {
        final Map<String, List<Object>> personAttributes = new HashMap<>();

        for (final Map.Entry<String, ?> stringObjectEntry : personAttributesMap.entrySet()) {
            final Object value = stringObjectEntry.getValue();
            if (value instanceof List) {
                personAttributes.put(stringObjectEntry.getKey(), (List) value);
            } else {
                personAttributes.put(stringObjectEntry.getKey(), new ArrayList<>(Arrays.asList(value)));
            }
        }
        return personAttributes;
    }
}
