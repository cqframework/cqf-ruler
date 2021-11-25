package org.opencds.cqf.ruler.plugin.utility;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.function.Function;

import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.junit.jupiter.api.Test;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.BaseRuntimeChildDefinition.IAccessor;

public class ReflectionUtilitiesTest implements ReflectionUtilities {

    @Test
    public void testGetFhirVersion() {
        FhirVersionEnum fhirVersion = this.getFhirVersion(Library.class);

        assertEquals(FhirVersionEnum.DSTU3, fhirVersion);
    }

    @Test
    public void testGetFhirVersionUnknownClass() {
        Library library = new Library();
        FhirVersionEnum fhirVersion = this.getFhirVersion(library.getClass());

        assertEquals(FhirVersionEnum.DSTU3, fhirVersion);
    }

    @Test
    public void testAccessor() {
        Library library = new Library().setName("test");

        IAccessor accessor = this.getAccessor(library.getClass(), "name");

        Optional<IBase> opt = accessor.getFirstValueOrNull(library);

        @SuppressWarnings("unchecked")
        IPrimitiveType<String> value = (IPrimitiveType<String>) opt.get();
        assertEquals("test", value.getValue());
    }

    @Test
    public void testGetName() {
        Library library = new Library().setName("test");

        Function<Library, String> getName = this.getNameFunction(library.getClass());

        String name = getName.apply(library);

        assertEquals("test", name);
    }

    @Test
    public void testGetNameLiteral() {
        Library library = new Library().setName("test");

        Function<Library, String> getName = this.getNameFunction(Library.class);

        String name = getName.apply(library);

        assertEquals("test", name);
    }

    @Test
    public void testGetNameNotExists() {
        assertThrows(RuntimeException.class, () -> {
            this.getNameFunction(Observation.class);
        });
    }

    @Test
    public void testGetVersion() {
        Library library = new Library().setVersion("test");

        Function<Library, String> getVersion = this.getVersionFunction(library.getClass());

        String version = getVersion.apply(library);

        assertEquals("test", version);
    }

    @Test
    public void testGetUrl() {
        Library library = new Library().setUrl("http://test.com");

        Function<Library, String> getVersion = this.getUrlFunction(library.getClass());

        String version = getVersion.apply(library);

        assertEquals("http://test.com", version);
    }
}
