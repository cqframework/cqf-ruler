package org.opencds.cqf.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.CustomThymeleafNarrativeGenerator;
import org.hl7.fhir.dstu3.model.PlanDefinition;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Christopher on 2/4/2017.
 */
public class NarrativeGenerator {

    public static void main(String[] args) {
        Path pathToResources = Paths.get("src/main/resources/narratives").toAbsolutePath();
        Path pathToProp = pathToResources.resolve("narrative.properties");
        String propFile = "file:" + pathToProp.toString();
        CustomThymeleafNarrativeGenerator gen = new CustomThymeleafNarrativeGenerator(propFile);
        FhirContext ctx = FhirContext.forDstu3();
        ctx.setNarrativeGenerator(gen);

        Path pathToResource = pathToResources.resolve("ZikaVirusIntervention.json");
        try {
            PlanDefinition planDefinition = (PlanDefinition) ctx.newJsonParser().parseResource(new FileReader(pathToResource.toFile()));
            // gen.generateNarrative(ctx, planDefinition, new Narrative());
            String resource = ctx.newJsonParser().encodeResourceToString(planDefinition);
            planDefinition = (PlanDefinition) ctx.newJsonParser().parseResource(resource);

            try {
                PrintWriter writer = new PrintWriter(new File(pathToResources.resolve("scratch.xml").toString()), "UTF-8");
                writer.println(planDefinition.getText().getDiv().getValue());
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
