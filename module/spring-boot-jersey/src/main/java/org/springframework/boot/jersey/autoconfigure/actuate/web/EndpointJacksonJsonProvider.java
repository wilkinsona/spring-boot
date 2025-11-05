/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jersey.autoconfigure.actuate.web;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.util.Assert;

/**
 * Jakarta RS {@link MessageBodyWriter} to support actuator endpoint serialization with
 * Jackson 3.
 *
 * @author Phillip Webb
 */
@Provider
@Produces({ MediaType.APPLICATION_JSON, "text/json", MediaType.WILDCARD })
@Priority(0)
class EndpointJacksonJsonProvider implements MessageBodyWriter<Object> {

	private final Providers providers;

	EndpointJacksonJsonProvider(@Context Providers providers) {
		this.providers = providers;
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return OperationResponseBody.class.isAssignableFrom(type) && getJsonMapper(type, mediaType) != null;
	}

	@Override
	public void writeTo(Object value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
			throws IOException, WebApplicationException {
		JsonMapper jsonMapper = getJsonMapper(type, mediaType);
		Assert.state(jsonMapper != null, "No JsonMapper found");
		jsonMapper.writer().writeValue(entityStream, value);
	}

	private @Nullable JsonMapper getJsonMapper(Class<?> type, MediaType mediaType) {
		ContextResolver<JsonMapper> resolver = this.providers.getContextResolver(JsonMapper.class, mediaType);
		return resolver.getContext(type);
	}

}
