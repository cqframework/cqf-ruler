package org.opencds.cqf.ruler.cql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
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
	private final RequestDetails myRequestDetails;
	private final Map<VersionedIdentifier, List<Code>> myGlobalCodeCache;

	public JpaTerminologyProvider(ITermReadSvc theTerminologySvc, IValidationSupport theValidationSupport,
			Map<VersionedIdentifier, List<Code>> theGlobalCodeCache) {
		this(theTerminologySvc, theValidationSupport, theGlobalCodeCache, null);
	}

	public JpaTerminologyProvider(ITermReadSvc theTerminologySvc, IValidationSupport theValidationSupport,
			Map<VersionedIdentifier, List<Code>> theGlobalCodeCache,
			RequestDetails theRequestDetails) {
		myTerminologySvc = theTerminologySvc;
		myValidationSupport = theValidationSupport;
		myGlobalCodeCache = theGlobalCodeCache;
		myRequestDetails = theRequestDetails;
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

	protected boolean hasUrlId(ValueSetInfo valueSet) {
		return valueSet.getId().startsWith("http://") || valueSet.getId().startsWith("https://");
	}

	protected boolean hasVersion(ValueSetInfo valueSet) {
		return valueSet.getVersion() != null;
	}

	protected boolean hasVersionedCodeSystem(ValueSetInfo valueSet) {
		return valueSet.getCodeSystems() != null && valueSet.getCodeSystems().size() > 1
				|| valueSet.getCodeSystems() != null
						&& valueSet.getCodeSystems().stream().anyMatch(x -> x.getVersion() != null);
	}

	@Override
	public Iterable<Code> expand(ValueSetInfo valueSet) throws ResourceNotFoundException {
		// This could possibly be refactored into a single call to the underlying HAPI
		// Terminology service. Need to think through that..,
		IBaseResource vs;
		if (hasUrlId(valueSet)) {
			if (hasVersion(valueSet) || hasVersionedCodeSystem(valueSet)) {
				throw new UnsupportedOperationException(String.format(
						"Could not expand value set %s; version and code system bindings are not supported at this time.",
						valueSet.getId()));
			}
		}

		VersionedIdentifier vsId = new VersionedIdentifier().withId(valueSet.getId()).withVersion(valueSet.getVersion());

		if (this.myGlobalCodeCache.containsKey(vsId)) {
			return this.myGlobalCodeCache.get(vsId);
		}

		ValueSetExpansionOptions valueSetExpansionOptions = new ValueSetExpansionOptions();
		valueSetExpansionOptions.setFailOnMissingCodeSystem(false);
		valueSetExpansionOptions.setCount(Integer.MAX_VALUE);

		vs = myTerminologySvc.expandValueSet(valueSetExpansionOptions, valueSet.getId());
		// TODO: There's probably a way to share a bit more code between the various
		// versions of FHIR
		// here. The cql-evaluator has a CodeUtil class that read any version of a
		// ValueSet, but it
		// relies heavily on reflection.

		List<Code> codes = getCodes((org.hl7.fhir.r4.model.ValueSet) vs);
		this.myGlobalCodeCache.put(vsId, codes);
		return codes;
	}

	@Override
	public Code lookup(Code code, CodeSystemInfo codeSystem) throws ResourceNotFoundException {
		LookupCodeResult cs = myTerminologySvc.lookupCode(new ValidationSupportContext(myValidationSupport),
				codeSystem.getId(), code.getCode());

		code.setDisplay(cs.getCodeDisplay());
		code.setSystem(codeSystem.getId());

		return code;
	}

	protected List<Code> getCodes(org.hl7.fhir.r4.model.ValueSet theValueSet) {
		List<Code> codes = new ArrayList<>();

		// If expansion was successful, use the codes.
		if (theValueSet.hasExpansion() && theValueSet.getExpansion().hasContains()) {
			for (org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent vse : theValueSet.getExpansion()
					.getContains()) {
				codes.add(new Code().withCode(vse.getCode()).withSystem(vse.getSystem()));
			}
		}
		// If not, best-effort based on codes. Should probably make this configurable to
		// match the behavior of the
		// underlying terminology service implementation
		else if (theValueSet.hasCompose() && theValueSet.getCompose().hasInclude()) {
			for (org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent include : theValueSet.getCompose().getInclude()) {
				for (org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent concept : include.getConcept()) {
					if (concept.hasCode()) {
						codes.add(new Code().withCode(concept.getCode()).withSystem(include.getSystem()));
					}
				}
			}
		}

		return codes;
	}
}
