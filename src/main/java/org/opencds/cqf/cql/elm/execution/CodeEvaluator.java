package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

/*
structured type Code
{
  code String,
  display String,
  system String,
  version String
}

The Code type represents single terminology codes within CQL.
*/

/**
 * Created by Bryn on 5/25/2016.
 */
public class CodeEvaluator extends org.cqframework.cql.elm.execution.Code {
    @Override
    public Object evaluate(Context context) {
        org.opencds.cqf.cql.runtime.Code code = new org.opencds.cqf.cql.runtime.Code().withCode(this.getCode()).withDisplay(this.getDisplay());
        org.cqframework.cql.elm.execution.CodeSystemRef codeSystemRef = this.getSystem();
        if (codeSystemRef != null) {
            boolean enteredLibrary = context.enterLibrary(codeSystemRef.getLibraryName());
            try {
                org.cqframework.cql.elm.execution.CodeSystemDef codeSystemDef = context.resolveCodeSystemRef(codeSystemRef.getName());
                code.setSystem(codeSystemDef.getId());
                code.setVersion(codeSystemDef.getVersion());
            }
            finally {
                context.exitLibrary(enteredLibrary);
            }
        }

        return code;
    }
}
