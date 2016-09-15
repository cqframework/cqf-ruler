package org.opencds.cqf.cql.terminology;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bryn on 8/2/2016.
 */
public class ValueSetInfo {
    private String id;
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public ValueSetInfo withId(String id) {
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
    public ValueSetInfo withVersion(String version) {
        this.setVersion(version);
        return this;
    }

    private List<CodeSystemInfo> codeSystems;
    public List<CodeSystemInfo> getCodeSystems() {
        if (codeSystems == null) {
            codeSystems = new ArrayList<CodeSystemInfo>();
        }
        return codeSystems;
    }
    public ValueSetInfo withCodeSystem(CodeSystemInfo codeSystem) {
        getCodeSystems().add(codeSystem);
        return this;
    }
}
