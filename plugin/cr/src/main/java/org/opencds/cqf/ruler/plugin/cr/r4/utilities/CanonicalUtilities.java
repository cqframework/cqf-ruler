package org.opencds.cqf.ruler.plugin.cr.r4.utilities;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;

public interface CanonicalUtilities {

    public default String getId(CanonicalType canonical) {
        if (canonical.hasValue()) {
            String id = canonical.getValue();
            String temp = id.contains("/") ? id.substring(id.lastIndexOf("/") + 1) : id;
            return temp.split("\\|")[0];
        }

        throw new RuntimeException("CanonicalType must have a value for id extraction");
    }

    public default IdType getIdType(CanonicalType canonical) {
        if (canonical.hasValue()) {
            String id = canonical.getValue();
            String temp = id.contains("/") ? id.substring(id.lastIndexOf("/") + 1) : id;
            return new IdType(temp.split("\\|")[0]);
        }

        throw new RuntimeException("CanonicalType must have a value for id extraction");
    }

    public default String getResourceName(CanonicalType canonical) {
        if (canonical.hasValue()) {
            String id = canonical.getValue();
            if (id.contains("/")) {
                id = id.replace(id.substring(id.lastIndexOf("/")), "");
                return id.contains("/") ? id.substring(id.lastIndexOf("/") + 1) : id;
            }
            return null;
        }

        throw new RuntimeException("CanonicalType must have a value for resource name extraction");
    }
}
