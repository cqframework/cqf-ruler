package org.opencds.cqf;

import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.junit.Assert;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.helpers.LibraryHelper;
import org.opencds.cqf.qdm.providers.QdmDataProvider;

import java.util.List;

public class QdmDataProviderTests {

    private TestServer server;
    private final String qdmDataLocation = "qdm-data-provider/";
    private ModelManager modelManager;
    private LibraryManager libraryManager;
    private QdmDataProvider qdmDataProvider;
    private TerminologyProvider terminologyProvider;

    public QdmDataProviderTests(TestServer server) {
        this.server = server;
        this.server.dataProvider = new QdmDataProvider(this.server.dataProvider.getCollectionProviders());
        this.terminologyProvider = this.server.dataProvider.getTerminologyProvider();
        this.qdmDataProvider = new QdmDataProvider(this.server.dataProvider.getCollectionProviders());
        this.qdmDataProvider.setPackageName("org.hl7.fhir.dstu3.model");
        this.qdmDataProvider.setTerminologyProvider(terminologyProvider);
        modelManager = new ModelManager();
        libraryManager = new LibraryManager(modelManager);

        this.server.putResource(qdmDataLocation + "qdm-test-bundle.json", "");
        this.server.putResource(qdmDataLocation + "qdm-terminology-bundle.json", "");
    }

    private void runTest(String cql, String expressionName) {
        Context context = new Context(LibraryHelper.translateLibrary(cql, libraryManager, modelManager));
        context.registerDataProvider("urn:healthit-gov:qdm:v5_3", server.dataProvider);
        context.registerDataProvider("org.hl7.fhir.dstu3.model", qdmDataProvider);
        Object result = context.resolveExpressionRef(expressionName).getExpression().evaluate(context);
        Assert.assertTrue(result instanceof Iterable);
        Assert.assertTrue(((List) result).size() > 0);
    }

    void SimpleEncounterPerformedTest() {
        String cql = "library EncounterPerformedTest version '1.0'\n" +
                "using QDM version '5.3'\n" +
                "context Patient\n" +
                "define \"Qualifying Encounters\":\n" +
                "\t[\"Encounter, Performed\"] E " +
                "\t\twhere E.relevantPeriod during day of Interval[@2003-01-01, @2003-12-31]\n" +
                "\t\t\tand E.admissionSource.code = 'hosp-trans'\n" +
                "\t\t\tand E.diagnoses[0].code = '385763009'\n" +
                "\t\t\tand E.dischargeDisposition.code = '306689006'\n" +
                "\t\t\tand E.lengthOfStay.value > 60 and E.lengthOfStay.unit = 'min'\n" +
                "\t\t\tand E.principalDiagnosis.code = '385763009'\n" +
                "\t\t\tand E.facilityLocations[0].code.code = 'HOSP'\n" +
                "\t\t\tand E.facilityLocations[0].locationPeriod during day of Interval[@2003-01-01, @2003-12-31]\n";

        runTest(cql, "Qualifying Encounters");
    }

    void EncounterPerformedWithCodeFilterTest() {
        String cql = "library EncounterPerformedWithCodeFilterTest version '1.0'\n" +
                "using QDM version '5.3'\n" +
                "valueset \"Encounter Inpatient\": 'urn:oid:2.16.840.1.113883.3.666.5.307'\n" +
                "context Patient\n" +
                "define \"Qualifying Encounters\":\n" +
                "\t[\"Encounter, Performed\": \"Encounter Inpatient\"] E " +
                "\t\twhere E.relevantPeriod during day of Interval[@2003-01-01, @2003-12-31]\n";

        runTest(cql, "Qualifying Encounters");
    }

    void DiagnosisTest() {
        String cql = "library DiagnosisTest version '1.0'\n" +
                "using QDM version '5.3'\n" +
                "codesystem \"SNOMEDCT:2017-09\": 'http://snomed.info/sct' version 'urn:hl7:version:2017-09'\n" +
                "code \"Congenital absence of cervix (disorder)\": '37687000' from \"SNOMEDCT:2017-09\" display 'Congenital absence of cervix (disorder)'\n" +
                "context Patient\n" +
                "define \"Absence of Cervix\":\n" +
                "\t[\"Diagnosis\": \"Congenital absence of cervix (disorder)\"] NoCervixBirth\n" +
                "\t\twhere NoCervixBirth.prevalencePeriod starts before end of Interval[@2012-01-01, @2012-12-31]\n" +
                "\t\t\tand NoCervixBirth.anatomicalLocationSite.code = '279882009'\n" +
                "\t\t\tand NoCervixBirth.severity.display = 'Mild'\n";

        runTest(cql, "Absence of Cervix");
    }

    void DiagnosticStudyPerformedTest() {
        String cql = "library DiagnosticStudyPerformedTest version '1.0'\n" +
                "using QDM version '5.3'\n" +
                "valueset \"Mammography\": 'urn:oid:2.16.840.1.113883.3.464.1003.108.12.1018'\n" +
                "context Patient\n" +
                "define \"Mammography Diagnostics\":\n" +
                "\t[\"Diagnostic Study, Performed\": \"Mammography\"] Mammogram\n" +
                "\t\twhere Mammogram.relevantPeriod ends 27 months or less before day of end Interval[@2012-01-01, @2012-12-31]\n" +
                "\t\t\tand (Mammogram.result as Quantity).value = 2.3\n" +
                "\t\t\tand Mammogram.resultDatetime in day of Interval[@2012-01-01, @2012-12-31]\n" +
                "\t\t\tand Mammogram.status.code = 'final'\n" +
                "\t\t\tand Mammogram.components[0].code.code = '12503-9'\n" +
                "\t\t\tand Mammogram.method.code = 'MG'";

        runTest(cql, "Mammography Diagnostics");
    }

