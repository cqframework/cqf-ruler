package org.opencds.cqf.ruler.sdc;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hapi.fhir.sdc")
public class SDCProperties {

    private boolean enabled = true;

    public boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private Extract extract = new Extract();

    public Extract getExtract() {
        return extract;
    }

    public void setExtract(Extract extract) {
        this.extract = extract;
    }

    public static class Extract {

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

	 private Transform transform = new Transform();

	 public Transform getTransform() {
		 return transform;
	 }

	 public void setTransform(Transform newTransform) {
		 transform = newTransform;
	 }

	 public static class Transform {

		 private String replaceCode;
		 private String username;
		 private String password;
		 private String endpoint;

		 public String getReplaceCode() {
			 return replaceCode;
		 }

		 public void setReplaceCode(String newReplaceCode) {
			 replaceCode = newReplaceCode;
		 }

		 public String getUsername() {
			 return username;
		 }

		 public void setUsername(String newUsername) {
			 username = newUsername;
		 }

		 public String getPassword() {
			 return password;
		 }

		 public void setPassword(String newPassword) {
			 password = newPassword;
		 }

		 public String getEndpoint() {
			 return endpoint;
		 }

		 public void setEndpoint(String newEndpoint) {
			 endpoint = newEndpoint;
		 }
	 }
}
