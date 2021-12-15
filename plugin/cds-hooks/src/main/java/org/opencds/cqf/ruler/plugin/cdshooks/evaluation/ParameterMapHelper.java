package org.opencds.cqf.ruler.plugin.cdshooks.evaluation;

import org.opencds.cqf.ruler.plugin.cdshooks.hooks.Hook;

import java.util.*;

public class ParameterMapHelper {
    private final int URI_MAX_LENGTH = 8192;

    private boolean isCompoundSearch;
    private String compoundParam;
    private String url;
    private Hook hook;
    private Map<String, List<String>> parameterMap;

    public ParameterMapHelper(String url, Hook hook) {
        this.isCompoundSearch = false;
        this.compoundParam = "";
        this.url = url;
        this.hook = hook;
        this.parameterMap = new HashMap<>();
    }

    public boolean isCompoundSearch() {
        return isCompoundSearch;
    }

    public String getCompoundParam() {
        return compoundParam;
    }

    public Map<String, List<String>> getParameterMap() {
        String cleanUrl = getCleanUrl();
        String[] temp = cleanUrl.split("\\?");

        if (temp.length > 1) {
            temp = temp[1].split("&"); // split on each param
            for (String t : temp) {
                String[] tArr = t.split("="); // split to key value pair
                if (tArr.length == 2) { // make sure there is a key and a value
                    if (tArr[1].length() > URI_MAX_LENGTH) {
                        String s = "";
                        String[] sArr = tArr[1].split(","); // split on each argument
                        for (String ss : sArr) {
                            String tmp = s.isEmpty() ? ss : s + "," + ss;
                            if (tmp.length() < URI_MAX_LENGTH) {
                                s = tmp;
                            }
                            else if (parameterMap.containsKey(tArr[0])) {
                                parameterMap.get(tArr[0]).add(s);
                                s = ss;
                            }
                            else {
                                parameterMap.put(tArr[0], new ArrayList<>(Collections.singletonList(s)));
                                isCompoundSearch = true;
                                compoundParam = tArr[0];
                                s = ss;
                            }
                        }
                    }
                    if (!isCompoundSearch) {
                        parameterMap.put(tArr[0], Collections.singletonList(tArr[1]));
                    }
                }
            }
        }
        return parameterMap;
    }

    public Map<String, List<String>> getParameterMapCompundIncludeIndex(int idx) {
        Map<String, List<String>> ret = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : parameterMap.entrySet()) {
            ret.put(
                    entry.getKey(),
                    entry.getKey().equals(compoundParam)
                            ? Collections.singletonList(entry.getValue().get(idx))
                            : entry.getValue()
            );
        }

        return ret;
    }

    private String getCleanUrl() {
        return url.replaceAll("\\{\\{context.patientId}}", hook.getRequest().getContext().getPatientId())
                .replaceAll("\\{\\{context.encounterId}}", hook.getRequest().getContext().getEncounterId())
                .replaceAll("\\{\\{context.user}}", hook.getRequest().getUser())
                .replaceAll("\\{\\{user}}", hook.getRequest().getUser());
    }
}
