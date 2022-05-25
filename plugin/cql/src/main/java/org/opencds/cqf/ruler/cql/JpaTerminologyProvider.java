package org.opencds.cqf.ruler.cql;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.terminology.CodeSystemInfo;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.opencds.cqf.ruler.utility.Ids;
import org.opencds.cqf.ruler.utility.Searches;

import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport.LookupCodeResult;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.context.support.ValueSetExpansionOptions;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.term.api.ITermReadSvc;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

/**
 * This class provides an implementation of the cql-engine's TerminologyProvider
 * interface, which is used for Terminology operations
 * in CQL
 */
public class JpaTerminologyProvider implements TerminologyProvider, DaoRegistryUser {

	private final ITermReadSvc myTerminologySvc;
	private final DaoRegistry myDaoRegistry;
	private final IValidationSupport myValidationSupport;
	private final RequestDetails myRequestDetails;

	public JpaTerminologyProvider(ITermReadSvc theTerminologySvc, DaoRegistry theDaoRegistry,
			IValidationSupport theValidationSupport) {
		this(theTerminologySvc, theDaoRegistry, theValidationSupport, null);
	}

	public JpaTerminologyProvider(ITermReadSvc theTerminologySvc, DaoRegistry theDaoRegistry,
			IValidationSupport theValidationSupport, RequestDetails theRequestDetails) {
		myTerminologySvc = theTerminologySvc;
		myDaoRegistry = theDaoRegistry;
		myValidationSupport = theValidationSupport;
		myRequestDetails = theRequestDetails;
	}

	@Override
	public DaoRegistry getDaoRegistry() {
		return this.myDaoRegistry;
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

			IBundleProvider bundleProvider = search(getClass("ValueSet"), Searches.byUrl(valueSet.getId()),
					myRequestDetails);
			List<IBaseResource> valueSets = bundleProvider.getAllResources();
			if (valueSets.isEmpty()) {
				throw new IllegalArgumentException(String.format("Could not resolve value set %s.", valueSet.getId()));
			} else if (valueSets.size() == 1) {
				vs = valueSets.get(0);
			} else {
				throw new IllegalArgumentException("Found more than 1 ValueSet with url: " + valueSet.getId());
			}
		} else {
			vs = read(Ids.newId(this.myTerminologySvc.getFhirContext(), "ValueSet", valueSet.getId()),
					myRequestDetails);
			if (vs == null) {
				throw new IllegalArgumentException(String.format("Could not resolve value set %s.", valueSet.getId()));
			}
		}

		// TODO: There's probably a way to share a bit more code between the various
		// versions of FHIR
		// here. The cql-evaluator has a CodeUtil class that read any version of a
		// ValueSet, but it
		// relies heavily on reflection.
		switch (vs.getStructureFhirVersionEnum()) {
			case DSTU3:
				return getCodes((org.hl7.fhir.dstu3.model.ValueSet) vs);
			case R4:
				return getCodes((org.hl7.fhir.r4.model.ValueSet) vs);
			case R5:
				return getCodes((org.hl7.fhir.r5.model.ValueSet) vs);
			default:
				throw new IllegalArgumentException(String.format("expand does not support FHIR version %s",
						vs.getStructureFhirVersionEnum().getFhirVersionString()));
		}
	}

	@Override
	public Code lookup(Code code, CodeSystemInfo codeSystem) throws ResourceNotFoundException {
		LookupCodeResult cs = myTerminologySvc.lookupCode(new ValidationSupportContext(myValidationSupport),
				codeSystem.getId(), code.getCode());

		code.setDisplay(cs.getCodeDisplay());
		code.setSystem(codeSystem.getId());

		return code;
	}

	protected List<Code> getCodes(org.hl7.fhir.dstu3.model.ValueSet theValueSet) {
		// Attempt to expand the ValueSet if it's not already expanded.
		if (!(theValueSet.hasExpansion() && theValueSet.getExpansion().hasContains())) {
			theValueSet = this.innerExpand(theValueSet);
		}

		List<Code> codes = new ArrayList<>();

		// If expansion was successful, use the codes.
		if (theValueSet.hasExpansion() && theValueSet.getExpansion().hasContains()) {
			for (org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionContainsComponent vse : theValueSet.getExpansion()
					.getContains()) {
				codes.add(new Code().withCode(vse.getCode()).withSystem(vse.getSystem()));
			}
		}
		// If not, best-effort based on codes. Should probably make this configurable to
		// match the behavior of the
		// underlying terminology service implementation
		else if (theValueSet.hasCompose() && theValueSet.getCompose().hasInclude()) {
			for (org.hl7.fhir.dstu3.model.ValueSet.ConceptSetComponent include : theValueSet.getCompose()
					.getInclude()) {
				for (org.hl7.fhir.dstu3.model.ValueSet.ConceptReferenceComponent concept : include.getConcept()) {
					if (concept.hasCode()) {
						codes.add(new Code().withCode(concept.getCode()).withSystem(include.getSystem()));
					}
				}
			}
		}

		return codes;
	}

	protected List<Code> getCodes(org.hl7.fhir.r4.model.ValueSet theValueSet) {
		// Attempt to expand the ValueSet if it's not already expanded.
		if (!(theValueSet.hasExpansion() && theValueSet.getExpansion().hasContains())) {
			theValueSet = this.innerExpand(theValueSet);
		}

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

	protected List<Code> getCodes(org.hl7.fhir.r5.model.ValueSet theValueSet) {
		// Attempt to expand the ValueSet if it's not already expanded.
		if (!(theValueSet.hasExpansion() && theValueSet.getExpansion().hasContains())) {
			theValueSet = this.innerExpand(theValueSet);
		}

		List<Code> codes = new ArrayList<>();

		// If expansion was successful, use the codes.
		if (theValueSet.hasExpansion() && theValueSet.getExpansion().hasContains()) {
			for (org.hl7.fhir.r5.model.ValueSet.ValueSetExpansionContainsComponent vse : theValueSet.getExpansion()
					.getContains()) {
				codes.add(new Code().withCode(vse.getCode()).withSystem(vse.getSystem()));
			}
		}
		// If not, best-effort based on codes. Should probably make this configurable to
		// match the behavior of the
		// underlying terminology service implementation
		else if (theValueSet.hasCompose() && theValueSet.getCompose().hasInclude()) {
			for (org.hl7.fhir.r5.model.ValueSet.ConceptSetComponent include : theValueSet.getCompose().getInclude()) {
				for (org.hl7.fhir.r5.model.ValueSet.ConceptReferenceComponent concept : include.getConcept()) {
					if (concept.hasCode()) {
						codes.add(new Code().withCode(concept.getCode()).withSystem(include.getSystem()));
					}
				}
			}
		}

		return codes;
	}

	@SuppressWarnings("unchecked")
	protected <T extends IBaseResource> T innerExpand(T theValueSet) {
		return (T) myTerminologySvc.expandValueSet(
				new ValueSetExpansionOptions().setCount(Integer.MAX_VALUE).setFailOnMissingCodeSystem(false), theValueSet);
	}
}
