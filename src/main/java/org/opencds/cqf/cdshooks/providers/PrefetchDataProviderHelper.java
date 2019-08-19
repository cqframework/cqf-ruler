package org.opencds.cqf.cdshooks.providers;

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

import java.util.*;

public class PrefetchDataProviderHelper {

    public static Map<String, List<Object>> populateMap(List<Object> resources) {
        Map<String, List<Object>> prefetchResources = new HashMap<>();
        for (Object resource : resources) {
            if (resource instanceof Resource) {
                if (prefetchResources.containsKey(((Resource) resource).fhirType())) {
                    prefetchResources.get(((Resource) resource).fhirType()).add(resource);
                }
                else {
                    List<Object> resourceList = new ArrayList<>();
                    resourceList.add(resource);
                    prefetchResources.put(((Resource) resource).fhirType(), resourceList);
                }
            }
            else if (resource instanceof ca.uhn.fhir.model.dstu2.resource.BaseResource) {
                if (prefetchResources.containsKey(((ca.uhn.fhir.model.dstu2.resource.BaseResource) resource).getResourceName())) {
                    prefetchResources.get(((ca.uhn.fhir.model.dstu2.resource.BaseResource) resource).getResourceName()).add(resource);
                }
                else {
                    List<Object> resourceList = new ArrayList<>();
                    resourceList.add(resource);
                    prefetchResources.put(((ca.uhn.fhir.model.dstu2.resource.BaseResource) resource).getResourceName(), resourceList);
                }
            }
        }
        return prefetchResources;
    }

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
            } else {
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
        } else if (codeObject instanceof Coding) {
            return new Code().withSystem(((Coding) codeObject).getSystem()).withCode(((Coding) codeObject).getCode());
        }
        else if (codeObject instanceof CodeableConcept) {
            List<Code> codes = new ArrayList<>();
            for (Coding coding : ((CodeableConcept) codeObject).getCoding()) {
                codes.add((Code) getStu3Code(coding));
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
}
