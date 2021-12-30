package org.opencds.cqf.ruler.api;

import org.hl7.fhir.instance.model.api.IBaseConformance;

/**
 * Interface for plugins to modify the conformance/capability statement of the
 * server
 */
public interface MetadataExtender<T extends IBaseConformance> {
	void extend(T metadata);
}
