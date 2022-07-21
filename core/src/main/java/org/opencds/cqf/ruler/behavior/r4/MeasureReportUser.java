package org.opencds.cqf.ruler.behavior.r4;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.SearchParameter;
import org.hl7.fhir.r4.model.SearchParameter.XPathUsageType;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.opencds.cqf.ruler.behavior.IdCreator;
import org.opencds.cqf.ruler.utility.Ids;
import org.opencds.cqf.ruler.utility.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.api.server.RequestDetails;

public interface MeasureReportUser extends DaoRegistryUser, IdCreator {
	Logger ourLog = LoggerFactory.getLogger(ParameterUser.class);

	String MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM = "http://terminology.hl7.org/CodeSystem/measure-improvement-notation";
	String MEASUREREPORT_MEASURE_POPULATION_SYSTEM = "http://terminology.hl7.org/CodeSystem/measure-population";
	String SDE_EXTENSION_URL = "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-supplementalData";

	default Map<String, Resource> getEvaluatedResources(MeasureReport report) {
		Map<String, Resource> resources = new HashMap<>();
		getEvaluatedResources(report, resources);

		return resources;
	}

	default MeasureReportUser getEvaluatedResources(MeasureReport report, Map<String, Resource> resources) {
		report.getEvaluatedResource().forEach(evaluatedResource -> {
			IIdType resourceId = evaluatedResource.getReferenceElement();
			if (resourceId.getResourceType() == null || resources.containsKey(Ids.simple(resourceId))) {
				return;
			}
			IBaseResource resourceBase = read(resourceId);
			if (resourceBase instanceof Resource) {
				Resource resource = (Resource) resourceBase;
				resources.put(Ids.simple(resourceId), resource);
			}
		});

		return this;
	}

	default Map<String, Resource> getSDE(MeasureReport report) {
		Map<String, Resource> sdeMap = new HashMap<>();
		getSDE(report, sdeMap);
		return sdeMap;
	}

	default MeasureReportUser getSDE(MeasureReport report, Map<String, Resource> resources) {
		if (report.hasExtension()) {
			for (Extension extension : report.getExtension()) {
				if (extension.hasUrl() && extension.getUrl().equals(SDE_EXTENSION_URL)) {
					Reference sdeRef = extension.hasValue() && extension.getValue() instanceof Reference
							? (Reference) extension.getValue()
							: null;
					if (sdeRef != null && sdeRef.hasReference() && !sdeRef.getReference().startsWith("#")) {
						IdType sdeId = new IdType(sdeRef.getReference());
						if (!resources.containsKey(Ids.simple(sdeId))) {
							resources.put(Ids.simple(sdeId), read(sdeId));
						}
					}
				}
			}
		}
		return this;
	}

	default OperationOutcome generateIssue(String severity, String issue) {
		OperationOutcome error = new OperationOutcome();
		error.addIssue()
				.setSeverity(OperationOutcome.IssueSeverity.fromCode(severity))
				.setCode(OperationOutcome.IssueType.PROCESSING)
				.setDetails(new CodeableConcept().setText(issue));
		return error;
	}

	default void ensureSupplementalDataElementSearchParameter(RequestDetails requestDetails) {
		if (!search(SearchParameter.class,
				Searches.byUrl("http://hl7.org/fhir/us/davinci-deqm/SearchParameter/measurereport-supplemental-data",
						"0.1.0"),
				requestDetails).isEmpty())
			return;

		ArrayList<ContactDetail> contact = new ArrayList<>();
		contact.add(
				new ContactDetail()
						.addTelecom(
								new ContactPoint()
										.setSystem(ContactPointSystem.URL)
										.setValue("http://www.hl7.org/Special/committees/cqi/index.cfm")));

		ArrayList<CodeableConcept> jurisdiction = new ArrayList<>();
		jurisdiction.add(
				new CodeableConcept()
						.addCoding(
								new Coding("urn:iso:std:iso:3166", "US", "United States of America")));

		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(2022, 7, 20);

		SearchParameter searchParameter = new SearchParameter()
				.setUrl("http://hl7.org/fhir/us/davinci-deqm/SearchParameter/measurereport-supplemental-data")
				.setVersion("0.1.0")
				.setName("DEQMMeasureReportSupplementalData")
				.setStatus(PublicationStatus.ACTIVE)
				.setDate(calendar.getTime())
				.setPublisher("HL7 International - Clinical Quality Information Work Group")
				.setContact(contact)
				.setDescription(
						"Returns resources (supplemental data) from references on extensions on the MeasureReport with urls matching http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-supplementalData.")
				.setJurisdiction(jurisdiction)
				.addBase("MeasureReport")
				.setCode("supplemental-data")
				.setType(SearchParamType.REFERENCE)
				.setExpression(
						"MeasureReport.extension('http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-supplementalData').value")
				.setXpath(
						"f:MeasureReport/f:extension[@url='http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-supplementalData'].value")
				.setXpathUsage(XPathUsageType.NORMAL);

		searchParameter.setId("deqm-measurereport-supplemental-data");
		searchParameter.setTitle("Supplemental Data");

		create(searchParameter, requestDetails);
	}
}
