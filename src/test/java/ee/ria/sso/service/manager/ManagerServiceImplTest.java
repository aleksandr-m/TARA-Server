package ee.ria.sso.service.manager;

import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.config.CasOAuthConfiguration;
import org.apereo.cas.services.AbstractRegisteredService;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceProperty;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.when;

public class ManagerServiceImplTest {

    private static final String SERVICE_NAME = "ServiceName";
    private static final String SERVICE_NAME_KEY = "service.name";
    private static final String SERVICE_NAME_KEY_EN = "service.name.en";
    private static final String SERVICE_NAME_KEY_RU = "service.name.ru";
    private static final String SERVICE_NAME_VALUE = "openIdDemoName";
    private static final String SERVICE_NAME_VALUE_EN = "openIdDemoNameEN";
    private static final String SERVICE_NAME_VALUE_RU = "openIdDemoNameRU";
    private static final String SERVICE_SHORT_NAME_KEY = "service.shortName";
    private static final String SERVICE_SHORT_NAME_KEY_EN = "service.shortName.en";
    private static final String SERVICE_SHORT_NAME_KEY_RU = "service.shortName.ru";
    private static final String SERVICE_SHORT_NAME_VALUE = "openIdDemoShortName";
    private static final String SERVICE_SHORT_NAME_VALUE_EN = "openIdDemoShortNameEN";
    private static final String SERVICE_SHORT_NAME_VALUE_RU = "openIdDemoShortNameRU";
    private static final String SERVICE_ID = "https://cas.server.url";

    private final CasOAuthConfiguration casOAuthConfiguration = Mockito.mock(CasOAuthConfiguration.class);

    @Test
    public void getServiceByID_managerReturnsValidService_shouldReturnNonEmptyOptional() {
        Collection<RegisteredService> registeredServices = new ArrayList<>();
        OidcRegisteredService oidcRegisteredService = new OidcRegisteredService();
        oidcRegisteredService.setClientId(SERVICE_NAME);
        registeredServices.add(oidcRegisteredService);
        ServicesManager servicesManager = createValidServicesManagerWith(registeredServices);
        ManagerService managerService = new ManagerServiceImpl(servicesManager, casOAuthConfiguration);

        Assert.assertTrue(managerService.getServiceByName(SERVICE_NAME).isPresent());
    }

    @Test
    public void getServiceByID_managerReturnsNoService_shouldReturnEmptyOptional() {
        Collection<RegisteredService> registeredServices = new ArrayList<>();
        ServicesManager servicesManager = createValidServicesManagerWith(registeredServices);
        ManagerService managerService = new ManagerServiceImpl(servicesManager, casOAuthConfiguration);

        Assert.assertFalse(managerService.getServiceByName(SERVICE_NAME).isPresent());
    }

    @Test
    public void getServiceByID_managerReturnsDuplicateService_shouldReturnEmptyOptional() {
        Collection<RegisteredService> registeredServices = new ArrayList<>();
        OidcRegisteredService oidcRegisteredService = new OidcRegisteredService();
        oidcRegisteredService.setClientId(SERVICE_NAME);
        OidcRegisteredService duplicateOidcRegisteredService = new OidcRegisteredService();
        duplicateOidcRegisteredService.setClientId(SERVICE_NAME);
        registeredServices.add(oidcRegisteredService);
        registeredServices.add(duplicateOidcRegisteredService);

        ServicesManager servicesManager = createValidServicesManagerWith(registeredServices);
        ManagerService managerService = new ManagerServiceImpl(servicesManager, casOAuthConfiguration);

        Assert.assertEquals(Optional.empty(), managerService.getServiceByName(SERVICE_NAME));
    }

    @Test
    public void getServiceByID_serviceManagerThrowsRuntimeException_shouldReturnEmptyOptional() {
        ServicesManager servicesManager = Mockito.mock(ServicesManager.class);
        when(servicesManager.findServiceBy(SERVICE_NAME)).thenThrow(RuntimeException.class);
        ManagerService managerService = new ManagerServiceImpl(servicesManager, casOAuthConfiguration);

        Assert.assertFalse(managerService.getServiceByName(SERVICE_NAME).isPresent());
    }

