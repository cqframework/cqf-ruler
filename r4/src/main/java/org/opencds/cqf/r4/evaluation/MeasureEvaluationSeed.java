package org.opencds.cqf.r4.evaluation;

import java.util.Objects;

import org.hl7.fhir.r4.model.Measure;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.runtime.Interval;

public class MeasureEvaluationSeed {
    private final Measure measure;
    private final Context context;
    private final Interval measurementPeriod;
    private final DataProvider dataProvider;

    public MeasureEvaluationSeed(
            Measure measure,
            Context context,
            Interval measurementPeriod,
            DataProvider dataProvider) {
        this.measure = measure;
        this.context = context;
        this.measurementPeriod = measurementPeriod;
        this.dataProvider = dataProvider;
    }

    public Measure getMeasure() {
        return this.measure;
    }

    public Context getContext() {
        return this.context;
    }

    public Interval getMeasurementPeriod() {
        return this.measurementPeriod;
    }

    public DataProvider getDataProvider() {
        return this.dataProvider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeasureEvaluationSeed that = (MeasureEvaluationSeed) o;
        return Objects.equals(measure, that.measure) &&
                Objects.equals(context, that.context) &&
                Objects.equals(measurementPeriod, that.measurementPeriod) &&
                Objects.equals(dataProvider, that.dataProvider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(measure, context, measurementPeriod, dataProvider);
    }

    @Override
    public String toString() {
        return "MeasureEvaluationSeed{" +
                "measure=" + measure +
                ", context=" + context +
                ", measurementPeriod=" + measurementPeriod +
                ", dataProvider=" + dataProvider +
                '}';
    }
}
