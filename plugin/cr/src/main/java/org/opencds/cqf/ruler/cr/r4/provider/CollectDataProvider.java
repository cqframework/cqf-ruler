package org.opencds.cqf.ruler.cr.r4.provider;

import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.part;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class CollectDataProvider extends DaoRegistryOperationProvider {

	@Autowired
	private ca.uhn.fhir.cr.r4.measure.MeasureOperationsProvider measureEvaluateProvider;

	/**
	 * Implements the <a href=
	 * "http://hl7.org/fhir/R4/measure-operation-collect-data.html">$collect-data</a>
	 * operation found in the
	 * <a href="http://hl7.org/fhir/R4/clinicalreasoning-module.html">FHIR Clinical
	 * Reasoning Module</a>.
	 *
	 * <p>
	 * Returns a set of parameters with the generated MeasureReport and the
	 * resources that were used during the Measure evaluation
	 *
	 * @param theRequestDetails generally auto-populated by the HAPI server
	 *                          framework.
	 * @param theId             the Id of the Measure to sub data for
	 * @param periodStart       The start of the reporting period
	 * @param periodEnd         The end of the reporting period
	 * @param subject           the subject to use for the evaluation
	 * @param practitioner      the practitioner to use for the evaluation
	 * @param lastReceivedOn    the date the results of this measure were last
	 *                          received.
	 * @return Parameters the parameters containing the MeasureReport and the
	 *         evaluated Resources
	 */
	@Description(shortDefinition = "$collect-data", value = "Implements the <a href=\"http://hl7.org/fhir/R4/measure-operation-collect-data.html\">$collect-data</a> operation found in the <a href=\"http://hl7.org/fhir/R4/clinicalreasoning-module.html\">FHIR Clinical Reasoning Module</a>.")
	@Operation(name = "$collect-data", idempotent = true, type = Measure.class)
	public Parameters collectData(RequestDetails theRequestDetails, @IdParam IdType theId,
			@OperationParam(name = "periodStart") String periodStart,
			@OperationParam(name = "periodEnd") String periodEnd, @OperationParam(name = "subject") String subject,
			@OperationParam(name = "practitioner") String practitioner,
			@OperationParam(name = "lastReceivedOn") String lastReceivedOn) {

		MeasureReport report = measureEvaluateProvider.evaluateMeasure(theId, periodStart, periodEnd,
				"subject", subject, practitioner, lastReceivedOn, null, null, null, theRequestDetails);
		report.setType(MeasureReport.MeasureReportType.DATACOLLECTION);
		report.setGroup(null);

		Parameters parameters = parameters(part("measureReport", report));

		addEvaluatedResourcesToParameters(report, parameters);

		return parameters;
	}

	private List<Resource> readEvaluatedResources(MeasureReport report) {
		List<Resource> resources = new ArrayList<>();
		for (Reference reference : report.getEvaluatedResource()) {
			IIdType id = reference.getReferenceElement();
			if (id.isEmpty()) {
				continue;
			}
			Resource resource = read(id);
			if (resource != null) {
				resources.add(resource);
			}
		}

		return resources;
	}

	private void addEvaluatedResourcesToParameters(MeasureReport report, Parameters parameters) {
		readEvaluatedResources(report)
				.forEach(resource -> parameters.addParameter(part("resource", resource)));
	}
}
