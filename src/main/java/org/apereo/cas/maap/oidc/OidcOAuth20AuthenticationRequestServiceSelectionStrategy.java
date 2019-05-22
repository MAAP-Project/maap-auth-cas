package org.apereo.cas.maap.oidc;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.services.*;
import org.apereo.cas.support.oauth.services.OAuth20AuthenticationServiceSelectionStrategy;

@Slf4j
public class OidcOAuth20AuthenticationRequestServiceSelectionStrategy extends OAuth20AuthenticationServiceSelectionStrategy {
    private final transient ServicesManager servicesManager; // I need a copy too

    public OidcOAuth20AuthenticationRequestServiceSelectionStrategy(ServicesManager servicesManager, ServiceFactory<WebApplicationService> webApplicationServiceFactory, String callbackUrl) {
        super(servicesManager, webApplicationServiceFactory, callbackUrl);
        this.servicesManager = servicesManager;
    }

    @Override
    public Service resolveServiceFrom(final Service service) {
        Service resolvedService = super.resolveServiceFrom(service);
        if (!service.matches(resolvedService)) { // If the service has been extracted from redirect_uri
            final RegisteredService callbackSvc = this.servicesManager.findServiceBy(service);
            final RegisteredService redirectSvc = this.servicesManager.findServiceBy(resolvedService);

            // Set the oauth2 callback to release all attributes to the original service
            if (callbackSvc.getAttributeReleasePolicy().getClass().equals(DenyAllAttributeReleasePolicy.class)) {
                ((AbstractRegisteredService) callbackSvc).setAttributeReleasePolicy(new ReturnAllAttributeReleasePolicy());
            }

            if (callbackSvc instanceof AbstractRegisteredService) {
                AbstractRegisteredService aCallbackSvc = (AbstractRegisteredService) callbackSvc;
                aCallbackSvc.setDescription(redirectSvc.getDescription());
                aCallbackSvc.setName(redirectSvc.getName());
                aCallbackSvc.setLogo(redirectSvc.getLogo());
                aCallbackSvc.setLogoutType(redirectSvc.getLogoutType());
                aCallbackSvc.setLogoutUrl(redirectSvc.getLogoutUrl());
            }

            resolvedService = service; // restore the callback service as the redirect service
        }

        return resolvedService;
    }

}
