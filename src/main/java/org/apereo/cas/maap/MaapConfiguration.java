package org.apereo.cas.maap;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.apereo.cas.authentication.AuthenticationServiceSelectionStrategy;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.maap.oidc.OidcOAuth20AuthenticationRequestServiceSelectionStrategy;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.util.OAuth20Utils;
import org.apereo.cas.support.pac4j.authentication.DelegatedClientFactory;
import org.apereo.services.persondir.IPersonAttributeDao;
import org.apereo.services.persondir.support.RestfulPersonAttributeDao;
import org.apereo.services.persondir.support.SyncopeRestfulPersonAttributeDao;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;

@Configuration("MaapConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class MaapConfiguration {
    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    @Qualifier("servicesManager")
    private ObjectProvider<ServicesManager> servicesManager;

    @Autowired
    @Qualifier("webApplicationServiceFactory")
    private ServiceFactory<WebApplicationService> webApplicationServiceFactory;

    @RefreshScope
    @Bean
    public DelegatedClientFactory pac4jDelegatedClientFactory() {
        return new MaapDelegatedClientFactory(casProperties.getAuthn().getPac4j(), casProperties.getCustom());
    }

    @Bean
    @RefreshScope
    public AuthenticationServiceSelectionStrategy oauth20AuthenticationRequestServiceSelectionStrategy() {
        return new OidcOAuth20AuthenticationRequestServiceSelectionStrategy(servicesManager.getIfAvailable(),
                webApplicationServiceFactory, 
		OAuth20Utils.casOAuthCallbackUrl(casProperties.getServer().getPrefix()));
    }

    @Bean
    @RefreshScope
    public List<IPersonAttributeDao> restfulAttributeRepositories() {
        val list = new ArrayList<IPersonAttributeDao>();
        casProperties.getAuthn().getAttributeRepository().getRest().forEach(rest -> {
            if (StringUtils.isNotBlank(rest.getUrl())) {

                val dao = new SyncopeRestfulPersonAttributeDao();
                dao.setCaseInsensitiveUsername(rest.isCaseInsensitive());
                dao.setOrder(rest.getOrder());
                dao.setUrl(rest.getUrl());
                dao.setMethod(HttpMethod.resolve(rest.getMethod()).name());

                if (StringUtils.isNotBlank(rest.getBasicAuthPassword()) && StringUtils.isNotBlank(rest.getBasicAuthUsername())) {
                    dao.setBasicAuthPassword(rest.getBasicAuthPassword());
                    dao.setBasicAuthUsername(rest.getBasicAuthUsername());
                    //LOGGER.debug("Basic authentication credentials are located for REST endpoint [{}]", rest.getUrl());
                } else {
                    //LOGGER.debug("Basic authentication credentials are not defined for REST endpoint [{}]", rest.getUrl());
                }

                //LOGGER.debug("Configured REST attribute sources from [{}]", rest.getUrl());
                list.add(dao);
            }
        });

        return list;
    }
}
