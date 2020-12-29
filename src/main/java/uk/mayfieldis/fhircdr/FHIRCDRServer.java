package uk.mayfieldis.fhircdr;


import ca.uhn.fhir.jpa.searchparam.registry.ISearchParamRegistry;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

import org.hl7.fhir.utilities.cache.NpmPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import uk.mayfieldis.hapifhir.PackageManager;
import uk.mayfieldis.hapifhir.support.ServerFHIRValidation;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication(exclude = {ElasticsearchAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class})
@EnableTransactionManagement()
@ServletComponentScan
@ComponentScan({"uk.mayfieldis.fhircdr", "uk.mayfieldis.hapifhir"})
public class FHIRCDRServer extends SpringBootServletInitializer {

    @Autowired
    ApplicationContext context;


    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FHIRCDRServer.class);


    public static void main(String[] args) {

        System.setProperty("hawtio.authenticationEnabled", "false");
        System.setProperty("management.security.enabled","false");
        System.setProperty("management.contextPath","");

        System.setProperty("spring.batch.job.enabled", "false");

        SpringApplication.run(FHIRCDRServer.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(FHIRCDRServer.class);
    }




    @Bean
    CorsConfigurationSource
    corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", new CorsConfiguration().applyPermitDefaultValues());
        return source;
    }


    @Bean(name="serverIgPackage")
    public NpmPackage getServerIgPackage() throws Exception {
        NpmPackage serverIgPackage =null;

        if (!HapiProperties.getServerIgPackage().isEmpty()) {

                    serverIgPackage = PackageManager.getPackage(HapiProperties.getServerIgPackage(),
                            HapiProperties.getServerIgVersion(),
                            HapiProperties.getServerIgUrl());

        }
        if (serverIgPackage== null)  throw new InternalErrorException("Unable to load API Server Conformance package");
        return serverIgPackage;
    }

    @Bean(name="coreIgPackage")
    public NpmPackage getCoreIgPackage() throws Exception {
        NpmPackage validationIgPackage =null;
        log.info(HapiProperties.getCoreIgPackage());
        if (!HapiProperties.getCoreIgPackage().isEmpty()) {

            validationIgPackage = PackageManager
                    .getPackage(HapiProperties.getCoreIgPackage(),
                    HapiProperties.getCoreIgVersion(),
                    HapiProperties.getCoreIgUrl());

            if (validationIgPackage== null)  throw new InternalErrorException("Unable to load API Server Conformance package");


        }
        return validationIgPackage;
    }

    @Bean
    public ServletRegistrationBean ServletRegistrationBean(
                                                           @Qualifier("coreIgPackage") NpmPackage coreIgPackage,
                                                           @Qualifier("serverIgPackage") NpmPackage serverIgPackage,
                                                           ServerFHIRValidation serverFHIRvalidation,
                                                           ISearchParamRegistry searchParamRegistry
    ) {

        ServletRegistrationBean registration = new ServletRegistrationBean(
                new JpaRestfulServer(context,
                        serverIgPackage,
                        coreIgPackage,
                        serverFHIRvalidation,
                       searchParamRegistry
                ),
                "/R4/*");
        Map<String,String> params = new HashMap<>();
        params.put("FhirVersion","R4");
        params.put("ImplementationDescription","FHIR CDR");
        registration.setInitParameters(params);
        registration.setName("FhirServlet");
        registration.setLoadOnStartup(1);
        return registration;
    }

    @Bean
    public ServerFHIRValidation getServerFHIRValidation() {
        // Use basic constructor.. HAPI FHIR needs to setup
        return new ServerFHIRValidation();
    }

}
