package org.opencds.cqf.ruler.cr.dstu3;

import java.util.Arrays;
import java.util.Date;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.StringType;

public class Patients {
	private static final String OMB_CATEGORY_RACE_BLACK = "2054-5";
	private static final String BLACK_OR_AFRICAN_AMERICAN = "Black or African American";
	private static final String URL_SYSTEM_RACE = "urn:oid:2.16.840.1.113883.6.238";
	private static final String OMB_CATEGORY = "ombCategory";
	private static final String EXT_URL_US_CORE_RACE = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race";

	
	public static Patient john_doe() {
		Patient patient = new Patient();
		patient.setId("john-doe");
		patient.setName(
				  Arrays.asList(new HumanName().setFamily("Doe").setGiven(Arrays.asList(new StringType("John")))));
		patient.setBirthDate(new Date());
		patient.setGender(AdministrativeGender.MALE);

		Extension usCoreRace = new Extension();
		usCoreRace.setUrl(EXT_URL_US_CORE_RACE).addExtension().setUrl(OMB_CATEGORY).setValue(new Coding()
				  .setSystem(URL_SYSTEM_RACE).setCode(OMB_CATEGORY_RACE_BLACK).setDisplay(BLACK_OR_AFRICAN_AMERICAN));
		patient.getExtension().add(usCoreRace);
		return patient;
  }

  public static Patient jane_doe() {
		Patient patient = new Patient();
		patient.setId("jane-doe");
		patient.setName(
				  Arrays.asList(new HumanName().setFamily("Doe").setGiven(Arrays.asList(new StringType("Jane")))));
		patient.setBirthDate(new Date());
		patient.setGender(AdministrativeGender.FEMALE);

		Extension usCoreRace = new Extension();
		usCoreRace.setUrl(EXT_URL_US_CORE_RACE).addExtension().setUrl(OMB_CATEGORY).setValue(new Coding()
				  .setSystem(URL_SYSTEM_RACE).setCode(OMB_CATEGORY_RACE_BLACK).setDisplay(BLACK_OR_AFRICAN_AMERICAN));
		patient.getExtension().add(usCoreRace);
		return patient;
  }
}
