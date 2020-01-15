package org.opencds.cqf.r4.providers;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import org.hl7.fhir.r4.model.Type;

import ca.uhn.fhir.model.api.annotation.DatatypeDef;

@DatatypeDef(name = "PopulationCriteriaMap")
public class PopulationCriteriaMap extends Type {
	protected Map<String, Pair<String, String>> map;

	public PopulationCriteriaMap() {
		super();
		this.map = new HashMap<String, Pair<String, String>>();
	}

	public Map<String, Pair<String, String>> getMap() {
		return this.map;
	}

	public PopulationCriteriaMap setMap(Map<String, Pair<String, String>> theMap) {
		this.map = theMap;
		return this;
	}

	@Override
	protected Type typedCopy() {
		// TODO Auto-generated method stub
		return null;
	}
}
