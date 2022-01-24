package org.opencds.cqf.ruler.utility;

import static com.google.common.base.Preconditions.checkNotNull;

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

		return SearchParameterMap.newSynchronous(theParamName, theParam);
	}

	public static SearchParameterMap byName(String theName) {
		checkNotNull(theName);

		return SearchParameterMap.newSynchronous("name", new StringParam(theName));
	}

	public static SearchParameterMap byUrl(String theUrl) {
		checkNotNull(theUrl);

		String url = Canonicals.getUrl(theUrl);

		return SearchParameterMap.newSynchronous("url", new UriParam(url));
	}
}
