package org.opencds.cqf.cql.runtime;

import java.util.HashMap;

/**
* Created by Chris Schuler on 6/15/2016
*/
public class Tuple {

  protected HashMap<String, Object> elements;

  public HashMap<String, Object> getElements() {
    if (elements == null) { return new HashMap<String, Object>(); }
    return elements;
  }

  public void setElements(HashMap<String, Object> elements) {
    this.elements = elements;
  }

  public Tuple withElements(HashMap<String, Object> elements) {
    setElements(elements);
    return this;
  }
}
