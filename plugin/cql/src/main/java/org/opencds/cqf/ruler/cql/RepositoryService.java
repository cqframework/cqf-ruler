package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
//import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

//import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class RepositoryService extends DaoRegistryOperationProvider {

	@Autowired
	private CqlConfig cqlConfig;

    private String majorVersion = "0";
    private String minorVersion = "0";
    private String minorSubVersion = "1";

    public RepositoryService() {
    }

	 //@SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE")
	 @Operation(name = "$createNewVersion", idempotent = true, type = Library.class)
	 @Description(shortDefinition = "$createNewVersion", value = "Create a new library version")
		 public Library createNewVersion(@OperationParam(name = "currentLibrary") Library currentLibrary) throws FHIRException {
		 List<RelatedArtifact> relatedArtifactList = currentLibrary.getRelatedArtifact();
		 //List<RelatedArtifact> updatedRelatedArtifactsList = new ArrayList<RelatedArtifact>();
		 Library newLibrary = new Library();

		 for (RelatedArtifact ra : relatedArtifactList) {
			 if (ra.getType().ordinal() == RelatedArtifact.RelatedArtifactType.COMPOSEDOF.ordinal()) {
				 RelatedArtifact newRA = new RelatedArtifact();
				 if (ra.getType().toString().equals("PlanDefinition")) {
					 ra.setResource("http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-");
					 currentLibrary.addRelatedArtifact(ra);
					 newLibrary = createNewVersionData(currentLibrary);
					 newLibrary.setVersion("1.0.0");
				 } else if (ra.getType().toString().equals("Library")) {
					 ra.setResource("http://ersd.aimsplatform.org/fhir/Library/us-ecr-rctc");
					 currentLibrary.addRelatedArtifact(ra);
					 newLibrary = createNewVersionData(currentLibrary);
					 newLibrary.setVersion("1.0.0");
				 } else if (ra.getType().toString().equals("ValueSet")) {
					 ra.setResource("http://hl7.org/fhir/us/ecr/ValueSet/lotc");
					 currentLibrary.addRelatedArtifact(ra);
					 newLibrary = createNewVersionDataVS(currentLibrary);
					 newLibrary.setVersion("1.0.0");
				 }
				 if (newRA == null) {
					 relatedArtifactList.add(newRA);
				 }
			 } else if (ra.getType().ordinal() == RelatedArtifact.RelatedArtifactType.DEPENDSON.ordinal()) {
				 RelatedArtifact newRA = new RelatedArtifact();
				 if (ra.getType().toString().equals("PlanDefinition")) {
					 ra.setResource("http://cts.nlm.nih.gov/fhir/PlanDefinition/2.16");
					 currentLibrary.addRelatedArtifact(ra);
					 newLibrary = createNewVersionData(currentLibrary);
					 newLibrary.setVersion(null);
				 } else if (ra.getType().toString().equals("Library")) {
					 ra.setResource("http://cts.nlm.nih.gov/fhir/Library");
					 currentLibrary.addRelatedArtifact(ra);
					 newLibrary = createNewVersionData(currentLibrary);
					 newLibrary.setVersion(null);
				 } else if (ra.getType().toString().equals("ValueSet")) {
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
