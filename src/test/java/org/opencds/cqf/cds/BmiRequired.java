package org.opencds.cqf.cds;

import ca.uhn.fhir.model.primitive.IdDt;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencds.cqf.TestUtil;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.codesystems.LibraryType;
import org.hl7.fhir.dstu3.model.codesystems.PlanDefinitionType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.codesystems.ActionType;
import org.junit.Assert;
import org.opencds.cqf.builders.*;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class BmiRequired {
    String rootDir = "bmi";
    private Practitioner practitioner;
    private Patient patient;
    private Encounter encounter;
    private String cqlString;
    private PlanDefinition planDefinition;
    private PlanDefinition planDefinitionPlain;
    private ActivityDefinition createBmiAd;

    @BeforeClass
    public static void init() throws Exception {
        TestUtil.startServer();
    }

//    @AfterClass()
//    public static void stopServer() throws Exception {
//        TestUtil.stopServer();
//    }

    @Test()
    public void testBasicPd() throws Exception {
        List<IBaseResource> resources = loadResources();
        for (IBaseResource baseResource : resources) {
            TestUtil.putResource(baseResource);
        }

        Parameters outParams = TestUtil.getOurClient()
            .operation()
            .onInstance(new IdDt("PlanDefinition", getPlanDefinitionId()))
            .named("$apply")
            .withParameters(getParameters())
            .useHttpGet()
            .execute();

        List<Parameters.ParametersParameterComponent> response = outParams.getParameter();

        Assert.assertTrue(!response.isEmpty());

        Resource resource = response.get(0).getResource();

        Assert.assertTrue(resource instanceof CarePlan);

        CarePlan carePlan = (CarePlan) resource;

        assertNotNull( carePlan );
        carePlan.setId("bmiCarePlan");
        TestUtil.putResource(patient);
        TestUtil.putResource(carePlan);

    }

    private Observation createHeightObservation(Patient patient, String id, double height) {
        Observation observation = new ObservationBuilder()
            .buildId(id)
            .buildEffectiveDateTime( new Date() )
            .buildSubject(patient)
            .buildCode( "8302-2", "http://loinc.org", "Height")
            .build();
        observation.setValue( new Quantity()
            .setValue( height )
            .setCode("m")
            .setUnit("m")
            .setSystem("http://unitsofmeasure.org")
        );
        return observation;
    }

    private Observation createWeightObservation(Patient patient, String id, double weigth ) {
        Observation observation = new ObservationBuilder()
            .buildId(id)
            .buildEffectiveDateTime( new Date() )
            .buildSubject(patient)
            .buildCode( "27113001", "http://snomed.info/sct", "Weight")
            .build();
        observation.setValue( new Quantity()
            .setValue( weigth )
            .setCode("kg")
            .setUnit("kg")
            .setSystem("http://unitsofmeasure.org")
        );
        return observation;
    }

    private Observation createBmiObservation(Patient patient, String id, double bmi ) {
        Observation observation = new ObservationBuilder()
            .buildId(id)
            .buildEffectiveDateTime( new Date() )
            .buildSubject(patient)
            .buildCode( "39156-5", "http://loinc.org", "BMI observation")
            .build();
        observation.setValue( new Quantity()
            .setValue( bmi )
            .setCode("kg/m2")
            .setUnit("kg/m2")
            .setSystem("http://unitsofmeasure.org")
        );
        return observation;
    }


    ///////////////////////////////////////////////////////////////////////

    public List<IBaseResource> loadResources() throws IOException {
        ArrayList<IBaseResource> resources = new ArrayList<>();
        String baseDir = rootDir;

        //////////////////////////////////////////////////////////
        URL url = Resources.getResource(baseDir+"/bmi_library.cql");
        cqlString = Resources.toString(url, Charsets.UTF_8);
        Library library = new LibraryBuilder()
            .buildId("bmi")
            .buildVersion("0.1.0")
            .buildStatus(Enumerations.PublicationStatus.ACTIVE)
            .buildExperimental( true )
            .buildType(LibraryType.LOGICLIBRARY)
            .buildCqlContent( cqlString )
            .build();
        resources.add(library);

        //////////////////////////////////////////////\
        createBmiAd =
            new ActivityDefinitionBuilder()
                .buildId( "createBmi")
                .buildNameAndTitle("Creates BMI Observation")
                .buildDescription("Body Mass Index (BMI) is a person's weight in kilograms divided by the square of height" +
                    " in meters. A high BMI can be an indicator of high body fatness. BMI can be used to screen for" +
                    " weight categories that may lead to health problems but it is not diagnostic of the body" +
                    " fatness or health of an individual.")
                .buildLibrary( "Library/"+library.getId() )
                .buildKind( ActivityDefinition.ActivityDefinitionKind.OBSERVATION)
                .buildCqlDynamicValue("Create basic Observation", "$this", "bmi.observationFromCdr")
                .buildCqlDynamicValue("Set Observation value from storage","valueQuantity", "bmi.bmivalue" )
                .build();
        resources.add(createBmiAd);

        //////////////////////////////////////////////\
        ActivityDefinition createHeightPrAd =
            (ActivityDefinition) new ActivityDefinition()
                .setName("Creates Height ProcedureRequest")
                .setTitle("Creates Height ProcedureRequest")
                .setStatus(Enumerations.PublicationStatus.DRAFT)
                .setDescription(" Create a request for a heigth measurement")
                .setKind( ActivityDefinition.ActivityDefinitionKind.PROCEDUREREQUEST)
                .setCode( new CodeableConcept().addCoding( new CodingBuilder().buildLoincCode("8302-2").build()) )
                .addDynamicValue( new ActivityDefinition.ActivityDefinitionDynamicValueComponent()
                    .setDescription("Set required time to now")
                    .setLanguage("text/fhirpath")
                    .setExpression("now()")
                    .setPath("occurrence")
                )
                .setId( "createHeightPrAd");
        resources.add(createHeightPrAd);

        ////////////////////////////////////////////////
        ActivityDefinition createWeightPrAd =
            (ActivityDefinition) new ActivityDefinition()
                .setName("Creates Weight ProcedureRequest")
                .setTitle("Creates Weight ProcedureRequest")
                .setStatus(Enumerations.PublicationStatus.DRAFT)
                .setDescription(" Create a request for a weigth measurement")
                .setKind( ActivityDefinition.ActivityDefinitionKind.PROCEDUREREQUEST)
                .setCode( new CodeableConcept().addCoding( new CodingBuilder().buildLoincCode("8302-2").build()) )
                .addDynamicValue( new ActivityDefinition.ActivityDefinitionDynamicValueComponent()
                    .setDescription("Set required time to now")
                    .setLanguage("text/fhirpath")
                    .setExpression("now()")
                    .setPath("occurrence")
                )
                .setId( "createHeightPrAd");
        resources.add(createHeightPrAd);

        ////////////////////////////////////////////\
        planDefinition = (PlanDefinition) new PlanDefinition();
        planDefinition.setId(getPlanDefinitionId());
        planDefinition
            .setVersion("0.1.0")
            .setName("BMI required")
            .setTitle("BMI required")
            .setType( new CodeableConcept()
                .addCoding( new Coding()
                    .setCode(PlanDefinitionType.ECARULE.toCode())
                    .setDisplay(PlanDefinitionType.ECARULE.getDisplay())
                    .setSystem(PlanDefinitionType.ECARULE.getSystem())
                )
            )
            .setPublisher("Philips Research")
            .setDescription("Simple sample Plan Definition. It makes sure that all patients have recent BMI observation.")
            .addLibrary( new Reference().setReference("Library/"+library.getId()))
            .addAction( new PlanDefinitionActionBuilder()
                .buildTitle("BMI observation required")
                .buildDescription("Checks whether a BMI is present if possible calculate BMI.")
                .addCondition( new PlanDefinition.PlanDefinitionActionConditionComponent()
                    .setKind( PlanDefinition.ActionConditionKind.APPLICABILITY )
                    .setDescription("No BMI, height and weight present")
                    .setLanguage("text/cql")
                    .setExpression("not(bmi.recentBmiObservation) and bmi.recentHeightMeasurement and bmi.recentWeightMeasurement")
                )
                .buildType( ActionType.CREATE )
                .buildDefinition( "ActivityDefinition/"+createBmiAd.getId())
                .build()
            )
            .addAction( new PlanDefinitionActionBuilder()
                .buildTitle("Height measurement required")
                .buildDescription("Checks whether a height measurement is present, issues procedure request if not.")
                .addCondition( new PlanDefinition.PlanDefinitionActionConditionComponent()
                    .setKind( PlanDefinition.ActionConditionKind.APPLICABILITY )
                    .setDescription("No height present")
                    .setLanguage("text/cql")
                    .setExpression("not(bmi.recentBmiObservation) and not( bmi.recentHeightMeasurement )")
                )
                .buildType( ActionType.CREATE )
                .buildDefinition( "ActivityDefinition/"+createHeightPrAd.getId())
                .build()
            )
            .addAction( new PlanDefinitionActionBuilder()
                .buildTitle("Weight measurement required")
                .buildDescription("Checks whether a weight measurement is present, issues procedure request if not.")
                .addCondition( new PlanDefinition.PlanDefinitionActionConditionComponent()
                    .setKind( PlanDefinition.ActionConditionKind.APPLICABILITY )
                    .setDescription("No weight present")
                    .setLanguage("text/cql")
                    .setExpression("not(bmi.recentBmiObservation) and not( bmi.recentWeightMeasurement )")
                )
                .buildType( ActionType.CREATE )
                .buildDefinition( "ActivityDefinition/"+createWeightPrAd.getId())
                .build()
            );

        resources.add(planDefinition);

        ///////////////////////////////////////////////////////////
        practitioner = new PractitionerBuilder("BMI-Pr1", "1969-05-23").build();
        patient      = new PatientBuilder("BMI-Pa1", "1980-03-15", practitioner ).build() ;
        encounter    = new EncounterBuilder( "BMI-Enc", patient ).build();
        resources.add(practitioner);
        resources.add(patient);
        resources.add(encounter);

        return resources;
    }

    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        parameters.addParameter().setName("patient").setValue(new StringType("Patient/"+patient.getId()));
        parameters.addParameter().setName("practitioner").setValue(new StringType("Practitioner/performance"));
        return parameters;
    }

    public String getPlanDefinitionId() {
        return "bmiProtocol";
    }

    public Practitioner getPractitioner() {
        return practitioner;
    }

    public Patient getPatient() {
        return patient;
    }

    public Encounter getEncounter() {
        return encounter;
    }

    public String getCqlString() {
        return cqlString;
    }


}
