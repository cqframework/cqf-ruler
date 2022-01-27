package org.opencds.cqf.ruler.cr.r4.provider;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.opencds.cqf.ruler.cr.common.CommonDataRequirementsUtility;
import org.springframework.beans.factory.annotation.Autowired;

public class DataRequirementsUtility {

	@Autowired
	private CommonDataRequirementsUtility commonDataRequirementsUtility;

	public org.hl7.fhir.r4.model.Library getModuleDefinitionLibrary(LibraryManager libraryManager, TranslatedLibrary translatedLibrary, CqlTranslatorOptions options) {
		org.hl7.fhir.r5.model.Library libraryR5 = commonDataRequirementsUtility.getModuleDefinitionLibrary(libraryManager, translatedLibrary, options);
		VersionConvertor_40_50 versionConvertor_30_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
		org.hl7.fhir.r4.model.Library libraryR4 = (org.hl7.fhir.r4.model.Library) versionConvertor_30_50.convertResource(libraryR5);
		//libraryR4 = this.addDataRequirementFhirQueries(libraryR4);  // uncomment this when engine fhir query generation available
		return libraryR4;
	}
}
