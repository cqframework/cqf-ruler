package org.opencds.cqf.ruler.cr;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.ValueSet;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;

public class ValueSetInterceptor implements org.opencds.cqf.ruler.api.Interceptor {
  /** Handle updates */
   @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
   public void update(IBaseResource theOldResource, IBaseResource theResource) {
      if (theResource.fhirType().equals(ResourceType.ValueSet.toString())) {
        ValueSet vs = (ValueSet) theResource;
        List<Extension> extensions = vs.getExtension();
        extensions.add(new Extension("testuri", new StringType("testval")));
      }
   }
}
