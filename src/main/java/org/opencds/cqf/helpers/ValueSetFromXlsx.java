package org.opencds.cqf.helpers;

import ca.uhn.fhir.context.FhirContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Christopher on 4/11/2017.
 */
public class ValueSetFromXlsx {

    private static Map<String, ValueSet> valuesets;

    private static void populateOid(String oid) throws FHIRException {
        Enumeration<Enumerations.PublicationStatus> status =
                new Enumerations.PublicationStatusEnumFactory().fromType(new StringType("draft"));
        ValueSet temp = new ValueSet(status);
        temp.setId(oid);
        ValueSet.ValueSetExpansionComponent expansion = new ValueSet.ValueSetExpansionComponent();
        expansion.setId("urn:oid:" + oid);
        valuesets.put(oid, temp.setExpansion(expansion));
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
    //   oid ... name ... title ... system ... version ... code ... display
    private static void resolveRow(Iterator<Cell> colIterator) throws FHIRException {

        String oid = getNextCellString(colIterator);

        if (!valuesets.containsKey(oid)) {
            populateOid(oid);
        }

        colIterator.next(); // skipping name
        colIterator.next(); // skipping title

        String system = getNextCellString(colIterator);
        String version = getNextCellString(colIterator);
        String code = getNextCellString(colIterator);
        String display = getNextCellString(colIterator);

        ValueSet.ValueSetExpansionComponent expansion = valuesets.get(oid).getExpansion(); //.getContainsFirstRep();
        ValueSet.ValueSetExpansionContainsComponent contains = new ValueSet.ValueSetExpansionContainsComponent();
        contains.setSystem(system);
        contains.setVersion(version);
        contains.setCode(code);
        contains.setDisplay(display);
        expansion.addContains(contains);
    }

    public static Bundle valuesetBundle() {

        Bundle temp = new Bundle();
        for (String key : valuesets.keySet())
                temp.addEntry(new Bundle.BundleEntryComponent().setResource(valuesets.get(key)));

        return temp;
    }

    // library function use
    public static Bundle convert(Workbook workbook) {
        Iterator<Row> rowIterator = workbook.getSheetAt(0).iterator();

        while (rowIterator.hasNext()) {
            Iterator<Cell> colIterator = rowIterator.next().iterator();
            try {
                resolveRow(colIterator);
            } catch (FHIRException e) {
                e.printStackTrace();
            }
        }

        return valuesetBundle();
    }

    // command line use
    public static Bundle convert(String workbookPath) {
        valuesets = new HashMap<>();
        Workbook workbook = null;
        try {
            FileInputStream spreadsheetStream = new FileInputStream(new File(workbookPath));
            workbook = new XSSFWorkbook(spreadsheetStream);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return convert(workbook);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Path to excel file is required");
            return;
        }

        Bundle temp = convert(args[0]);
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
