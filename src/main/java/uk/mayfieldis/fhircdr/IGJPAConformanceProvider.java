package uk.mayfieldis.fhircdr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.r4.JpaConformanceProviderR4;
import ca.uhn.fhir.jpa.searchparam.registry.ISearchParamRegistry;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.utilities.cache.NpmPackage;
import org.json.JSONObject;
import uk.mayfieldis.hapifhir.provider.IGConformanceHelper;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;

public class IGJPAConformanceProvider extends JpaConformanceProviderR4 {

    private volatile CapabilityStatement capabilityStatement;

    private Instant lastRefresh;

    private JSONObject openIdObj;

    NpmPackage serverIgPackage;

    FhirContext ctx;

    IGConformanceHelper igConformanceHelper;

    public IGJPAConformanceProvider(RestfulServer theRestfulServer, IFhirSystemDao<Bundle, Meta> theSystemDao, DaoConfig theDaoConfig, NpmPackage _serverIgPackage, FhirContext _ctx,
                                    ISearchParamRegistry theSearchParamRegistry) {
        //

        super(theRestfulServer, theSystemDao, theDaoConfig,theSearchParamRegistry);
        serverIgPackage = _serverIgPackage;
        ctx = _ctx;
        this.igConformanceHelper = new IGConformanceHelper(ctx,_serverIgPackage);
    }

    @Override
    @Metadata
    public CapabilityStatement getServerConformance(HttpServletRequest theRequest, RequestDetails theRequestDetails) {
        if (capabilityStatement != null) {
            if (lastRefresh != null) {
                java.time.Duration duration = java.time.Duration.between(Instant.now(), lastRefresh);
                // May need to revisit
                if ((duration.getSeconds() * 60) < 2) return capabilityStatement;
            }
        }
        lastRefresh = Instant.now();

        capabilityStatement = super.getServerConformance(theRequest, theRequestDetails);

        return this.igConformanceHelper.getCapabilityStatement(capabilityStatement);
    }

    private HttpClient getHttpClient(){
        final HttpClient httpClient = HttpClientBuilder.create().build();
        return httpClient;
    }


}
