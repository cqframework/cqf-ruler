package org.opencds.cqf.ruler.cql.r4;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.provider.BaseJpaResourceProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class ArtifactAssessmentResourceProvider extends BaseJpaResourceProvider<ArtifactAssessment> {
	public ArtifactAssessmentResourceProvider(IFhirResourceDao<ArtifactAssessment> theDao) {
		super(theDao);
	}
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return ArtifactAssessment.class;
	}

}
