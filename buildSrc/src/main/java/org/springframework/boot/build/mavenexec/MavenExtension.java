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

/**
 * Gradle extension used to configure the MavenExec plugin.
 *
 * @author Cédric Champeau
 * @author Eric Wendelin
 */
public class MavenExtension {

	String version = "3.6.0";

	// TODO: See if Maven Central will work: https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.6.0/apache-maven-3.6.0-bin.zip
	String repositoryUrl = "http://mirrors.standaloneinstaller.com/apache/maven/maven-3/";

	public MavenExtension() {
	}

	public MavenExtension(String mavenVersion, String repositoryUrl) {
		this.version = mavenVersion;
		this.repositoryUrl = repositoryUrl;
	}

}
