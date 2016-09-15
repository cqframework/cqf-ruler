package org.opencds.cqf.cql.terminology;

/**
 * Created by Bryn on 8/2/2016.
 */
public class CodeSystemInfo {
    private String id;
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public CodeSystemInfo withId(String id) {
        this.setId(id);
        return this;
    }

    private String version;
    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }
    public CodeSystemInfo withVersion(String version) {
        this.setVersion(version);
        return this;
    }
}
