package org.opencds.cqf.ruler.cr.dstu3;

import org.hl7.fhir.dstu3.model.Library;

public class Libraries {

	public static Library library(String cql) {
		Library library = new Library();
		library.setId("Library/library-Test");
		library.setName("Test");
		library.setVersion("1.0.0");
		library.setUrl("http://test.com/fhir/Library/Test");
		library.getType().getCodingFirstRep().setCode("logic-library");
		library.getContentFirstRep().setContentType("text/cql").setData(cql.getBytes());
		return library;
	}
}
