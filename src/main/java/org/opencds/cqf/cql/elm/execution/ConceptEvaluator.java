package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.Code;

import java.util.ArrayList;

/*
structured type Concept
{
  codes List<Code>,
  display String
}

The Concept type represents a single terminological concept within CQL.
*/

/**
* Created by Chris Schuler on 6/7/2016
*/
public class ConceptEvaluator extends org.cqframework.cql.elm.execution.Concept {

  @Override
  public Object evaluate(Context context) {
    ArrayList<Code> codes = new ArrayList<>();
    for (int i = 0; i < this.getCode().size(); ++i) {
      codes.add((Code)this.getCode().get(i).evaluate(context));
    }
    String display = this.getDisplay();
    return new org.opencds.cqf.cql.runtime.Concept().withCodes(codes).withDisplay(display);
  }
}