    @Test
    public void getServiceName_managerReturnsValidName_shouldReturnName() {
        Collection<RegisteredService> registeredServices = new ArrayList<>();
        OidcRegisteredService oidcRegisteredService = new OidcRegisteredService();
        oidcRegisteredService.setClientId(SERVICE_NAME);
        oidcRegisteredService.setProperties(mockOidcRegisteredServiceProperties(SERVICE_NAME_KEY, SERVICE_NAME_VALUE));
        registeredServices.add(oidcRegisteredService);
        LocaleContextHolder.setLocale(Locale.forLanguageTag("et"));
        ServicesManager servicesManager = createValidServicesManagerWith(registeredServices);
        ManagerService managerService = new ManagerServiceImpl(servicesManager, casOAuthConfiguration);

        HashMap<String, RegisteredServiceProperty> serviceNames = new HashMap<>();
        serviceNames.put(SERVICE_NAME_KEY, oidcRegisteredService.getProperties().get(SERVICE_NAME_KEY));

        Assert.assertEquals(Optional.of(serviceNames), managerService.getServiceNames(SERVICE_NAME));
    }

    @Test
    public void getServiceShortName_managerReturnsValidEnglishShortName_shouldReturnEnglishShortName() {
        Collection<RegisteredService> registeredServices = new ArrayList<>();
        OidcRegisteredService oidcRegisteredService = new OidcRegisteredService();
        oidcRegisteredService.setClientId(SERVICE_NAME);
        oidcRegisteredService.setProperties(mockOidcRegisteredServiceProperties(SERVICE_SHORT_NAME_KEY_EN, SERVICE_SHORT_NAME_VALUE_EN));
        registeredServices.add(oidcRegisteredService);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        ServicesManager servicesManager = createValidServicesManagerWith(registeredServices);
        ManagerService managerService = new ManagerServiceImpl(servicesManager, casOAuthConfiguration);

        HashMap<String, RegisteredServiceProperty> serviceShortNames = new HashMap<>();
        serviceShortNames.put(SERVICE_SHORT_NAME_KEY_EN, oidcRegisteredService.getProperties().get(SERVICE_SHORT_NAME_KEY_EN));

        Assert.assertEquals(Optional.of(serviceShortNames), managerService.getServiceNames(SERVICE_NAME));
    }

    @Test
    public void getServiceShortName_managerReturnsValidRussianShortName_shouldReturnRussianShortName() {
        Collection<RegisteredService> registeredServices = new ArrayList<>();
        OidcRegisteredService oidcRegisteredService = new OidcRegisteredService();
        oidcRegisteredService.setClientId(SERVICE_NAME);
        oidcRegisteredService.setProperties(mockOidcRegisteredServiceProperties(SERVICE_SHORT_NAME_KEY_RU, SERVICE_SHORT_NAME_VALUE_RU));
        registeredServices.add(oidcRegisteredService);
        LocaleContextHolder.setLocale(Locale.forLanguageTag("ru"));
        ServicesManager servicesManager = createValidServicesManagerWith(registeredServices);
        ManagerService managerService = new ManagerServiceImpl(servicesManager, casOAuthConfiguration);

        HashMap<String, RegisteredServiceProperty> serviceShortNames = new HashMap<>();
        serviceShortNames.put(SERVICE_SHORT_NAME_KEY_RU, oidcRegisteredService.getProperties().get(SERVICE_SHORT_NAME_KEY_RU));

        Assert.assertEquals(Optional.of(serviceShortNames), managerService.getServiceNames(SERVICE_NAME));
    }

    @Test
    public void getServiceShortName_managerReturnsValidShortName_shouldReturnShortName() {
        Collection<RegisteredService> registeredServices = new ArrayList<>();
        OidcRegisteredService oidcRegisteredService = new OidcRegisteredService();
        oidcRegisteredService.setClientId(SERVICE_NAME);
        oidcRegisteredService.setProperties(mockOidcRegisteredServiceProperties(SERVICE_SHORT_NAME_KEY, SERVICE_SHORT_NAME_VALUE));
        registeredServices.add(oidcRegisteredService);
        LocaleContextHolder.setLocale(Locale.forLanguageTag("et"));
        ServicesManager servicesManager = createValidServicesManagerWith(registeredServices);
        ManagerService managerService = new ManagerServiceImpl(servicesManager, casOAuthConfiguration);

        HashMap<String, RegisteredServiceProperty> serviceShortNames = new HashMap<>();
        serviceShortNames.put(SERVICE_SHORT_NAME_KEY, oidcRegisteredService.getProperties().get(SERVICE_SHORT_NAME_KEY));

        Assert.assertEquals(Optional.of(serviceShortNames), managerService.getServiceNames(SERVICE_NAME));
    }

