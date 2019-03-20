package org.opencds.cqf.qdm.fivepoint4.exception;

public class ResourceNotFound extends RuntimeException
{
    private String resourceName;
    private String fieldName;
    private Object fieldValue;

    public ResourceNotFound() { }

    public ResourceNotFound(String resourceName, String fieldName, Object fieldValue)
    {
        super(String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName()
    {
        return resourceName;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    public Object getFieldValue()
    {
        return fieldValue;
    }
}
