package org.opencds.cqf.ruler.plugin.utility;

import static org.junit.jupiter.api.Assertions.*;

import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Observation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
        Application.class }, properties = {
                // Override is currently required when using MDM as the construction of the MDM
                // beans are ambiguous as they are constructed multiple places. This is evident
                // when running in a spring boot environment
                "spring.main.allow-bean-definition-overriding=true",
                "spring.batch.job.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:dbr4-mt",
                "hapi.fhir.fhir_version=r4",
                "hapi.fhir.tester_enabled=false",

})
@TestInstance(Lifecycle.PER_CLASS)
public class ResolutionUtilitiesIT implements ResolutionUtilities {

    @Autowired
    private DaoRegistry ourRegistry;

    @LocalServerPort
    private int port;

    @BeforeAll
    void beforeAll() {
        Library library1 = new Library().setName("TestLibrary").setVersion("1.0.0")
                .setUrl("http://test.com/Library/TestLibrary");
        library1.setId("libraryone");
        ourRegistry.getResourceDao(Library.class).update(library1);

        Library library2 = new Library().setName("TestLibrary").setVersion("2.0.0")
                .setUrl("http://test.com/Library/TestLibrary");
        library2.setId("librarytwo");
        ourRegistry.getResourceDao(Library.class).update(library2);

        Patient patient = new Patient();
        patient.setId("patientone");
        ourRegistry.getResourceDao(Patient.class).update(patient);

        Observation obs = new Observation();
        patient.setId("observationone");
        ourRegistry.getResourceDao(Observation.class).create(obs);
    }

    @Test
    public void testResolveByCanonicalUrl() {
        // Versionless resolves latest version
        Library lib = this.resolveByCanonicalUrl(ourRegistry, Library.class, "http://test.com/Library/TestLibrary");
        assertNotNull(lib);
        assertEquals("2.0.0", lib.getVersion());

        // Versioned resolves correct version
        lib = this.resolveByCanonicalUrl(ourRegistry, Library.class, "http://test.com/Library/TestLibrary|1.0.0");
        assertNotNull(lib);
        assertEquals("1.0.0", lib.getVersion());

        // Non-url Resource type explodes
        assertThrows(RuntimeException.class, () -> {
            this.resolveByCanonicalUrl(ourRegistry, Patient.class, "http://test.com/Patient");
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testResolveByName() {
        // Name only resolves latest version
        Library lib = this.resolveByName(ourRegistry, Library.class, "TestLibrary");
        assertNotNull(lib);
        assertEquals("2.0.0", lib.getVersion());

        // Name only on resource dao resolves latest version
        lib = (Library) this.resolveByName(ourRegistry.getResourceDao("Library"), "TestLibrary");
        assertNotNull(lib);
        assertEquals("2.0.0", lib.getVersion());

        // Non-name Resource type explodes
        assertThrows(RuntimeException.class, () -> {
            this.resolveByName(ourRegistry, Observation.class, "NotAName");
        });
    }

    @Test
    public void testResolveByNameAndVersion() {
        // Name only resolves latest version
        Library lib = this.resolveByNameAndVersion(ourRegistry, Library.class, "TestLibrary", null);
        assertNotNull(lib);
        assertEquals("2.0.0", lib.getVersion());

        // Version resolves correct version
        lib = this.resolveByNameAndVersion(ourRegistry, Library.class, "TestLibrary", "1.0.0");
        assertNotNull(lib);
        assertEquals("1.0.0", lib.getVersion());

        // Non-name Resource type explodes
        assertThrows(RuntimeException.class, () -> {
            this.resolveByNameAndVersion(ourRegistry, Observation.class, "NotAName", null);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testResolveById() {
        // Full Id resolves
        Library lib = this.resolveById(ourRegistry, Library.class, "Library/librarytwo");
        assertNotNull(lib);
        assertEquals("librarytwo", lib.getIdElement().getIdPart());

        // Partial Id resolves
        lib = this.resolveById(ourRegistry, Library.class, "libraryone");
        assertNotNull(lib);
        assertEquals("libraryone", lib.getIdElement().getIdPart());

        // FhirDao resolves
        lib = (Library) this.resolveById(ourRegistry.getResourceDao("Library"), "Library/librarytwo");
        assertNotNull(lib);
        assertEquals("librarytwo", lib.getIdElement().getIdPart());

        // Doesn't exist explodes
        assertThrows(RuntimeException.class, () -> {
            this.resolveById(ourRegistry, Library.class, "librarythree");
        });

        // Doesn't exist on partition explodes
        assertThrows(RuntimeException.class, () -> {
            this.resolveById(ourRegistry, Patient.class, "patienttwo");
        });

        // Mismatched Id types explodes
        assertThrows(RuntimeException.class, () -> {
            this.resolveById(ourRegistry, Library.class, "Patient/patientone");
        });
    }
}
