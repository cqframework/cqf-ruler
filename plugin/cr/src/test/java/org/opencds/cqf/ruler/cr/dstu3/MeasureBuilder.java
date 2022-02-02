package org.opencds.cqf.ruler.cr.dstu3;

import com.google.common.collect.Lists;

import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.dstu3.model.Reference;
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType;

public final class MeasureBuilder {

	private final Measure measure;

	private MeasureBuilder(Measure measure) {
		this.measure = measure;
	}

	public static MeasureBuilder newCohortMeasure(Library library) {
		MeasureBuilder mb = new MeasureBuilder(measure("cohort"));
		mb.setLibrary(library);
		mb.addPopulation(MeasurePopulationType.INITIALPOPULATION, "Initial Population");
		return mb;
	}

	public static MeasureBuilder newProportionMeasure(Library library) {
		MeasureBuilder mb = new MeasureBuilder(measure("proportion"));
		mb.setLibrary(library);
		mb.addPopulation(MeasurePopulationType.INITIALPOPULATION, "Initial Population");
		mb.addPopulation(MeasurePopulationType.DENOMINATOR, "Denominator");
		mb.addPopulation(MeasurePopulationType.NUMERATOR, "Numerator");
		return mb;
	}

	public static MeasureBuilder newContinuousVariableMeasure(Library library) {
		MeasureBuilder mb = new MeasureBuilder(measure("continuous-variable"));
		mb.setLibrary(library);
		mb.addPopulation(MeasurePopulationType.INITIALPOPULATION, "Initial Population");
		mb.addPopulation(MeasurePopulationType.MEASUREPOPULATION, "Measure Population");
		return mb;
	}


	private static Measure measure(String scoring) {
		Measure measure = new Measure();
		measure.setId("Measure/measure-Test");
		measure.setName("Test");
		measure.setVersion("1.0.0");
		measure.setUrl("http://test.com/fhir/Measure/Test");
		measure.getScoring().getCodingFirstRep().setCode(scoring);
		return measure;
	}


	private MeasureBuilder addPopulation(MeasurePopulationType measurePopulationType, String expression) {
		MeasureGroupPopulationComponent mgpc = this.measure.getGroupFirstRep().addPopulation();
		mgpc.getCode().getCodingFirstRep().setCode(measurePopulationType.toCode());
		mgpc.setCriteria(expression);
		return this;
	}

	public MeasureBuilder addStratifier(String stratifierId, String expression) {
		MeasureGroupStratifierComponent mgsc = measure.getGroupFirstRep().addStratifier();
		mgsc.setCriteria(expression);
		mgsc.setId(stratifierId);
		return this;
	}

	public MeasureBuilder addSDE(String sdeId, String expression) {
		MeasureSupplementalDataComponent sde = measure.getSupplementalDataFirstRep();
		sde.setId(sdeId);
		sde.setCriteria(expression);
		return this;
	}

	public MeasureBuilder addSDERace() {
		addSDE("sde-race", "SDE Race");
		return this;
	}

	private MeasureBuilder setLibrary(Library library) {
		measure.setLibrary(Lists.newArrayList(new Reference(library.getIdElement())));
		return this;
	}

	public Measure build(){
		return this.measure;
	}
}
