package org.opencds.cqf.r4.helpers;

import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContainedHelper {

  // > Contained resources SHALL NOT contain additional contained resources.
  // https://www.hl7.org/fhir/references.html#contained
  public static DomainResource liftContainedResourcesToParent(DomainResource resource) {
    getContainedResourcesInContainedResources(resource)
        .forEach(resource::addContained);   // add them to the parent

    return resource;  // Return the resource to allow for method chaining
  }

  private static List<Resource> getContainedResourcesInContainedResources(Resource resource) {
    if (!(resource instanceof DomainResource)) {
      return new ArrayList<>();
    }
    return streamContainedResourcesInContainedResources(resource).collect(Collectors.toList());
  }

  public static List<Resource> getAllContainedResources(Resource resource) {
    if (!(resource instanceof DomainResource)) {
      return new ArrayList<>();
    }
    return streamAllContainedResources(resource).collect(Collectors.toList());
  }

  private static Stream<Resource> streamContainedResourcesInContainedResources(Resource resource) {
    if (!(resource instanceof DomainResource)) {
      return Stream.empty();
    }
    return ((DomainResource) resource)
        .getContained() // We don't need to re-add any resources that are already on the parent.
        .stream()
        .flatMap(ContainedHelper::streamAllContainedResources);  // Get the resources contained
  }

  private static Stream<Resource> streamAllContainedResources(Resource resource) {
    if (!(resource instanceof DomainResource)) {
      return Stream.empty();
    }
    List<Resource> contained = ((DomainResource) resource).getContained();

    return Stream
        .concat(contained.stream(),
            contained
                .stream()
                .flatMap(ContainedHelper::streamAllContainedResources));
  }
}
