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

  public final IdType v1PlanDefinitionId = new IdType("PlanDefinition", "plandefinition-ersd-skeleton");

  public DaoRegistry getDaoRegistry() {
		return myDaoRegistry;
	}
}
