import nbformat as nbf
import argparse

snippets = [
    (
        "PKIX Errors",
        """# PKIX Errors with GitLab over HTTPS
openssl s_client -showcerts -connect gitlab.mycompany.com:443 </dev/null \
  | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > gitlab.crt

keytool -import \
  -alias gitlab.mycompany.com \
  -file gitlab.crt \
  -keystore $JAVA_HOME/jre/lib/security/cacerts \
  -storepass changeit \
  -noprompt

# eclipse.ini additions
-vmargs
-Djavax.net.ssl.trustStore=${env_var:JAVA_HOME}/jre/lib/security/cacerts
-Djavax.net.ssl.trustStorePassword=changeit
"""
    ),
    (
        "Gradle Configs",
        """// common/build.gradle
plugins { id 'java-library' }
dependencies {
  api     'org.example:shared-api:2.0'
  implementation 'org.example:internal-util:3.1'
  compileOnly    'org.thirdparty:optional:4.5'
}

// consumer/build.gradle
dependencies {
  implementation project(path: ':common', configuration: 'api')
  implementation project(':common')
}
"""
    ),
    (
        "Embedding POMs",
        """plugins { id 'maven-publish' }
publishing {
  publications {
    mavenJava(MavenPublication) {
      from components.java
      pom {
        withXml {
          def root = asNode()
          dependencies.each { dep ->
            root.appendNode('dependency').with {
              appendNode('groupId', dep.group)
              appendNode('artifactId', dep.name)
              appendNode('version', dep.version)
              appendNode('scope', dep.configuration)
            }
          }
        }
      }
    }
  }
}
tasks.register('embedPom', Copy) {
  from(publishing.publications.mavenJava.artifactId.map { "${it}.pom" })
  into("${buildDir}/libs/META-INF/maven/${group}/${archivesBaseName}")
}
tasks.named('jar') {
  dependsOn 'embedPom'
  from("${buildDir}/libs/META-INF") { into 'META-INF' }
}
"""
    ),
    (
        "Buildship Sources",
        """eclipse {
  classpath {
    file {
      whenMerged { cp ->
        cp.entries.removeAll { entry ->
          entry.kind.name() == 'src' && entry.path.endsWith('/src/java')
        }
      }
    }
  }
}
"""
    ),
    (
        "Three-Level Subprojects",
        """// settings.gradle
include 'api', 'api:core', 'api:core:impl'

// root build.gradle or buildSrc
subprojects {
  apply plugin: 'java-library'
  group = 'com.example'
  version = '1.0.0'
  repositories { mavenCentral() }
}

// api/core/build.gradle
plugins { id 'java-library' }
"""
    ),
]

def main(output):
    nb = nbf.v4.new_notebook()
    nb.cells = []
    for title, code in snippets:
        nb.cells.append(nbf.v4.new_markdown_cell(f"## {title}"))
        nb.cells.append(nbf.v4.new_code_cell(code))
    nbf.write(nb, output)

if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--output", required=True)
    args = p.parse_args()
    main(args.output)
