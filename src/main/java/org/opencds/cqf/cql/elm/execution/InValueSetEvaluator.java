package org.opencds.cqf.cql.elm.execution;

import org.cqframework.cql.elm.execution.ValueSetRef;
import org.cqframework.cql.elm.execution.ValueSetDef;
import org.cqframework.cql.elm.execution.CodeSystemRef;
import org.cqframework.cql.elm.execution.CodeSystemDef;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.Concept;
import org.opencds.cqf.cql.terminology.CodeSystemInfo;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.ValueSetInfo;
import java.util.List;
import java.util.ArrayList;

/*
in(code String, valueset ValueSetRef) Boolean
in(code Code, valueset ValueSetRef) Boolean
in(concept Concept, valueset ValueSetRef) Boolean

The in (Valueset) operators determine whether or not a given code is in a particular valueset.
  Note that these operators can only be invoked by referencing a defined valueset.
For the String overload, if the given valueset contains a code with an equivalent code element, the result is true.
For the Code overload, if the given valueset contains an equivalent code, the result is true.
For the Concept overload, if the given valueset contains a code equivalent to any code in the given concept, the result is true.
If the code argument is null, the result is null.
*/

/**
* Created by Chris Schuler on 7/13/2016
*/
public class InValueSetEvaluator extends org.cqframework.cql.elm.execution.InValueSet {

  public Object inValueSet(Context context, Object code, Object valueset) {
    if (code == null) { return null; }

    // Resolve ValueSetRef & CodeSystemRef -- Account for multiple codesystems represented within a valueset
    ValueSetDef vsd = resolveVSR(context, (ValueSetRef)valueset);
    List<CodeSystemDef> codeSystemDefs = new ArrayList<>();
    for (CodeSystemRef csr : vsd.getCodeSystem()) {
      codeSystemDefs.add(resolveCSR(context, csr));
    }

    List<CodeSystemInfo> codeSystemInfos = new ArrayList<>();
    if (codeSystemDefs.size() > 0) {
      for (CodeSystemDef csd : codeSystemDefs) {
        codeSystemInfos.add(new CodeSystemInfo().withId(csd.getId()).withVersion(csd.getVersion()));
      }
    }
    // TODO: find better solution than this -- temporary solution
    else {
      codeSystemInfos.add(new CodeSystemInfo().withId(null).withVersion(null));
    }

    List<ValueSetInfo> valueSetInfos = new ArrayList<>();
    for (CodeSystemInfo csi : codeSystemInfos) {
      valueSetInfos.add(new ValueSetInfo().withId(vsd.getId()).withVersion(vsd.getVersion()).withCodeSystem(csi));
    }

    TerminologyProvider provider = context.resolveTerminologyProvider();

    // perform operation
    if (code instanceof String) {
      for (ValueSetInfo vsi : valueSetInfos) {
        if (provider.in(new Code().withCode((String)code), vsi)) { return true; }
      }
      return false;
    }
    else if (code instanceof Code) {
      for (ValueSetInfo vsi : valueSetInfos) {
        if (provider.in((Code)code, vsi)) { return true; }
      }
      return false;
    }
    else if (code instanceof Concept) {
      for (ValueSetInfo vsi : valueSetInfos) {
        for (Code codes : ((Concept)code).getCodes()) {
          if (provider.in(codes, vsi)) { return true; }
        }
        return false;
      }
    }

    throw new IllegalArgumentException(String.format("Cannot InValueSet Code arguments of type '%s'.", code.getClass().getName()));
  }

  @Override
  public Object evaluate(Context context) {
    Object code = getCode().evaluate(context);
    Object valueset = getValueset();

    return inValueSet(context, code, valueset);
  }

  public ValueSetDef resolveVSR(Context context, ValueSetRef valueset) {
    return context.resolveValueSetRef(valueset.getLibraryName(), valueset.getName());
  }

  public CodeSystemDef resolveCSR(Context context, CodeSystemRef codesystem) {
    return context.resolveCodeSystemRef(codesystem.getLibraryName(), codesystem.getName());
  }
}
