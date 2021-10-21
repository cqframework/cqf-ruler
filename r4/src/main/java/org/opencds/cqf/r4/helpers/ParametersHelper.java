package org.opencds.cqf.r4.helpers;

import org.hl7.fhir.r4.model.Parameters;

import java.util.Iterator;

public class ParametersHelper {
    public static Parameters.ParametersParameterComponent getParameter(Parameters parameters, String name) {
        Iterator var2 = parameters.getParameter().iterator();

        Parameters.ParametersParameterComponent p;
        do {
            if (!var2.hasNext()) {
                return null;
            }

            p = (Parameters.ParametersParameterComponent)var2.next();
        } while(!p.getName().equals(name));

        return p;
    }
}