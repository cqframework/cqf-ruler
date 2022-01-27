package org.opencds.cqf.ruler.cql;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Attachment;
import org.opencds.cqf.ruler.utility.ReflectionUtilities;

import ca.uhn.fhir.context.FhirContext;

public interface LibraryUtilities extends ReflectionUtilities {

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

	default InputStream extractContentStream(org.hl7.fhir.r4.model.Library library) {
		Attachment cql = null;
		for (Attachment a : library.getContent()) {
			if (a.getContentType().equals("text/cql")) {
				cql = a;
				break;
			}
		}

		if (cql == null) {
			return null;
		}
		return new ByteArrayInputStream(Base64.getDecoder().decode(cql.getDataElement().getValueAsString()));
	}



	interface ContentFunctions {
		Function<IBase, List<IBase>> getAttachments();

		Function<IBase, String> getContentType();

		Function<IBase, byte[]> getContent();
	}
}
