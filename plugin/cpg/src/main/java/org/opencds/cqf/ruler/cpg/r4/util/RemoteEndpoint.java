package org.opencds.cqf.ruler.cpg.r4.util;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.opencds.cqf.ruler.cpg.EndpointInfo;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RemoteEndpoint {

    public static EndpointInfo resolveRemoteEndpoint(Endpoint endpoint) {
        if (endpoint == null || !endpoint.hasAddress()) return null;
        return new EndpointInfo(endpoint.getAddress(), getHeaders(endpoint));
    }

    public static List<String> getHeaders(Endpoint endpoint) {
        // NOTE: the content-type header is used when no headers are present
        // cql-evaluator operations require non-null header information
        return endpoint.hasHeader()
                ? endpoint.getHeader().stream().map(PrimitiveType::getValue).collect(Collectors.toList())
                : Collections.singletonList("Content-Type: application/json");
    }

}
