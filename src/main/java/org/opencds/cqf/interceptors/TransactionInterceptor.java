package org.opencds.cqf.interceptors;

import ca.uhn.fhir.model.api.TagList;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.providers.FHIRValueSetResourceProvider;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TransactionInterceptor implements IServerInterceptor {

    private boolean isTransaction;
    private FHIRValueSetResourceProvider valueSetResourceProvider;
    public TransactionInterceptor(FHIRValueSetResourceProvider valueSetResourceProvider) {
        isTransaction = false;
        this.valueSetResourceProvider = valueSetResourceProvider;
    }

    @Override
    public boolean handleException(RequestDetails requestDetails, BaseServerResponseException e, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        return true;
    }

    @Override
    public boolean incomingRequestPostProcessed(RequestDetails requestDetails, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws AuthenticationException {
        isTransaction = requestDetails.getRestOperationType() == RestOperationTypeEnum.TRANSACTION;
        return true;
    }

    @Override
    public void incomingRequestPreHandled(RestOperationTypeEnum restOperationTypeEnum, ActionRequestDetails actionRequestDetails) {
//        if (isTransaction && actionRequestDetails.getResource() instanceof Bundle) {
//            if (((Bundle) actionRequestDetails.getResource()).hasEntry()) {
//                for (Bundle.BundleEntryComponent entry : ((Bundle) actionRequestDetails.getResource()).getEntry()) {
//                    if (entry.hasResource() && entry.getResource() instanceof ValueSet) {
//                        valueSetResourceProvider.populateCodeSystem((ValueSet) entry.getResource());
//                    }
//                }
//            }
//        }
    }

    @Override
    public boolean incomingRequestPreProcessed(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return true;
    }

    @Override
    public boolean outgoingResponse(RequestDetails requestDetails) {
        return true;
    }

    @Override
    public boolean outgoingResponse(RequestDetails requestDetails, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws AuthenticationException {
        return true;
    }

    @Override
    public boolean outgoingResponse(RequestDetails requestDetails, IBaseResource iBaseResource) {
        return true;
    }

    @Override
    public boolean outgoingResponse(RequestDetails requestDetails, IBaseResource iBaseResource, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws AuthenticationException {
        return true;
    }

    @Override
    public boolean outgoingResponse(RequestDetails requestDetails, ResponseDetails responseDetails, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws AuthenticationException {
        try {
            return true;
        } finally {
            if (responseDetails.getResponseResource() instanceof Bundle) {
                Bundle responseBundle = (Bundle) responseDetails.getResponseResource();
                if (responseBundle.getType() == Bundle.BundleType.TRANSACTIONRESPONSE && responseBundle.hasEntry()) {
                    for (Bundle.BundleEntryComponent entry : responseBundle.getEntry()) {
                        if (entry.hasResponse() && entry.getResponse().hasLocation()) {
                            if (entry.getResponse().getLocation().startsWith("ValueSet")) {
                                String id = entry.getResponse().getLocation().replace("ValueSet/", "").split("/")[0];
                                valueSetResourceProvider.populateCodeSystem(valueSetResourceProvider.getDao().read(new IdType(id)));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean outgoingResponse(RequestDetails requestDetails, TagList tagList) {
        return true;
    }

    @Override
    public boolean outgoingResponse(RequestDetails requestDetails, TagList tagList, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws AuthenticationException {
        return true;
    }

    @Override
    public BaseServerResponseException preProcessOutgoingException(RequestDetails requestDetails, Throwable throwable, HttpServletRequest httpServletRequest) throws ServletException {
        return null;
    }

    @Override
    public void processingCompletedNormally(ServletRequestDetails servletRequestDetails) {

    }
}
