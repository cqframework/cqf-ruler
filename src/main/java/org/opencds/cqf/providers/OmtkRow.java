package org.opencds.cqf.providers;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Christopher Schuler on 4/29/2017.
 */
public class OmtkRow {

    private Map<String, Object> data = new HashMap<String, Object>();

    public Object getValue(String key) {
        return data.get(key);
    }

    public void setValue(String key, Object value) {
        data.put(key, value);
    }

}
