package org.opencds.cqf.ruler.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.Test;

public class LibraryUtilitiesTest implements LibraryUtilities {

	@Test
	public void libraryNoContentReturnsNull() {
		Library library = new Library();

		byte[] content = this.getContent(library, "text/cql");

		assertNull(content);
	}

	@Test
	public void libraryWithContentReturnsContent() {
		Library library = new Library();
		library.addContent().setContentType("text/cql").setData("test-data".getBytes());


		byte[] content = this.getContent(library, "text/cql");

		assertEquals("test-data", new String(content));
	}

	@Test
	public void libraryMismatchedContentReturnsNull() {
		Library library = new Library();
		library.addContent().setContentType("text/cql").setData("test-data".getBytes());


		byte[] content = this.getContent(library, "text/elm");

		assertNull(content);
	}

	@Test
	public void libraryDstu3WithContentReturnsContent() {
		org.hl7.fhir.dstu3.model.Library library = new org.hl7.fhir.dstu3.model.Library();
		library.addContent().setContentType("text/cql").setData("test-data".getBytes());


		byte[] content = this.getContent(library, "text/cql");

		assertEquals("test-data", new String(content));
	}

	@Test
	public void notALibraryThrowsException() {

		assertThrows(IllegalArgumentException.class, () -> {
			this.getContent(new Measure(), "text/cql");
		});
	}
}
