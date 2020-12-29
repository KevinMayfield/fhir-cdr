
package uk.mayfieldis.fhircdr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.binstore.BinaryStorageInterceptor;
import ca.uhn.fhir.jpa.bulk.provider.BulkDataExportProvider;
import ca.uhn.fhir.jpa.interceptor.CascadingDeleteInterceptor;
import ca.uhn.fhir.jpa.provider.GraphQLProvider;
import ca.uhn.fhir.jpa.provider.SubscriptionTriggeringProvider;
import ca.uhn.fhir.jpa.provider.TerminologyUploaderProvider;
import ca.uhn.fhir.jpa.provider.r4.JpaConformanceProviderR4;
import ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.searchparam.registry.ISearchParamRegistry;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.IValidatorModule;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.codesystems.BundleType;
import org.hl7.fhir.utilities.cache.NpmPackage;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import uk.mayfieldis.hapifhir.FHIRServerProperties;
import uk.mayfieldis.hapifhir.interceptor.ServerRequestValidatingInterceptor;
import uk.mayfieldis.hapifhir.interceptor.ServerResponseValidatingInterceptor;
import uk.mayfieldis.hapifhir.interceptor.oauth2.OAuth2Interceptor;
import uk.mayfieldis.hapifhir.provider.ServerValidationProvider;
import uk.mayfieldis.hapifhir.support.ServerFHIRValidation;
import uk.mayfieldis.hapifhir.validation.IGValidationSupport;
import uk.mayfieldis.hapifhir.validation.RemoteTerminologyServerValidation;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.util.*;

@WebServlet(urlPatterns = { "/R4/*" }, displayName = "FHIR CDR Server")
public class JpaRestfulServer extends RestfulServer {

	private static final long serialVersionUID = 1L;

	private final ApplicationContext appCtx;


	ServerFHIRValidation serverFHIRValidation;

	NpmPackage serverIgPackage;

	NpmPackage coreIgPackage;

