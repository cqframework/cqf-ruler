package org.opencds.cqf.helpers;

import ca.uhn.fhir.context.FhirContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hl7.fhir.dstu3.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class XlsxToValueSet {

    private static Map<String, ValueSet> valuesets = new HashMap<>();
    private static Map<String, CodeSystem> codeSystems = new HashMap<>();

    private static void populateOidVs(String oid) {
        ValueSet vs = new ValueSet();
        vs.setId(oid);
        vs.setStatus(Enumerations.PublicationStatus.DRAFT);
        valuesets.put(oid, vs);
    }

    private static void populateOidCs(String oid) {
        CodeSystem cs = new CodeSystem();
        cs.setId(oid);
        cs.setStatus(Enumerations.PublicationStatus.DRAFT);
        cs.setContent(CodeSystem.CodeSystemContentMode.EXAMPLE);
        codeSystems.put(oid, cs);
    }

    private static String getNextCellString(Iterator<Cell> colIterator) {
        if (colIterator.hasNext()) {
            Cell temp = colIterator.next();
            temp.setCellType(CellType.STRING);
            return temp.getStringCellValue();
        }
        return "";
    }

    // Format:
    //   oid ... system ... version ... code ... display
    private static void resolveRowVs(Iterator<Cell> colIterator) {
        String oid = getNextCellString(colIterator);

        if (!valuesets.containsKey(oid)) {
            populateOidVs(oid);
        }

        String system = getNextCellString(colIterator);
        String version = getNextCellString(colIterator);
        String code = getNextCellString(colIterator);
        String display = getNextCellString(colIterator);

        ValueSet.ValueSetComposeComponent vscc = valuesets.get(oid).getCompose();
        ValueSet.ConceptSetComponent component = new ValueSet.ConceptSetComponent();
        component.setSystem(system).setVersion(version);
        component.addConcept(new ValueSet.ConceptReferenceComponent().setCode(code).setDisplay(display));
        vscc.addInclude(component);
    }

    // oid ... url ... version ... code ... display
    private static void resolveRowCs(Iterator<Cell> colIterator) {
        String oid = getNextCellString(colIterator);

        if (!codeSystems.containsKey(oid)) {
            populateOidCs(oid);
        }

        String url = getNextCellString(colIterator);
        String version = getNextCellString(colIterator);
        String code = getNextCellString(colIterator);
        String display = getNextCellString(colIterator);

        CodeSystem cs = codeSystems.get(oid);
        cs.setUrl(url).setVersion(version);
        cs.getConcept().add(new CodeSystem.ConceptDefinitionComponent().setCode(code).setDisplay(display));
    }

    private static Bundle valuesetBundle() {
        Bundle temp = new Bundle();
        for (String key : valuesets.keySet())
            temp.addEntry(new Bundle.BundleEntryComponent().setResource(valuesets.get(key)));

        return temp;
    }

    private static Bundle codesystemBundle() {
        Bundle temp = new Bundle();
        for (String key : codeSystems.keySet())
            temp.addEntry(new Bundle.BundleEntryComponent().setResource(codeSystems.get(key)));

        return temp;
    }

    // library function use
    public static Bundle convertCs(Workbook workbook) {
        Iterator<Row> rowIterator = workbook.getSheetAt(0).iterator();

        while (rowIterator.hasNext()) {
            Iterator<Cell> colIterator = rowIterator.next().iterator();
            resolveRowCs(colIterator);
        }

        return codesystemBundle();
    }

    public static Bundle convertCs(String workbookPath) {
        return convertCs(getWorkbook(workbookPath));
    }

    // library function use
    public static Bundle convertVs(Workbook workbook) {
        Iterator<Row> rowIterator = workbook.getSheetAt(0).iterator();

        while (rowIterator.hasNext()) {
            Iterator<Cell> colIterator = rowIterator.next().iterator();
            resolveRowVs(colIterator);
        }

        return valuesetBundle();
    }

    // command line use
    public static Bundle convertVs(String workbookPath) {
        return convertVs(getWorkbook(workbookPath));
    }

    // command line use
    public static Workbook getWorkbook(String workbookPath) {
        Workbook workbook = null;
        try {
            FileInputStream spreadsheetStream = new FileInputStream(new File(workbookPath));
            workbook = new XSSFWorkbook(spreadsheetStream);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return workbook;
    }

    // TODO - this needs work
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Path to excel file is required");
            return;
        }

        Bundle temp = convertVs(args[0]);
        FhirContext context = FhirContext.forDstu3();
        for (Bundle.BundleEntryComponent component : temp.getEntry()) {
            File f = new File("src/main/resources/valuesets/" + component.getResource().getId() + ".json");
            if (f.createNewFile()) {
                PrintWriter writer = new PrintWriter(f);
                writer.println(context.newJsonParser().setPrettyPrint(true).encodeResourceToString(component.getResource()));
                writer.println();
                writer.close();
            }
        }
    }
}
