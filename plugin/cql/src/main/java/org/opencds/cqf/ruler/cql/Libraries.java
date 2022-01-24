package org.opencds.cqf.ruler.cql;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.ruler.common.utility.Reflections;

import ca.uhn.fhir.context.FhirContext;

public interface Libraries {

	default byte[] getContent(IBaseResource library, ContentFunctions contentFunctions, String contentType) {
		Objects.requireNonNull(library, "library can not be null");
		Objects.requireNonNull(contentFunctions, "contentFunctions can not be null");
		if (!library.fhirType().equals("Library")) {
			throw new IllegalArgumentException("the library parameter is not a FHIR Library Resource");
		}

		for (IBase attachment : contentFunctions.getAttachments().apply(library)) {
			String libraryContentType = contentFunctions.getContentType().apply(attachment);
			if (libraryContentType != null && libraryContentType.equals(contentType)) {
				byte[] content = contentFunctions.getContent().apply(attachment);
				if (content != null) {
					return content;
				}
			}
		}

		return null;
	}

	default byte[] getContent(IBaseResource library, String contentType) {
		ContentFunctions contentFunctions = this.getContentFunctions(library);
		return this.getContent(library, contentFunctions, contentType);
	}

	default ContentFunctions getContentFunctions(IBaseResource library) {
		return this.getContentFunctions(FhirContext.forCached(library.getStructureFhirVersionEnum()));
	}

	default ContentFunctions getContentFunctions(FhirContext fhirContext) {
		Function<IBase, List<IBase>> attachments = Reflections.getFunction(fhirContext.getResourceDefinition("Library").getImplementingClass(), "content");
		Function<IBase, String> contentType = Reflections.getPrimitiveFunction(
				fhirContext.getElementDefinition("Attachment").getImplementingClass(), "contentType");
		Function<IBase, byte[]> content = Reflections.getPrimitiveFunction(fhirContext.getElementDefinition("Attachment").getImplementingClass(), "data");
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
