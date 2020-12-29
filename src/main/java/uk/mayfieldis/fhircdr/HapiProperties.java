package uk.mayfieldis.fhircdr;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.search.elastic.ElasticsearchHibernatePropertiesBuilder;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import com.google.common.annotations.VisibleForTesting;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class HapiProperties {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HapiProperties.class);

    static final String ALLOW_EXTERNAL_REFERENCES = "allow_external_references";
    static final String ALLOW_MULTIPLE_DELETE = "allow_multiple_delete";
    static final String ALLOW_PLACEHOLDER_REFERENCES = "allow_placeholder_references";
    static final String REUSE_CACHED_SEARCH_RESULTS_MILLIS = "reuse_cached_search_results_millis";
    static final String DATASOURCE_DRIVER = "datasource.driver";
    static final String DATASOURCE_MAX_POOL_SIZE = "datasource.max_pool_size";
    static final String DATASOURCE_PASSWORD = "datasource.password";
    static final String DATASOURCE_URL = "datasource.url";
    static final String DATASOURCE_USERNAME = "datasource.username";
    static final String DEFAULT_ENCODING = "default_encoding";
    static final String DEFAULT_PAGE_SIZE = "default_page_size";
    static final String DEFAULT_PRETTY_PRINT = "default_pretty_print";
    static final String ETAG_SUPPORT = "etag_support";
    static final String FHIR_VERSION = "fhir_version";
    static final String HAPI_PROPERTIES = "hapi.properties";
    static final String LOGGER_ERROR_FORMAT = "logger.error_format";
    static final String LOGGER_FORMAT = "logger.format";
    static final String LOGGER_LOG_EXCEPTIONS = "logger.log_exceptions";
    static final String LOGGER_NAME = "logger.name";
    static final String MAX_FETCH_SIZE = "max_fetch_size";
    static final String MAX_PAGE_SIZE = "max_page_size";
    static final String SERVER_ADDRESS = "host_address";
    static final String SERVER_BASE = "server.base";
    static final String SERVER_ID = "server.id";
    static final String SERVER_NAME = "server.name";
    static final String SUBSCRIPTION_EMAIL_ENABLED = "subscription.email.enabled";
    static final String SUBSCRIPTION_RESTHOOK_ENABLED = "subscription.resthook.enabled";
    static final String SUBSCRIPTION_WEBSOCKET_ENABLED = "subscription.websocket.enabled";
    static final String CORS_ENABLED = "cors.enabled";
    static final String CORS_ALLOWED_ORIGIN = "cors.allowed_origin";
    static final String ALLOW_CONTAINS_SEARCHES = "allow_contains_searches";
    static final String ALLOW_OVERRIDE_DEFAULT_SEARCH_PARAMS = "allow_override_default_search_params";
    static final String EMAIL_FROM = "email.from";

    static final String SOFTWARE_NAME = "software.name";
    static final String SOFTWARE_VERSION = "software.version";
    static final String SOFTWARE_IMPLEMENTATION_DESC = "software.implementation";

    static final String VALIDATION_FLAG = "validate.flag";
    static final String VALIDATION_SERVER = "validation.server";

    static final String APP_USER = "jolokia.username";
    static final String APP_PASSWORD = "jolokia.password";

    static final String HIBERNATE_DIALECT = "hibernate.dialect";
    static final String HIBERNATE_ELASTICSEARCH_HOST = "hibernate.search.default.elasticsearch.host";
    static final String HIBERNATE_SHOW_SQL = "hibernate.show_sql";

    static final String SECURITY_OAUTH2 = "security.oauth2";
    static final String SECURITY_OAUTH2_SERVER = "security.oauth2.server";
    static final String SECURITY_OAUTH2_CONFIG = "security.oauth2.configuration.server";
    static final String SECURITY_OAUTH2_ALLOW_READONLY = "security.oauth2.allowReadOnly";
    static final String SECURITY_OAUTH2_SCOPE = "security.oauth2.scope";
    static final String SECURITY_SMART_SCOPE = "security.oauth2.smart";

    static final String ENABLE_INDEX_MISSING_FIELDS = "enable_index_missing_fields";
    static final String AUTO_CREATE_PLACEHOLDER_REFERENCE_TARGETS = "auto_create_placeholder_reference_targets";
    static final String ENFORCE_REFERENTIAL_INTEGRITY_ON_WRITE = "enforce_referential_integrity_on_write";
    static final String ENFORCE_REFERENTIAL_INTEGRITY_ON_DELETE = "enforce_referential_integrity_on_delete";
    static final String BINARY_STORAGE_ENABLED = "binary_storage.enabled";
    static final String ALLOW_CASCADING_DELETES = "allow_cascading_deletes";
    static final String ALLOWED_BUNDLE_TYPES = "allowed_bundle_types";
    static final String CORS_ALLOW_CREDENTIALS = "cors.allowCredentials";
    private static final String VALIDATE_REQUESTS_ENABLED = "validation.requests.enabled";
    private static final String VALIDATE_RESPONSES_ENABLED = "validation.responses.enabled";
    private static final String FILTER_SEARCH_ENABLED = "filter_search.enabled";
    private static final String GRAPHQL_ENABLED = "graphql.enabled";
    private static final String BULK_EXPORT_ENABLED = "bulk.export.enabled";
    public static final String EXPIRE_SEARCH_RESULTS_AFTER_MINS = "retain_cached_searches_mins";

    public static final String CORE_IG_PACKAGE = "core.ig.package";
    public static final String CORE_IG_VERSION = "core.ig.version";
    public static final String CORE_IG_URL = "core.ig.url";

    public static final String SERVER_IG_DESCRIPTION = "server.ig.description";
    public static final String SERVER_IG_PACKAGE = "server.ig.package";
    public static final String SERVER_IG_VERSION = "server.ig.version";
    public static final String SERVER_IG_URL = "server.ig.url";

    static final String SNOMED_VERSION_URL = "terminology.snomed.version";
    static final String TERMINOLOGY_VALIDATION_FLAG = "terminology.validation.flag";
    static final String TERMINOLOGY_SERVER = "terminology.server";

    static final String JMS_ENABLED_FLAG = "jms.flag";

    private static Properties properties;

    /*
     * Force the configuration to be reloaded
     */
    public static void forceReload() {
        properties = null;
        getProperties();
    }

    /**
     * This is mostly here for unit tests. Use the actual properties file
     * to set values
     */
    @VisibleForTesting
    public static void setProperty(String theKey, String theValue) {
        getProperties().setProperty(theKey, theValue);
    }

    public static Properties getProperties() {
        if (properties == null) {
            // Load the configurable properties file
            try (InputStream in = HapiProperties.class.getClassLoader().getResourceAsStream(HAPI_PROPERTIES)){
                HapiProperties.properties = new Properties();
                HapiProperties.properties.load(in);
            } catch (Exception e) {
                throw new ConfigurationException("Could not load HAPI properties", e);
            }

            Properties overrideProps = loadOverrideProperties();
            if(overrideProps != null) {
                properties.putAll(overrideProps);
            }
        }

        return properties;
    }

    /**
     * If a configuration file path is explicitly specified via -Dhapi.properties=<path>, the properties there will
     * be used to override the entries in the default hapi.properties file (currently under WEB-INF/classes)
     * @return properties loaded from the explicitly specified configuraiton file if there is one, or null otherwise.
     */
    private static Properties loadOverrideProperties() {
        String confFile = System.getProperty(HAPI_PROPERTIES);
        if(confFile != null) {
            try {
                Properties props = new Properties();
                props.load(new FileInputStream(confFile));
                return props;
            }
            catch (Exception e) {
                throw new ConfigurationException("Could not load HAPI properties file: " + confFile, e);
            }
        }

        return null;
    }

    private static String getProperty(String propertyName) {
        Properties properties = HapiProperties.getProperties();
        log.trace("Looking for property = {}", propertyName);
        if (System.getenv(propertyName)!= null) {
            String value= System.getenv(propertyName);
            log.info("System Environment property Found {} = {}", propertyName, value);
            return value;
        }
        if (System.getProperty(propertyName)!= null) {
            String value= System.getProperty(propertyName);
            log.info("System Property Found {} = {}" , propertyName, value);
            return value;
        }
        if (properties != null) {
            return properties.getProperty(propertyName);
        }

        return null;
    }

    private static String getProperty(String propertyName, String defaultValue) {
        Properties properties = HapiProperties.getProperties();

        if (properties != null) {
            String value = properties.getProperty(propertyName);

            if (value != null && value.length() > 0) {
                return value;
            }
        }

        return defaultValue;
    }

    private static Boolean getPropertyBoolean(String propertyName, Boolean defaultValue) {
        String value = HapiProperties.getProperty(propertyName);

        if (value == null || value.length() == 0) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value);
    }

    private static Integer getPropertyInteger(String propertyName, Integer defaultValue) {
        String value = HapiProperties.getProperty(propertyName);

        if (value == null || value.length() == 0) {
            return defaultValue;
        }

        return Integer.parseInt(value);
    }

    private static <T extends Enum> T getPropertyEnum(String thePropertyName, Class<T> theEnumType, T theDefaultValue) {
        String value = getProperty(thePropertyName, theDefaultValue.name());
        return (T) Enum.valueOf(theEnumType, value);
    }

    public static FhirVersionEnum getFhirVersion() {
        String fhirVersionString = HapiProperties.getProperty(FHIR_VERSION);

        if (fhirVersionString != null && fhirVersionString.length() > 0) {
            return FhirVersionEnum.valueOf(fhirVersionString);
        }

        return FhirVersionEnum.R4;
    }

    public static ETagSupportEnum getEtagSupport() {
        String etagSupportString = HapiProperties.getProperty(ETAG_SUPPORT);

        if (etagSupportString != null && etagSupportString.length() > 0) {
            return ETagSupportEnum.valueOf(etagSupportString);
        }

        return ETagSupportEnum.ENABLED;
    }

    public static EncodingEnum getDefaultEncoding() {
        String defaultEncodingString = HapiProperties.getProperty(DEFAULT_ENCODING);

        if (defaultEncodingString != null && defaultEncodingString.length() > 0) {
            return EncodingEnum.valueOf(defaultEncodingString);
        }

        return EncodingEnum.JSON;
    }

    public static Boolean getDefaultPrettyPrint() {
        return HapiProperties.getPropertyBoolean(DEFAULT_PRETTY_PRINT, true);
    }

    public static String getServerAddress() {
        return HapiProperties.getProperty(SERVER_ADDRESS);
    }

    public static Integer getMaximumFetchSize() {
        return HapiProperties.getPropertyInteger(MAX_FETCH_SIZE, Integer.MAX_VALUE);
    }


    public static String getDataSourceDriver() {
        return HapiProperties.getProperty(DATASOURCE_DRIVER);
    }

    public static Integer getDataSourceMaxPoolSize() {
        return HapiProperties.getPropertyInteger(DATASOURCE_MAX_POOL_SIZE, 10);
    }

    public static String getDataSourceUrl() {
        return HapiProperties.getProperty(DATASOURCE_URL);
    }

    public static String getDataSourceUsername() {
        return HapiProperties.getProperty(DATASOURCE_USERNAME);
    }

    public static String getDataSourcePassword() {
        return HapiProperties.getProperty(DATASOURCE_PASSWORD);
    }

    public static Boolean getAllowMultipleDelete() {
        return HapiProperties.getPropertyBoolean(ALLOW_MULTIPLE_DELETE, false);
    }

    public static Boolean getAllowExternalReferences() {
        return HapiProperties.getPropertyBoolean(ALLOW_EXTERNAL_REFERENCES, false);
    }

    public static Boolean getExpungeEnabled() {
        return HapiProperties.getPropertyBoolean("expunge_enabled", true);
    }


    public static Boolean getCorsEnabled() {
        return HapiProperties.getPropertyBoolean(CORS_ENABLED, true);
    }

    public static String getCorsAllowedOrigin() {
        return HapiProperties.getProperty(CORS_ALLOWED_ORIGIN, "*");
    }

    public static Integer getDefaultPageSize() {
        return HapiProperties.getPropertyInteger(DEFAULT_PAGE_SIZE, 20);
    }

    public static Integer getMaximumPageSize() {
        return HapiProperties.getPropertyInteger(MAX_PAGE_SIZE, 200);
    }

    public static String getServerBase() {
        return HapiProperties.getProperty(SERVER_BASE, "/fhir");
    }

    public static String getServerName() {
        return HapiProperties.getProperty(SERVER_NAME);
    }

    public static String getServerId() {
        return HapiProperties.getProperty(SERVER_ID, "home");
    }

    public static Boolean getAllowPlaceholderReferences() {
        return HapiProperties.getPropertyBoolean(ALLOW_PLACEHOLDER_REFERENCES, true);
    }

    public static Boolean getSubscriptionEmailEnabled() {
        return HapiProperties.getPropertyBoolean(SUBSCRIPTION_EMAIL_ENABLED, false);
    }

    public static Boolean getSubscriptionRestHookEnabled() {
        return HapiProperties.getPropertyBoolean(SUBSCRIPTION_RESTHOOK_ENABLED, false);
    }

    public static Boolean getSubscriptionWebsocketEnabled() {
        return HapiProperties.getPropertyBoolean(SUBSCRIPTION_WEBSOCKET_ENABLED, false);
    }

    public static Boolean getAllowContainsSearches() {
        return HapiProperties.getPropertyBoolean(ALLOW_CONTAINS_SEARCHES, true);
    }

    public static Boolean getAllowOverrideDefaultSearchParams() {
        return HapiProperties.getPropertyBoolean(ALLOW_OVERRIDE_DEFAULT_SEARCH_PARAMS, true);
    }

    public static String getEmailFrom() {
        return HapiProperties.getProperty(EMAIL_FROM, "some@test.com");
    }

    public static Boolean getEmailEnabled() {
        return HapiProperties.getPropertyBoolean("email.enabled", false);
    }

    public static String getEmailHost() {
        return HapiProperties.getProperty("email.host");
    }

    public static Integer getEmailPort() {
        return HapiProperties.getPropertyInteger("email.port", 0);
    }

    public static String getEmailUsername() {
        return HapiProperties.getProperty("email.username");
    }

    public static String getEmailPassword() {
        return HapiProperties.getProperty("email.password");
    }

    public static Long getReuseCachedSearchResultsMillis() {
        String value = HapiProperties.getProperty(REUSE_CACHED_SEARCH_RESULTS_MILLIS, "-1");
        return Long.valueOf(value);
    }

    public static String getSoftwareName() {
        return HapiProperties.getProperty(SOFTWARE_NAME);
    }

    public static String getSoftwareVersion() {
        return HapiProperties.getProperty(SOFTWARE_VERSION);
    }

    public static String getSoftwareImplementationDesc() {
        return HapiProperties.getProperty(SOFTWARE_IMPLEMENTATION_DESC);
    }



    public static String getAppUser() {
        return HapiProperties.getProperty(APP_USER);
    }

    public static String getAppPassword() {
        return HapiProperties.getProperty(APP_PASSWORD);
    }


    public static boolean getSecurityOAuth2() {
        return HapiProperties.getPropertyBoolean(SECURITY_OAUTH2, false);
    }


    public static boolean getSecurityOAuth2AllowReadOnly() {
        return HapiProperties.getPropertyBoolean(SECURITY_OAUTH2_ALLOW_READONLY, false);
    }

    public static String getSecurityOAuth2RequiredScope() {
        return HapiProperties.getProperty(SECURITY_OAUTH2_SCOPE);
    }

    public static String getSecurityOAuth2Config() {
        return HapiProperties.getProperty(SECURITY_OAUTH2_CONFIG);
    }

    public static String getSecurityOAuth2Server() {
        return HapiProperties.getProperty(SECURITY_OAUTH2_SERVER);
    }


    public static Long getExpireSearchResultsAfterMins() {
        String value = HapiProperties.getProperty(EXPIRE_SEARCH_RESULTS_AFTER_MINS, "60");
        return Long.valueOf(value);
    }

    public static Boolean getCorsAllowedCredentials() {
        return HapiProperties.getPropertyBoolean(CORS_ALLOW_CREDENTIALS, false);
    }

    public static boolean getValidateRequestsEnabled() {
        return HapiProperties.getPropertyBoolean(VALIDATE_REQUESTS_ENABLED, false);
    }

    public static boolean getValidateResponsesEnabled() {
        return HapiProperties.getPropertyBoolean(VALIDATE_RESPONSES_ENABLED, false);
    }

    public static boolean getFilterSearchEnabled() {
        return HapiProperties.getPropertyBoolean(FILTER_SEARCH_ENABLED, true);
    }

    public static boolean getGraphqlEnabled() {
        return HapiProperties.getPropertyBoolean(GRAPHQL_ENABLED, true);
    }

    public static boolean getEnforceReferentialIntegrityOnDelete() {
        return HapiProperties.getPropertyBoolean(ENFORCE_REFERENTIAL_INTEGRITY_ON_DELETE, true);
    }

    public static boolean getEnforceReferentialIntegrityOnWrite() {
        return HapiProperties.getPropertyBoolean(ENFORCE_REFERENTIAL_INTEGRITY_ON_WRITE, true);
    }

    public static boolean getAutoCreatePlaceholderReferenceTargets() {
        return HapiProperties.getPropertyBoolean(AUTO_CREATE_PLACEHOLDER_REFERENCE_TARGETS, true);
    }

    public static boolean getEnableIndexMissingFields() {
        return HapiProperties.getPropertyBoolean(ENABLE_INDEX_MISSING_FIELDS, false);
    }

    public static boolean isElasticSearchEnabled() {
        return HapiProperties.getPropertyBoolean("elasticsearch.enabled", false);
    }

    public static boolean isJmsEnabledFlag() {
        return HapiProperties.getPropertyBoolean(JMS_ENABLED_FLAG, false);
    }


    public static Properties getJpaProperties() {
        Properties retVal = loadProperties();

        if (isElasticSearchEnabled()) {
            ElasticsearchHibernatePropertiesBuilder builder = new ElasticsearchHibernatePropertiesBuilder();
            builder.setRequiredIndexStatus(getPropertyEnum("elasticsearch.required_index_status", ElasticsearchIndexStatus.class, ElasticsearchIndexStatus.YELLOW));
            builder.setRestUrl(getProperty("elasticsearch.rest_url"));
            builder.setUsername(getProperty("elasticsearch.username"));
            builder.setPassword(getProperty("elasticsearch.password"));
            builder.setIndexSchemaManagementStrategy(getPropertyEnum("elasticsearch.schema_management_strateg", IndexSchemaManagementStrategy.class, IndexSchemaManagementStrategy.CREATE));
            builder.setDebugRefreshAfterWrite(getPropertyBoolean("elasticsearch.debug.refresh_after_write", false));
            builder.setDebugPrettyPrintJsonLog(getPropertyBoolean("elasticsearch.debug.pretty_print_json_log", false));
            builder.apply(retVal);
        }

        return retVal;
    }

    public static boolean isBinaryStorageEnabled() {
        return HapiProperties.getPropertyBoolean(BINARY_STORAGE_ENABLED, true);
    }

    public static  String getAllowedBundleTypes() {
        return HapiProperties.getProperty(ALLOWED_BUNDLE_TYPES, "");
    }

    public static Boolean getAllowCascadingDeletes() {
        return HapiProperties.getPropertyBoolean(ALLOW_CASCADING_DELETES, false);
    }

    public static boolean getBulkExportEnabled() {
        return HapiProperties.getPropertyBoolean(BULK_EXPORT_ENABLED, true);
    }

    public static String getCoreIgPackage() {
        return HapiProperties.getProperty(CORE_IG_PACKAGE,"");
    }
    public static String getCoreIgVersion() {
        return HapiProperties.getProperty(CORE_IG_VERSION,"");
    }
    public static String getCoreIgUrl() {
        return HapiProperties.getProperty(CORE_IG_URL,"");
    }

    public static String getServerIgPackage() {
        return HapiProperties.getProperty(SERVER_IG_PACKAGE,"");
    }
    public static String getServerIgVersion() {
        return HapiProperties.getProperty(SERVER_IG_VERSION,"");
    }
    public static String getServerIgUrl() {
        return HapiProperties.getProperty(SERVER_IG_URL,"");
    }

    public static String getSnomedVersionUrl() {
        return HapiProperties.getProperty(SNOMED_VERSION_URL);
    }
    public static String getTerminologyServer() {
        return HapiProperties.getProperty(TERMINOLOGY_SERVER);
    }
    public static boolean getValidateTerminologyEnabled() {
        return HapiProperties.getPropertyBoolean(TERMINOLOGY_VALIDATION_FLAG, false);
    }


    @NotNull
    private static Properties loadProperties() {
        // Load the configurable properties file
        Properties properties;
        try (InputStream in = HapiProperties.class.getClassLoader().getResourceAsStream(HAPI_PROPERTIES)) {
            properties = new Properties();
            properties.load(in);
        } catch (Exception e) {
            throw new ConfigurationException("Could not load HAPI properties", e);
        }

        Properties overrideProps = loadOverrideProperties();
        if (overrideProps != null) {
            properties.putAll(overrideProps);
        }
        return properties;
    }

    public static String getLoggerName() {
        return HapiProperties.getProperty(LOGGER_NAME, "fhirtest.access");
    }

    public static String getLoggerFormat() {
        return HapiProperties.getProperty(LOGGER_FORMAT, "Path[${servletPath}] Source[${requestHeader.x-forwarded-for}] Operation[${operationType} ${operationName} ${idOrResourceName}] UA[${requestHeader.user-agent}] Params[${requestParameters}] ResponseEncoding[${responseEncodingNoDefault}]");
    }

    public static String getLoggerErrorFormat() {
        return HapiProperties.getProperty(LOGGER_ERROR_FORMAT, "ERROR - ${requestVerb} ${requestUrl}");
    }

    public static Boolean getLoggerLogExceptions() {
        return HapiProperties.getPropertyBoolean(LOGGER_LOG_EXCEPTIONS, true);
    }

}
