package org.opencds.cqf.ruler.cdshooks.r4.epic;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class EpicLogging {
	private final FhirContext fhirContext = FhirContext.forR4Cached();
	private final Logger logger;
	private static final Logger performanceLogger =
		LoggerFactory.getLogger("org.opencds.cds.performance");

	// Errors with PHI
	private static final Logger errorLogger =
		LoggerFactory.getLogger("org.opencds.cds.error");

	// Info with PHI
	private static final Logger infoLogger =
		LoggerFactory.getLogger("org.opencds.cds.info");

	// Patients that do not generate guidance
	private static final Logger noGuidanceLogger =
		LoggerFactory.getLogger("org.opencds.cds.info.unguided");

	private final long startTime;

	EpicLogging(Logger logger) {
		this.logger = logger;
		startTime = System.currentTimeMillis();
	}

	public long getDuration() {
		var endTime = System.currentTimeMillis();
		return endTime - startTime;
	}

	public void logError(String error) {
		logger.error(error);
		errorLogger.error(error);
	}

	public void logError(String error, Exception e) {
		logger.error(error, e);
		errorLogger.error(error, e);
	}

	public void logInfo(String info) {
		logger.info(info);
		infoLogger.info(info);
	}

	public void logDraftOrders(JsonObject draftOrders) {
		var draftOrderBundle = fhirContext.newJsonParser().parseResource(Bundle.class, new Gson().toJson(draftOrders));
		if (draftOrderBundle.hasEntry()) {
			infoLogger.info("================== Draft Orders Start ==================");
			for (var draftOrder : draftOrderBundle.getEntry()) {
				if (draftOrder.hasResource()) {
					var resourceJson = fhirContext.newJsonParser().encodeResourceToString(draftOrder.getResource());
					infoLogger.info(resourceJson);
				}
			}
			infoLogger.info("================== Draft Orders End ==================");
		}
	}

	public void logPerformanceInfo(String info) {
		logger.info(info);
		performanceLogger.info(info);
	}

	public void logMclQueryPerformance(Map<String, Long> mclQueryPerformanceMap) {
		performanceLogger.info("================== MCL Query Performance Log Start ==================");
		mclQueryPerformanceMap.forEach((k, v) ->
			performanceLogger.info("Time for query: {}, {} ms", k, v)
		);
		performanceLogger.info("================== MCL Query Performance Log End ==================");
	}

	public void logMclQueryPerformanceResult(Map<String, MCLQueryResult> mclQueryPerformanceMap) {
		performanceLogger.info("================== MCL Query Performance Log Start ==================");
		mclQueryPerformanceMap.forEach((k, v) -> {
				var count = v.getCount();
				var duration = v.getDuration();
				var bytes = v.getBytes();
				performanceLogger.info("Time for query: {} took {} ms, resulting in {} resource(s) ({} bytes)", k, duration, count, bytes);
			}
		);
		performanceLogger.info("================== MCL Query Performance Log End ==================");
	}

	public void logBundleResources(Bundle data) {
		logger.info("================== Resource Log Start ==================");
		infoLogger.info("================== Resource Log Start ==================");

		for (IBaseResource r : BundleUtil.toListOfResources(fhirContext, data)) {
			String resourceJson = fhirContext.newJsonParser().encodeResourceToString(r);
			logger.info(resourceJson);
			infoLogger.info(resourceJson);
		}

		logger.info("================== Resource Log End ==================");
		infoLogger.info("================== Resource Log End ==================");
	}

	public void logNoGuidance(String message) {
		noGuidanceLogger.info(message);
	}

	public void logNoGuidance(String patientId, String hookInstance) {
		noGuidanceLogger.info(
			"CDS Hook instance {} for patient {} produced no guidance", hookInstance, patientId
		);
	}

	public void logRequestDuration(String hookInstance) {
		performanceLogger.info(
			"CDS Hook request for hook instance {} took {} ms", hookInstance, getDuration()
		);
	}

	public void logTimeBetweenRequests(String patientId, Long duration) {
		performanceLogger.info("Time between order-select and order-sign requests for patient: {} took {} ms", patientId, duration);
	}
}
