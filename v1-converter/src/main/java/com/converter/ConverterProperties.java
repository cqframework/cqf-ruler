package com.converter;
import org.hl7.fhir.r5.model.IdType;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;

@ConfigurationProperties(prefix = "converter")
public class ConverterProperties implements DaoRegistryUser {
  @Autowired
	private DaoRegistry myDaoRegistry;

  public static final IdType v1PlanDefinitionId = new IdType("PlanDefinition", "plandefinition-ersd-skeleton");
  public static final String usPHTriggeringVSProfile = "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-triggering-valueset";
  public static final String usPHTriggeringVSLibProfile = "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-triggering-valueset-library";
  public static final String ersdVSLibProfile = "http://hl7.org/fhir/us/ecr/StructureDefinition/ersd-valueset-library";
  public static final String ersdVSProfile = "http://hl7.org/fhir/us/ecr/StructureDefinition/ersd-valueset";
  public static final String ersdPlanDefinitionProfile = "http://hl7.org/fhir/us/ecr/StructureDefinition/ersd-plandefinition";
  public static final String usPHSpecLibProfile = "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-specification-library";
  public static final String usPHUsageContextType = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context-type";
  public static final String usPHUsageContext = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context";

  public DaoRegistry getDaoRegistry() {
		return myDaoRegistry;
	}
}
