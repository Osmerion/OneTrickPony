/*
 * Copyright 2025 Leon Linhart
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
gradle.allprojects {
    pluginManager.apply(MavenArtifactBundlePlugin::class)
}

public class MavenArtifactBundlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val localRepo = project.rootProject.layout.buildDirectory.dir("localRepo")

        if (project == project.rootProject) {
            project.tasks.register<Tar>("artifactBundle") {
                dependsOn(project.provider {
                    buildList {
                        val projects = project.subprojects + project

                        for (project in projects) {
                            if (project.pluginManager.hasPlugin("maven-publish")
                            ) {
                                add(project.tasks.named("publishAllPublicationsToIntermediateBundlingRepository"))
                            }
                        }
                    }
                })

                compression = Compression.GZIP

                // TODO Investigate why using conventions is insufficient for KMP projects
                destinationDirectory.set(project.layout.buildDirectory.dir("libs"))
                archiveFileName.set("maven-artifact-bundle.tgz")

                from(localRepo) {
                    exclude("**/maven-metadata.xml")
                    exclude("**/maven-metadata.xml.*")
                }
            }
        }

        project.pluginManager.withPlugin("maven-publish") {
            val publishing = project.extensions.getByType<PublishingExtension>()

            publishing.repositories.maven {
                name = "IntermediateBundling"
                url = project.uri(localRepo)

                metadataSources {
                    mavenPom()
                }
            }
        }
    }

}
