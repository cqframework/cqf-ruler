package org.opencds.cqf.ruler.plugin.cql;

import java.util.List;
import java.util.function.Function;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.ruler.plugin.utility.ReflectionUtilities;

import ca.uhn.fhir.context.FhirContext;

public interface LibraryUtilities extends ReflectionUtilities {

	public default ContentFunctions getContentFunctions(FhirContext fhirContext) {
		Function<IBaseResource, List<IBaseResource>> attachments = this
				.getFunction(fhirContext.getResourceDefinition("Library").getImplementingClass(), "attachment");
		Function<IBaseResource, String> contentType = this.getPrimitiveFunction(
				fhirContext.getResourceDefinition("Attachment").getImplementingClass(), "contentType");
		Function<IBaseResource, byte[]> content = this
				.getPrimitiveFunction(fhirContext.getResourceDefinition("Attachment").getImplementingClass(), "data");
		return new ContentFunctions() {

			@Override
			public Function<IBaseResource, List<IBaseResource>> getAttachments() {
				return attachments;
			}

			@Override
			public Function<IBaseResource, String> getContentType() {
				return contentType;
			}

			@Override
			public Function<IBaseResource, byte[]> getContent() {
				return content;
			}
		};
	}

	interface ContentFunctions {
		Function<IBaseResource, List<IBaseResource>> getAttachments();

		Function<IBaseResource, String> getContentType();

		Function<IBaseResource, byte[]> getContent();
	}
}
