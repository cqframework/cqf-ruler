package org.opencds.cqf.ruler.cpg.dstu3.util;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.PrimitiveType;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RemoteEndpoint {

    public static Pair<String, List<String>> resolveRemoteEndpoint(Endpoint endpoint) {
        if (endpoint == null || !endpoint.hasAddress()) return null;
        List<String> headers = getHeaders(endpoint);
        return Pair.of(endpoint.getAddress(),
                headers == null ? Collections.singletonList("Content-Type: application/json") : headers);
    }

    public static List<String> getHeaders(Endpoint endpoint) {
        return endpoint.hasHeader()
                ? endpoint.getHeader().stream().map(PrimitiveType::getValue).collect(Collectors.toList()) : null;
    }

}
