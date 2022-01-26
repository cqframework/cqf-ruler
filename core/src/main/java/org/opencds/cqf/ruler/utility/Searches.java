package org.opencds.cqf.ruler.utility;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IPrimitiveType;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.UriParam;

public class Searches {
	private Searches() {
	}

	public static SearchParameterMap all() {
		return sync();
	}

	public static SearchParameterMap sync() {
		return SearchParameterMap.newSynchronous();
	}

	public static SearchParameterMap async() {
		return new SearchParameterMap();
	}

	public static SearchParameterMap byParam(String theParamName, IQueryParameterType theParam) {
		checkNotNull(theParamName);
		checkNotNull(theParam);

		return sync().add(theParamName, theParam);
	}

	public static SearchParameterMap byName(String theName) {
		checkNotNull(theName);

		return byParam("name", new StringParam(theName));
	}

	public static SearchParameterMap byNameAndVersion(String theName, String theVersion) {
		checkNotNull(theName);

		return byName(theName).add("version", new StringParam(theVersion));
	}

	public static SearchParameterMap byUrl(String theUrl) {
		checkNotNull(theUrl);
		return byParam("url", new UriParam(theUrl));
	}

	public static SearchParameterMap byCanonical(String theCanonical) {
		checkNotNull(theCanonical);

		SearchParameterMap search = byUrl(Canonicals.getUrl(theCanonical));
		String version = Canonicals.getVersion(theCanonical);
		if (version != null) {
			search.add("version", new StringParam(version));
		}

		return search;
	}

	public static <C extends IPrimitiveType<String>> SearchParameterMap byCanonical(C theCanonicalType) {
		checkNotNull(theCanonicalType);
		checkArgument(theCanonicalType.hasValue());

		return byCanonical(theCanonicalType.getValue());
	}
}
