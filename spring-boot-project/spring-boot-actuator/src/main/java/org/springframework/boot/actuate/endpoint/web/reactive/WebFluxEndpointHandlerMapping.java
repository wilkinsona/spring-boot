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

package org.springframework.boot.actuate.endpoint.web.reactive;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.EndpointAccessFilter;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.Link;
import org.springframework.boot.actuate.endpoint.web.reactive.WebFluxEndpointHandlerMapping.WebFluxEndpointHandlerMappingRuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A custom {@link HandlerMapping} that makes web endpoints available over HTTP using
 * Spring WebFlux.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Brian Clozel
 * @since 2.0.0
 */
@ImportRuntimeHints(WebFluxEndpointHandlerMappingRuntimeHints.class)
public class WebFluxEndpointHandlerMapping extends AbstractWebFluxEndpointHandlerMapping implements InitializingBean {

	private final EndpointLinksResolver linksResolver;

	/**
	 * Creates a new {@code WebFluxEndpointHandlerMapping} instance that provides mappings
	 * for the given endpoints.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param endpoints the web endpoints
	 * @param endpointMediaTypes media types consumed and produced by the endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints or {@code null}
	 * @param linksResolver resolver for determining links to available endpoints
	 * @param shouldRegisterLinksMapping whether the links endpoint should be registered
	 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of
	 * {@link #WebFluxEndpointHandlerMapping(EndpointMapping, Collection, Collection, EndpointMediaTypes, CorsConfiguration, EndpointLinksResolver, boolean)}
	 */
	@Deprecated(since = "3.4.0", forRemoval = true)
	public WebFluxEndpointHandlerMapping(EndpointMapping endpointMapping, Collection<ExposableWebEndpoint> endpoints,
			EndpointMediaTypes endpointMediaTypes, CorsConfiguration corsConfiguration,
			EndpointLinksResolver linksResolver, boolean shouldRegisterLinksMapping) {
		this(endpointMapping, endpoints, Collections.emptyList(), endpointMediaTypes, corsConfiguration, linksResolver,
				shouldRegisterLinksMapping);
	}

	/**
	 * Creates a new {@code WebFluxEndpointHandlerMapping} instance that provides mappings
	 * for the given endpoints.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param endpoints the web endpoints
	 * @param accessFilters filters that restrict access to the endpoints and their
	 * operations
	 * @param endpointMediaTypes media types consumed and produced by the endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints or {@code null}
	 * @param linksResolver resolver for determining links to available endpoints
	 * @param shouldRegisterLinksMapping whether the links endpoint should be registered
	 * @since 3.4.0
	 */
	public WebFluxEndpointHandlerMapping(EndpointMapping endpointMapping, Collection<ExposableWebEndpoint> endpoints,
			Collection<EndpointAccessFilter> accessFilters, EndpointMediaTypes endpointMediaTypes,
			CorsConfiguration corsConfiguration, EndpointLinksResolver linksResolver,
			boolean shouldRegisterLinksMapping) {
		super(endpointMapping, endpoints, accessFilters, endpointMediaTypes, corsConfiguration,
				shouldRegisterLinksMapping);
		this.linksResolver = linksResolver;
		setOrder(-100);
	}

	@Override
	protected LinksHandler getLinksHandler() {
		return new WebFluxLinksHandler();
	}

	/**
	 * Handler for root endpoint providing links.
	 */
	class WebFluxLinksHandler implements LinksHandler {

		@Override
		@ResponseBody
		@Reflective
		public Mono<Map<String, Map<String, Link>>> links(ServerWebExchange exchange) {
			String requestUri = UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
				.replaceQuery(null)
				.toUriString();
			return getSecurityContext()
				.map((securityContext) -> OperationResponseBody.of(Collections.singletonMap("_links",
						WebFluxEndpointHandlerMapping.this.linksResolver.resolveLinks(requestUri, securityContext))));
		}

		@Override
		public String toString() {
			return "Actuator root web endpoint";
		}

		private Mono<? extends SecurityContext> getSecurityContext() {
			if (ClassUtils.isPresent("org.springframework.security.core.context.ReactiveSecurityContextHolder",
					getClass().getClassLoader())) {
				return SpringSecuritySecurityContextProvider.securityContext();
			}
			return Mono.just(SecurityContext.NONE);
		}

		static class SpringSecuritySecurityContextProvider {

			static Mono<? extends SecurityContext> securityContext() {
				return ReactiveSecurityContextHolder.getContext()
					.map((securityContext) -> (SecurityContext) new ReactiveSecurityContext(
							securityContext.getAuthentication()))
					.switchIfEmpty(Mono.just(SecurityContext.NONE));
			}

		}

	}

	static class WebFluxEndpointHandlerMappingRuntimeHints implements RuntimeHintsRegistrar {

		private final ReflectiveRuntimeHintsRegistrar reflectiveRegistrar = new ReflectiveRuntimeHintsRegistrar();

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			this.reflectiveRegistrar.registerRuntimeHints(hints, WebFluxLinksHandler.class);
			this.bindingRegistrar.registerReflectionHints(hints.reflection(), Link.class);
		}

	}

}
