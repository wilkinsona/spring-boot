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

package org.springframework.boot.build.bom;

import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.component.SoftwareComponent;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

import groovy.util.Node;
import groovy.xml.QName;

public class BomPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		PluginContainer plugins = project.getPlugins();
		plugins.apply(MavenPublishPlugin.class);
		plugins.apply(JavaLibraryPlugin.class);
		project.getTasks().create("processBom", ProcessBom.class);
		BomExtension bom = new BomExtension(project.getDependencies());
		project.getExtensions().add("bom", bom);
		new PublishingCustomizer(project, bom).customize();
	}

	private static final class PublishingCustomizer {

		private final Project project;

		private final BomExtension bom;

		private PublishingCustomizer(Project project, BomExtension bom) {
			this.project = project;
			this.bom = bom;
		}

		private void customize() {
			PublishingExtension publishing = this.project.getExtensions().getByType(PublishingExtension.class);
			publishing.getPublications().create("bom", MavenPublication.class, this::configurePublication);
		}

		private void configurePublication(MavenPublication publication) {
			SoftwareComponent javaLibrary = project.getComponents().getByName("javaLibraryPlatform");
			publication.from(javaLibrary);
			publication.pom(this::customizePom);
		}

		@SuppressWarnings("unchecked")
		private void customizePom(MavenPom pom) {
			pom.withXml((xml) -> {
				Node projectNode = xml.asNode();
				Node properties = new Node(null, "properties");
				this.bom.getProperties().forEach(properties::appendNode);
				projectNode.children().add(5, properties);
				Node dependencyManagement = findChild(projectNode, "dependencyManagement");
				Node dependencies = findChild(dependencyManagement, "dependencies");
				for (Node dependency: findChildren(dependencies, "dependency")) {
						String groupId = findChild(dependency, "groupId").text();
						String artifactId = findChild(dependency, "artifactId").text();
						findChild(dependency, "version").setValue(bom.getVersion(groupId, artifactId));
				}
			});
		}

		private Node findChild(Node parent, String name) {
			for (Object child: parent.children()) {
				if (child instanceof Node) {
					Node node = (Node) child;
					if ((node.name() instanceof QName) && name.equals(((QName)node.name()).getLocalPart())) {
						return node;
					}
					if (name.equals(node.name())) {
						return node;
					}
				}
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		private List<Node> findChildren(Node parent, String name) {
			return (List<Node>) parent.children().stream().filter((child) -> isNodeWithName(child, name)).collect(Collectors.toList());

		}

		private boolean isNodeWithName(Object candidate, String name) {
			if (candidate instanceof Node) {
				Node node = (Node) candidate;
				if ((node.name() instanceof QName) && name.equals(((QName)node.name()).getLocalPart())) {
					return true;
				}
				if (name.equals(node.name())) {
					return true;
				}
			}
			return false;
		}

	}

}
