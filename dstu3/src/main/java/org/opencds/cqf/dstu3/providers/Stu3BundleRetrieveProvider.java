// package org.opencds.cqf.dstu3.providers;

// import org.hl7.fhir.dstu3.model.Bundle;
// import org.hl7.fhir.dstu3.model.Resource;
// import org.opencds.cqf.cql.engine.elm.execution.InEvaluator;
// import org.opencds.cqf.cql.engine.elm.execution.IncludesEvaluator;
// import org.opencds.cqf.cql.engine.runtime.Code;
// import org.opencds.cqf.cql.engine.runtime.DateTime;
// import org.opencds.cqf.cql.engine.runtime.Interval;
// import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
// import org.opencds.cqf.dstu3.helpers.DataProviderHelper;

// import org.opencds.cqf.cql.retrieve.RetrieveProvider;

// import java.util.*;

// public class Stu3BundleRetrieveProvider implements RetrieveProvider {

// private Map<String, List<Object>> resourceMap;

// public Stu3BundleRetrieveProvider(Bundle sourceData) {
// resourceMap = new HashMap<>();

// // populate map
// if (sourceData != null) {
// if (sourceData.hasEntry()) {
// for (Bundle.BundleEntryComponent entry : sourceData.getEntry()) {
// if (entry.hasResource()) {
// Resource resource = entry.getResource();
// if (resourceMap.containsKey(resource.fhirType())) {
// resourceMap.get(resource.fhirType()).add(resource);
// } else {
// List<Object> resources = new ArrayList<>();
// resources.add(resource);
// resourceMap.put(resource.fhirType(), resources);
// }
// }
// }
// }
// }
// }

// @Override
// public Iterable<Object> retrieve(String context, String contextPath, Object
// contextValue, String dataType, String templateId,
// String codePath, Iterable<Code> codes, String valueSet, String datePath,
// String dateLowPath, String dateHighPath, Interval dateRange)
// {
// if (codePath == null && (codes != null || valueSet != null)) {
// throw new IllegalArgumentException("A code path must be provided when
// filtering on codes or a valueset.");
// }

// if (dataType == null) {
// throw new IllegalArgumentException("A data type (i.e. Procedure, Valueset,
// etc...) must be specified for clinical data retrieval");
// }

// List<Object> resourcesOfType = resourceMap.get(dataType);

// if (resourcesOfType == null) {
// return Collections.emptyList();
// }

// // no resources or no filtering -> return list
// if (resourcesOfType.isEmpty() || (dateRange == null && codePath == null)) {
// return resourcesOfType;
// }

// List<Object> returnList = new ArrayList<>();
// for (Object resource : resourcesOfType) {
// boolean includeResource = true;
// if (dateRange != null) {
// if (datePath != null) {
// if (dateHighPath != null || dateLowPath != null) {
// throw new IllegalArgumentException("If the datePath is specified, the
// dateLowPath and dateHighPath attributes must not be present.");
// }

// Object dateObject = DataProviderHelper.getStu3DateTime(resolvePath(resource,
// datePath));
// DateTime date = dateObject instanceof DateTime ? (DateTime) dateObject :
// null;
// Interval dateInterval = dateObject instanceof Interval ? (Interval)
// dateObject : null;
// String precision = DataProviderHelper.getPrecision(Arrays.asList(dateRange,
// date));
// if (date != null && !(InEvaluator.in(date, dateRange, precision))) {
// includeResource = false;
// }

// else if (dateInterval != null && !IncludesEvaluator.includes(dateRange,
// dateInterval, precision)) {
// includeResource = false;
// }
// } else {
// if (dateHighPath == null && dateLowPath == null) {
// throw new IllegalArgumentException("If the datePath is not given, either the
// lowDatePath or highDatePath must be provided.");
// }

// DateTime lowDate = dateLowPath == null ? null : (DateTime)
// DataProviderHelper.getStu3DateTime(resolvePath(resource, dateLowPath));
// DateTime highDate = dateHighPath == null ? null : (DateTime)
// DataProviderHelper.getStu3DateTime(resolvePath(resource, dateHighPath));

// String precision = DataProviderHelper.getPrecision(Arrays.asList(dateRange,
// lowDate, highDate));

// Interval interval = new Interval(lowDate, true, highDate, true);

// if (!IncludesEvaluator.includes(dateRange, interval, precision)) {
// includeResource = false;
// }
// }
// }

// if (codePath != null && !codePath.equals("") && includeResource) {
// if (valueSet != null && terminologyProvider != null) {
// if (valueSet.startsWith("urn:oid:")) {
// valueSet = valueSet.replace("urn:oid:", "");
// }
// ValueSetInfo valueSetInfo = new ValueSetInfo().withId(valueSet);
// codes = terminologyProvider.expand(valueSetInfo);
// }
// if (codes != null) {
// Object codeObject = DataProviderHelper.getStu3Code(resolvePath(resource,
// codePath));
// includeResource = DataProviderHelper.checkCodeMembership(codes, codeObject);
// }

// if (includeResource) {
// returnList.add(resource);
// }
// }
// }

// return returnList;
// }
// }
