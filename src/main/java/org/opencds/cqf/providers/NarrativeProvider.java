package org.opencds.cqf.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.CustomThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative.INarrativeGenerator;
import ca.uhn.fhir.parser.IParser;

import org.cqframework.cql.tools.formatter.CqlFormatterVisitor;
import org.cqframework.cql.tools.formatter.CqlFormatterVisitor.FormatResult;
import org.antlr.v4.parse.ANTLRParser.exceptionGroup_return;
import org.apache.lucene.facet.FacetResult;
import org.hl7.elm.r1.Library;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Base64BinaryType;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Narrative.NarrativeStatus;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Created by Christopher on 2/4/2017.
 */
public class NarrativeProvider {

    private INarrativeGenerator generator;

    public NarrativeProvider() {
        this(NarrativeProvider.class.getClassLoader().getResource("narratives/narrative.properties").toString()); 
    }

    public NarrativeProvider(String pathToPropertiesFile)
    {
        this.generator = new CustomThymeleafNarrativeGenerator(pathToPropertiesFile);
    }

    public void generateNarrative(FhirContext context, DomainResource resource) {
        // Remove the old generated narrative.
        resource.setText(null);

        if (resource instanceof org.hl7.fhir.dstu3.model.Library) {
            formatCql((org.hl7.fhir.dstu3.model.Library)resource);
        }
        Narrative narrative = new Narrative();

        this.generator.generateNarrative(context, resource, narrative);

        resource.setText(narrative);
    }

    private void formatCql(org.hl7.fhir.dstu3.model.Library library) {
        for (Attachment att : library.getContent()) {
            if (att.getContentType().equals("text/cql")) {
                try {
                    FormatResult fr = CqlFormatterVisitor.getFormattedOutput(
                            new ByteArrayInputStream(Base64.getDecoder().decode(att.getDataElement().getValueAsString())));

                    // Only update the content if it's valid CQL.
                    if (fr.getErrors().size() == 0) {
                        Base64BinaryType bt = new Base64BinaryType(new String(Base64.getEncoder().encode(fr.getOutput().getBytes())));
                        att.setDataElement(bt);
                    }
                }
                catch(IOException e) {
                    // Intentionally empty for now
                }
            }
        }   
    }

    // args[0] == relative path to json resource -> i.e. library/library-demo.json
    // args[1] == path to narrative output -> i.e. library-demo-narrative.html (optional)
    // args[2] == path to resource output -> i.e. library-demo.json(optional)
    // args[3] == path to narrative.properties file -> i.e narrative.properties
    // (optional)

    public static void main(String[] args) {

        try {
            Path pathToResource = Paths.get(NarrativeProvider.class.getClassLoader()
            .getResource("narratives/examples/library/library-demo.json").toURI());
            Path pathToNarrativeOutput = Paths.get("src/main/resources/narratives/library-demo-narrative.html").toAbsolutePath();
            Path pathToResourceOutput = Paths.get("src/main/resources/narratives/library-demo.json").toAbsolutePath();
            Path pathToProp = Paths.get(
                    NarrativeProvider.class.getClassLoader().getResource("narratives/narrative.properties").toURI());

            if (args.length >= 4) {
                pathToNarrativeOutput = Paths.get(new URI(args[2]));
            }

            if (args.length >= 4) {
                pathToResourceOutput = Paths.get(new URI(args[2]));
            }

            if (args.length >= 2) {
                pathToProp = Paths.get(new URI(args[1]));
            }

            if (args.length >= 1) {
                pathToResource = Paths.get(new URI(args[0]));
            }

            FhirContext context = FhirContext.forDstu3();

            // examples are here: src/main/resources/narratives/example//
            IParser parser = pathToResource.toString().endsWith("json") ? context.newJsonParser() : context.newXmlParser();
            DomainResource resource = (DomainResource) parser.parseResource(new FileReader(pathToResource.toFile()));

            NarrativeProvider provider = new NarrativeProvider(pathToProp.toUri().toString());

            provider.generateNarrative(context, resource);

            PrintWriter writer = new PrintWriter(new File(pathToNarrativeOutput.toString()), "UTF-8");
            writer.println(resource.getText().getDivAsString());
            writer.println();
            writer.close();

            writer = new PrintWriter(new File(pathToResourceOutput.toString()), "UTF-8");
            writer.println(parser.setPrettyPrint(true).encodeResourceToString(resource));
            writer.println();
            writer.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return;
        }
    }
}
