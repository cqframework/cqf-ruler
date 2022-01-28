package org.opencds.cqf.ruler.utility;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;

public class Searches {
	private static final String VERSION_SP = "version";
	private static final String URL_SP = "url";
	private static final String NAME_SP = "name";
	private static final String ID_SP = "_id";

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

		return byParam(NAME_SP, new StringParam(theName));
	}

	public static SearchParameterMap byName(String theName, String theVersion) {
		checkNotNull(theName);
		checkNotNull(theVersion);

		return byName(theName).add(VERSION_SP, new StringParam(theVersion));
	}

	public static SearchParameterMap byUrl(String theUrl, String theVersion) {
		checkNotNull(theUrl);
		checkNotNull(theVersion);

		return byParam(URL_SP, new UriParam(theUrl)).add(VERSION_SP, new StringParam(theVersion));
	}

	public static SearchParameterMap byUrl(String theUrl) {
		checkNotNull(theUrl);

		return byParam(URL_SP, new UriParam(theUrl));
	}

	public static SearchParameterMap byCanonical(String theCanonical) {
		checkNotNull(theCanonical);

		SearchParameterMap search = byUrl(Canonicals.getUrl(theCanonical));
		String version = Canonicals.getVersion(theCanonical);
		if (version != null) {
			search.add(VERSION_SP, new TokenParam(version));
		}

		return search;
	}

	public static <C extends IPrimitiveType<String>> SearchParameterMap byCanonical(C theCanonicalType) {
		checkNotNull(theCanonicalType);
		checkArgument(theCanonicalType.hasValue());

		return byCanonical(theCanonicalType.getValue());
	}

	public static SearchParameterMap byId(IIdType theId) {
		checkNotNull(theId);
		return byParam(ID_SP, new TokenParam(theId.getIdPart()));
	}

	public static SearchParameterMap byId(String theIdPart) {
		checkNotNull(theIdPart);
		return byParam(ID_SP, new TokenParam(theIdPart));
	}
}
