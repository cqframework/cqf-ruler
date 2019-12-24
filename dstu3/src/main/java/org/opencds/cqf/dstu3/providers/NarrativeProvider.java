package org.opencds.cqf.dstu3.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.INarrativeGenerator;
import ca.uhn.fhir.narrative2.ThymeleafNarrativeGenerator;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.common.narrative.JarEnabledCustomThymeleafNarrativeGenerator;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Christopher on 2/4/2017.
 */
public class NarrativeProvider {

    private INarrativeGenerator generator;

    public NarrativeProvider() {
        this(Thread.currentThread().getContextClassLoader().getResource("narratives/narrative.properties").toString()); 
    }

    public NarrativeProvider(String pathToPropertiesFile)
    {
        ThymeleafNarrativeGenerator myGenerator = new JarEnabledCustomThymeleafNarrativeGenerator("classpath:ca/uhn/fhir/narrative/narratives.properties", pathToPropertiesFile);
        this.generator = myGenerator;
    }

    public Narrative getNarrative(FhirContext context, IBaseResource resource) {
//        Narrative narrative = new Narrative();
//        this.generator.generateNarrative(context, resource, narrative);
        this.generator.populateResourceNarrative(context, resource);
        return ((DomainResource) resource).getText();
    }

    // args[0] == relative path to json resource -> i.e. library/library-demo.json
    // args[1] == path to narrative output -> i.e. library-demo-narrative.html (optional)
    // args[2] == path to resource output -> i.e. library-demo.json(optional)
    // args[3] == path to narrative.properties file -> i.e narrative.properties (optional)
    public static void main(String[] args) {

        try {
            Path pathToResource = Paths.get(NarrativeProvider.class.getClassLoader().getResource("narratives/examples/measure/cms146.json").toURI());
            Path pathToNarrativeOutput = Paths.get("src/main/resources/narratives/output.html").toAbsolutePath();
            Path pathToResourceOutput = Paths.get("src/main/resources/narratives/output.json").toAbsolutePath();
            Path pathToProp = Paths.get(
                    NarrativeProvider.class.getClassLoader().getResource("narratives/narrative.properties").toURI());

            if (args.length >= 4) {
                pathToNarrativeOutput = Paths.get(new URI(args[3]));
            }

            if (args.length >= 3) {
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

            Narrative narrative = provider.getNarrative(context, resource);

            resource.setText(narrative);

            PrintWriter writer = new PrintWriter(new File(pathToNarrativeOutput.toString()), "UTF-8");
            writer.println(narrative.getDivAsString());
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
        }
    }
}
