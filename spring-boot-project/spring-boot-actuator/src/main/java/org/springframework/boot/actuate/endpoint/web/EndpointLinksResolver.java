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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.SecurityContext;

/**
 * A resolver for {@link Link links} to web endpoints.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class EndpointLinksResolver {

	private static final Log logger = LogFactory.getLog(EndpointLinksResolver.class);

	private final Collection<? extends ExposableEndpoint<?>> endpoints;

	private final Collection<EndpointAccessFilter> accessFilters;

	/**
	 * Creates a new {@code EndpointLinksResolver} that will resolve links to the given
	 * {@code endpoints}.
	 * @param endpoints the endpoints
	 */
	public EndpointLinksResolver(Collection<? extends ExposableEndpoint<?>> endpoints) {
		this.endpoints = endpoints;
		this.accessFilters = Collections.emptyList();
	}

	/**
	 * Creates a new {@code EndpointLinksResolver} that will resolve links to the given
	 * {@code endpoints} that are exposed beneath the given {@code basePath}.
	 * @param endpoints the endpoints
	 * @param basePath the basePath
	 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of
	 * {@link #EndpointLinksResolver(Collection, Collection, String)}
	 */
	@Deprecated(since = "3.4.0", forRemoval = true)
	public EndpointLinksResolver(Collection<? extends ExposableEndpoint<?>> endpoints, String basePath) {
		this(endpoints, Collections.emptyList(), basePath);
	}

	/**
	 * Creates a new {@code EndpointLinksResolver} that will resolve links to the given
	 * {@code endpoints} that are exposed beneath the given {@code basePath}.
	 * @param endpoints the endpoints
	 * @param accessFilters filters the restrict access to the endpoints' operations
	 * @param basePath the basePath
	 */
	public EndpointLinksResolver(Collection<? extends ExposableEndpoint<?>> endpoints,
			Collection<EndpointAccessFilter> accessFilters, String basePath) {
		this.endpoints = endpoints;
		this.accessFilters = accessFilters;
		if (logger.isInfoEnabled()) {
			String suffix = (endpoints.size() == 1) ? "" : "s";
			logger
				.info("Exposing " + endpoints.size() + " endpoint" + suffix + " beneath base path '" + basePath + "'");
		}
	}

	/**
	 * Resolves links to the known endpoints based on a request with the given
	 * {@code requestUrl}.
	 * @param requestUrl the url of the request for the endpoint links
	 * @return the links
	 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of
	 * {@link #resolveLinks(String, SecurityContext)}
	 */
	@Deprecated(since = "3.4.0", forRemoval = true)
	public Map<String, Link> resolveLinks(String requestUrl) {
		return resolveLinks(requestUrl, SecurityContext.NONE);
	}

	/**
	 * Resolves links to the known endpoints based on a request with the given
	 * {@code requestUrl}.
	 * @param requestUrl the url of the request for the endpoint links
	 * @param securityContext the security context of the request for the endpoint links
	 * @return the links
	 * @since 3.4.0
	 */
	public Map<String, Link> resolveLinks(String requestUrl, SecurityContext securityContext) {
		String normalizedUrl = normalizeRequestUrl(requestUrl);
		Map<String, Link> links = new LinkedHashMap<>();
		links.put("self", new Link(normalizedUrl));
		for (ExposableEndpoint<?> endpoint : this.endpoints) {
			if (endpoint instanceof ExposableWebEndpoint exposableWebEndpoint) {
				collectLinks(links, securityContext, exposableWebEndpoint, normalizedUrl);
			}
			else if (endpoint instanceof PathMappedEndpoint pathMappedEndpoint
					&& permitAccess(securityContext, endpoint.getEndpointId(), null)) {
				String rootPath = pathMappedEndpoint.getRootPath();
				Link link = createLink(normalizedUrl, rootPath);
				links.put(endpoint.getEndpointId().toLowerCaseString(), link);
			}
		}
		return links;
	}

	private String normalizeRequestUrl(String requestUrl) {
		if (requestUrl.endsWith("/")) {
			return requestUrl.substring(0, requestUrl.length() - 1);
		}
		return requestUrl;
	}

	private void collectLinks(Map<String, Link> links, SecurityContext securityContext, ExposableWebEndpoint endpoint,
			String normalizedUrl) {
		for (WebOperation operation : endpoint.getOperations()) {
			if (permitAccess(securityContext, endpoint.getEndpointId(), operation)) {
				links.put(operation.getId(), createLink(normalizedUrl, operation));
			}
		}
	}

	private boolean permitAccess(SecurityContext securityContext, EndpointId endpointId, WebOperation operation) {
		for (EndpointAccessFilter accessFilter : this.accessFilters) {
			if (!accessFilter.allow(securityContext, endpointId, operation)) {
				return false;
			}
		}
		return true;
	}

	private Link createLink(String requestUrl, WebOperation operation) {
		return createLink(requestUrl, operation.getRequestPredicate().getPath());
	}

	private Link createLink(String requestUrl, String path) {
		return new Link(requestUrl + (path.startsWith("/") ? path : "/" + path));
	}

}
