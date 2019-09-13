package org.opencds.cqf.dstu3.helpers;

import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.primitive.CodeDt;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import org.hl7.fhir.dstu3.model.*;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DataProviderHelper {
    // Get lowest precision from list of date objects
    public static String getPrecision(List<Object> dateObjects) {
        int precision = 7;
        for (Object dateObject : dateObjects) {
            if (dateObject instanceof Interval) {
                DateTime start = (DateTime) ((Interval) dateObject).getStart();
                DateTime end = (DateTime) ((Interval) dateObject).getEnd();

                if (start != null) {
                    if (precision > (start.getPrecision().toDateTimeIndex() + 1)) {
                        precision = start.getPrecision().toDateTimeIndex() + 1;
                    }
                }
                if (end != null) {
                    if (precision > (end.getPrecision().toDateTimeIndex() + 1)) {
                        precision = end.getPrecision().toDateTimeIndex() + 1;
                    }
                }
            }
            else {
                if (dateObject == null) {
                    continue;
                }
                precision = ((DateTime) dateObject).getPrecision().toDateTimeIndex() + 1;
            }
        }

        switch (precision) {
            case 1: return "year";
            case 2: return "month";
            case 3: return "day";
            case 4: return "hour";
            case 5: return "minute";
            case 6: return "second";
            default: return "millisecond";
        }
    }

    public static Object getDstu2DateTime(Object dateObject) {
        if (dateObject instanceof DateDt) {
            return DateTime.fromJavaDate(((DateDt) dateObject).getValue());
        } else if (dateObject instanceof DateTimeDt) {
            return DateTime.fromJavaDate(((DateTimeDt) dateObject).getValue());
        } else if (dateObject instanceof InstantDt) {
            return DateTime.fromJavaDate(((InstantDt) dateObject).getValue());
        } else if (dateObject instanceof PeriodDt) {
            return new Interval(
                    DateTime.fromJavaDate(((PeriodDt) dateObject).getStart()), true,
                    DateTime.fromJavaDate(((PeriodDt) dateObject).getEnd()), true
            );
        }
        return dateObject;
    }

    public static Object getDstu2Code(Object codeObject) {
        if (codeObject instanceof CodeDt) {
            return ((CodeDt) codeObject).getValue();
        } else if (codeObject instanceof CodingDt) {
            return new Code().withSystem(((CodingDt) codeObject).getSystem()).withCode(((CodingDt) codeObject).getCode());
        }
        else if (codeObject instanceof CodeableConceptDt) {
            List<Code> codes = new ArrayList<>();
            for (CodingDt coding : ((CodeableConceptDt) codeObject).getCoding()) {
                codes.add((Code) getDstu2Code(coding));
            }
            return codes;
        }
        else if (codeObject instanceof Iterable) {
            List<Object> codes = new ArrayList<>();
            for (Object code : (Iterable) codeObject) {
                Object obj = getDstu2Code(code);
                if (obj instanceof Iterable) {
                    for (Object codeObj : (Iterable) obj) {
                        codes.add(codeObj);
                    }
                }
                else {
                    codes.add(obj);
                }
            }
            return codes;
        }
        return codeObject;
    }

    public static Object getStu3DateTime(Object dateObject) {
        if (dateObject instanceof Date) {
            return DateTime.fromJavaDate((Date) dateObject);
        } else if (dateObject instanceof DateTimeType) {
            return DateTime.fromJavaDate(((DateTimeType) dateObject).getValue());
        } else if (dateObject instanceof InstantType) {
            return DateTime.fromJavaDate(((InstantType) dateObject).getValue());
        } else if (dateObject instanceof Period) {
            return new Interval(
                    DateTime.fromJavaDate(((Period) dateObject).getStart()), true,
                    DateTime.fromJavaDate(((Period) dateObject).getEnd()), true
            );
        }
        return dateObject;
    }

    public static Object getStu3Code(Object codeObject) {
        if (codeObject instanceof CodeType) {
            return ((CodeType) codeObject).getValue();
        }
        else if (codeObject instanceof Coding) {
            return new Code().withSystem(((Coding) codeObject).getSystem()).withCode(((Coding) codeObject).getCode());
        }
        else if (codeObject instanceof CodeableConcept) {
            List<Code> codes = new ArrayList<>();
            for (Coding coding : ((CodeableConcept) codeObject).getCoding()) {
                codes.add((Code) getStu3Code(coding));
            }
            return codes;
        }
        else if (codeObject instanceof Iterable) {
            List<Object> codes = new ArrayList<>();
            for (Object code : (Iterable) codeObject) {
                Object obj = getStu3Code(code);
                if (obj instanceof Iterable) {
                    for (Object codeObj : (Iterable) obj) {
                        codes.add(codeObj);
                    }
                }
                else {
                    codes.add(obj);
                }
            }
            return codes;
        }
        return codeObject;
    }

    public static boolean checkCodeMembership(Iterable<Code> codes, Object codeObject) {
        // for now, just checking whether code values are equal... TODO - add intelligent checks for system and version
        for (Code code : codes) {
            if (codeObject instanceof String && code.getCode().equals(codeObject)) {
                return true;
            }
            else if (codeObject instanceof Code && code.getCode().equals(((Code) codeObject).getCode())) {
                return true;
            }
            else if (codeObject instanceof Iterable) {
                for (Object obj : (Iterable) codeObject) {
                    if (code.getCode().equals(((Code) obj).getCode())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static String convertStu3PathToSearchParam(String type, String path) {
        path = path.replace(".value", "");
        switch (type) {
            case "AllergyIntolerance":
                if (path.equals("clinicalStatus")) return "clinical-status";
                else if (path.contains("substance")) return "code";
                else if (path.equals("assertedDate")) return "date";
                else if (path.equals("lastOccurrence")) return "last-date";
                else if (path.startsWith("reaction")) {
                    if (path.endsWith("manifestation")) return "manifestation";
                    else if (path.endsWith("onset")) return "onset";
                    else if (path.endsWith("exposureRoute")) return "route";
                    else if (path.endsWith("severity")) return "severity";
                }
                else if (path.equals("verificationStatus")) return "verification-status";
                break;
            case "Claim":
                if (path.contains("careTeam")) return "care-team";
                else if (path.contains("payee")) return "payee";
                break;
            case "Condition":
                if (path.equals("abatementDateTime")) return "abatement-date";
                else if (path.equals("abatementPeriod")) return "abatement-date";
                else if (path.equals("abatementRange")) return "abatement-age";
                else if (path.equals("onsetDateTime")) return "onset-date";
                else if (path.equals("onsetPeriod")) return "onset-date";
                else if (path.equals("onsetRange")) return "onset-age";
                break;
            case "MedicationRequest":
                if (path.equals("authoredOn")) return "authoredon";
                else if (path.equals("medicationCodeableConcept")) return "code";
                else if (path.equals("medicationReference")) return "medication";
                else if (path.contains("event")) return "date";
                else if (path.contains("performer")) return "intended-dispenser";
                else if (path.contains("requester")) return "requester";
                break;
            case "NutritionOrder":
                if (path.contains("additiveType")) return "additive";
                else if (path.equals("dateTime")) return "datetime";
                else if (path.contains("baseFormulaType")) return "formula";
                else if (path.contains("oralDiet")) return "oraldiet";
                else if (path.equals("orderer")) return "provider";
                else if (path.contains("supplement")) return "supplement";
                break;
            case "ProcedureRequest":
                if (path.equals("authoredOn")) return "authored";
                else if (path.equals("basedOn")) return "based-on";
                else if (path.equals("bodySite")) return "body-site";
                else if (path.equals("context")) return "encounter";
                else if (path.equals("performerType")) return "performer-type";
                else if (path.contains("requester")) return "requester";
                break;
            case "ReferralRequest":
                if (path.equals("authoredOn")) return "authored";
                else if (path.equals("basedOn")) return "based-on";
                else if (path.equals("context")) return "encounter";
                else if (path.equals("groupIdentifier")) return "group-identifier";
                else if (path.equals("occurrence")) return "occurrence-date";
                else if (path.contains("requester")) return "requester";
                else if (path.equals("serviceRequested")) return "service";
                break;
            case "VisionPrescription":
                if (path.equals("dateWritten")) return "datewritten";
                break;
            default:
                if (path.startsWith("effective")) return "date";
                else if (path.equals("period")) return "date";
                else if (path.equals("vaccineCode")) return "vaccine-code";
                break;
        }
        return path.replace('.', '-').toLowerCase();
    }
}
