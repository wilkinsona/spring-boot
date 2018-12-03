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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.gradle.api.Task;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.tasks.TaskAction;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Temporary helper {@link Task} to keep the bom configuration in {@code build.gradle}
 * aligned with the equivalent configuration in {@code pom.xml} during the Maven to
 * Gradle migration.
 *
 * @author Andy Wilkinson
 */
public class ProcessBom extends AbstractTask {

	@TaskAction
	public void processBom() throws Exception {
		Document bom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(getProject().file("pom.xml"));
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList properties = (NodeList) xpath.evaluate("//properties/*", bom, XPathConstants.NODESET);
		System.out.println("bom {");
		for (int i = 0; i < properties.getLength(); i++) {
			Node property = properties.item(i);
			if (property.getNodeName().endsWith(".version")) {
				System.out.println("    property '" + property.getNodeName() + "', '" + property.getTextContent() + "'");
			}
		}
		NodeList dependencies = (NodeList) xpath.evaluate("//dependencyManagement/dependencies/dependency", bom, XPathConstants.NODESET);
		for (int i = 0; i < dependencies.getLength(); i++) {
			Node dependency = dependencies.item(i);
			String groupId = (String) xpath.evaluate("groupId/text()", dependency, XPathConstants.STRING);
			String artifactId = (String) xpath.evaluate("artifactId/text()", dependency, XPathConstants.STRING);
			String version = (String) xpath.evaluate("version/text()", dependency, XPathConstants.STRING);
			if ("${revision}".equals(version)) {
				version = "'" + getProject().getVersion() + "'";
			}
			else {
				version = "'" + version + "'";
			}
			String scope = (String)xpath.evaluate("scope/text()", dependency, XPathConstants.STRING);
			String type = ("import".equals(scope)) ? "bomImport" : "dependency";
			System.out.println("    " + type + " '" + groupId + "', '" + artifactId + "'," + version);
		}
		System.out.println("}");
	}

}
