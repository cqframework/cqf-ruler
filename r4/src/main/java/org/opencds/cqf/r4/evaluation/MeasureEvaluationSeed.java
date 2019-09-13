package org.opencds.cqf.r4.evaluation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import lombok.Data;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.UsingDef;
import org.hl7.fhir.r4.model.Measure;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;
import org.opencds.cqf.r4.helpers.DateHelper;
import org.opencds.cqf.r4.helpers.LibraryHelper;
import org.opencds.cqf.r4.providers.ApelonFhirTerminologyProvider;
import org.opencds.cqf.r4.providers.JpaDataProvider;
import org.opencds.cqf.r4.providers.LibraryResourceProvider;
import org.opencds.cqf.qdm.providers.Qdm54DataProvider;

import java.util.Date;

@Data
public class MeasureEvaluationSeed
{
    private Measure measure;
    private Context context;
    private DataProvider dataProvider;
    private Interval measurementPeriod;
    private LibraryLoader libraryLoader;
    private LibraryResourceProvider libraryResourceProvider;

    public MeasureEvaluationSeed(JpaDataProvider provider, LibraryLoader libraryLoader, LibraryResourceProvider libraryResourceProvider)
    {
        this.dataProvider = provider;
        this.libraryLoader = libraryLoader;
        this.libraryResourceProvider = libraryResourceProvider;
    }

    public void setup(
            Measure measure, String periodStart, String periodEnd,
            String productLine, String source, String user, String pass)
    {
        this.measure = measure;

        LibraryHelper.loadLibraries(measure, this.libraryLoader, this.libraryResourceProvider);

        // resolve primary library
        Library library = LibraryHelper.resolvePrimaryLibrary(measure, libraryLoader, this.libraryResourceProvider);

        // resolve execution context
        context = new Context(library);
        context.registerLibraryLoader(libraryLoader);

        TerminologyProvider terminologyProvider = getTerminologyProvider(source, user, pass);

        for (UsingDef using : library.getUsings().getDef())
        {
            if (using.getLocalIdentifier().equals("System")) continue;

            dataProvider = getDataProvider(using.getLocalIdentifier(), using.getVersion());
            if (dataProvider instanceof JpaDataProvider)
            {
                ((JpaDataProvider) dataProvider).setTerminologyProvider(terminologyProvider);
                ((JpaDataProvider) dataProvider).setExpandValueSets(true);
                context.registerDataProvider("http://hl7.org/fhir", dataProvider);
                context.registerLibraryLoader(getLibraryLoader());
                context.registerTerminologyProvider(terminologyProvider);
            }
            else
            {
                ((Qdm54DataProvider) dataProvider).setTerminologyProvider(terminologyProvider);
                context.registerDataProvider("urn:healthit-gov:qdm:v5_4", dataProvider);
                context.registerLibraryLoader(getLibraryLoader());
                context.registerTerminologyProvider(terminologyProvider);
            }
        }

        // resolve the measurement period
        measurementPeriod = new Interval(DateHelper.resolveRequestDate(periodStart, true), true,
                DateHelper.resolveRequestDate(periodEnd, false), true);

        context.setParameter(null, "Measurement Period",
                new Interval(DateTime.fromJavaDate((Date) measurementPeriod.getStart()), true,
                        DateTime.fromJavaDate((Date) measurementPeriod.getEnd()), true));

        if (productLine != null) {
            context.setParameter(null, "Product Line", productLine);
        }

        context.setExpressionCaching(true);
    }

    private TerminologyProvider getTerminologyProvider(String url, String user, String pass)
    {
        if (url != null) {
            if (url.contains("apelon.com")) {
                return new ApelonFhirTerminologyProvider().withBasicAuth(user, pass).setEndpoint(url, false);
            } else {
                return new FhirTerminologyProvider().withBasicAuth(user, pass).setEndpoint(url, false);
            }
        }
        else return ((JpaDataProvider) dataProvider).getTerminologyProvider();
    }

    private DataProvider getDataProvider(String model, String version)
    {
        if (model.equals("FHIR") && version.equals("4.0.0"))
        {
            FhirContext fhirContext = ((JpaDataProvider) dataProvider).getFhirContext();
            fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
            ((JpaDataProvider) dataProvider).setFhirContext(fhirContext);
            return dataProvider;
        }

        else if (model.equals("QDM") && version.equals("5.4"))
        {
            return new Qdm54DataProvider();
        }

        throw new IllegalArgumentException("Could not resolve data provider for data model: " + model + " using version: " + version);
    }
}
