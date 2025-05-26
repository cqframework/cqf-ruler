package org.opencds.cqf.ruler.cdshooks.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

class EpicLogging {
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

	long getDuration() {
		var endTime = System.currentTimeMillis();
		return endTime - startTime;
	}

	void logError(String error) {
		logger.error(error);
		errorLogger.error(error);
	}

	void logError(String error, Exception e) {
		logger.error(error, e);
		errorLogger.error(error, e);
	}

	void logInfo(String info) {
		logger.info(info);
		infoLogger.info(info);
	}

	void logMclQueryPerformance(Map<String, Long> mclQueryPerformanceMap) {
		performanceLogger.info("================== MCL Query Performance Log Start ==================");
		mclQueryPerformanceMap.forEach((k, v) ->
			performanceLogger.info("Time for query: {}, {} ms", k, v)
		);
		performanceLogger.info("================== MCL Query Performance Log End ==================");
	}

	void logMclQueryPerformanceWithCount(Map<String, Long> mclQueryPerformanceMap, Map<String, Integer> resourceCountMap) {
		performanceLogger.info("================== MCL Query Performance Log Start ==================");
		mclQueryPerformanceMap.forEach((k, v) -> {
				int count = 0;
				if (resourceCountMap.containsKey(k)) {
					count = resourceCountMap.get(k);
				}
				performanceLogger.info("Time for query: {} took {} ms, resulting in {} resource(s)", k, v, count);
			}
		);
		performanceLogger.info("================== MCL Query Performance Log End ==================");
	}

	void logBundleResources(Bundle data) {
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

	void logNoGuidance(String patientId, String hookInstance) {
		noGuidanceLogger.info(
			"CDS Hook instance {} for patient {} produced no guidance", patientId, hookInstance
		);
	}

	void logRequestDuration(String hookInstance) {
		performanceLogger.info(
			"CDS Hook request for hook instance {} took {} ms", hookInstance, getDuration()
		);
	}

	void logTimeBetweenRequests(String patientId, Long duration) {
		performanceLogger.info("Time between order-select and order-sign requests for patient: {} took {} ms", patientId, duration);
	}
}
