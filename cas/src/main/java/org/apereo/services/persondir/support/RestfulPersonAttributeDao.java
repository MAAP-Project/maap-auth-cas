package org.apereo.services.persondir.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apereo.services.persondir.IPersonAttributeDaoFilter;
import org.apereo.services.persondir.IPersonAttributes;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides the ability to contact a URL resource to ask for attributes.
 * Support GET/POST endpoints, and provides the username in form of a parameter.
 * The response is expected to be a JSON map.
 *
 * @author Misagh Moayyed
 */
public class RestfulPersonAttributeDao extends BasePersonAttributeDao {
    private final ObjectMapper jacksonObjectMapper = new ObjectMapper().findAndRegisterModules();
    private IUsernameAttributeProvider usernameAttributeProvider = new SimpleUsernameAttributeProvider();
   
    private boolean caseInsensitiveUsername = false;
    private String url;
    private String basicAuthUsername;
    private String basicAuthPassword;
    private String method;
    private Map<String, String> parameters = new LinkedHashMap<>();
    private Map<String, String> headers = new LinkedHashMap<>();

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(final Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public IUsernameAttributeProvider getUsernameAttributeProvider() {
        return usernameAttributeProvider;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(final Map<String, String> headers) {
        this.headers = headers;
    }

    public void setUsernameAttributeProvider(final IUsernameAttributeProvider usernameAttributeProvider) {
        this.usernameAttributeProvider = usernameAttributeProvider;
    }

    public boolean isCaseInsensitiveUsername() {
        return caseInsensitiveUsername;
    }

    public void setCaseInsensitiveUsername(final boolean caseInsensitiveUsername) {
        this.caseInsensitiveUsername = caseInsensitiveUsername;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(final String method) {
        this.method = method;
    }

    public String getBasicAuthUsername() {
        return basicAuthUsername;
    }

    public void setBasicAuthUsername(final String basicAuthUsername) {
        this.basicAuthUsername = basicAuthUsername;
    }

    public String getBasicAuthPassword() {
        return basicAuthPassword;
    }

    public void setBasicAuthPassword(final String basicAuthPassword) {
        this.basicAuthPassword = basicAuthPassword;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    @Override
    public IPersonAttributes getPerson(final String uid, final IPersonAttributeDaoFilter filter) {
        try {
            if (!this.isEnabled()) {
                return null;
            }
            
            var builder = HttpClientBuilder.create();

            if (StringUtils.isNotBlank(this.basicAuthUsername) && StringUtils.isNotBlank(this.basicAuthPassword)) {
                var provider = new BasicCredentialsProvider();
                var credentials = new UsernamePasswordCredentials(this.basicAuthUsername, this.basicAuthPassword);
                provider.setCredentials(AuthScope.ANY, credentials);
                builder.setDefaultCredentialsProvider(provider);
            }

            var client = builder.build();                       
            
            //CUSTOM MAAP OVERLAY - BEGIN
            //Handle Syncope's custom user endpoint and attribute schema.
            final URIBuilder uriBuilder = new URIBuilder(this.url + "/users/" + uid);
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
            //CUSTOM MAAP OVERLAY - END

            if (this.caseInsensitiveUsername) {
                return new CaseInsensitiveNamedPersonImpl(uid, MultivaluedPersonAttributeUtils.stuffAttributesIntoListValues(attributes, filter));
            }
            return new NamedPersonImpl(uid, MultivaluedPersonAttributeUtils.stuffAttributesIntoListValues(attributes, filter));

        } catch (final Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public Set<IPersonAttributes> getPeople(final Map<String, Object> query,
                                            final IPersonAttributeDaoFilter filter) {
        return getPeopleWithMultivaluedAttributes(MultivaluedPersonAttributeUtils.stuffAttributesIntoListValues(query, filter), filter);
    }

    @Override
    public Set<IPersonAttributes> getPeopleWithMultivaluedAttributes(final Map<String, List<Object>> query,
                                                                     final IPersonAttributeDaoFilter filter) {
        var people = new LinkedHashSet<IPersonAttributes>();
        var username = usernameAttributeProvider.getUsernameFromQuery(query);
        var person = getPerson(username, filter);
        if (person != null) {
            people.add(person);
        }
        return people;
    }

    @Override
    @JsonIgnore
    public Set<String> getPossibleUserAttributeNames(final IPersonAttributeDaoFilter filter) {
        return Collections.EMPTY_SET;
    }

    @Override
    @JsonIgnore
    public Set<String> getAvailableQueryAttributes(final IPersonAttributeDaoFilter filter) {
        return Collections.EMPTY_SET;
    }

}