    @Test
    public void getServiceName_managerReturnsValidEnglishName_shouldReturnEnglishName() {
        Collection<RegisteredService> registeredServices = new ArrayList<>();
        OidcRegisteredService oidcRegisteredService = new OidcRegisteredService();
        oidcRegisteredService.setClientId(SERVICE_NAME);
        oidcRegisteredService.setProperties(mockOidcRegisteredServiceProperties(SERVICE_NAME_KEY_EN, SERVICE_NAME_VALUE_EN));
        registeredServices.add(oidcRegisteredService);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        ServicesManager servicesManager = createValidServicesManagerWith(registeredServices);
        ManagerService managerService = new ManagerServiceImpl(servicesManager, casOAuthConfiguration);

        HashMap<String, RegisteredServiceProperty> serviceNames = new HashMap<>();
        serviceNames.put(SERVICE_NAME_KEY_EN, oidcRegisteredService.getProperties().get(SERVICE_NAME_KEY_EN));

        Assert.assertEquals(Optional.of(serviceNames), managerService.getServiceNames(SERVICE_NAME));
    }

    @Test
    public void getServiceName_managerReturnsValidRussianName_shouldReturnRussianName() {
        Collection<RegisteredService> registeredServices = new ArrayList<>();
        OidcRegisteredService oidcRegisteredService = new OidcRegisteredService();
        oidcRegisteredService.setClientId(SERVICE_NAME);
        oidcRegisteredService.setProperties(mockOidcRegisteredServiceProperties(SERVICE_NAME_KEY_RU, SERVICE_NAME_VALUE_RU));
        registeredServices.add(oidcRegisteredService);
        LocaleContextHolder.setLocale(Locale.forLanguageTag("ru"));
        ServicesManager servicesManager = createValidServicesManagerWith(registeredServices);
        ManagerService managerService = new ManagerServiceImpl(servicesManager, casOAuthConfiguration);

        HashMap<String, RegisteredServiceProperty> serviceNames = new HashMap<>();
        serviceNames.put(SERVICE_NAME_KEY_RU, oidcRegisteredService.getProperties().get(SERVICE_NAME_KEY_RU));

        Assert.assertEquals(Optional.of(serviceNames), managerService.getServiceNames(SERVICE_NAME));
    }

    @Test
    public void getServiceShortName_managerReturnsNoProperties_shouldReturnEmptyName() {
        Collection<RegisteredService> registeredServices = new ArrayList<>();
        OidcRegisteredService oidcRegisteredService = new OidcRegisteredService();
        oidcRegisteredService.setClientId(SERVICE_NAME);
        registeredServices.add(oidcRegisteredService);
        ServicesManager servicesManager = createValidServicesManagerWith(registeredServices);
        ManagerService managerService = new ManagerServiceImpl(servicesManager, casOAuthConfiguration);

        Assert.assertEquals(Optional.of(new HashMap<String, RegisteredServiceProperty>()), managerService.getServiceNames(SERVICE_NAME));
    }

    @Test
    public void getAllAbstractRegisteredServices_managerReturnsValidServices_shouldReturnNonEmptyOptional() {
        Collection<RegisteredService> registeredServices = new ArrayList<>();
        Mockito.when(casOAuthConfiguration.oauthCallbackService()).thenReturn(mockService());
        Mockito.when(casOAuthConfiguration.oauthCallbackService().getId()).thenReturn(SERVICE_ID);
        AbstractRegisteredService abstractRegisteredService = Mockito.mock(AbstractRegisteredService.class);
        Mockito.when(abstractRegisteredService.getServiceId()).thenReturn(SERVICE_ID);
        registeredServices.add(abstractRegisteredService);

        ServicesManager servicesManager = createValidServicesManagerWith(registeredServices);
        ManagerService managerService = new ManagerServiceImpl(servicesManager, casOAuthConfiguration);

        Assert.assertTrue(managerService.getAllRegisteredServicesExceptType(OAuthRegisteredService.class).isPresent());
    }

