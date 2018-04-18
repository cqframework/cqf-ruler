package org.opencds.cqf.cdshooks.providers;

public class DiscoveryDataProviderStu3 extends DiscoveryDataProvider {

    @Override
    public String convertPathToSearchParam(String dataType, String codeOrDatePath) {
        switch (dataType) {
            case "AllergyIntolerance":
                if (codeOrDatePath.equals("clinicalStatus")) return "clinical-status";
                else if (codeOrDatePath.contains("substance")) return "code";
                else if (codeOrDatePath.equals("assertedDate")) return "date";
                else if (codeOrDatePath.equals("lastOccurrence")) return "last-date";
                else if (codeOrDatePath.startsWith("reaction")) {
                    if (codeOrDatePath.endsWith("manifestation")) return "manifestation";
                    else if (codeOrDatePath.endsWith("onset")) return "onset";
                    else if (codeOrDatePath.endsWith("exposureRoute")) return "route";
                    else if (codeOrDatePath.endsWith("severity")) return "severity";
                }
                else if (codeOrDatePath.equals("verificationStatus")) return "verification-status";
                break;
            case "Claim":
                if (codeOrDatePath.contains("careTeam")) return "care-team";
                else if (codeOrDatePath.contains("payee")) return "payee";
                break;
            case "Condition":
                if (codeOrDatePath.equals("abatementDateTime")) return "abatement-date";
                else if (codeOrDatePath.equals("abatementPeriod")) return "abatement-date";
                else if (codeOrDatePath.equals("abatementRange")) return "abatement-age";
                else if (codeOrDatePath.equals("onsetDateTime")) return "onset-date";
                else if (codeOrDatePath.equals("onsetPeriod")) return "onset-date";
                else if (codeOrDatePath.equals("onsetRange")) return "onset-age";
                break;
            case "MedicationRequest":
                if (codeOrDatePath.equals("authoredOn")) return "authoredon";
                else if (codeOrDatePath.equals("medicationCodeableConcept")) return "code";
                else if (codeOrDatePath.equals("medicationReference")) return "medication";
                else if (codeOrDatePath.contains("event")) return "date";
                else if (codeOrDatePath.contains("performer")) return "intended-dispenser";
                else if (codeOrDatePath.contains("requester")) return "requester";
                break;
            case "NutritionOrder":
                if (codeOrDatePath.contains("additiveType")) return "additive";
                else if (codeOrDatePath.equals("dateTime")) return "datetime";
                else if (codeOrDatePath.contains("baseFormulaType")) return "formula";
                else if (codeOrDatePath.contains("oralDiet")) return "oraldiet";
                else if (codeOrDatePath.equals("orderer")) return "provider";
                else if (codeOrDatePath.contains("supplement")) return "supplement";
                break;
            case "ProcedureRequest":
                if (codeOrDatePath.equals("authoredOn")) return "authored";
                else if (codeOrDatePath.equals("basedOn")) return "based-on";
                else if (codeOrDatePath.equals("bodySite")) return "body-site";
                else if (codeOrDatePath.equals("context")) return "encounter";
                else if (codeOrDatePath.equals("performerType")) return "performer-type";
                else if (codeOrDatePath.contains("requester")) return "requester";
                break;
            case "ReferralRequest":
                if (codeOrDatePath.equals("authoredOn")) return "authored";
                else if (codeOrDatePath.equals("basedOn")) return "based-on";
                else if (codeOrDatePath.equals("context")) return "encounter";
                else if (codeOrDatePath.equals("groupIdentifier")) return "group-identifier";
                else if (codeOrDatePath.equals("occurrence")) return "occurrence-date";
                else if (codeOrDatePath.contains("requester")) return "requester";
                else if (codeOrDatePath.equals("serviceRequested")) return "service";
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
