package org.opencds.cqf.dstu3.providers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.dstu3.model.Base;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.DecimalType;
import org.hl7.fhir.dstu3.model.Element;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.Property;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.common.evaluation.EvaluationProviderFactory;
import org.opencds.cqf.common.helpers.TranslatorHelper;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.runtime.DateTime;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

public class ApplyCqlOperationProvider {

    private EvaluationProviderFactory providerFactory;
    private IFhirResourceDao<Bundle> bundleDao;
    FhirContext context;

    public ApplyCqlOperationProvider(EvaluationProviderFactory providerFactory, IFhirResourceDao<Bundle> bundleDao, FhirContext context) {
        this.providerFactory = providerFactory;
        this.bundleDao = bundleDao;
        this.context = context;
    }

    @Operation(name = "$apply-cql", type = Bundle.class)
    public Bundle apply(@IdParam IdType id) throws FHIRException {
        Bundle bundle = this.bundleDao.read(id);
        if (bundle == null) {
            throw new IllegalArgumentException("Could not find Bundle/" + id.getIdPart());
        }
        return applyCql(bundle);
    }

    @Operation(name = "$apply-cql", type = Bundle.class)
    public Bundle apply(@OperationParam(name = "resourceBundle", min = 1, max = 1, type = Bundle.class) Bundle bundle)
            throws FHIRException {
        return applyCql(bundle);
    }

    public Bundle applyCql(Bundle bundle) throws FHIRException {
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource()) {
                applyCqlToResource(entry.getResource());
            }
        }

        return bundle;
    }

    public Resource applyCqlToResource(Resource resource) throws FHIRException {
        Library library;
        Context context;
        // String fhirVersion = this.context.getVersion().getVersion().getFhirVersionString();
        String fhirVersion = "3.0.0";
        for (Property child : resource.children()) {
            for (Base base : child.getValues()) {
                if (base != null) {
                    List<String> extension = getExtension(base);
                    if (!extension.isEmpty()) {
                        String cql = String.format("using FHIR version '"+ fhirVersion + "' define x: %s", extension.get(1));
                        library = TranslatorHelper.translateLibrary(cql, new LibraryManager(new ModelManager()),
                                new ModelManager());
                        context = new Context(library);
                        context.registerDataProvider("http://hl7.org/fhir",
                                this.providerFactory.createDataProvider("FHIR", fhirVersion));
                        Object result = context.resolveExpressionRef("x").getExpression().evaluate(context);
                        if (extension.get(0).equals("extension")) {
                            resource.setProperty(child.getName(), resolveType(result, base.fhirType()));
                        } else {
                            String type = base.getChildByName(extension.get(0)).getTypeCode();
                            base.setProperty(extension.get(0), resolveType(result, type));
                        }
                    }
                }
            }
        }
        return resource;
    }

    private List<String> getExtension(Base base) {
        List<String> retVal = new ArrayList<>();
        for (Property child : base.children()) {
            for (Base childBase : child.getValues()) {
                if (childBase != null) {
                    if (((Element) childBase).hasExtension()) {
                        for (Extension extension : ((Element) childBase).getExtension()) {
                            if (extension.getUrl()
                                    .equals("http://hl7.org/fhir/StructureDefinition/cqif-cqlExpression")) {
                                retVal.add(child.getName());
                                retVal.add(extension.getValue().primitiveValue());
                            }
                        }
                    } else if (childBase instanceof Extension) {
                        retVal.add(child.getName());
                        retVal.add(((Extension) childBase).getValue().primitiveValue());
                    }
                }
            }
        }
        return retVal;
    }

    private Base resolveType(Object source, String type) {
        if (source instanceof Integer) {
            return new IntegerType((Integer) source);
        } else if (source instanceof BigDecimal) {
            return new DecimalType((BigDecimal) source);
        } else if (source instanceof Boolean) {
            return new BooleanType().setValue((Boolean) source);
        } else if (source instanceof String) {
            return new StringType((String) source);
        } else if (source instanceof DateTime) {
            if (type.equals("dateTime")) {
                return new DateTimeType().setValue(Date.from(((DateTime) source).getDateTime().toInstant()));
            }
            if (type.equals("date")) {
                return new DateType().setValue(Date.from(((DateTime) source).getDateTime().toInstant()));
            }
        } else if (source instanceof org.opencds.cqf.cql.engine.runtime.Date) {
            if (type.equals("dateTime")) {
                return new DateTimeType()
                        .setValue(java.sql.Date.valueOf(((org.opencds.cqf.cql.engine.runtime.Date) source).getDate()));
            }
            if (type.equals("date")) {
                return new DateType()
                        .setValue(java.sql.Date.valueOf(((org.opencds.cqf.cql.engine.runtime.Date) source).getDate()));
            }
        }

        if (source instanceof Base) {
            return (Base) source;
        }

        throw new RuntimeException("Unable to resolve type: " + source.getClass().getSimpleName());
    }
}
