package org.opencds.cqf.ruler.cql;

//import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;


import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.ruler.utility.r4.Parameters.parameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.part;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = { RepositoryServiceTest.class, CqlConfig.class },
	properties = {"hapi.fhir.fhir_version=r4", "hapi.fhir.security.basic_auth.enabled=false"})
class RepositoryServiceTest extends RestIntegrationTest {

	@Test
	void draftOperation_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");
		Library specLibrary = (Library) readResource("ersd-library-example.json");
		//Library specLibrary = (Library) readResource("ersdv2bundle1-1-bundle-trimmed.json");
		//Bundle specLibrary = (Bundle) readResource("ersdv2bundle1-1-bundle-trimmed.json");
		//loadTransaction("ersdv2bundle1-1-bundle-trimmed.json");
		specLibrary.setStatus(Enumerations.PublicationStatus.ACTIVE);

		Parameters params = parameters( part("specification", specLibrary) );

		Resource returnResource = getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$draft")
			.withNoParameters(Parameters.class)
			.returnResourceType(Library.class)
			.execute();

//		Library returnResource = getClient().operation()
//			//.onInstance("Library/ersd20220329")
//			.onInstance("Library/SpecificationLibrary")
//			.named("$release")
//			.withNoParameters(Parameters.class)
//			.useHttpGet()
//			.returnResourceType(Library.class)
//			.execute();
//
		assertNotNull(returnResource);

//		Resource returnResource1 = getClient().operation()
//			.onServer()
//			.named("$draft")
//			.withParameters(params)
//			.returnResourceType(Library.class)
//			.execute();
//
//		assertNotNull(returnResource1);
	}

	@Test
	void releaseResource_test() {
		loadTransaction("ersd-draft-transaction-bundle-example.json");
		Library returnResource = getClient().operation()
			//.onInstance("Library/ersd20220329")
			.onInstance("Library/SpecificationLibrary")
			.named("$release")
			.withNoParameters(Parameters.class)
			.useHttpGet()
			.returnResourceType(Library.class)
			.execute();

		assertNotNull(returnResource);
		//assertTrue(isActive("Library/SpecificationLibrary"));
		//assertTrue(isActive("PlanDefinition/plandefinition-ersd-instance-example"));
		//assertTrue(isActive("Library/library-rctc-example"));
		//assertTrue(isActive("ValueSet/dxtc"));
		//assertTrue(isActive("ValueSet/lotc"));
		//assertTrue(isActive("ValueSet/lrtc"));
		//assertTrue(isActive("ValueSet/mrtc"));
		//assertTrue(isActive("ValueSet/ostc"));
		//assertTrue(isActive("ValueSet/sdtc"));
	}

	@Test
	void publishResource_test() {
		Library specLibrary = (Library) readResource("ersd-library-example.json");
		loadTransaction("ersd-active-transaction-bundle-example.json");

		Parameters params = parameters( part("specification", specLibrary) );

		Library returnResource = getClient().operation()
			.onServer()
			.named("$publish")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();

		assertNotNull(returnResource);
		assertTrue(isActive("Library/SpecificationLibrary"));
		assertTrue(isActive("PlanDefinition/plandefinition-ersd-instance-example"));
		assertTrue(isActive("Library/library-rctc-example"));
		assertTrue(isActive("ValueSet/dxtc"));
		assertTrue(isActive("ValueSet/lotc"));
		assertTrue(isActive("ValueSet/lrtc"));
		assertTrue(isActive("ValueSet/mrtc"));
		assertTrue(isActive("ValueSet/ostc"));
		assertTrue(isActive("ValueSet/sdtc"));
	}

	@Test
	void reviseOperation_test() {
		Library specLibrary = (Library) readResource("ersd-library-example.json");
		loadTransaction("ersd-transaction-bundle-example.json");

		Parameters params = parameters( part("specification", specLibrary) );

		Library returnResource = getClient().operation()
			.onServer()
			.named("$revise")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();

		assertNotNull(returnResource);
//		assertTrue(isActive("Library/SpecificationLibrary"));
//		assertTrue(isActive("PlanDefinition/plandefinition-ersd-instance-example"));
//		assertTrue(isActive("Library/library-rctc-example"));
//		assertTrue(isActive("ValueSet/dxtc"));
//		assertTrue(isActive("ValueSet/lotc"));
//		assertTrue(isActive("ValueSet/lrtc"));
//		assertTrue(isActive("ValueSet/mrtc"));
//		assertTrue(isActive("ValueSet/ostc"));
//		assertTrue(isActive("ValueSet/sdtc"));
	}

	private boolean isActive(String id) {
		MetadataResource resource = read(new IdType(id));
		return resource.getStatus() == Enumerations.PublicationStatus.ACTIVE;
	}
}
