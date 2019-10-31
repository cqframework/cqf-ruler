package org.opencds.cqf.dstu3.evaluation;

import java.util.Date;

import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.UsingDef;
import org.hl7.fhir.dstu3.model.Measure;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.dstu3.helpers.DateHelper;
import org.opencds.cqf.dstu3.helpers.LibraryHelper;
import org.opencds.cqf.dstu3.providers.LibraryResourceProvider;

import lombok.Data;

@Data
public class MeasureEvaluationSeed
{
    private Measure measure;
    private Context context;
    private Interval measurementPeriod;
    private LibraryLoader libraryLoader;
    private LibraryResourceProvider libraryResourceProvider;
    private ProviderFactory providerFactory;
    private DataProvider dataProvider;

    public MeasureEvaluationSeed(ProviderFactory providerFactory, LibraryLoader libraryLoader, LibraryResourceProvider libraryResourceProvider)
    {
        this.providerFactory = providerFactory;
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

        TerminologyProvider terminologyProvider = this.providerFactory.createTerminologyProvider(source, user, pass);
        context.registerTerminologyProvider(terminologyProvider);


        for (UsingDef using : library.getUsings().getDef())
        {
            if (using.getLocalIdentifier().equals("System")) continue;

            if (this.dataProvider != null) {
                throw new IllegalArgumentException("Evaluation of Measure using multiple Models is not supported at this time.");
            }

            this.dataProvider = this.providerFactory.createDataProvider(using.getLocalIdentifier(), using.getVersion(), terminologyProvider);
            context.registerDataProvider(
                using.getLocalIdentifier().equals("FHIR") ? 
                "http://hl7.org/fhir" :
                "urn:healthit-gov:qdm:v5_4", 
                dataProvider);
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
}
