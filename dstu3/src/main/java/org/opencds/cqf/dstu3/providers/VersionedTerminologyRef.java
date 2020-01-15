package org.opencds.cqf.dstu3.providers;

import org.hl7.fhir.dstu3.model.Type;

import ca.uhn.fhir.model.api.annotation.DatatypeDef;

@DatatypeDef(name = "VersionedTerminologyRef")
public class VersionedTerminologyRef extends TerminologyRef {

	public VersionedTerminologyRef(TerminologyRefType type, String name, String id) {
		this.type = type;
		this.name = name;
		this.id = id;
	}

	public VersionedTerminologyRef(TerminologyRefType type, String name, String id, String version) {
		this(type, name, id);
		this.version = version;
	}

	protected String version;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String getDefinition() {
		String definition = "";
		switch (type) {
		case CODESYSTEM:
			definition += "codesystem";
			break;
		case VALUESET:
			definition += "valueset";
			break;
		default:
			break;
		}

		if (name != null && id != null) {
			definition += (" \"" + name + "\" : '" + id + "'");
		}

		if (version != null) {
			definition += (" version \"" + version + "\"");
		}

		return definition;
	}

	@Override
	protected Type typedCopy() {
		// TODO Auto-generated method stub
		return null;
	}
}
