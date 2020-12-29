package uk.mayfieldis.fhircdr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import uk.mayfieldis.fhircdr.builder.ObservationBuilder;
import uk.mayfieldis.hapifhir.FHIRServerProperties;
import uk.mayfieldis.fhircdr.builder.PatientBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FHIRCDRServerR4IT {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FHIRCDRServerR4IT.class);
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    FhirContext ctxtest = FhirContext.forR4();
    PatientBuilder patientBuilder = new PatientBuilder();
    ObservationBuilder observationBuilder = new ObservationBuilder();

    @TestConfiguration
    static class GenomeServerR4ITContextConfiguration {

    }

    /*
    @Test
    public void metadataShouldReturnCapabilityStatement() throws Exception {
        assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/R4/metadata",
                String.class)).contains("CapabilityStatement");
    }
*/
    @Test
    public void createPatient() throws Exception {
        log.info("createPatient()");
        Patient patient = patientBuilder.build();
        String patientJson = ctxtest.newJsonParser().encodeResourceToString(patient);

        ResponseEntity<String> out = postResource(patientJson,"Patient");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.CREATED);
    }

    @Test
    public void validateObservationFail() throws Exception {
        log.info("validateObservationFail()");
        Observation observation = observationBuilder.buildFaulty();

        String patientJson = ctxtest.newJsonParser().encodeResourceToString(observation);

        ResponseEntity<String> out = postResource(patientJson,"Observation");
        log.info(out.getBody());
        if (!FHIRServerProperties.getServerIgPackage().isEmpty()) {
            assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    @Test
    public void validatePatientPostWrongEndpoint() throws Exception {
        log.info("validatePatientPostWrongEndpoint()");
        Patient patient = patientBuilder.build();

        String patientJson = ctxtest.newJsonParser().encodeResourceToString(patient);
        ResponseEntity<String> out = postResource(patientJson, "Observation" );
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.BAD_REQUEST);

    }

    private ResponseEntity postResource(String json, String resourceType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> entity = new HttpEntity<Object>(json, headers);
        return restTemplate.exchange("http://localhost:" + port + "/R4/"+resourceType, HttpMethod.POST, entity, java.lang.String.class);
    }

    private IBaseResource getFileResource(String fileName) {
        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("examples/"+fileName);
        assertNotNull(inputStream);
        Reader reader = new InputStreamReader(inputStream);
        return ctxtest.newJsonParser().parseResource(reader);
    }
    @Test
    public void postObservation9084() throws Throwable {
        IBaseResource resource = getFileResource("Observation-observation-9084.json");
        ResponseEntity<String> out = postResource(ctxtest.newJsonParser().encodeResourceToString(resource),resource.getClass().getSimpleName());
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.CREATED);
    }

    @Test
    public void postObservationNoStatus() throws Throwable {
        IBaseResource resource = getFileResource("Observation-observation-nostatus.json");

        ResponseEntity<String> out = postResource(ctxtest.newJsonParser().encodeResourceToString(resource),resource.getClass().getSimpleName());
        log.info(out.getBody());
        if (!FHIRServerProperties.getServerIgPackage().isEmpty()) {
            assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    @Test
    public void postOrganisation2() throws Throwable {
        IBaseResource resource = getFileResource("Organization-organization-2.json");
        ResponseEntity<String> out = postResource(ctxtest.newJsonParser().encodeResourceToString(resource),resource.getClass().getSimpleName());
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.CREATED);
    }
    @Test
    public void postOrganisation3() throws Throwable {
        IBaseResource resource = getFileResource("Organization-organization-3.json");
        ResponseEntity<String> out = postResource(ctxtest.newJsonParser().encodeResourceToString(resource),resource.getClass().getSimpleName());
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.CREATED);
    }
    @Test
    public void postPatient1() throws Throwable {
        IBaseResource resource = getFileResource("Patient-patient-1.json");
        ResponseEntity<String> out = postResource(ctxtest.newJsonParser().encodeResourceToString(resource),resource.getClass().getSimpleName());
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.CREATED);
    }

   @Test()
    public void postPatientGPConnect() throws Throwable {
        IBaseResource resource = getFileResource("Patient-gpconnect.json");
        ResponseEntity<String> out = postResource(ctxtest.newJsonParser().encodeResourceToString(resource),resource.getClass().getSimpleName());
       log.info(out.getBody());
       if (!FHIRServerProperties.getServerIgPackage().isEmpty()) {
           assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.UNPROCESSABLE_ENTITY);
       }
    }

    @Test(expected = DataFormatException.class)
    public void postPatientBad() throws Throwable {
        IBaseResource resource = getFileResource("Patient-patient-bad.json");
        ResponseEntity<String> out = postResource(ctxtest.newJsonParser().encodeResourceToString(resource),resource.getClass().getSimpleName());
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.BAD_REQUEST);
    }
    @Test
    public void postPatientCommCore() throws Throwable {
        IBaseResource resource = getFileResource("Patient-patient-comm-core.json");
        ResponseEntity<String> out = postResource(ctxtest.newJsonParser().encodeResourceToString(resource),resource.getClass().getSimpleName());
      //  if (!HapiProperties.getServerIgPackage().isEmpty()) {
      //      assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.UNPROCESSABLE_ENTITY);
     //   } else {
        log.info(out.getBody());
            assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.CREATED);
     //   }
    }

    @Test
    public void postPatientCommUK() throws Throwable {
        IBaseResource resource = getFileResource("Patient-patient-comm-uk.json");
        ResponseEntity<String> out = postResource(ctxtest.newJsonParser().encodeResourceToString(resource),resource.getClass().getSimpleName());
        log.info(out.getBody());
        if (!FHIRServerProperties.getServerIgPackage().isEmpty()) {
            assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.CREATED);
        }
    }


    @Test
    public void postClaim() throws Throwable {
        IBaseResource resource = getFileResource("claim.json");
        ResponseEntity<String> out = postResource(ctxtest.newJsonParser().encodeResourceToString(resource),resource.getClass().getSimpleName());
        log.info(out.getBody());
        if (!FHIRServerProperties.getServerIgPackage().isEmpty()) {
            assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.CREATED);
        }
    }

    @Test
    public void postMedicationDispense() throws Throwable {
        IBaseResource resource = getFileResource("MedicationDispense.json");
        ResponseEntity<String> out = postResource(ctxtest.newJsonParser().encodeResourceToString(resource),resource.getClass().getSimpleName());
        log.info(out.getBody());
        if (!FHIRServerProperties.getServerIgPackage().isEmpty()) {
            assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.CREATED);
        }
    }

    @Test
    public void postPractitioner4() throws Throwable {
        IBaseResource resource = getFileResource("Practitioner-practitioner-4.json");
        ResponseEntity<String> out = postResource(ctxtest.newJsonParser().encodeResourceToString(resource),resource.getClass().getSimpleName());
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.CREATED);
    }

    @Test
    public void contextLoads() throws Exception {
    }

}
