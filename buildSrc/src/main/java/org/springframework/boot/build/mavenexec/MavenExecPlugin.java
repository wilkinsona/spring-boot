/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.build.mavenexec;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;

/**
 * Gradle {@link org.gradle.api.Plugin} that executes configured Maven goals.
 *
 * @author Cédric Champeau
 * @author Eric Wendelin
 */
public class MavenExecPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		MavenExtension extension = project.getExtensions().create("maven",
				MavenExtension.class);

		Configuration mavenConfiguration = project.getConfigurations().create("maven",
				(conf) -> {
					conf.setCanBeConsumed(false);
					conf.setCanBeResolved(true);
				});

		project.afterEvaluate((proj) -> {
			proj.getRepositories().ivy((ivyRepo) -> {
				ivyRepo.setUrl(extension.repositoryUrl);
				ivyRepo.patternLayout((layout) -> layout.artifact(
						"[revision]/binaries/[artifact]-[revision]-bin(.[ext])"));
			});
		});

		project.getDependencies().registerTransform((transform) -> {
			transform.getFrom().attribute(Attribute.of("artifactType", String.class),
					"zip");
			transform.getTo().attribute(Attribute.of("artifactType", String.class),
					"exploded");
			transform.artifactTransform(ExplodeZip.class);
		});

		project.afterEvaluate(
				(proj) -> mavenConfiguration.getDependencies().add(proj.getDependencies()
						.create("maven:apache-maven:" + extension.version + "@zip")));
	}

}
