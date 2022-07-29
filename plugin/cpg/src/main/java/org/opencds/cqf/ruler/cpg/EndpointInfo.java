package org.opencds.cqf.ruler.cpg;

import java.util.ArrayList;
import java.util.List;

public class EndpointInfo {

    private final String address;
    private final List<String> headers;

    public EndpointInfo(String address, List<String> headers) {
        this.address = address;
        this.headers = headers;
    }

    public String getAddress() {
        return address;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<HeaderInfo> getHeaderNameValuePairs() {
        List<HeaderInfo> headerNameValuePairs = new ArrayList<>();
        for (String header : headers) {
            // NOTE: assuming the headers will be key value pairs separated by a colon (key: value)
            String[] headerNameAndValue = header.split("\\s*:\\s*");
            if (headerNameAndValue.length == 2) {
                headerNameValuePairs.add(new HeaderInfo(headerNameAndValue[0], headerNameAndValue[1]));
            }
        }
        return headerNameValuePairs;
    }

    static class HeaderInfo {
        private final String name;
        private final String value;

        public HeaderInfo(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }
}
