/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.build.context.properties;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * {@link Task} to preserve {@code spring-configuration-metadata.json} so that previously
 * generated metadata is available during incremental annotation processing.
 *
 * @author Andy Wilkinson
 */
public class PreserveSpringConfigurationMetadata extends DefaultTask {

	private final RegularFileProperty metadataLocation = getProject().getObjects().fileProperty();

	private final DirectoryProperty outputDirectory = getProject().getObjects().directoryProperty();

	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public RegularFileProperty getMetadataLocation() {
		return this.metadataLocation;
	}

	@OutputDirectory
	public DirectoryProperty getOutputDirectory() {
		return this.outputDirectory;
	}

	@TaskAction
	void preserveMetadata() {
		getProject().sync((copySpec) -> {
			copySpec.from(this.metadataLocation);
			copySpec.into(this.outputDirectory);
		});
	}

}
