package org.opencds.cqf.ruler.cr.repo;

import static java.util.Objects.nonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.api.AddProfileTagEnum;
import ca.uhn.fhir.interceptor.api.HookParams;
import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
import ca.uhn.fhir.interceptor.api.IInterceptorService;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.ElementsSupportEnum;
import ca.uhn.fhir.rest.server.IPagingProvider;
import ca.uhn.fhir.rest.server.IRestfulServerDefaults;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;

public class RulerRequestDetails extends RequestDetails {
	private FhirContext myFhirContext;
	private ListMultimap<String, String> myHeaders;

	private IRestfulServerDefaults myServer = new MyRestfulServerDefaults();

	public RulerRequestDetails() {
		this(new MyInterceptorBroadcaster());
	}

	public RulerRequestDetails(IInterceptorBroadcaster theInterceptorBroadcaster) {
		super(theInterceptorBroadcaster);
	}

	public RulerRequestDetails(RequestDetails theDetails) {
		super((ServletRequestDetails) theDetails);
		if (nonNull(theDetails.getServer())) {
			myServer = theDetails.getServer();
		}
	}

	@Override
	protected byte[] getByteStreamRequestContents() {
		return new byte[0];
	}

	@Override
	public Charset getCharset() {
		return null;
	}

	@Override
	public FhirContext getFhirContext() {
		return myFhirContext;
	}

	public void setFhirContext(FhirContext theFhirContext) {
		myFhirContext = theFhirContext;
	}

	@Override
	public String getHeader(String name) {
		List<String> headers = getHeaders(name);
		if (headers.isEmpty()) {
			return null;
		} else {
			return headers.get(0);
		}
	}

	@Override
	public List<String> getHeaders(String name) {
		ListMultimap<String, String> headers = myHeaders;
		if (headers == null) {
			headers = ImmutableListMultimap.of();
		}
		return headers.get(name);
	}

	public void addHeader(String theName, String theValue) {
		if (myHeaders == null) {
			myHeaders = ArrayListMultimap.create();
		}
		myHeaders.put(theName, theValue);
	}

	@Override
	public Object getAttribute(String theAttributeName) {
		return null;
	}

	@Override
	public void setAttribute(String theAttributeName, Object theAttributeValue) {

	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public Reader getReader() throws IOException {
		return null;
	}

	@Override
	public IRestfulServerDefaults getServer() {
		return myServer;
	}

	@Override
	public String getServerBaseForRequest() {
		return null;
	}

	@SuppressWarnings("all")
	private static class MyRestfulServerDefaults implements IRestfulServerDefaults {

		@Deprecated
		public AddProfileTagEnum getAddProfileTag() {
			return null;
		}

		@Override
		public EncodingEnum getDefaultResponseEncoding() {
			return null;
		}

		@Override
		public ETagSupportEnum getETagSupport() {
			return null;
		}

		@Override
		public ElementsSupportEnum getElementsSupport() {
			return null;
		}

		@Override
		public FhirContext getFhirContext() {
			return null;
		}

		@Override
		public List<IServerInterceptor> getInterceptors_() {
			return null;
		}

		@Override
		public IPagingProvider getPagingProvider() {
			return null;
		}

		@Override
		public boolean isDefaultPrettyPrint() {
			return false;
		}

		@Override
		public IInterceptorService getInterceptorService() {
			return null;
		}
	}

	private static class MyInterceptorBroadcaster implements IInterceptorBroadcaster {

		@Override
		public boolean callHooks(Pointcut thePointcut, HookParams theParams) {
			return true;
		}

		@Override
		public Object callHooksAndReturnObject(Pointcut thePointcut, HookParams theParams) {
			return null;
		}

		@Override
		public boolean hasHooks(Pointcut thePointcut) {
			return false;
		}
	}
}
