package org.opencds.cqf.ruler.cr;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;

public class ValueSetInterceptor implements org.opencds.cqf.ruler.api.Interceptor {
  /** Handle updates */
   @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
   public void update(IBaseResource theOldResource, IBaseResource theResource) {
      if (theResource.fhirType().equals(ResourceType.ValueSet.toString())) {
        ValueSet vs = (ValueSet) theResource;
        if (vs.getUrl().equals("http://ersd.aimsplatform.org/fhir/ValueSet/rckms-condition-codes")) {
          // for each code in the value set we need to pull all the libraries which reference it
          // once we have all the libraries we need to check every related artifact and update the VSCondition URLs accordingly
          List<ConceptReferenceComponent> concepts = vs.getCompose().getInclude().get(0).getConcept();
          // for each concept.code we need to update the extension.valueCodeableConcept.coding.display to concept.designation(where code = synonym).value
          // create a Transaction search
        }
      }
   }
}
