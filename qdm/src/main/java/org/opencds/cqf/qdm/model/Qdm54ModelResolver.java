package org.opencds.cqf.qdm.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.opencds.cqf.cql.model.ModelResolver;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.TemporalHelper;
import org.opencds.cqf.qdm.fivepoint4.model.*;

public class Qdm54ModelResolver implements ModelResolver {

        protected String packageName;

        public Qdm54ModelResolver() {
                this.packageName = "org.opencds.cqf.qdm.fivepoint4.model";
        }

        @Override
        public String getPackageName() {
                return this.packageName;
        }

        @Override
        public void setPackageName(String packageName) {
                this.packageName = packageName;
        }

        @Override
        public Object resolvePath(Object target, String path) {
                Method method;
                Object result;
                String methodName = String.format("get%s", path.substring(0, 1).toUpperCase() + path.substring(1));
                try {
                        method = target.getClass().getMethod(methodName);
                        result = method.invoke(target);
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                        throw new RuntimeException(String.format("Unable to resolve method: %s on type: %s", methodName,
                                        target.getClass().getName()));
                }

                if (path.toLowerCase().endsWith("datetime") && result != null && result instanceof String
                                && !((String) result).isEmpty()) {
                        return new DateTime((String) result, TemporalHelper.getDefaultZoneOffset());
                }

                if (result instanceof DateTimeInterval) {
                        String start = ((DateTimeInterval) result).getStart();
                        String end = ((DateTimeInterval) result).getEnd();
                        return new Interval(
                                        start != null && !start.isEmpty()
                                                        ? new DateTime(start, TemporalHelper.getDefaultZoneOffset())
                                                        : null,
                                        true,
                                        end != null && !end.isEmpty()
                                                        ? new DateTime(end, TemporalHelper.getDefaultZoneOffset())
                                                        : null,
                                        true);
                }

                if (result instanceof org.opencds.cqf.qdm.fivepoint4.model.Code) {
                        return new Code().withCode(((org.opencds.cqf.qdm.fivepoint4.model.Code) result).getCode())
                                        .withSystem(((org.opencds.cqf.qdm.fivepoint4.model.Code) result).getSystem())
                                        .withDisplay(((org.opencds.cqf.qdm.fivepoint4.model.Code) result).getDisplay())
                                        .withVersion(((org.opencds.cqf.qdm.fivepoint4.model.Code) result).getVersion());
                }

                if (result instanceof Quantity) {
                        return new org.opencds.cqf.cql.runtime.Quantity().withValue(((Quantity) result).getValue())
                                        .withUnit(((Quantity) result).getUnit());
                }

                if (result instanceof QuantityInterval) {
                        return new Interval(
                                        new org.opencds.cqf.cql.runtime.Quantity()
                                                        .withValue(((QuantityInterval) result).getStart().getValue())
                                                        .withUnit(((QuantityInterval) result).getStart().getUnit()),
                                        true,
                                        new org.opencds.cqf.cql.runtime.Quantity()
                                                        .withValue(((QuantityInterval) result).getEnd().getValue())
                                                        .withUnit(((QuantityInterval) result).getEnd().getUnit()),
                                        true);
                }

                return result;
        }

        @Override
        public Object getContextPath(String contextType, String targetType) {
                return null;
        }

        @Override
        public Class<?> resolveType(String typeName) {
                return null;
        }

        @Override
        public Class<?> resolveType(Object value) {
                return null;
        }

        @Override
        public Object createInstance(String typeName) {
                return null;
        }

        @Override
        public void setValue(Object target, String path, Object value) {
        }

        @Override
        public Boolean objectEqual(Object left, Object right) {
                return left.equals(right);
        }

        @Override
        public Boolean objectEquivalent(Object left, Object right) {
                return left.equals(right);
        }

}