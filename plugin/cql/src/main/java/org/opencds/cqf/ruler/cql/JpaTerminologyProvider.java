package org.opencds.cqf.ruler.cql;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.terminology.CodeSystemInfo;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;

import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport.LookupCodeResult;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.context.support.ValueSetExpansionOptions;
import ca.uhn.fhir.jpa.term.api.ITermReadSvc;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

/**
 * This class provides an implementation of the cql-engine's TerminologyProvider
 * interface, which is used for Terminology operations
 * in CQL
 */
public class JpaTerminologyProvider implements TerminologyProvider {

	private final ITermReadSvc myTerminologySvc;
	private final IValidationSupport myValidationSupport;
	private final Map<VersionedIdentifier, List<Code>> myGlobalCodeCache;

	public JpaTerminologyProvider(ITermReadSvc theTerminologySvc, IValidationSupport theValidationSupport,
			Map<VersionedIdentifier, List<Code>> theGlobalCodeCache) {
		this(theTerminologySvc, theValidationSupport, theGlobalCodeCache, null);
	}

	public JpaTerminologyProvider(ITermReadSvc theTerminologySvc, IValidationSupport theValidationSupport,
			Map<VersionedIdentifier, List<Code>> theGlobalCodeCache, RequestDetails theRequestDetails) {
		myTerminologySvc = theTerminologySvc;
		myValidationSupport = theValidationSupport;
		myGlobalCodeCache = theGlobalCodeCache;
	}

	@Override
	public boolean in(Code code, ValueSetInfo valueSet) throws ResourceNotFoundException {
		for (Code c : expand(valueSet)) {
			if (c == null)
				continue;
			if (c.getCode().equals(code.getCode()) && c.getSystem().equals(code.getSystem())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterable<Code> expand(ValueSetInfo valueSet) throws ResourceNotFoundException {
		// This could possibly be refactored into a single call to the underlying HAPI
		// Terminology service. Need to think through that..,

		VersionedIdentifier vsId = new VersionedIdentifier().withId(valueSet.getId()).withVersion(valueSet.getVersion());

		if (this.myGlobalCodeCache.containsKey(vsId)) {
			return this.myGlobalCodeCache.get(vsId);
		}

		ValueSetExpansionOptions valueSetExpansionOptions = new ValueSetExpansionOptions();
		valueSetExpansionOptions.setFailOnMissingCodeSystem(false);
		valueSetExpansionOptions.setCount(Integer.MAX_VALUE);

		org.hl7.fhir.r4.model.ValueSet vs =
			myTerminologySvc.expandValueSet(valueSetExpansionOptions, valueSet.getId());

		List<Code> codes = getCodes(vs);
		this.myGlobalCodeCache.put(vsId, codes);
		return codes;
	}

	@Override
	public Code lookup(Code code, CodeSystemInfo codeSystem) throws ResourceNotFoundException {
		LookupCodeResult cs = myTerminologySvc.lookupCode(
			new ValidationSupportContext(myValidationSupport), codeSystem.getId(), code.getCode());

		if (cs != null) {
			code.setDisplay(cs.getCodeDisplay());
		}
		code.setSystem(codeSystem.getId());

		return code;
	}

	protected List<Code> getCodes(org.hl7.fhir.r4.model.ValueSet theValueSet) {
		checkState(theValueSet.hasExpansion(),
				"ValueSet {} did not have an expansion. Unable to get codes unexpanded ValueSet.", theValueSet.getUrl());
		List<Code> codes = new ArrayList<>();

		for (org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent vse : theValueSet.getExpansion()
				.getContains()) {
			codes.add(new Code().withCode(vse.getCode()).withSystem(vse.getSystem()));
		}

		return codes;
	}
}
