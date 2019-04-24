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
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class HQMFProvider {

    private TemplateEngine templateEngine;

    public HQMFProvider() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setTemplateMode(TemplateMode.XML);
        resolver.setPrefix("hqmf/templates/");
        resolver.setSuffix(".xml");

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    public String generateHQMF(Measure m) {

        Context c = new Context();
        c.setVariable("m", m);
        return this.templateEngine.process("measure", c);
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
            Path pathToResource = Paths.get(HQMFProvider.class.getClassLoader().getResource("hqmf/examples/input/measure-cms124-QDM.json").toURI());
            Path pathToResourceOutput = Paths.get("src/main/resources/hqmf/hqmf.xml").toAbsolutePath();

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