	ISearchParamRegistry searchParamRegistry;

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JpaRestfulServer.class);

	JpaRestfulServer(ApplicationContext context,
					 NpmPackage serverIgPackage,
					 NpmPackage coreIgPackage,
					 ServerFHIRValidation serverFHIRValidation,
					 ISearchParamRegistry searchParamRegistry) {

		// This is called from spring boot
		this.appCtx = context;

		this.coreIgPackage = coreIgPackage;
		this.serverIgPackage = serverIgPackage;
		this.serverFHIRValidation = serverFHIRValidation;
		this.searchParamRegistry = searchParamRegistry;

	}



	@SuppressWarnings("unchecked")

	@Override
	protected void initialize() throws ServletException {
		super.initialize();

		FhirVersionEnum fhirVersion = FhirVersionEnum.R4;

		/*
		 * ResourceProviders are fetched from the Spring context
		 */

		ResourceProviderFactory resourceProviders;
		Object systemProvider;

		resourceProviders = appCtx.getBean("myResourceProvidersR4", ResourceProviderFactory.class);
		systemProvider = appCtx.getBean("mySystemProviderR4", JpaSystemProviderR4.class);


		setFhirContext(appCtx.getBean(FhirContext.class));

		registerProviders(resourceProviders.createProviders());
		registerProvider(systemProvider);

		// Plain provider

		List<Object> plainProviders=new ArrayList<Object>();
		if (FHIRServerProperties.getValidationFlag()) {
			plainProviders.add(appCtx.getBean(ServerValidationProvider.class));
		}
		registerProviders(plainProviders);

		/*
		 * The conformance provider exports the supported resources, search parameters, etc for
		 * this server. The JPA version adds resourceProviders counts to the exported statement, so it
		 * is a nice addition.
		 *
		 * You can also create your own subclass of the conformance provider if you need to
		 * provide further customization of your server's CapabilityStatement
		 */



		/*
		 * This server tries to dynamically generate narratives
		 */
		FhirContext ctx = getFhirContext();

		IFhirSystemDao<Bundle, Meta> systemDao = appCtx.getBean("mySystemDaoR4", IFhirSystemDao.class);

		JpaConformanceProviderR4 confProvider = new IGJPAConformanceProvider(this, systemDao, appCtx.getBean(DaoConfig.class), serverIgPackage, ctx,searchParamRegistry);
		confProvider.setImplementationDescription("HAPI FHIR R4 Server");
		setServerConformanceProvider(confProvider);

		/*
		 * Enable ETag Support (this is already the default)
		 */
		setETagSupport(HapiProperties.getEtagSupport());



		/*
		 * Default to JSON and pretty printing
		 */
		setDefaultPrettyPrint(HapiProperties.getDefaultPrettyPrint());

		/*
		 * Default encoding
		 */
		setDefaultResponseEncoding(HapiProperties.getDefaultEncoding());


		setPagingProvider(appCtx.getBean(DatabaseBackedPagingProvider.class));


		LoggingInterceptor
				loggingInterceptor = new LoggingInterceptor();
		loggingInterceptor.setLoggerName(HapiProperties.getLoggerName());
		loggingInterceptor.setMessageFormat(HapiProperties.getLoggerFormat());
		loggingInterceptor.setErrorMessageFormat(HapiProperties.getLoggerErrorFormat());
		loggingInterceptor.setLogExceptions(HapiProperties.getLoggerLogExceptions());
		this.registerInterceptor(loggingInterceptor);


		String serverAddress = HapiProperties.getServerAddress();
		if (serverAddress != null && serverAddress.length() > 0) {
			setServerAddressStrategy(new HardcodedServerAddressStrategy(serverAddress));
		}

		setServerName(HapiProperties.getServerName());
		setServerVersion(HapiProperties.getSoftwareVersion());
		setImplementationDescription(HapiProperties.getSoftwareImplementationDesc());


		if (HapiProperties.getSecurityOAuth2()) {
			try {
				OAuth2Interceptor oAuth2Interceptor = new OAuth2Interceptor(appCtx);
				getInterceptorService().registerInterceptor(oAuth2Interceptor);
			} catch (Exception ex) {
				log.error(ex.getMessage());
				throw new ServletException(ex.getMessage());
			}
		}
		/*
		 * If you are using DSTU3+, you may want to add a terminology uploader, which allows
		 * uploading of external terminologies such as Snomed CT. Note that this uploader
		 * does not have any security attached (any anonymous user may use it by default)
		 * so it is a potential security vulnerability. Consider using an AuthorizationInterceptor
		 * with this feature.
		 */
		if (false) { // <-- DISABLED RIGHT NOW
			registerProvider(appCtx.getBean(TerminologyUploaderProvider.class));
		}

		// If you want to enable the $trigger-subscription operation to allow
		// manual triggering of a subscription delivery, enable this provider
		if (false) { // <-- DISABLED RIGHT NOW
			SubscriptionTriggeringProvider retriggeringProvider = appCtx
					.getBean(SubscriptionTriggeringProvider.class);
			registerProvider(retriggeringProvider);
		}

		// Define your CORS configuration. This is an example
		// showing a typical setup. You should customize this
		// to your specific needs
		if (HapiProperties.getCorsEnabled()) {
			CorsConfiguration config = new CorsConfiguration();
			config.addAllowedHeader(HttpHeaders.ORIGIN);
			config.addAllowedHeader(HttpHeaders.ACCEPT);
			config.addAllowedHeader(HttpHeaders.CONTENT_TYPE);
			config.addAllowedHeader(HttpHeaders.AUTHORIZATION);
			config.addAllowedHeader(HttpHeaders.CACHE_CONTROL);
			config.addAllowedHeader("x-fhir-starter");
			config.addAllowedHeader("X-Requested-With");
			config.addAllowedHeader("Prefer");
			String allAllowedCORSOrigins = HapiProperties.getCorsAllowedOrigin();
			Arrays.stream(allAllowedCORSOrigins.split(",")).forEach(o -> {
				config.addAllowedOrigin(o);
			});
			config.addAllowedOrigin(HapiProperties.getCorsAllowedOrigin());

			config.addExposedHeader("Location");
			config.addExposedHeader("Content-Location");
			config.setAllowedMethods(
					Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
			config.setAllowCredentials(HapiProperties.getCorsAllowedCredentials());

			// Create the interceptor and register it
			CorsInterceptor interceptor = new CorsInterceptor(config);
			registerInterceptor(interceptor);
		}

		// If subscriptions are enabled, we want to register the interceptor that
		// will activate them and match results against them
		/*
		if (HapiProperties.getSubscriptionWebsocketEnabled() ||
				HapiProperties.getSubscriptionEmailEnabled() ||
				HapiProperties.getSubscriptionRestHookEnabled()) {
			// Loads subscription interceptors (SubscriptionActivatingInterceptor, SubscriptionMatcherInterceptor)
			// with activation of scheduled subscription
			SubscriptionInterceptorLoader subscriptionInterceptorLoader = appCtx
					.getBean(SubscriptionInterceptorLoader.class);
			subscriptionInterceptorLoader.registerInterceptors();

			// Subscription debug logging
			IInterceptorService interceptorService = appCtx.getBean(IInterceptorService.class);
			interceptorService.registerInterceptor(new SubscriptionDebugLogInterceptor());
		}*/

		// Cascading deletes
		DaoRegistry daoRegistry = appCtx.getBean(DaoRegistry.class);
		IInterceptorBroadcaster interceptorBroadcaster = appCtx.getBean(IInterceptorBroadcaster.class);
		if (HapiProperties.getAllowCascadingDeletes()) {
			CascadingDeleteInterceptor
					cascadingDeleteInterceptor = new CascadingDeleteInterceptor(
					ctx, daoRegistry, interceptorBroadcaster);
			getInterceptorService().registerInterceptor(cascadingDeleteInterceptor);
		}

		// Binary Storage
		if (HapiProperties.isBinaryStorageEnabled()) {
			BinaryStorageInterceptor binaryStorageInterceptor = appCtx
					.getBean(BinaryStorageInterceptor.class);
			getInterceptorService().registerInterceptor(binaryStorageInterceptor);
		}



		// Validation
		IValidatorModule validatorModule = null;

		try {
			validatorModule = fhirValidatorR4(ctx);

		} catch (Exception e) {
			throw new ServletException(e.getMessage());
		}

		if (validatorModule != null && FHIRServerProperties.getValidationFlag()) {

			if (FHIRServerProperties.getValidateRequestsEnabled() && serverIgPackage != null) {
				ServerRequestValidatingInterceptor interceptor = new ServerRequestValidatingInterceptor(ctx,serverFHIRValidation);
				interceptor.setFailOnSeverity(ResultSeverityEnum.ERROR);
				interceptor.setValidatorModules(Collections.singletonList(validatorModule));
				registerInterceptor(interceptor);
			}
			if (FHIRServerProperties.getValidateResponsesEnabled() && serverIgPackage != null) {
				ServerResponseValidatingInterceptor interceptor = new ServerResponseValidatingInterceptor(serverIgPackage, serverFHIRValidation);
				interceptor.setFailOnSeverity(ResultSeverityEnum.ERROR);
				interceptor.setValidatorModules(Collections.singletonList(validatorModule));
				registerInterceptor(interceptor);
			}
		}


		// GraphQL
		if (HapiProperties.getGraphqlEnabled()
			&& fhirVersion.isEqualOrNewerThan(FhirVersionEnum.R4)) {
				registerProvider(appCtx.getBean(GraphQLProvider.class));
		}

		if (!HapiProperties.getAllowedBundleTypes().isEmpty()) {
			String allowedBundleTypesString = HapiProperties.getAllowedBundleTypes();
			Set<String> allowedBundleTypes = new HashSet<>();
			Arrays.stream(allowedBundleTypesString.split(",")).forEach(o -> {
				BundleType type = BundleType.valueOf(o);
				allowedBundleTypes.add(type.toCode());
			});
			DaoConfig config = appCtx.getBean(DaoConfig.class);
			config.setBundleTypesAllowedForStorage(
					Collections.unmodifiableSet(new TreeSet<>(allowedBundleTypes)));
		}

		// Bulk Export
		if (HapiProperties.getBulkExportEnabled()) {
			registerProvider(appCtx.getBean(BulkDataExportProvider.class));
		}

	}


	public FhirInstanceValidator fhirValidatorR4 (FhirContext r4ctx) throws Exception {

		FhirValidator val = r4ctx.newValidator();

		ValidationSupportChain validationSupportChain = new ValidationSupportChain();



		DefaultProfileValidationSupport defaultProfileValidationSupport = new DefaultProfileValidationSupport(r4ctx);
		validationSupportChain.addValidationSupport(defaultProfileValidationSupport);

		IWorkerContext context = new HapiWorkerContext(r4ctx,validationSupportChain);


		if (coreIgPackage !=null) {
			IGValidationSupport igvs = new IGValidationSupport();

			PrePopulatedValidationSupport igCoreVS = igvs.getValidationSupport(r4ctx, coreIgPackage);
			validationSupportChain.addValidationSupport(igCoreVS);
			igvs.createSnapshots(context,validationSupportChain);

		}


		if (serverIgPackage !=null) {
			// Ordering is important here. Need core loaded before validation package
			IGValidationSupport igvs = new IGValidationSupport();
			PrePopulatedValidationSupport igServerVS = igvs.getValidationSupport(r4ctx, serverIgPackage);
			validationSupportChain.addValidationSupport(igServerVS);
			igvs.createSnapshots(context,validationSupportChain);

		}



		if (FHIRServerProperties.getValidateTerminologyEnabled()) {
			if (FHIRServerProperties.getTerminologyServer() != null) {

				// Use in memory validation first
				// Note: this requires ValueSets to be expanded
				validationSupportChain.addValidationSupport(new InMemoryTerminologyServerValidationSupport(r4ctx));

				// Use ontoserver
				// Create a module that uses a remote terminology service
				RemoteTerminologyServerValidation remoteTermSvc = new RemoteTerminologyServerValidation(r4ctx);
				//RemoteTerminologyServiceValidationSupport remoteTermSvc = new RemoteTerminologyServiceValidationSupport(r4ctx);
				//RemoteTerminologyServiceValidationSupportOnto remoteTermSvc = new RemoteTerminologyServiceValidationSupportOnto(r4ctx);

				remoteTermSvc.setBaseUrl(FHIRServerProperties.getTerminologyServer());
				validationSupportChain.addValidationSupport(remoteTermSvc);
			} else {

			}

		}
// Wrap the chain in a cache to improve performance
		CachingValidationSupport cache = new CachingValidationSupport(validationSupportChain);

		// A ping to force loading of validation support classes
		try {
			Patient patient = new Patient();
			val.validateWithResult(patient);
		} catch (Exception ex){
			log.error(ex.getMessage());
		}

		val.setValidateAgainstStandardSchema(FHIRServerProperties.getValidationSchemaFlag());

		val.setValidateAgainstStandardSchematron(FHIRServerProperties.getValidationSchemaTronFlag());

		FhirInstanceValidator instanceValidator = new FhirInstanceValidator(r4ctx);
		val.registerValidatorModule(instanceValidator);
		this.serverFHIRValidation.setup(val,r4ctx, serverIgPackage);

		instanceValidator.setValidationSupport(validationSupportChain);

		return instanceValidator;
	}



}
