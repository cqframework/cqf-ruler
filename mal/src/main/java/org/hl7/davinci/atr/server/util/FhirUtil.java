package org.hl7.davinci.atr.server.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.Group.GroupMemberComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;

// TODO: Auto-generated Javadoc
/**
 * The Class FhirUtil.
 */
public class FhirUtil {
	
	/** The Constant logger. */
	private static final Logger logger = LoggerFactory.getLogger(FhirUtil.class); 
	
	/**
	 * Creates the token and list param.
	 *
	 * @param theParameters the the parameters
	 * @param parameterName the parameter name
	 * @return the token and list param
	 */
	public static TokenAndListParam createTokenAndListParam(Parameters theParameters, String parameterName) {
		TokenAndListParam tokenParam =  new TokenAndListParam();
		
		TokenOrListParam tokenOrParam = new TokenOrListParam();
		
		
		Identifier memberIdentifier = (Identifier) theParameters.getParameter(parameterName);
		TokenParam param = new TokenParam();
		param.setSystem(memberIdentifier.getSystem());
		param.setValue(memberIdentifier.getValue());
		tokenOrParam.add(param);
		tokenParam.addValue(tokenOrParam);
		
		return tokenParam;
	}
	
	
	/**
	 * Gets the reference.
	 *
	 * @param theId the the id
	 * @param resourceType the resource type
	 * @return the reference
	 */
	public static Reference getReference(String theId, String resourceType) {
		Reference theReference = null;
		try {
			if(StringUtils.isNotBlank(theId) && StringUtils.isNotBlank(resourceType)) {
				StringBuilder reference = new StringBuilder();
				reference.append(resourceType);
				reference.append(TextConstants.SINGLE_FORWORD_SLASH);
				reference.append(theId);
				theReference = initReference(theReference);
				theReference.setReference(reference.toString());
			}
		}catch(Exception ex) {
			logger.error("\n Exception while setting getReference in FhirUtils class ", ex);
		}
		return theReference;
	}
	
	/**
	 * Inits the reference.
	 *
	 * @param theReference the the reference
	 * @return the reference
	 */
	private static Reference initReference(Reference theReference){
		if(theReference == null) {
			return new Reference();
		}
		else {
			return theReference;
		}
	}
	
	
	/**
	 * Gets the code type.
	 *
	 * @param theValue the the value
	 * @return the code type
	 */
	public static CodeType getCodeType(String theValue) {
		CodeType codeType = null;
		try {
			if(StringUtils.isNotBlank(theValue)) {
				codeType = new CodeType();
				codeType.setValue(theValue);
			}
		}catch (Exception e) {
			logger.error("Exception in getCodeType of FhirUtility ", e);
		}
		return codeType;
	}
	
	
	/**
	 * Gets the extension for code type.
	 *
	 * @param typeText the type text
	 * @return the extension for code type
	 */
	public static Extension getExtensionForCodeType(String typeText) {
		Extension theExtension = null;
		try {
			if(StringUtils.isNotBlank(typeText)) { 
				theExtension = new Extension();
				UriType uri = getUriType(TextConstants.MEMBER_CHANGETYPE_SYSTEM);
				theExtension.setUrlElement(uri);
				CodeType theCode = getCodeType(typeText);
				theExtension.setValue(theCode);
			}
		}catch(Exception ex) {
			logger.error("\n Exception while setting getExtensionForCodeType in FhirUtility class ", ex);
		}
		return theExtension;
	}

	/**
	 * Gets the extension for reference.
	 *
	 * @param id the id
	 * @param resourceType the resource type
	 * @param system the system
	 * @return the extension for reference
	 */
	public static Extension getExtensionForReference(String id, String resourceType, String system) {
		Extension theExtension = null;
		try {
			if(StringUtils.isNotBlank(id) && StringUtils.isNotBlank(resourceType)) { 
				theExtension = new Extension();
				UriType uri = getUriType(system);
				theExtension.setUrlElement(uri);
				Reference theReference = getReference(id, resourceType);
				theExtension.setValue(theReference);
			}
		}catch(Exception ex) {
			logger.error("\n Exception while setting getExtensionForReference in FhirUtility class ", ex);
		}
		return theExtension;
	}
	
	
	/**
	 * Gets the uri type.
	 *
	 * @param theValue the the value
	 * @return the uri type
	 */
	public static UriType getUriType(String theValue) {
		UriType uriType = null;
		try {
			if(StringUtils.isNotBlank(theValue)) {
				uriType = new UriType();
				uriType.setValue(theValue);
			}
		}catch (Exception e) {
			logger.error("Exception in getUriType of FhirUtility ", e);
		}
		return uriType;
	}
	
	
	/**
	 * Gets the boolean type.
	 *
	 * @param data the data
	 * @return the boolean type
	 */
	public static BooleanType getBooleanType(boolean data) {
		BooleanType booleanType = null;
		try {
			booleanType = new BooleanType();
			booleanType.setValue(data);
		}catch (Exception e) {
			logger.error("Exception in getBooleanType of FhirUtility ", e);
		}
		return booleanType;
	}
	
	
	/**
	 * Gets the period.
	 *
	 * @param start the start
	 * @param end the end
	 * @return the period
	 */
	public static Period getPeriod(DateTimeType start, DateTimeType end) {
		Period thePeriod = null;
		try {
			if(start != null) {
				thePeriod=initPeriod(thePeriod); 
				thePeriod.setStartElement(start);
			}
			if(end != null) {
				thePeriod=initPeriod(thePeriod); 
				thePeriod.setEndElement(end);
			}	
		}catch(Exception ex) {
			logger.error("\n Exception while setting getPeriod in FhirUtils class ", ex);
		}
		return thePeriod;
	}
	
	
	/**
	 * Inits the period.
	 *
	 * @param thePeriod the the period
	 * @return the period
	 */
	private static Period initPeriod(Period thePeriod){
		if(thePeriod == null) {
			return new Period();
		}
		else {
			return thePeriod;
		}
	}
	
	
	/**
	 * Inits the extension list.
	 *
	 * @param extensionList the extension list
	 * @return the list
	 */
	public static List<Extension> initExtensionList(List<Extension> extensionList) {
		if(extensionList == null) {
			return new ArrayList<Extension>();
		}
		else {
			return extensionList;
		}
	}
	