    @Test
    public void getAllAbstractRegisteredServices_managerReturnsNoProperties_shouldReturnEmptyList() {
        Collection<RegisteredService> registeredServices = new ArrayList<>();
        OidcRegisteredService oidcRegisteredService = new OidcRegisteredService();
        oidcRegisteredService.setClientId(SERVICE_NAME);
        registeredServices.add(oidcRegisteredService);
        ServicesManager servicesManager = createValidServicesManagerWith(registeredServices);
        ManagerService managerService = new ManagerServiceImpl(servicesManager, casOAuthConfiguration);

        Assert.assertEquals(Optional.of(new ArrayList<AbstractRegisteredService>()), managerService.getAllRegisteredServicesExceptType(OAuthRegisteredService.class));
    }

    @Test
    public void getAllAbstractRegisteredServices_managerFiltersUnnecessaryServices_shouldReturnMultipleServices() {
        Collection<RegisteredService> registeredServices = new ArrayList<>(mockGetAllAbstractRegisteredServices().get());
        Mockito.when(casOAuthConfiguration.oauthCallbackService()).thenReturn(mockService());
        Mockito.when(casOAuthConfiguration.oauthCallbackService().getId()).thenReturn(SERVICE_ID);
        OidcRegisteredService oidcRegisteredService = new OidcRegisteredService();
        oidcRegisteredService.setClientId(SERVICE_NAME);
        registeredServices.add(oidcRegisteredService);
        ServicesManager servicesManager = createValidServicesManagerWith(registeredServices);
        ManagerService managerService = new ManagerServiceImpl(servicesManager, casOAuthConfiguration);

        Assert.assertEquals(1, managerService.getAllRegisteredServicesExceptType(OAuthRegisteredService.class).get().size());
    }

    private Map<String, RegisteredServiceProperty> mockOidcRegisteredServiceProperties(String key, String value) {
        Map<String, RegisteredServiceProperty> serviceProperties = new HashMap<>();
        RegisteredServicePropertyValues rspv = new RegisteredServicePropertyValues();
        Set<String> values = new HashSet<>();
        values.add(value);
        rspv.setValues(values);
        serviceProperties.put(key, rspv);

        return serviceProperties;
    }

    private Optional<List<AbstractRegisteredService>> mockGetAllAbstractRegisteredServices() {
        List<AbstractRegisteredService> abstractRegisteredServices = new ArrayList<>();
        AbstractRegisteredService oneAbstractRegisteredService = Mockito.mock(AbstractRegisteredService.class);
        AbstractRegisteredService twoAbstractRegisteredService = Mockito.mock(AbstractRegisteredService.class);
        Mockito.when(oneAbstractRegisteredService.getServiceId()).thenReturn(SERVICE_ID);
        Mockito.when(oneAbstractRegisteredService.getName()).thenReturn(SERVICE_NAME + "1");
        Mockito.when(twoAbstractRegisteredService.getServiceId()).thenReturn(SERVICE_ID + ".secondUrl");
        Mockito.when(twoAbstractRegisteredService.getName()).thenReturn(SERVICE_NAME + "2");

        abstractRegisteredServices.add(oneAbstractRegisteredService);
        abstractRegisteredServices.add(twoAbstractRegisteredService);

        return Optional.of(abstractRegisteredServices);
    }

    private static ServicesManager createValidServicesManagerWith(Collection<RegisteredService> services) {
        ServicesManager servicesManager = Mockito.mock(ServicesManager.class);
        when(servicesManager.getAllServices())
                .thenReturn(services);

        return servicesManager;
    }

    private static Service mockService() {
        return Mockito.mock(Service.class);
    }

    static class RegisteredServicePropertyValues implements RegisteredServiceProperty {

        private Set<String> values = new HashSet<>();

        public void setValues(Set<String> values) {
            this.values = values;
        }

        @Override
        public Set<String> getValues() {
            return values;
        }

        @Override
        public String getValue() {
            return values.iterator().next();
        }

        @Override
        public boolean contains(String value) {
            return false;
        }
    }
}
