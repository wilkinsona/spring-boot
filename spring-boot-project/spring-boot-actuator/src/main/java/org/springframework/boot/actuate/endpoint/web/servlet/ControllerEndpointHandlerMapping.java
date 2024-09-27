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

package org.springframework.boot.actuate.endpoint.web.servlet;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.EndpointAccessDeniedException;
import org.springframework.boot.actuate.endpoint.web.EndpointAccessFilter;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.annotation.ExposableControllerEndpoint;
import org.springframework.util.Assert;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

/**
 * {@link HandlerMapping} that exposes
 * {@link org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint @ControllerEndpoint}
 * and
 * {@link org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint @RestControllerEndpoint}
 * annotated endpoints over Spring MVC.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@SuppressWarnings("removal")
public class ControllerEndpointHandlerMapping extends RequestMappingHandlerMapping {

	private final EndpointMapping endpointMapping;

	private final CorsConfiguration corsConfiguration;

	private final Map<Object, ExposableControllerEndpoint> handlers;

	private final Collection<EndpointAccessFilter> accessFilters;

	/**
	 * Create a new {@link ControllerEndpointHandlerMapping} instance providing mappings
	 * for the specified endpoints.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param endpoints the web endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints or {@code null}
	 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of
	 * {@link #ControllerEndpointHandlerMapping(EndpointMapping, Collection, Collection, CorsConfiguration)}
	 */
	@Deprecated(since = "3.4.0", forRemoval = true)
	public ControllerEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<ExposableControllerEndpoint> endpoints, CorsConfiguration corsConfiguration) {
		this(endpointMapping, endpoints, Collections.emptyList(), corsConfiguration);
	}

	/**
	 * Create a new {@link ControllerEndpointHandlerMapping} instance providing mappings
	 * for the specified endpoints.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param endpoints the web endpoints
	 * @param accessFilters filters the control access to the endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints or {@code null}
	 */
	public ControllerEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<ExposableControllerEndpoint> endpoints, Collection<EndpointAccessFilter> accessFilters,
			CorsConfiguration corsConfiguration) {
		Assert.notNull(endpointMapping, "EndpointMapping must not be null");
		Assert.notNull(endpoints, "Endpoints must not be null");
		this.endpointMapping = endpointMapping;
		this.handlers = getHandlers(endpoints);
		this.accessFilters = accessFilters;
		this.corsConfiguration = corsConfiguration;
		setOrder(-100);
	}

	private Map<Object, ExposableControllerEndpoint> getHandlers(Collection<ExposableControllerEndpoint> endpoints) {
		Map<Object, ExposableControllerEndpoint> handlers = new LinkedHashMap<>();
		endpoints.forEach((endpoint) -> handlers.put(endpoint.getController(), endpoint));
		return Collections.unmodifiableMap(handlers);
	}

	@Override
	protected void initHandlerMethods() {
		this.handlers.keySet().forEach(this::detectHandlerMethods);
	}

	@Override
	protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
		ExposableControllerEndpoint endpoint = this.handlers.get(handler);
		mapping = withEndpointMappedPatterns(endpoint, mapping);
		super.registerHandlerMethod(handler, method, mapping);
	}

	private RequestMappingInfo withEndpointMappedPatterns(ExposableControllerEndpoint endpoint,
			RequestMappingInfo mapping) {
		Set<PathPattern> patterns = mapping.getPathPatternsCondition().getPatterns();
		if (patterns.isEmpty()) {
			patterns = Collections.singleton(getPatternParser().parse(""));
		}
		String[] endpointMappedPatterns = patterns.stream()
			.map((pattern) -> getEndpointMappedPattern(endpoint, pattern))
			.toArray(String[]::new);
		return mapping.mutate().paths(endpointMappedPatterns).build();
	}

	private String getEndpointMappedPattern(ExposableControllerEndpoint endpoint, PathPattern pattern) {
		return this.endpointMapping.createSubPath(endpoint.getRootPath() + pattern);
	}

	@Override
	protected boolean hasCorsConfigurationSource(Object handler) {
		return this.corsConfiguration != null;
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mapping) {
		return this.corsConfiguration;
	}

	@Override
	protected void extendInterceptors(List<Object> interceptors) {
		interceptors.add(new SkipPathExtensionContentNegotiation());
		interceptors.add(new EndpointAccessHandlerInterceptor(this.handlers, this.accessFilters));
	}

	private static final class EndpointAccessHandlerInterceptor implements HandlerInterceptor {

		private final Map<Object, ExposableControllerEndpoint> handlers;

		private final Collection<EndpointAccessFilter> accessFilters;

		private EndpointAccessHandlerInterceptor(Map<Object, ExposableControllerEndpoint> handlers,
				Collection<EndpointAccessFilter> accessFilters) {
			this.handlers = handlers;
			this.accessFilters = accessFilters;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
				throws Exception {
			if (accessAllowed(request, handler)) {
				return true;
			}
			throw new EndpointAccessDeniedException();
		}

		private boolean accessAllowed(HttpServletRequest request, Object handler) {
			if (handler instanceof HandlerMethod handlerMethod) {
				ExposableControllerEndpoint endpoint = this.handlers.get(handlerMethod.getBean());
				if (endpoint != null) {
					return accessAllowed(new ServletSecurityContext(request), endpoint.getEndpointId());
				}
			}
			return false;
		}

		private boolean accessAllowed(SecurityContext securityContext, EndpointId endpointId) {
			for (EndpointAccessFilter accessFilter : this.accessFilters) {
				if (!accessFilter.allow(securityContext, endpointId, null)) {
					return false;
				}
			}
			return true;
		}

	}

}
