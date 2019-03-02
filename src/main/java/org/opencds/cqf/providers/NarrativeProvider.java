package org.opencds.cqf.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.CustomThymeleafNarrativeGenerator;
import ca.uhn.fhir.parser.IParser;

import org.antlr.v4.parse.ANTLRParser.exceptionGroup_return;
import org.hl7.elm.r1.Library;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Narrative.NarrativeStatus;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Christopher on 2/4/2017.
 */
public class NarrativeProvider {

    // args[0] == relative path to json resource -> i.e. measure/cms146.json
    // args[1] == path to output -> i.e. scratch.xml (optional)
    // args[2] == path to narrative.properties file -> i.e narrative.properties (optional)
    public static void main(String[] args) {

        try {
            Path pathToProp = Paths.get(NarrativeProvider.class.getClassLoader().getResource("narratives/narrative.properties").toURI());
            Path pathToOutput = Paths.get("src/main/resources/narratives/hobos.json").toAbsolutePath();
            Path pathToHTML = Paths.get("src/main/resources/narratives/hobos.html").toAbsolutePath();
            Path pathToResource = Paths.get(NarrativeProvider.class.getClassLoader().getResource("narratives/examples/library/library-cms130-FHIR.json").toURI());

            if (args.length >= 3) {
                pathToOutput = Paths.get(new URI(args[2]));
            }

            if (args.length >= 2) {
                pathToProp = Paths.get(new URI(args[1]));
            }

            if (args.length >= 1) {
                pathToResource = Paths.get(new URI(args[0]));
            }

            CustomThymeleafNarrativeGenerator gen = new CustomThymeleafNarrativeGenerator(pathToProp.toUri().toString());
            FhirContext ctx = FhirContext.forDstu3();


            // examples are here: src/main/resources/narratives/example//
            IParser parser = pathToResource.toString().endsWith("json") ? ctx.newJsonParser() : ctx.newXmlParser();
            DomainResource res = (DomainResource)parser.parseResource(new FileReader(pathToResource.toFile()));

            // Remove the old generated narrative.
            res.setText(null);

            Narrative nar = new Narrative();

            gen.generateNarrative(ctx, res, nar);

            res.setText(nar);

            String resource = parser.setPrettyPrint(true).encodeResourceToString(res);

            PrintWriter writer = new PrintWriter(new File(pathToOutput.toString()), "UTF-8");
            writer.println(resource);
            writer.println();
            writer.close();

            writer = new PrintWriter(new File(pathToHTML.toString()), "UTF-8");
            writer.println(nar.getDivAsString());
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
