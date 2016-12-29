package org.opencds.cqf.provider;

import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.cqframework.cql.cql2elm.DefaultLibrarySourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Patient;
import org.opencds.cqf.cql.data.fhir.FhirDataProvider;
import org.opencds.cqf.cql.data.fhir.FhirMeasureEvaluator;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.CqlLibraryReader;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

// for meaningful error reporting

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
                                       @RequiredParam(name="patient") String patientId, @RequiredParam(name="startPeriod") String startPeriod,
                                       @RequiredParam(name="endPeriod") String endPeriod) throws InternalErrorException
  {
    if (source == null) {
      source = "https://api2.hspconsortium.org/payerextract/open";
    }
    MeasureReport report;

    try {
      Path currentRelativePath = Paths.get("src/main/java/org/opencds/cqf/resources");
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
      LibraryLoader libraryLoader = new MeasureLibraryLoader(libraryManager);
      context.registerLibraryLoader(libraryLoader);

      FhirDataProvider provider = new FhirDataProvider().withEndpoint(source);
      FhirTerminologyProvider terminologyProvider = new FhirTerminologyProvider()
              .withBasicAuth("brhodes", "apelon123!")
              .withEndpoint("http://fhir.ext.apelon.com/dtsserverws/fhir");
      provider.setTerminologyProvider(terminologyProvider);
      provider.setExpandValueSets(true);
      context.registerDataProvider("http://hl7.org/fhir", provider);

      xmlFile = new File(path.resolve(theId.getIdPart() + ".xml").toString());
      Measure measure = provider.getFhirClient().getFhirContext().newXmlParser().parseResource(Measure.class, new FileReader(xmlFile));

      Patient patient = provider.getFhirClient().read().resource(Patient.class).withId(patientId).execute();

      if (patient == null) {
        throw new InternalErrorException("Patient is null");
      }

      context.setContextValue("Patient", patientId);

      if (startPeriod == null || endPeriod == null) {
        throw new InternalErrorException("The start and end dates of the measurement period must be specified in request.");
      }

      Date periodStart = resolveRequestDate(startPeriod, true);
      Date periodEnd = resolveRequestDate(endPeriod, false);

      FhirMeasureEvaluator evaluator = new FhirMeasureEvaluator();
      report = evaluator.evaluate(provider.getFhirClient(), context, measure, patient, periodStart, periodEnd);

      if (report == null) {
        throw new InternalErrorException("MeasureReport is null");
      }

      if (report.getEvaluatedResources() == null) {
        throw new InternalErrorException("EvaluatedResources is null");
      }
    } catch (JAXBException | IOException e) {
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

  // Helper class to resolve period dates
  public static Date resolveRequestDate(String date, boolean start) {
    // split it up - support dashes or slashes
    String[] dissect = date.contains("-") ? date.split("-") : date.split("/");
    List<Integer> dateVals = new ArrayList<>();
    for (String dateElement : dissect) {
      dateVals.add(Integer.parseInt(dateElement));
    }

    if (dateVals.isEmpty())
      throw new IllegalArgumentException("Invalid date");

    // for now support dates up to day precision
    Calendar calendar = Calendar.getInstance();
    calendar.clear();
    calendar.set(Calendar.YEAR, dateVals.get(0));
    if (dateVals.size() > 1) {
      // java.util.Date months are zero based, hence the negative 1 -- 2014-01 == February 2014
      calendar.set(Calendar.MONTH, dateVals.get(1) - 1);
    }
    if (dateVals.size() > 2)
      calendar.set(Calendar.DAY_OF_MONTH, dateVals.get(2));
    else {
      if (start) {
        calendar.set(Calendar.DAY_OF_MONTH, 1);
      }
      else {
        // get last day of month for end period
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.DATE, -1);
      }
    }
    return calendar.getTime();
  }
}
