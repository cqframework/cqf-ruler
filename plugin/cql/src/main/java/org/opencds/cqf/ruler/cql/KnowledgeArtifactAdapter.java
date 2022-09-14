package org.opencds.cqf.ruler.cql;

import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeArtifactAdapter<T extends MetadataResource> {
//	protected List<RelatedArtifact> relatedArtifact;
	protected MetadataResource resource;

//	getRelatedArtifact() {
//		switch (MetadataResource.className) {
//			case "Library": return ((Library)metadataResource).getRelatedArtifact();
//			case "PlanDefinition": ...
//		}
		public KnowledgeArtifactAdapter(MetadataResource resource) {
			this.resource = resource;
		}

		public java.util.List<org.hl7.fhir.r4.model.RelatedArtifact> getRelatedArtifact() {
			switch (resource.getClass().getSimpleName()) {
				case "Library":
					return ((Library) resource).getRelatedArtifact();
				case "PlanDefinition":
					return ((PlanDefinition) resource).getRelatedArtifact();
				default: return new ArrayList<org.hl7.fhir.r4.model.RelatedArtifact>();
			}
		}

		public MetadataResource setRelatedArtifact(List<RelatedArtifact> theRelatedArtifact) {
			switch (resource.getClass().getSimpleName()) {
				case "Library":
					((Library) resource).setRelatedArtifact(theRelatedArtifact);
				case "PlanDefinition":
					((PlanDefinition) resource).setRelatedArtifact(theRelatedArtifact);
			}
			return resource;
		}

		public MetadataResource copy() {
			switch (resource.getClass().getName()) {
				case "Library":
					return ((Library) resource).copy();
				case "PlanDefinition":
					return ((PlanDefinition) resource).copy();
				default: return resource.copy();
				//TODO: Is calling MetadataResource.copy() the right default behavior?
			}
		}
}
