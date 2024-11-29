/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.build.deprecation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Task to check that there are no deprecated APIs whose removal is overdue.
 *
 * @author Andy Wilkinson
 */
public abstract class CheckDeprecatedApisForOverdueRemoval extends DefaultTask {

	@Input
	public abstract Property<String> getVersion();

	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract RegularFileProperty getReportFile();

	@TaskAction
	void execute() throws StreamReadException, DatabindException, IOException {
		File reportFile = getReportFile().getAsFile().get();
		if (!reportFile.exists()) {
			return;
		}
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new ParameterNamesModule());
		DeprecationReport deprecationReport = objectMapper.readValue(reportFile, DeprecationReport.class);
		List<TypeDeprecation> overdueTypes = removalIsOverdue(deprecationReport.getTypes());
		List<FieldDeprecation> overdueFields = removalIsOverdue(deprecationReport.getFields());
		List<ConstructorDeprecation> overdueConstructors = removalIsOverdue(deprecationReport.getConstructors());
		List<MethodDeprecation> overdueMethods = removalIsOverdue(deprecationReport.getMethods());
		if (!overdueTypes.isEmpty() || !overdueFields.isEmpty() || !overdueConstructors.isEmpty()
				|| !overdueMethods.isEmpty()) {
			throw new GradleException(String.format("Deprecated API removal overdue:%n%s%n%s%n%s%n%s",
					display("Types", overdueTypes), display("Fields", overdueFields),
					display("Constructors", overdueConstructors), display("Methods", overdueMethods)));
		}
	}

	private String display(String title, List<? extends Deprecation> deprecations) {
		Stream<String> items = deprecations.isEmpty() ? Stream.of("None")
				: deprecations.stream().map(Deprecation::toString);
		return String.format("  %s:%n%s", title,
				items.map((string) -> "    - " + string).collect(Collectors.joining("\n")));
	}

	private <T extends Deprecation> List<T> removalIsOverdue(List<T> deprecations) {
		Version version = this.getVersion().map(Version::from).get();
		return deprecations.stream().filter((deprecation) -> isOverdue(deprecation, version)).toList();
	}

	private boolean isOverdue(Deprecation deprecation, Version version) {
		if ("unknown".equals(deprecation.getRemoval())) {
			return false;
		}
		Version removal = Version.from(deprecation.getRemoval());
		return version.isGreaterThanOrEqualTo(removal);
	}

	private record Version(int major, int minor) {

		static Version from(String version) {
			String[] components = version.split("\\.");
			return new Version(Integer.parseInt(components[0]), Integer.parseInt(components[1]));
		}

		boolean isGreaterThanOrEqualTo(Version other) {
			if (this.major == other.major) {
				return this.minor >= other.minor;
			}
			return this.major > other.major;
		}

	}

}