    void InterventionOrderTest() {
        String cql = "library InterventionOrderTest version '1.0'\n" +
                "using QDM version '5.3'\n" +
                "valueset \"Hospice care ambulatory\": 'urn:oid:2.16.840.1.113762.1.4.1108.15'\n" +
                "context Patient\n" +
                "define \"Has Hospice\":\n" +
                "\t[\"Intervention, Order\": \"Hospice care ambulatory\"] HospiceOrder\n" +
                "\t\twhere HospiceOrder.authorDatetime during day of Interval[@2012-01-01, @2012-12-31]\n" +
                "\t\t\tand HospiceOrder.negationRationale.code = '440621003'\n" +
                "\t\t\tand HospiceOrder.reason.code = '42343007'";

        runTest(cql, "Has Hospice");
    }

    void InterventionPerformedTest() {
        String cql = "library InterventionPerformedTest version '1.0'\n" +
                "using QDM version '5.3'\n" +
                "valueset \"Hospice care ambulatory\": 'urn:oid:2.16.840.1.113762.1.4.1108.15'\n" +
                "context Patient\n" +
                "define \"Has Hospice\":\n" +
                "\t[\"Intervention, Performed\": \"Hospice care ambulatory\"] HospicePerformed\n" +
                "\t\twhere HospicePerformed.relevantPeriod overlaps day of Interval[@2003-01-01, @2003-12-31]\n" +
                "\t\t\tand HospicePerformed.negationRationale.code = '440621003'\n" +
                "\t\t\tand HospicePerformed.reason.display = 'Generalized abdominal pain 24 hours. Localized in RIF with rebound and guarding'\n" +
                "\t\t\tand HospicePerformed.result.code = '170936009'\n" +
                "\t\t\tand HospicePerformed.status.code = 'completed'\n";

        runTest(cql, "Has Hospice");
    }

    void LaboratoryTestPerformedTest() {
        String cql = "library LaboratoryTestPerformedTest version '1.0'\n" +
                "using QDM version '5.3'\n" +
                "valueset \"Fecal Occult Blood Test (FOBT)\": 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1011'\n" +
                "context Patient\n" +
                "define \"Fecal Occult Blood Test Performed\":\n" +
                "\t[\"Laboratory Test, Performed\": \"Fecal Occult Blood Test (FOBT)\"] FecalOccultResult\n" +
                "\t\twhere FecalOccultResult.result is not null\n" +
                "\t\t\tand FecalOccultResult.relevantPeriod during day of Interval[@2012-01-01, @2012-12-31]\n" +
                "\t\t\tand FecalOccultResult.status.code = 'final'\n" +
                "\t\t\tand FecalOccultResult.method.code = '104435004'\n" +
                "\t\t\tand FecalOccultResult.resultDatetime during day of Interval[@2012-01-01, @2012-12-31]\n" +
                "\t\t\tand FecalOccultResult.reason.code = 'ProcedureRequest/qdm-test-procedurerequest'\n" +
                "\t\t\tand (start of FecalOccultResult.referenceRange).value = 1.5\n" +
                "\t\t\tand FecalOccultResult.negationRationale.code = 'not-asked'\n" +
                "\t\t\tand FecalOccultResult.components[0].code.code = '12503-9'";

        runTest(cql, "Fecal Occult Blood Test Performed");
    }

    void ProcedurePerformedTest() {
        String cql = "library ProcedurePerformedTest version '1.0'\n" +
                "using QDM version '5.3'\n" +
                "valueset \"Flexible Sigmoidoscopy\": 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1010'\n" +
                "context Patient\n" +
                "define \"Flexible Sigmoidoscopy Performed\":\n" +
                "\t[\"Procedure, Performed\": \"Flexible Sigmoidoscopy\"] FlexibleSigmoidoscopy\n" +
                "\t\twhere FlexibleSigmoidoscopy.relevantPeriod ends 5 years or less on or before day of end of Interval[@2004-01-01, @2004-12-31]\n" +
                "\t\t\tand FlexibleSigmoidoscopy.anatomicalLocationSite.code = '34381000'\n" +
                "\t\t\tand FlexibleSigmoidoscopy.incisionDatetime in day of Interval[@2003-01-01, @2003-12-31]";

        runTest(cql, "Flexible Sigmoidoscopy Performed");
    }

    void PatientCharacteristicBirthdateTest() {
        String cql = "library PatientCharacteristicBirthdateTest version '1.0'\n" +
                "using QDM version '5.3'\n" +
                "context Patient\n" +
                "define \"Birthdate Characteristic\":\n" +
                "\t[\"Patient Characteristic Birthdate\"] BirthDate\n" +
                "\t\twhere BirthDate.birthDatetime in day of Interval[@1974-01-01, @1974-12-31]";

        runTest(cql, "Birthdate Characteristic");
    }

    void PatientCharacteristicSexTest() {
        String cql = "library PatientCharacteristicSexTest version '1.0'\n" +
                "using QDM version '5.3'\n" +
                "context Patient\n" +
                "define \"Sex Characteristic\":\n" +
                "\t[\"Patient Characteristic Sex\"]";

        runTest(cql, "Sex Characteristic");
    }
}