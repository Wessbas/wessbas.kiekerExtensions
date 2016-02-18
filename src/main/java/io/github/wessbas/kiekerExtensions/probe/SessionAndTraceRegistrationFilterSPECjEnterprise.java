/***************************************************************************
 * Copyright 2015 Kieker Project (http://kieker-monitoring.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package io.github.wessbas.kiekerExtensions.probe;

import io.github.wessbas.kiekerExtensions.record.ServletEntryRecord;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import kieker.common.logging.Log;
import kieker.common.logging.LogFactory;
import kieker.common.record.controlflow.OperationExecutionRecord;
import kieker.common.util.signature.ClassOperationSignaturePair;
import kieker.common.util.signature.Signature;
import kieker.monitoring.core.controller.IMonitoringController;
import kieker.monitoring.core.controller.MonitoringController;
import kieker.monitoring.core.registry.ControlFlowRegistry;
import kieker.monitoring.core.registry.SessionRegistry;
import kieker.monitoring.core.sampler.ISampler;
import kieker.monitoring.probe.IMonitoringProbe;
import kieker.monitoring.sampler.mxbean.GCSampler;
import kieker.monitoring.sampler.mxbean.MemorySampler;
import kieker.monitoring.timer.ITimeSource;

/**
 * For each incoming request via {@link #doFilter(ServletRequest, ServletResponse, FilterChain)}, this class
 * (i) registers session and trace information into the thread-local data structures {@link SessionRegistry} and
 * {@link kieker.monitoring.core.registry.TraceRegistry} accessible to other probes in
 * the control-flow of this request, (ii) executes the given {@link FilterChain} and subsequently (iii) unregisters the thread-local
 * data. If configured in the {@link FilterConfig} (see below), the execution of the {@link #doFilter(ServletRequest, ServletResponse, FilterChain)} method
 * is also part of the trace and logged to the {@link IMonitoringController} (note that this is the default behavior when no property is found).
 * 
 * The filter can be integrated into the web.xml as follows:
 * 
 * <pre>
 * {@code
 * <filter>
 *   <filter-name>sessionAndTraceRegistrationFilter</filter-name>
 *   <filter-class>kieker.monitoring.probe.servlet.SessionAndTraceRegistrationFilter</filter-class>
 * </filter>
 * <filter-mapping>
 *   <filter-name>sessionAndTraceRegistrationFilter</filter-name>
 *   <url-pattern>/*</url-pattern>
 * </filter-mapping>
 * }
 * </pre>
 * 
 * @author Andre van Hoorn, Marco Luebcke, Jan Waller
 * 
 * @since 1.5
 */
public class SessionAndTraceRegistrationFilterSPECjEnterprise implements Filter, IMonitoringProbe {
	public static final String CONFIG_PROPERTY_NAME_LOG_FILTER_EXECUTION = "logFilterExecution";

	protected static final IMonitoringController MONITORING_CTRL = MonitoringController.getInstance();
	protected static final SessionRegistry SESSION_REGISTRY = SessionRegistry.INSTANCE;
	protected static final ControlFlowRegistry CF_REGISTRY = ControlFlowRegistry.INSTANCE;

	protected static final ITimeSource TIMESOURCE = MONITORING_CTRL.getTimeSource();
	protected static final String VM_NAME = MONITORING_CTRL.getHostname();

	private static final Log LOG = LogFactory.getLog(SessionAndTraceRegistrationFilterSPECjEnterprise.class);

	/**
	 * Signature for the {@link #doFilter(ServletRequest, ServletResponse, FilterChain)} which will be used when logging
	 * executions of this method.
	 */
	private final String filterOperationSignatureString;

	private volatile boolean logFilterExecution = true; // default

	public SessionAndTraceRegistrationFilterSPECjEnterprise() {
		final Signature methodSignature =
				new Signature("doFilter", // operation name
						new String[] { "public" }, // modifier list
						"void", // return type
						new String[] { ServletRequest.class.getName(), ServletResponse.class.getName(), FilterChain.class.getName() }); // arg types
		final ClassOperationSignaturePair filterOperationSignaturePair =
				new ClassOperationSignaturePair(SessionAndTraceRegistrationFilterSPECjEnterprise.class.getName(), methodSignature);
		this.filterOperationSignatureString = filterOperationSignaturePair.toString();
	}

	public SessionAndTraceRegistrationFilterSPECjEnterprise(final boolean logFilterExecution) {
		this();
		this.logFilterExecution = logFilterExecution;
	}

	/**
	 * Returns the operation signature of this filter's {@link #doFilter(ServletRequest, ServletResponse, FilterChain)} operation
	 * to be used when logging executions of this operation.
	 * 
	 * Extending classes may override this method in order to provide an alternative signature. However,
	 * note that this method is executed on each filter execution. Hence, you should return a final
	 * value here instead of executing expensive String operations.
	 * 
	 * @return The operation signature as a string.
	 */
	protected String getFilterOperationSignatureString() {
		return this.filterOperationSignatureString;
	}

	public void init(final FilterConfig config) throws ServletException {
		// by default, we do nothing here. Extending classes may override this method
		final String valString = config.getInitParameter(CONFIG_PROPERTY_NAME_LOG_FILTER_EXECUTION);
		if (valString != null) {
			this.logFilterExecution = Boolean.parseBoolean(valString);
		} else {
			LOG.warn("Filter configuration '"
					+ CONFIG_PROPERTY_NAME_LOG_FILTER_EXECUTION
					+ "' not set. Using the value: " + this.logFilterExecution);
		}

		final ISampler[] samplers = new ISampler[] { new GCSampler(), new MemorySampler() };
		for (final ISampler sampler : samplers) {
			MONITORING_CTRL.schedulePeriodicSampler(sampler, 0, 10, TimeUnit.SECONDS);
		}
	}

