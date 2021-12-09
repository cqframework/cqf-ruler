package org.opencds.cqf.ruler.plugin.cql;

import java.util.List;
import java.util.function.Function;

import org.hl7.fhir.instance.model.api.IBase;
import org.opencds.cqf.ruler.plugin.utility.ReflectionUtilities;

import ca.uhn.fhir.context.FhirContext;

public interface LibraryUtilities extends ReflectionUtilities {

	public default ContentFunctions getContentFunctions(FhirContext fhirContext) {
		Function<IBase, List<IBase>> attachments = this
				.getFunction(fhirContext.getResourceDefinition("Library").getImplementingClass(), "content");
		Function<IBase, String> contentType = this.getPrimitiveFunction(
				fhirContext.getElementDefinition("Attachment").getImplementingClass(), "contentType");
		Function<IBase, byte[]> content = this
				.getPrimitiveFunction(fhirContext.getElementDefinition("Attachment").getImplementingClass(), "data");
		return new ContentFunctions() {

			@Override
			public Function<IBase, List<IBase>> getAttachments() {
				return attachments;
			}

			@Override
			public Function<IBase, String> getContentType() {
				return contentType;
			}

			@Override
			public Function<IBase, byte[]> getContent() {
				return content;
			}
		};
	}

	interface ContentFunctions {
		Function<IBase, List<IBase>> getAttachments();

		Function<IBase, String> getContentType();

		Function<IBase, byte[]> getContent();
	}
}