	/**
	 * Gets the group member component.
	 *
	 * @param patientMemberId the patient member id
	 * @param providerId the provider id
	 * @param providerReference the provider reference
	 * @param coverageReference the coverage reference
	 * @param attributionPeriod the attribution period
	 * @return the group member component
	 */
	public static GroupMemberComponent getGroupMemberComponent(String patientMemberId, String providerId, String providerReference, String coverageReference, Period attributionPeriod) {
		GroupMemberComponent theGroupMemberComponent = new GroupMemberComponent();
		List<Extension> theMembeEextensionList = null;
		try {
			if(StringUtils.isNotBlank(patientMemberId)) {
				Reference theReference = getReference(patientMemberId, "Patient");
				if(theReference != null) {
					theGroupMemberComponent.setEntity(theReference);
					BooleanType theBoolean = getBooleanType(false);
					theGroupMemberComponent.setInactiveElement(theBoolean);
				}
			}
			theMembeEextensionList = getGroupMemberComponentExtension(providerId, providerReference, coverageReference, TextConstants.NEW_TYPE);
			if(theMembeEextensionList != null && !theMembeEextensionList.isEmpty()) {
				theGroupMemberComponent.setExtension(theMembeEextensionList);
			}
			if(attributionPeriod != null) {
				Period thePeriod = getPeriod(attributionPeriod.getStartElement(), attributionPeriod.getEndElement());
				theGroupMemberComponent.setPeriod(thePeriod);
			}
		}catch(Exception ex) {
			logger.error("\n Exception while setting getGroupMemberComponent in FhirUtility class ", ex);
		}
		return theGroupMemberComponent;
	}
	
	
	/**
	 * Gets the group member component extension.
	 *
	 * @param providerId the provider id
	 * @param providerReference the provider reference
	 * @param coverageReference the coverage reference
	 * @param changeCode the change code
	 * @return the group member component extension
	 */
	public static List<Extension> getGroupMemberComponentExtension(String providerId, String providerReference,
			String coverageReference, String changeCode) {
		List<Extension> theMembeEextensionList = null;
		try {
			if(StringUtils.isNotBlank(changeCode)) {
				Extension codeExtension = FhirUtil.getExtensionForCodeType(changeCode);
				if(codeExtension != null) {
					theMembeEextensionList = FhirUtil.initExtensionList(theMembeEextensionList);
					theMembeEextensionList.add(codeExtension);
				}
			}
			if(StringUtils.isNotBlank(coverageReference)) {
				Extension coverageExtension = FhirUtil.getExtensionForReference(coverageReference, "Coverage", TextConstants.MEMBER_COVERAGE_SYSTEM);
				if(coverageExtension != null) {
					theMembeEextensionList = FhirUtil.initExtensionList(theMembeEextensionList);
					theMembeEextensionList.add(coverageExtension);
				}
			}
			if(StringUtils.isNotBlank(providerId) && StringUtils.isNotBlank(providerReference)) {
				Extension providerExtension = FhirUtil.getExtensionForReference(providerId, providerReference, TextConstants.MEMBER_PROVIDER_SYSTEM);
				if(providerExtension != null) {
					theMembeEextensionList = FhirUtil.initExtensionList(theMembeEextensionList);
					theMembeEextensionList.add(providerExtension);
				}
			}
		}catch(Exception ex) {
			logger.error("\n Exception while setting getGroupMemberComponent in FhirUtility class ", ex);
		}
		return theMembeEextensionList;
	}

}