	/**
	 * Register thread-local session and trace information, executes the given {@link FilterChain} and unregisters
	 * the session/trace information. If configured, the execution of this filter is also logged to the {@link IMonitoringController}.
	 * This method returns immediately if monitoring is not enabled.
	 * 
	 * @param request
	 *            The request.
	 * @param response
	 *            The response.
	 * @param chain
	 *            The filter chain to be used.
	 * 
	 * @throws IOException
	 * @throws ServletException
	 */
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		if (!MONITORING_CTRL.isMonitoringEnabled()) {
			chain.doFilter(request, response);
			return;
		}
		if (!MONITORING_CTRL.isProbeActivated(this.filterOperationSignatureString)) {
			chain.doFilter(request, response);
			return;
		}

		// Register session information which needs to be reset after the chain has been executed.
		String sessionId = this.registerSessionInformation(request); // {@link OperationExecutionRecord#NO_SESSION_ID} if no session ID
		long traceId = OperationExecutionRecord.NO_TRACE_ID; // note that we must NOT register anything to the CF_REGISTRY here!

		// If this filter execution shall be part of the traced control flow, we need to register some control flow information.
		if (this.logFilterExecution) {
			traceId = CF_REGISTRY.getAndStoreUniqueThreadLocalTraceId();
			CF_REGISTRY.storeThreadLocalEOI(0); // current execution's eoi is 0
			CF_REGISTRY.storeThreadLocalESS(1); // *current* execution's ess is 0; next execution is at stack depth 1
		}

		final long tin = TIMESOURCE.getTime(); // the entry timestamp
		try {
			chain.doFilter(request, response);
		} finally {
			SESSION_REGISTRY.unsetThreadLocalSessionId();
			if (this.logFilterExecution) {
				String uri = ((HttpServletRequest) request).getRequestURI();
				int port = request.getLocalPort();
				String host = request.getLocalAddr();
				String protocol = request.getProtocol();
				String method = ((HttpServletRequest) request).getMethod();
				String queryString = ((HttpServletRequest) request).getQueryString();
				String encoding = request.getCharacterEncoding();
				MONITORING_CTRL.newMonitoringRecord(
						new ServletEntryRecord(traceId, uri, port, host, protocol, method, queryString, encoding));

				final long tout = TIMESOURCE.getTime();
				// if sessionId == null, try again to fetch it (should exist after being within the application logic)
				if (sessionId == OperationExecutionRecord.NO_SESSION_ID) { // yes, == and not equals
					sessionId = this.registerSessionInformation(request);
				}

				// Log this execution
				MONITORING_CTRL.newMonitoringRecord(
						new OperationExecutionRecord(SessionAndTraceRegistrationFilterSPECjEnterprise.extractUseCaseFromRequest(request),
								sessionId, traceId, tin, tout, VM_NAME, 0, 0)); // 0,0 state that this method is the application entry point

				// Reset the thread-local trace information
				CF_REGISTRY.unsetThreadLocalTraceId();
				CF_REGISTRY.unsetThreadLocalEOI();
				CF_REGISTRY.unsetThreadLocalESS();
			}
		}
	}

	public void destroy() {
		// by default, we do nothing here. Extending classes may override this method
	}

	/**
	 * If the given {@link ServletRequest} is an instance of {@link HttpServletRequest}, this methods extracts the session ID and registers it in the
	 * {@link #SESSION_REGISTRY} in order to be accessible for other probes in this thread. In case no session is associated with this request (or if the request is
	 * not an instance of {@link HttpServletRequest}), this method returns without any further actions and returns {@link OperationExecutionRecord#NO_SESSION_ID}.
	 * 
	 * @param request
	 *            The request.
	 * 
	 * @return The session ID.
	 */
	protected String registerSessionInformation(final ServletRequest request) {
		String sessionId = OperationExecutionRecord.NO_SESSION_ID;

		if ((request == null) || !(request instanceof HttpServletRequest)) {
			return sessionId;
		}

		final HttpSession session = ((HttpServletRequest) request).getSession(false);
		if (session != null) {
			sessionId = session.getId();
			SESSION_REGISTRY.storeThreadLocalSessionId(sessionId);
		}

		return sessionId;
	}

	public static String USE_CASE_NO_HTTP_REQUEST = "NO-HTTP";

	// TODO: We should use a cache
	private static String extractUseCaseFromRequest(final ServletRequest request) {
		final String useCase = USE_CASE_NO_HTTP_REQUEST;

		if (request instanceof HttpServletRequest) {
			// final String url = ((HttpServletRequest) request).getRequestURL().toString();
			final String queryString = ((HttpServletRequest) request).getQueryString();

			return SessionAndTraceRegistrationFilterSPECjEnterprise.extractFromQueryString(queryString);
		}

		return useCase;
	}

	private static String extractFromQueryString(final String queryString) {
		String useCase = "NOACTION";
		boolean vehicle = false;		
		if (queryString != null) {
			final String[] keyValues = queryString.split("&");
			for (final String keyValue : keyValues) {
				final String[] paramSplit = keyValue.split("=");
				if ("action".equals(paramSplit[0])) {
					useCase = paramSplit[1];
				} 
				if ("vehicles".equals(paramSplit[0])) {
					vehicle = true;
				}
			}
			if ("View_Items".equals(useCase) && vehicle ) {
				useCase = useCase + "_quantity";
			}			
		}
		return useCase;
	}

	// Tests the method extraction
	public static void main(final String[] args) {
		final String testQueryStr = null; // "item=66&action=sellInventory77";
		System.out.println(SessionAndTraceRegistrationFilterSPECjEnterprise.extractFromQueryString(testQueryStr));
	}
}
