package org.opencds.cqf.ruler.cr;

import java.util.ArrayList;
import java.util.List;

import org.cqframework.fhir.api.FhirDal;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.opencds.cqf.ruler.utility.TypedBundleProvider;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class ValueSetInterceptor implements org.opencds.cqf.ruler.api.Interceptor, DaoRegistryUser {
  	@Autowired
	  private DaoRegistry myDaoRegistry;

    public DaoRegistry getDaoRegistry() {
      return myDaoRegistry;
    }

    /** Handle updates */
   @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
   public void update(
    IBaseResource theOldResource,
    IBaseResource theResource,
    RequestDetails theRequestDetails) {
      if (theResource.fhirType().equals(ResourceType.ValueSet.toString())) {
        ValueSet vs = (ValueSet) theResource;
        FhirDal fhirDal = new JpaFhirDal(myDaoRegistry,theRequestDetails);
        if (vs.getUrl().equals("http://ersd.aimsplatform.org/fhir/ValueSet/rckms-condition-codes")) {
          // for each code in the value set we need to pull all the libraries which reference it
          // once we have all the libraries we need to check every related artifact and update the VSCondition URLs accordingly
          List<ConceptSetComponent> sets = vs.getCompose().getInclude();
          Bundle transactionBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
          List<Library> librariesToUpdate = new ArrayList<Library>();
          for (ConceptSetComponent set: sets) {
            String system = set.getSystem();
            List<ConceptReferenceComponent> concepts = set.getConcept();
            // for each concept.code we need to update the extension.valueCodeableConcept.coding.display to concept.designation(where code = synonym).value
            for (ConceptReferenceComponent concept:concepts) {
              SearchParameterMap map = new SearchParameterMap();
              TokenParam token = new TokenParam(system,concept.getCode());
              map.add("manifest-contains-code",token);
              TypedBundleProvider<Library> result = search(Library.class, map);
              result.getAllResourcesTyped().stream().forEach(lib -> {
                Library addToListIfMissing = librariesToUpdate
                  .stream().filter(existing -> existing.getId().equals(lib.getId())).findFirst()
                  .orElseGet(() -> {
                    librariesToUpdate.add(lib);
                    return lib;
                  });
                String newText = concept.getDesignation()
                  .stream().filter(designation -> designation.getUse().getCode().equals("synonym")).findFirst().orElseThrow(()-> new UnprocessableEntityException("No synonym found!"))
                  .getValue();
                updateConditionWithNewValue(addToListIfMissing,system, concept.getCode(),newText );
              });
              // transactionBundle.addEntry(createEntry(system, concept.getCode()));
            }
          }
          for (Library library:librariesToUpdate) {
            fhirDal.update(library);
          }
          
          // create a Transaction search'
          // Bundle transactionBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
          // Bundle result = transaction(transactionBundle);
          // if(result.getEntry().size() > 0){
          //   var zeo = result.getEntry().get(0);
          //   zeo.getResource();
          // }
        }
      }
   }
   private void updateConditionWithNewValue(Library library, String system, String code, String newText) {
    List<Extension> extensions = library.getExtension();
    extensions.stream()
      .filter(ext -> ext.getUrl().equals(KnowledgeArtifactProcessor.valueSetConditionUrl))
      .map(ext -> (CodeableConcept)ext.getValue())
      .filter(concept -> concept.getCoding().stream().anyMatch(coding -> coding.getSystem().equals(system) && coding.getCode().equals(code)))
      .forEach(concept -> {
        concept.setText(newText);
      });
   }
  private BundleEntryComponent createEntry(String system, String code) {
		BundleEntryComponent entry = new Bundle.BundleEntryComponent()
				.setRequest(createRequest(system + "|" + code));
		return entry;
	}

	private BundleEntryRequestComponent createRequest(String token) {
		Bundle.BundleEntryRequestComponent request = new Bundle.BundleEntryRequestComponent();
    String query = "Library?manifest-contains-code=" + token;
    request
      .setMethod(Bundle.HTTPVerb.GET)
      .setUrl(query);
		return request;
	}

}
