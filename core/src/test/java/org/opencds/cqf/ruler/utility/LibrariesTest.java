package org.opencds.cqf.ruler.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.Test;

public class LibrariesTest {

	@Test
	public void libraryNoContentReturnsNull() {
		Library library = new Library();

		byte[] content = Libraries.getContent(library, "text/cql");

		assertNull(content);
	}

	@Test
	public void libraryWithContentReturnsContent() {
		Library library = new Library();
		library.addContent().setContentType("text/cql").setData("test-data".getBytes());


		byte[] content = Libraries.getContent(library, "text/cql");

		assertEquals("test-data", new String(content));
	}

	@Test
	public void libraryMismatchedContentReturnsNull() {
		Library library = new Library();
		library.addContent().setContentType("text/cql").setData("test-data".getBytes());


		byte[] content = Libraries.getContent(library, "text/elm");

		assertNull(content);
	}

	@Test
	public void libraryDstu3WithContentReturnsContent() {
		org.hl7.fhir.dstu3.model.Library library = new org.hl7.fhir.dstu3.model.Library();
		library.addContent().setContentType("text/cql").setData("test-data".getBytes());


		byte[] content = Libraries.getContent(library, "text/cql");

		assertEquals("test-data", new String(content));
	}

	@Test
	public void notALibraryThrowsException() {
		Measure m = new Measure();
		assertThrows(IllegalArgumentException.class, () -> {
			Libraries.getContent(m, "text/cql");
		});
	}

	@Test
	public void libraryWithVersionReturnsVersion() {
		Library library = new Library().setVersion("1.0.0");

		String version = Libraries.getVersion(library);

		assertEquals("1.0.0", version);
	}

	@Test
	public void libraryNoVersionReturnsNull() {
		Library library = new Library();

		String version = Libraries.getVersion(library);

		assertNull(version);
	}
}
