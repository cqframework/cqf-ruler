package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.rest.annotation.Operation;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
@Repository
public class RepositoryService {

    private String majorVersion = "0";
    private String minorVersion = "0";
    private String minorSubVersion = "1";

    public RepositoryService() {
    }

	@Operation(name = "$createNewVersion", idempotent = true, type = Library.class)
	 public Library createNewVersion(Library currentLibrary) {
		 List<RelatedArtifact> relatedArtifactList = currentLibrary.getRelatedArtifact();
		 List<RelatedArtifact> updatedRelatedArtifactsList = new ArrayList<RelatedArtifact>();
		 Library newLibrary = new Library();

		 for (RelatedArtifact ra : relatedArtifactList) {
			 if (ra.equals(RelatedArtifact.RelatedArtifactType.COMPOSEDOF)) {
				 RelatedArtifact newRA = new RelatedArtifact();
				 if (ra.getType().equals("PlanDefinition")) {
					 ra.setResource("http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-");
					 currentLibrary.addRelatedArtifact(ra);
					 newLibrary = createNewVersionData(currentLibrary);
					 newLibrary.setVersion("1.0.0");
				 } else if (ra.getType().equals("Library")) {
					 ra.setResource("http://ersd.aimsplatform.org/fhir/Library/us-ecr-rctc");
					 currentLibrary.addRelatedArtifact(ra);
					 newLibrary = createNewVersionData(currentLibrary);
					 newLibrary.setVersion("1.0.0");
				 } else if (ra.getType().equals("ValueSet")) {
					 ra.setResource("hhttp://hl7.org/fhir/us/ecr/ValueSet/lotc");
					 currentLibrary.addRelatedArtifact(ra);
					 newLibrary = createNewVersionDataVS(currentLibrary);
					 newLibrary.setVersion("1.0.0");
				 }
				 if (newRA == null) {
					 relatedArtifactList.add(newRA);
				 }
			 } else if (ra.equals(RelatedArtifact.RelatedArtifactType.DEPENDSON)) {
				 RelatedArtifact newRA = new RelatedArtifact();
				 if (ra.getType().equals("PlanDefinition")) {
					 ra.setResource("http://cts.nlm.nih.gov/fhir/PlanDefinition/2.16");
					 currentLibrary.addRelatedArtifact(ra);
					 newLibrary = createNewVersionData(currentLibrary);
					 newLibrary.setVersion(null);
				 } else if (ra.getType().equals("Library")) {
					 ra.setResource("http://cts.nlm.nih.gov/fhir/Library");
					 currentLibrary.addRelatedArtifact(ra);
					 newLibrary = createNewVersionData(currentLibrary);
					 newLibrary.setVersion(null);
				 } else if (ra.getType().equals("ValueSet")) {
					 ra.setResource("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.4");
					 currentLibrary.addRelatedArtifact(ra);
					 newLibrary = createNewVersionDataVS(currentLibrary);
					 newLibrary.setVersion(null);
				 }
				 if (newRA == null) {
					 relatedArtifactList.add(newRA);
				 }
			 }
		 }
		 return newLibrary;
	 }

	 public Library createNewVersionData(Library currentLibrary) {
		 Library newLibrary = new Library();

		 newLibrary.setUrl("http://ersd.aimsplatform.org/fhir/Library/us-ecr-specification");
		 newLibrary.setName("USECRSpecification");
		 newLibrary.setTitle("US eCR Specification");
		 newLibrary.setDescription("Related Artifact Description");
		 newLibrary.setStatus(Enumerations.PublicationStatus.ACTIVE);
		 newLibrary.setVersion("alphora-draft-" + majorVersion + "." + minorVersion + "." + minorSubVersion);
		 newLibrary.setRelatedArtifact(currentLibrary.getRelatedArtifact());
		 return newLibrary;
	 }

	public Library createNewVersionDataVS(Library currentLibrary) {
		Library newLibrary = new Library();

		newLibrary.setUrl("http://ersd.aimsplatform.org/fhir/Library/us-ecr-specification");
		newLibrary.setName("USECRSpecification");
		newLibrary.setTitle("US eCR Specification");
		newLibrary.setDescription("Related Artifact Description");
		newLibrary.setStatus(Enumerations.PublicationStatus.ACTIVE);
		newLibrary.setVersion("alphora-draft-" + majorVersion + "." + minorVersion + "." + minorSubVersion);
		newLibrary.setContained(currentLibrary.getContained());
		return newLibrary;
	}
}
