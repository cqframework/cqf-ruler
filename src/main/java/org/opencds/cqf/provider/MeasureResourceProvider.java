package org.opencds.cqf.provider;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.ValidationModeEnum;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

import org.cqframework.cql.cql2elm.DefaultLibrarySourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.elm.execution.Library;
import org.opencds.cqf.cql.execution.*;
import org.opencds.cqf.cql.data.fhir.FhirMeasureEvaluator;
import org.opencds.cqf.cql.data.fhir.FhirDataProvider;

import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.MeasureReport;

import org.testng.annotations.Test;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBException;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Date;

// for meaningful error reporting
import java.io.StringWriter;
import java.io.PrintWriter;

public class MeasureResourceProvider implements IResourceProvider {

  private Map<String, Measure> myMeasures = new HashMap<String, Measure>();

  private long myNextId = 1;

  // public MeasureResourceProvider() {
  //   String resourceId = "measure-cbp";
  //   Measure measure = new Measure();
  //   measure.setId("measure-cbp");
  //   myMeasures.put(resourceId, measure);
  // }

  @Operation(name = "$evaluate", idempotent = true)
  public MeasureReport evaluateMeasure(@IdParam IdType theId, @OptionalParam(name="source") String source,
                                       @RequiredParam(name="patient") String patientId, @RequiredParam(name="start") String startPeriod,
                                       @RequiredParam(name="end") String endPeriod) throws InternalErrorException
  {
    if (source == null) {
      source = "http://wildfhir.aegis.net/fhir";
    }
    MeasureReport report = new MeasureReport();

    try {
      Path currentRelativePath = Paths.get("../resources/");
      Path path = currentRelativePath.toAbsolutePath();

      // NOTE: I am using a naming convention here:
      //        <id>.elm.xml == library
      //        <id>.xml == measure
      File xmlFile = new File(path.resolve(theId.getIdPart() + ".elm.xml").toString());
      Library library = CqlLibraryReader.read(xmlFile);
      Context context = new Context(library);

      // Register a library loader that loads library out of the current path
      LibraryManager libraryManager = new LibraryManager();
      libraryManager.getLibrarySourceLoader().registerProvider(new DefaultLibrarySourceProvider(path));
      LibraryLoader libraryLoader = new EngineLibraryLoader(libraryManager);
      context.registerLibraryLoader(libraryLoader);

      FhirDataProvider provider = new FhirDataProvider().withEndpoint(source);
      context.registerDataProvider("http://hl7.org/fhir", provider);

      xmlFile = new File(path.resolve(theId.getIdPart() + ".xml").toString());
      Measure measure = provider.getFhirClient().getFhirContext().newXmlParser().parseResource(Measure.class, new FileReader(xmlFile));

      Patient patient = provider.getFhirClient().read().resource(Patient.class).withId(patientId).execute();

      if (patient == null) {
        throw new InternalErrorException("Patient is null");
      }

      context.setContextValue("Patient", patient.getId());

      if (startPeriod == null || endPeriod == null) {
        throw new InternalErrorException("The start and end dates of the measurement period must be specified in request.");
      }

      // this is deprecated, but I don't care
      Date periodStart = new Date(startPeriod);
      Date periodEnd = new Date(endPeriod);

      FhirMeasureEvaluator evaluator = new FhirMeasureEvaluator();
      report = evaluator.evaluate(provider.getFhirClient(), context, measure, patient, periodStart, periodEnd);

      if (report == null) {
        throw new InternalErrorException("MeasureReport is null");
      }

      if (report.getEvaluatedResources() == null) {
        throw new InternalErrorException("EvaluatedResources is null");
      }
    }
    catch (FileNotFoundException e) {
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      throw new InternalErrorException(errors.toString(), e);
    }
    catch (JAXBException e) {
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      throw new InternalErrorException(errors.toString(), e);
    }
    catch (IOException e) {
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      throw new InternalErrorException(errors.toString(), e);
    }
    return report;
  }

  @Create()
  public MethodOutcome createMeasure(@ResourceParam Measure theMeasure) throws UnsupportedEncodingException, FileNotFoundException
  {
    long id = myNextId++;
    myMeasures.put(Long.toString(id), theMeasure);

    return new MethodOutcome(new IdType(id));
  }

  @Read()
  public Measure readMeasure(@IdParam IdType theId) {
    Measure retVal;
    try {
			retVal = myMeasures.get(theId.getIdPartAsLong());
		} catch (NumberFormatException e) {
      throw new ResourceNotFoundException(theId);
		}
    return retVal;
  }

  @Override
	public Class<Measure> getResourceType() {
		return Measure.class;
	}
}
