package org.opencds.cqf.evaluation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import lombok.Data;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.UsingDef;
import org.hl7.fhir.dstu3.model.Measure;
import org.opencds.cqf.config.STU3LibraryLoader;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;
import org.opencds.cqf.helpers.DateHelper;
import org.opencds.cqf.helpers.LibraryHelper;
import org.opencds.cqf.providers.JpaDataProvider;
import org.opencds.cqf.providers.Qdm54DataProvider;

import java.util.Date;

@Data
public class MeasureEvaluationSeed
{
    private Measure measure;
    private Context context;
    private STU3LibraryLoader libraryLoader;
    private DataProvider dataProvider;
    private Interval measurementPeriod;

    private LibraryResourceProvider libraryResourceProvider;

    public MeasureEvaluationSeed(JpaDataProvider provider, STU3LibraryLoader libraryLoader)
    {
        this.dataProvider = provider;
        this.libraryLoader = libraryLoader;
        this.libraryResourceProvider = (LibraryResourceProvider) provider.resolveResourceProvider("Library");
    }

    public void setup(
            Measure measure, String periodStart, String periodEnd,
            String source, String user, String pass)
    {
        this.measure = measure;

        LibraryHelper.loadLibraries(measure, libraryLoader, libraryResourceProvider);

        // resolve primary library
        Library library;
        if (libraryLoader.getLibraries().size() == 1) {
            library = libraryLoader.getLibraries().values().iterator().next();
        } else {
            library = LibraryHelper.resolvePrimaryLibrary(measure, libraryLoader);
        }

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
            }
            else
            {
                ((Qdm54DataProvider) dataProvider).setTerminologyProvider(terminologyProvider);
                context.registerDataProvider("urn:healthit-gov:qdm:v5_4", dataProvider);
                context.registerLibraryLoader(getLibraryLoader());
            }
        }

        // resolve the measurement period
        measurementPeriod = new Interval(DateHelper.resolveRequestDate(periodStart, true), true,
                DateHelper.resolveRequestDate(periodEnd, false), true);

        context.setParameter(null, "Measurement Period",
                new Interval(DateTime.fromJavaDate((Date) measurementPeriod.getStart()), true,
                        DateTime.fromJavaDate((Date) measurementPeriod.getEnd()), true));
    }

    private TerminologyProvider getTerminologyProvider(String url, String user, String pass)
    {
        if (url != null) {
            // TODO: Change to cache-value-sets
            return new FhirTerminologyProvider()
                    .withBasicAuth(user, pass)
                    .setEndpoint(url, false);
        }
        else return ((JpaDataProvider) dataProvider).getTerminologyProvider();
    }

    private DataProvider getDataProvider(String model, String version)
    {
        if (model.equals("FHIR") && version.equals("3.0.0"))
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
