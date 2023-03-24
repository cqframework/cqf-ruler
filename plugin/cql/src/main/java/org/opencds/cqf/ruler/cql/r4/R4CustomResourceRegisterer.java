package org.opencds.cqf.ruler.cql.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.dao.JpaResourceDao;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.opencds.cqf.ruler.api.CustomResourceRegisterer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class R4CustomResourceRegisterer implements CustomResourceRegisterer {
	private static final Logger ourLog = LoggerFactory.getLogger(R4CustomResourceRegisterer.class);
	@Override
	public void register(FhirContext context) {
		ourLog.info("registering ArtifactAssessment");
		context.registerCustomType(ArtifactAssessment.class);
		context.registerCustomType(ArtifactCommentExtension.class);
	}

	@Override
	public List<IResourceProvider> getResourceProviders(FhirContext context) {
		var dao = new JpaResourceDao<ArtifactAssessment>();
		dao.setContext(context);
		return List.of(new ArtifactAssessmentResourceProvider(dao));
	}
}
