package org.example

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import com.diffplug.gradle.spotless.SpotlessExtension

class JavaModulePlugin implements Plugin<Project> {
    void apply(Project project) {
        project.pluginManager.apply('java-library')
        project.pluginManager.apply('eclipse')
        project.pluginManager.apply('com.diffplug.spotless')

        project.extensions.getByType(JavaPluginExtension).toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }

        project.tasks.withType(JavaCompile).configureEach {
            options.encoding = 'UTF-8'
        }

        project.dependencies {
            testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
        }

        project.tasks.withType(Test).configureEach {
            useJUnitPlatform()
        }

        project.extensions.getByType(SpotlessExtension).java {
            googleJavaFormat()
        }
    }
}
