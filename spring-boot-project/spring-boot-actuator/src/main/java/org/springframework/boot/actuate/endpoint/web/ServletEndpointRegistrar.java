/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint.web;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.servlet.ServletSecurityContext;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link ServletContextInitializer} to register {@link ExposableServletEndpoint servlet
 * endpoints}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 * @deprecated since 3.3.0 in favor of {@code @Endpoint} and {@code @WebEndpoint} support
 */
@Deprecated(since = "3.3.0", forRemoval = true)
@SuppressWarnings("removal")
public class ServletEndpointRegistrar implements ServletContextInitializer {

	private static final Log logger = LogFactory.getLog(ServletEndpointRegistrar.class);

	private final String basePath;

	private final Collection<ExposableServletEndpoint> servletEndpoints;

	private final Collection<EndpointAccessFilter> accessFilters;

	public ServletEndpointRegistrar(String basePath, Collection<ExposableServletEndpoint> servletEndpoints) {
		this(basePath, servletEndpoints, Collections.emptyList());
	}

	public ServletEndpointRegistrar(String basePath, Collection<ExposableServletEndpoint> servletEndpoints,
			Collection<EndpointAccessFilter> accessFilters) {
		Assert.notNull(servletEndpoints, "ServletEndpoints must not be null");
		Assert.notNull(servletEndpoints, "AccessFilters must not be null");
		this.basePath = cleanBasePath(basePath);
		this.servletEndpoints = servletEndpoints;
		this.accessFilters = accessFilters;
	}

	private static String cleanBasePath(String basePath) {
		if (StringUtils.hasText(basePath) && basePath.endsWith("/")) {
			return basePath.substring(0, basePath.length() - 1);
		}
		return (basePath != null) ? basePath : "";
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		this.servletEndpoints.forEach((servletEndpoint) -> register(servletContext, servletEndpoint));
	}

	private void register(ServletContext servletContext, ExposableServletEndpoint endpoint) {
		String name = endpoint.getEndpointId().toLowerCaseString() + "-actuator-endpoint";
		String path = this.basePath + "/" + endpoint.getRootPath();
		String urlMapping = path.endsWith("/") ? path + "*" : path + "/*";
		EndpointServlet endpointServlet = endpoint.getEndpointServlet();
		Dynamic registration = servletContext.addServlet(name, endpointServlet.getServlet());
		registration.addMapping(urlMapping);
		registration.setInitParameters(endpointServlet.getInitParameters());
		registration.setLoadOnStartup(endpointServlet.getLoadOnStartup());
		logger.info("Registered '" + path + "' to " + name);
		servletContext
			.addFilter(name + "-access-filter", new AccessFilter(this.accessFilters, endpoint.getEndpointId()))
			.addMappingForServletNames(EnumSet.allOf(DispatcherType.class), false, name);
	}

	private static final class AccessFilter implements Filter {

		private final Collection<EndpointAccessFilter> accessFilters;

		private final EndpointId endpointId;

		private AccessFilter(Collection<EndpointAccessFilter> accessFilters, EndpointId endpointId) {
			this.accessFilters = accessFilters;
			this.endpointId = endpointId;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			if (request instanceof HttpServletRequest httpRequest && permitAccess(httpRequest)) {
				chain.doFilter(httpRequest, response);
			}
			else if (response instanceof HttpServletResponse httpResponse) {
				httpResponse.sendError(HttpStatus.UNAUTHORIZED.value());
			}
		}

		private boolean permitAccess(HttpServletRequest request) {
			SecurityContext securityContext = new ServletSecurityContext(request);
			for (EndpointAccessFilter accessFilter : this.accessFilters) {
				if (!accessFilter.allow(securityContext, this.endpointId, null)) {
					return false;
				}
			}
			return true;
		}

	}

}
