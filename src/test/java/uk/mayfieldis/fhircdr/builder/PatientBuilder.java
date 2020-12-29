package uk.mayfieldis.fhircdr.builder;


import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;


public class PatientBuilder {

    private Long patientId = 100002L;
    private boolean active = true;
 //   private Date dateOfBirth = new Date("2001-01-01");



    public Patient build() {
        final Patient patientEntity = new Patient();
      //  patientEntity.setId(patientId.toString());
        patientEntity.setActive(active);
      //  patientEntity.setBirthDate(dateOfBirth);
        patientEntity.setGender(Enumerations.AdministrativeGender.MALE);
        final HumanName name = patientEntity.addName();
        name.setUse(HumanName.NameUse.OFFICIAL);
        name.addGiven("John");
        name.setFamily("Smith");
        name.addPrefix("Mr");
        // KGM 18/12/2017 Removed following line. Add name does this functionality
       // patientEntity.getNames().add(name);
        return patientEntity;
    }
}
