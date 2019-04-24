package org.opencds.cqf.providers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.hl7.fhir.dstu3.model.Measure;
import org.xml.sax.SAXException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class HQMFProvider {

    public String generateHQMF(Measure m) {
        return "<xml></xml>";
    }


    private boolean validateHQMF(String xml) {
        try {
            return this.validateXML(this.loadHQMFSchema(), xml);
        }
        catch (SAXException e) {
            return false;
        }
    }

    private boolean validateXML(Schema schema, String xml){    
        try {
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (IOException | SAXException e) {
            System.out.println("Exception: " + e.getMessage());
            return false;
        }
        return true;
    }

    private Schema loadHQMFSchema() throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL hqmfSchema = ClassLoader.getSystemClassLoader().getResource("hqmf/schemas/EMeasure_N1.xsd");
        return factory.newSchema(hqmfSchema);
    }

    // args[0] == relative path to json resource -> i.e. library/library-demo.json
    // args[1] == path to resource output -> i.e. library-demo.json(optional)
    public static void main(String[] args) {

        try {
            Path pathToResource = Paths.get(HQMFProvider.class.getClassLoader().getResource("narratives/examples/measure/measure-demo.json").toURI());
            Path pathToResourceOutput = Paths.get("src/main/resources/narratives/hqmf.xml").toAbsolutePath();

            if (args.length >= 2) {
                pathToResourceOutput = Paths.get(new URI(args[1]));
            }

            if (args.length >= 1) {
                pathToResource = Paths.get(new URI(args[0]));
            }

            HQMFProvider provider = new HQMFProvider();

            FhirContext context = FhirContext.forDstu3();

            IParser parser = pathToResource.toString().endsWith("json") ? context.newJsonParser() : context.newXmlParser();
            Measure resource = (Measure) parser.parseResource(new FileReader(pathToResource.toFile()));
            
            String result = provider.generateHQMF(resource);

            PrintWriter writer = new PrintWriter(new File(pathToResourceOutput.toString()), "UTF-8");
            writer.println(result);
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
