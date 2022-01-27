package org.opencds.cqf.ruler.qualitymeasure.dstu3;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_30_50;
import org.hl7.fhir.convertors.conv30_50.VersionConvertor_30_50;
import org.opencds.cqf.ruler.qualitymeasure.common.CommonDataRequirementsUtility;
import org.springframework.beans.factory.annotation.Autowired;

public class DataRequirementsUtility {

	@Autowired
	private CommonDataRequirementsUtility commonDataRequirementsUtility;

	public org.hl7.fhir.dstu3.model.Library getModuleDefinitionLibrary(LibraryManager libraryManager, TranslatedLibrary translatedLibrary, CqlTranslatorOptions options) {
		org.hl7.fhir.r5.model.Library libraryR5 = commonDataRequirementsUtility.getModuleDefinitionLibrary(libraryManager, translatedLibrary, options);
		VersionConvertor_30_50 versionConvertor_30_50 = new VersionConvertor_30_50(new BaseAdvisor_30_50());
		org.hl7.fhir.dstu3.model.Library libraryDstu3 = (org.hl7.fhir.dstu3.model.Library) versionConvertor_30_50.convertResource(libraryR5);
		//libraryR4 = this.addDataRequirementFhirQueries(libraryDstu3);  // uncomment this when engine fhir query generation available
		return libraryDstu3;
	}
}
