package org.opencds.cqf.cdshooks.providers;

public class DiscoveryDataProviderDstu2 extends DiscoveryDataProvider {

    @Override
    public String convertPathToSearchParam(String dataType, String codeOrDatePath) {
        switch (dataType) {
            case "Condition":
                if (codeOrDatePath.equals("bodySite")) return "body-site";
                else if (codeOrDatePath.equals("clinicalStatus")) return "clinical-status";
                else if (codeOrDatePath.equals("dateRecorded")) return "date-recorded";
                else if (codeOrDatePath.contains("evidence")) return "evidence";
                else if (codeOrDatePath.equals("onsetDateTime")) return "onset";
                else if (codeOrDatePath.equals("onsetPeriod")) return "onset";
                else if (codeOrDatePath.contains("onset")) return "onset-info";
                else if (codeOrDatePath.contains("stage")) return "stage";
                break;
            case "DiagnosticOrder":
                if (codeOrDatePath.contains("actor")) return "actor";
                else if (codeOrDatePath.contains("bodySite")) return "bodysite";
                else if (codeOrDatePath.contains("code")) return "code";
                else if (codeOrDatePath.contains("item") && codeOrDatePath.contains("dateTime")) return "item-date";
                else if (codeOrDatePath.contains(("dateTime"))) return "event-date";
                else if (codeOrDatePath.contains("item") && codeOrDatePath.contains("event") && codeOrDatePath.contains("status")) return "item-past-status";
                else if (codeOrDatePath.contains("item") && codeOrDatePath.contains("status")) return "item-status";
                else if (codeOrDatePath.contains(("status"))) return "event-status";
                else if (codeOrDatePath.contains(("specimen"))) return "specimen";
            case "MedicationOrder":
                if (codeOrDatePath.equals("medicationCodeableConcept")) return "code";
                else if (codeOrDatePath.equals("medicationReference")) return "medication";
                else if (codeOrDatePath.contains("dateWritten")) return "datewritten";
                break;
            case "NutritionOrder":
                if (codeOrDatePath.contains("additiveType")) return "additive";
                else if (codeOrDatePath.equals("dateTime")) return "datetime";
                else if (codeOrDatePath.contains("baseFormulaType")) return "formula";
                else if (codeOrDatePath.contains("oralDiet")) return "oraldiet";
                else if (codeOrDatePath.equals("orderer")) return "provider";
                else if (codeOrDatePath.contains("supplement")) return "supplement";
                break;
            case "VisionPrescription":
                if (codeOrDatePath.equals("dateWritten")) return "datewritten";
                break;
            default:
                if (codeOrDatePath.startsWith("effective")) return "date";
                else if (codeOrDatePath.equals("period")) return "date";
                else if (codeOrDatePath.equals("vaccineCode")) return "vaccine-code";
                break;
        }
        return codeOrDatePath.replace('.', '-').toLowerCase();
    }
}
