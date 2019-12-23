package org.opencds.cqf.dstu3.providers;

import org.hl7.fhir.dstu3.model.Type;

import ca.uhn.fhir.model.api.annotation.DatatypeDef;

@DatatypeDef(name = "CodeTerminologyRef")
public class CodeTerminologyRef extends TerminologyRef {

	public CodeTerminologyRef(String name, String id, String codeSystemName, String codeSystemId, String displayName) {
		this.type = TerminologyRefType.CODE;
		this.name = name;
		this.id = id;
		this.codeSystemName = codeSystemName;
		this.codeSystemId = codeSystemId;
		this.displayName = displayName;

	}

	protected String codeSystemName;

	public String getcodeSystemName() {
		return codeSystemName;
	}

	public void setcodeSystemName(String codeSystemName) {
		this.codeSystemName = codeSystemName;
	}

	protected String codeSystemId;

	public String getcodeSystemId() {
		return codeSystemId;
	}

	public void setcodeSystemId(String codeSystemId) {
		this.codeSystemId = codeSystemId;
	}

	protected String displayName;

	public String getdisplayName() {
		return displayName;
	}

	public void setdisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String getDefinition() {
		String definition = "code \"" + name + "\" : '" + id + "' from \"" + codeSystemName + "\"";
		if (this.displayName != null) {
			definition += (" display '" + this.displayName + "'");
		}

		return definition;
	}

	@Override
	protected Type typedCopy() {
		// TODO Auto-generated method stub
		return null;
	}

}
