package org.opencds.cqf.ruler.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import ca.uhn.fhir.jpa.partition.PartitionManagementProvider;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
        Application.class }, properties = {
                // Override is currently required when using MDM as the construction of the MDM
                // beans are ambiguous as they are constructed multiple places. This is evident
                // when running in a spring boot environment
                "spring.main.allow-bean-definition-overriding=true",
                "spring.batch.job.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:dbr4-mt",
                "hapi.fhir.fhir_version=r4"

})
@TestInstance(Lifecycle.PER_CLASS)
public class MultiTenantResolutionUtilitiesIT implements ResolutionUtilities {

    @Autowired
    private DaoRegistry ourRegistry;

    @Autowired
    private PartitionManagementProvider ourPartitionManagementProvider;

    @Autowired
	protected PartitionSettings myPartitionSettings;

    @LocalServerPort
    private int port;

    @BeforeAll
    void beforeAll() {
        myPartitionSettings.setPartitioningEnabled(true);
        ourPartitionManagementProvider.addPartition(null, new IntegerType(1), new StringType("test"), new StringType("test partition"));
       
        SystemRequestDetails details = new SystemRequestDetails().setRequestPartitionId(RequestPartitionId.fromPartitionId(1));

        Patient patient2 = new Patient();
        patient2.setId("patienttwo");

        ourRegistry.getResourceDao(Patient.class).update(patient2, details);
    }

    @Test
    public void testResolveById() {
        // Resolve on tenant succeeds
        SystemRequestDetails details = new SystemRequestDetails().setRequestPartitionId(RequestPartitionId.fromPartitionId(1));
        Patient patient = this.resolveById(ourRegistry, Patient.class, "patienttwo", details);
        assertNotNull(patient);
        assertEquals("patienttwo", patient.getIdElement().getIdPart());
    }
}
