package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.UriParam;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.test.DaoIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = { JpaFhirDalIT.class, CqlConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
public class JpaFhirDalIT extends DaoIntegrationTest {

	@Autowired
	private JpaFhirDalFactory fhirDalFactory;

	@Test
	void testFhirDalSearch() throws Exception {
		JpaFhirDal fhirDal = new JpaFhirDal(this.getDaoRegistry());

		String url = "http://somewhere.org/fhir/Library/SpecificationLibrary";
		this.update(newResource(Library.class, "A123").setUrl(url));
		this.update(newResource(Library.class, "A234").setUrl(url));

		Map<String, List<List<IQueryParameterType>>> searchParams = new HashMap<>();

		List<IQueryParameterType> urlList = new ArrayList<>();
		urlList.add(new UriParam(Canonicals.getUrl(url)));
		searchParams.put("url", List.of(urlList));

		IBaseBundle searchResultsBundle = (IBaseBundle)fhirDal.search(Canonicals.getResourceType(url), searchParams);

		assertNotNull(searchResultsBundle);
		assertTrue(((Bundle)searchResultsBundle).getEntry().size() == 2);
		assertTrue(((Bundle)searchResultsBundle).getType() == Bundle.BundleType.SEARCHSET);
		assertTrue(((Library)((Bundle)searchResultsBundle).getEntryFirstRep().getResource()).getUrl().equals(url));
	}
}
