package org.opencds.cqf.ruler.plugin.dev;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hapi.fhir.dev")
public class DevToolsProperties {

    private Boolean enabled = true;

    public Boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    private CodeSystemUpdate codeSystemUpdate;

    public CodeSystemUpdate getCodeSystemUpdate() {
        return codeSystemUpdate;
    }

    public void setCodeSystemUpdate(CodeSystemUpdate codeSystemUpdate) {
        this.codeSystemUpdate = codeSystemUpdate;
    }

    public static class CodeSystemUpdate {

        private String endpoint;
        private String username;
        private String password;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
