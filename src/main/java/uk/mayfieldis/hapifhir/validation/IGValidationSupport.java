package uk.mayfieldis.hapifhir.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValueSetExpansionOptions;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.conformance.ProfileUtilities;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.utilities.cache.NpmPackage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class IGValidationSupport
{

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(uk.mayfieldis.hapifhir.validation.IGValidationSupport.class);

    private Map<String, IBaseResource> myCodeSystems;
    private Map<String, IBaseResource> myStructureDefinitions;
    private Map<String, IBaseResource> myValueSets;
    FhirContext ctx;



    NpmPackage npm = null;

    public PrePopulatedValidationSupport getValidationSupport(FhirContext ctx, NpmPackage _npm) throws Exception {


        this.myCodeSystems = new HashMap();
        this.myValueSets = new HashMap<>();
        this.myStructureDefinitions = new HashMap<>();
        this.ctx = ctx;
        npm = _npm;

        LOG.info("Loading IG Validation Support {}", _npm.getPath());
        for (String resource : npm.listResources( "StructureDefinition")) {

            StructureDefinition structureDefinition = (StructureDefinition) ctx.newJsonParser().parseResource(npm.load("package", resource));
            LOG.debug("Loading: {} fhirVersion {}",structureDefinition.getUrl(), structureDefinition.getFhirVersion().toString());
            if (!structureDefinition.hasSnapshot() && structureDefinition.getDerivation().equals(StructureDefinition.TypeDerivationRule.CONSTRAINT)) {
                LOG.debug("Missing Snapshot {}", structureDefinition.getUrl());
            }
            this.myStructureDefinitions.put(structureDefinition.getUrl(),structureDefinition);
        }
        for (String resource : npm.listResources("ValueSet")) {
            ValueSet valueSet = (ValueSet) ctx.newJsonParser().parseResource(npm.load("package", resource));
            LOG.debug("Loading: {}", valueSet.getUrl());
            this.myValueSets.put(valueSet.getUrl(), valueSet);
        }
        for (String resource : npm.listResources("CodeSystem")) {
            CodeSystem codeSys = (CodeSystem) ctx.newJsonParser().parseResource(npm.load("package", resource));
            LOG.debug("Loading: {}", codeSys.getUrl());
            this.myCodeSystems.put(codeSys.getUrl(), codeSys);
        }
        return new PrePopulatedValidationSupport(ctx,this.myStructureDefinitions, this.myValueSets,this.myCodeSystems);
    }



    public void createSnapshots(IWorkerContext context, IValidationSupport validationSupport) {

        ProfileUtilities tool = new ProfileUtilities(context, null, null);
        // This section first processes the level 2 profiles and the following section the level derived
        for (IBaseResource resource : myStructureDefinitions.values()) {
            StructureDefinition structureDefinition = (StructureDefinition) resource;
            if (!structureDefinition.hasSnapshot()
                    && structureDefinition.getDerivation().equals(StructureDefinition.TypeDerivationRule.CONSTRAINT)
                    && structureDefinition.getBaseDefinition().contains("http://hl7.org/fhir/")
            ) {
               buildSnapshot(validationSupport,tool,structureDefinition);
            }
        }
        for (IBaseResource resource : myStructureDefinitions.values()) {
            StructureDefinition structureDefinition = (StructureDefinition) resource;
            if (!structureDefinition.hasSnapshot()
                    && structureDefinition.getDerivation().equals(StructureDefinition.TypeDerivationRule.CONSTRAINT)
                    && !structureDefinition.getBaseDefinition().contains("http://hl7.org/fhir/")) {
                buildSnapshot(validationSupport,tool,structureDefinition);
            }
        }
    }
    public StructureDefinition buildSnapshot(IValidationSupport validationSupport, ProfileUtilities tool, StructureDefinition structureDefinition) {
        LOG.debug("Creating Snapshot {}", structureDefinition.getUrl());

        StructureDefinition base = (StructureDefinition) validationSupport.fetchStructureDefinition(structureDefinition.getBaseDefinition());
        if (base != null) {
            if (!base.hasSnapshot() && base.getDerivation().equals(StructureDefinition.TypeDerivationRule.CONSTRAINT)) {
                LOG.warn("Base Missing Snapshot {}", base.getUrl());
                base = buildSnapshot(validationSupport,tool,base);
            }
            tool.generateSnapshot(base,
                    structureDefinition,
                    structureDefinition.getUrl(),
                    "https://fhir.nhs.uk/R4",
                    structureDefinition.getName());
            if (!structureDefinition.hasSnapshot() && structureDefinition.getDerivation().equals(StructureDefinition.TypeDerivationRule.CONSTRAINT)) {
                LOG.warn("Missing Snapshot {}", structureDefinition.getUrl());
            }
        } else {
            LOG.error("No base profile for {}",structureDefinition.getUrl());
        }
        return structureDefinition;
    }


}
