package org.opencds.cqf.ruler.ra.r4;

import ca.uhn.fhir.context.FhirContext;
import com.opencsv.bean.CsvToBeanBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.ruler.ra.RAConstants;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings({"unchecked", "squid:S1989", "squid:S112", "rawtypes"})
public class AssistedServlet extends HttpServlet {
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (request.getContentType() == null || !request.getContentType().startsWith("text/csv")) {
			response.setStatus(400);
			response.getWriter().println(String.format(
				"Invalid content type %s. Please use text/csv.",
				request.getContentType()));
			return;
		}

		List<AssistedRowData> data = new CsvToBeanBuilder(request.getReader()).withType(AssistedRowData.class).build().parse();
		Map<String, MeasureReport> mrMap = new HashMap<>();
		for (AssistedRowData row : data) {
			String hash = getHash(row);
			if (mrMap.containsKey(hash)) {
				addGroup(row, mrMap.get(hash));
			}
			else {
				mrMap.put(hash, createMeasureReport(row));
			}
		}

		Bundle transaction = new Bundle();
		transaction.setType(Bundle.BundleType.TRANSACTION);
		for (Map.Entry<String, MeasureReport> entry : mrMap.entrySet()) {
			transaction.addEntry().setResource(entry.getValue()).setRequest(new Bundle.BundleEntryRequestComponent()
				.setMethod(Bundle.HTTPVerb.PUT).setUrl(entry.getValue().getIdElement().getValue()));
		}

		response.setStatus(200);
		response.getWriter().println(FhirContext.forR4Cached()
			.newJsonParser().setPrettyPrint(true).encodeResourceToString(transaction));
	}

	private String getHash(AssistedRowData data) {
		return data.getPeriodStart() + data.getPeriodEnd() + data.getModelId() + data.getModelVersion() + data.getPatientId();
	}

	private MeasureReport createMeasureReport(AssistedRowData data) {
		MeasureReport mr = new MeasureReport();
		mr.setId("assisted-" + UUID.randomUUID());
		mr.setMeta(new Meta().addProfile(RAConstants.MEASURE_REPORT_PROFILE_URL));
		mr.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
		mr.setType(MeasureReport.MeasureReportType.INDIVIDUAL);
		mr.setMeasure(data.getModelId());
		mr.setSubject(new Reference(data.getPatientId().startsWith("Patient/")
			? data.getPatientId() : "Patient/" + data.getPatientId()));
		mr.setDate(new Date());
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		Date start;
		Date end;
		try {
			start = formatter.parse(data.getPeriodStart());
			end = formatter.parse(data.getPeriodEnd());
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		mr.setPeriod(new Period().setStart(start).setEnd(end));
		addGroup(data, mr);
		return mr;
	}

	private void addGroup(AssistedRowData data, MeasureReport mr) {
		MeasureReport.MeasureReportGroupComponent group = new MeasureReport.MeasureReportGroupComponent();
		group.setId("group-" + UUID.randomUUID());
		group.setCode(new CodeableConcept(new Coding().setCode(data.getCcCode())
			.setSystem(RAConstants.HCC_CODESYSTEM_URL).setVersion(data.getModelVersion())));
		// TODO: use RAConstant values when pushed to master
		if (data.getSuspectType() != null && !data.getSuspectType().isBlank()) {
			group.addExtension(createCodeableConceptExtension(
				"http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-suspectType",
				"http://hl7.org/fhir/us/davinci-ra/CodeSystem/suspect-type", data.getSuspectType()));
		}
		if (data.getEvidenceStatus() != null && !data.getEvidenceStatus().isBlank()) {
			group.addExtension(createCodeableConceptExtension(
				"http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-evidenceStatus",
				"http://hl7.org/fhir/us/davinci-ra/CodeSystem/evidence-status", data.getEvidenceStatus()));
		}
		if (data.getEvidenceStatusDate() != null && !data.getEvidenceStatusDate().isBlank()) {
			group.addExtension(new Extension().setUrl(
				"http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-evidenceStatusDate")
				.setValue(new DateType(data.getEvidenceStatusDate())));
		}
		if (data.getHiearchicalStatus() != null && !data.getHiearchicalStatus().isBlank()) {
			group.addExtension(createCodeableConceptExtension(
				"http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-hierarchicalStatus",
				"http://hl7.org/fhir/us/davinci-ra/CodeSystem/hierarchical-status", data.getHiearchicalStatus()));
		}
		mr.addGroup(group);
	}

	private Extension createCodeableConceptExtension(String url, String system, String code) {
		return new Extension().setUrl(url).setValue(new CodeableConcept(new Coding().setCode(code).setSystem(system)));
	}
}

