package org.opencds.cqf.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.CustomThymeleafNarrativeGenerator;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Christopher on 2/4/2017.
 */
public class NarrativeGenerator {

    // args[0] == relative path to json resource -> i.e. measure/cms146.json
    public static void main(String[] args) {
        Path pathToResources = Paths.get("src/main/resources/narratives").toAbsolutePath();
        Path pathToProp = pathToResources.resolve("narrative.properties");
        String propFile = "file:" + pathToProp.toString();
        CustomThymeleafNarrativeGenerator gen = new CustomThymeleafNarrativeGenerator(propFile);
        FhirContext ctx = FhirContext.forDstu3();
        ctx.setNarrativeGenerator(gen);

        // examples are here: src/main/resources/narratives/examples
        if (args.length < 1) { throw new IllegalArgumentException("provide a file name..."); }
        Path pathToResource = pathToResources.resolve("examples/" + args[0]);

        try {
            IBaseResource res = ctx.newJsonParser().parseResource(new FileReader(pathToResource.toFile()));
            String resource = ctx.newJsonParser()
                    .encodeResourceToString(ctx.newJsonParser().parseResource(new FileReader(pathToResource.toFile())));

            try {
                PrintWriter writer = new PrintWriter(new File(pathToResources.resolve("scratch.xml").toString()), "UTF-8");
                writer.println(((DomainResource) ctx.newJsonParser().parseResource(resource)).getText().getDiv().getValue());
                writer.println();
                writer.close();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
