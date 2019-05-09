package org.opencds.cqf.providers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.tools.formatter.CqlFormatterVisitor;
import org.cqframework.cql.tools.formatter.CqlFormatterVisitor.FormatResult;
import org.hl7.elm.r1.CodeDef;
import org.hl7.elm.r1.IncludeDef;
import org.hl7.elm.r1.UsingDef;
import org.hl7.elm.r1.ValueSetDef;
import org.hl7.elm.r1.ValueSetRef;
import org.hl7.elm.r1.Library.Codes;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Base64BinaryType;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Type;
import org.opencds.cqf.config.STU3LibrarySourceProvider;
import org.opencds.cqf.helpers.LibraryHelper;
import org.opencds.cqf.helpers.LibraryResourceHelper;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class NarrativeLibraryResourceProvider extends LibraryResourceProvider {

    private NarrativeProvider narrativeProvider;

    public NarrativeLibraryResourceProvider(NarrativeProvider narrativeProvider) {
        this.narrativeProvider = narrativeProvider;
    }

    private ModelManager modelManager;

    private ModelManager getModelManager() {
        if (modelManager == null) {
            modelManager = new ModelManager();
        }
        return modelManager;
    }

    private LibraryManager libraryManager;

    private LibraryManager getLibraryManager() {
        if (libraryManager == null) {
            libraryManager = new LibraryManager(getModelManager());
            libraryManager.getLibrarySourceLoader().clearProviders();
            libraryManager.getLibrarySourceLoader().registerProvider(getLibrarySourceProvider());
        }
        return libraryManager;
    }

    private STU3LibrarySourceProvider librarySourceProvider;

    private STU3LibrarySourceProvider getLibrarySourceProvider() {
        if (librarySourceProvider == null) {
            librarySourceProvider = new STU3LibrarySourceProvider(getLibraryResourceProvider());
        }
        return librarySourceProvider;
    }

    private LibraryResourceProvider getLibraryResourceProvider() {
        return this;
    }

    @Operation(name = "$refresh-generated-content")
    public MethodOutcome refreshGeneratedContent(HttpServletRequest theRequest, RequestDetails theRequestDetails,
            @IdParam IdType theId) {
        Library theResource = this.getDao().read(theId);
        this.formatCql(theResource);

        CqlTranslator translator = this.getTranslator(theResource);
        if (translator != null) {
            this.ensureElm(theResource, translator);
            this.ensureRelatedArtifacts(theResource, translator);
            this.ensureDataRequirements(theResource, translator);
        }

        Narrative n = this.narrativeProvider.getNarrative(this.getContext(), theResource);
        theResource.setText(n);

        return super.update(theRequest, theResource, theId,
                theRequestDetails.getConditionalUrl(RestOperationTypeEnum.UPDATE), theRequestDetails);
    }

    @Operation(name = "$get-elm", idempotent = true)
    public Parameters getElm(@IdParam IdType theId, @OptionalParam(name="format") String format) {
        Library theResource = this.getDao().read(theId);
        this.formatCql(theResource);

        String elm = "";
        CqlTranslator translator = this.getTranslator(theResource);
        if (translator != null) {
            if (format.equals("json")) {
                elm = translator.toJson();
            }
            else {
                elm = translator.toXml();
            }
        }
        Parameters p = new Parameters();
        p.addParameter().setValue(new StringType(elm));
        return p;
    }

    @Operation(name = "$get-narrative", idempotent = true)
    public Parameters getNarrative(@IdParam IdType theId) {
        Library theResource = this.getDao().read(theId);
        Narrative n = this.narrativeProvider.getNarrative(this.getContext(), theResource);
        Parameters p = new Parameters();
        p.addParameter().setValue(new StringType(n.getDivAsString()));
        return p;
    }

    private CqlTranslator getTranslator(Library library) {
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

        CqlTranslator translator = LibraryHelper.getTranslator(
                new ByteArrayInputStream(Base64.getDecoder().decode(cql.getDataElement().getValueAsString())),
                this.getLibraryManager(), this.getModelManager());

        return translator;
    }

    private void formatCql(Library library) {
        for (Attachment att : library.getContent()) {
            if (att.getContentType().equals("text/cql")) {
                try {
                    FormatResult fr = CqlFormatterVisitor.getFormattedOutput(new ByteArrayInputStream(
                            Base64.getDecoder().decode(att.getDataElement().getValueAsString())));

                    // Only update the content if it's valid CQL.
                    if (fr.getErrors().size() == 0) {
                        Base64BinaryType bt = new Base64BinaryType(
                                new String(Base64.getEncoder().encode(fr.getOutput().getBytes())));
                        att.setDataElement(bt);
                    }
                } catch (IOException e) {
                    // Intentionally empty for now
                }
            }
        }
    }

    private void ensureElm(Library library, CqlTranslator translator) {

        library.getContent().removeIf(a -> a.getContentType().equals("application/elm+xml"));
        String xml = translator.toXml();
        Attachment elm = new Attachment();
        elm.setContentType("application/elm+xml");
        elm.setData(xml.getBytes());
        library.getContent().add(elm);
    }

    private void ensureRelatedArtifacts(Library library, CqlTranslator translator) {
        library.getRelatedArtifact().clear();
        org.hl7.elm.r1.Library elm = translator.toELM();
        if (elm.getIncludes() != null && !elm.getIncludes().getDef().isEmpty()) {
            for (IncludeDef def : elm.getIncludes().getDef()) {
                library.addRelatedArtifact(new RelatedArtifact().setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                        .setResource(new Reference().setReference(
                                LibraryResourceHelper.resolveLibraryByName(this, def.getPath(), def.getVersion()).getId())));
            }
        }

        if (elm.getUsings() != null && !elm.getUsings().getDef().isEmpty()) {
            for (UsingDef def : elm.getUsings().getDef()) {
                String uri = def.getUri();
                String version = def.getVersion();
                if (version != null && !version.isEmpty()) {
                    uri = uri + "|" + version;
                }
                library.addRelatedArtifact(
                        new RelatedArtifact().setType(RelatedArtifact.RelatedArtifactType.DEPENDSON).setUrl(uri));
            }
        }
    }

    private void ensureDataRequirements(Library library, CqlTranslator translator) {
        library.getDataRequirement().clear();

        List<DataRequirement> reqs = new ArrayList<DataRequirement>();

        for (org.hl7.elm.r1.Retrieve retrieve : translator.toRetrieves()) {
            DataRequirement dataReq = new DataRequirement();
            dataReq.setType(retrieve.getDataType().getLocalPart());
            if (retrieve.getCodeProperty() != null) {
                DataRequirement.DataRequirementCodeFilterComponent codeFilter = new DataRequirement.DataRequirementCodeFilterComponent();
                codeFilter.setPath(retrieve.getCodeProperty());
                if (retrieve.getCodes() instanceof ValueSetRef) {
                    Type valueSetName = new StringType(
                            getValueSetId(((ValueSetRef) retrieve.getCodes()).getName(), translator));
                    codeFilter.setValueSet(valueSetName);
                }
                dataReq.setCodeFilter(Collections.singletonList(codeFilter));
            }
            // TODO - Date filters - we want to populate this with a $data-requirements
            // request as there isn't a good way through elm analysis
            reqs.add(dataReq);
        }


        // 
        // org.hl7.elm.r1.Library elm = translator.toELM();
        // Codes codes = elm.getCodes();
        // for (CodeDef cd : codes.getDef()) {
        //     cd.
        // }

        library.setDataRequirement(reqs);
        
    }

    private String getValueSetId(String valueSetName, CqlTranslator translator) {
        org.hl7.elm.r1.Library.ValueSets valueSets = translator.toELM().getValueSets();
        if (valueSets != null) {
            for (ValueSetDef def : valueSets.getDef()) {
                if (def.getName().equals(valueSetName)) {
                    return def.getId();
                }
            }
        }

        return valueSetName;
    }
}