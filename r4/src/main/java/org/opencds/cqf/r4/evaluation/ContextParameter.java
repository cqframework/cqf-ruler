package org.opencds.cqf.r4.evaluation;

import java.util.Objects;

public class ContextParameter {
    private final String libraryName;
    private final String name;
    private final Object value;

    public ContextParameter(String libraryName, String name, Object value) {
        this.libraryName = libraryName;
        this.name = name;
        this.value = value;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContextParameter that = (ContextParameter) o;
        return Objects.equals(libraryName, that.libraryName) &&
                Objects.equals(name, that.name) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(libraryName, name, value);
    }

    @Override
    public String toString() {
        return "ContextParameter{" +
                "libraryName='" + libraryName + '\'' +
                ", name='" + name + '\'' +
                ", value=" + value +
                '}';
    }
}
