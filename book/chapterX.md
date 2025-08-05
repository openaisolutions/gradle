## Part V – Advanced Troubleshooting & Real-World Q&A

### Chapter X: Forum-Style Deep Dives

#### 1. PKIX Errors with GitLab over HTTPS

> **Q (StackOverflow style):**
> When Clone/Pull in Eclipse fails with
> `SunCertPathBuilderException: unable to find valid certification path to requested target`
> against my self-hosted GitLab, how do I configure Eclipse and my JVM to trust the certificate?
>
> **A (accepted):**
>
> 1. **Export** your GitLab server’s X.509 cert:
>
>    ```bash
>    openssl s_client -showcerts -connect gitlab.mycompany.com:443 </dev/null \
>      | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > gitlab.crt
>    ```
> 2. **Import** into your JDK’s cacerts (default password `changeit`):
>
>    ```bash
>    keytool -import \
>      -alias gitlab.mycompany.com \
>      -file gitlab.crt \
>      -keystore $JAVA_HOME/jre/lib/security/cacerts \
>      -storepass changeit \
>      -noprompt
>    ```
> 3. **Point Eclipse to the same truststore** via `eclipse.ini`:
>
>    ```ini
>    -vmargs
>    -Djavax.net.ssl.trustStore=${env_var:JAVA_HOME}/jre/lib/security/cacerts
>    -Djavax.net.ssl.trustStorePassword=changeit
>    ```
>
> Now EGit’s HTTPS transport and any embedded JGit calls will succeed.

#### 2. Gradle Configurations & Cross-Project SourceSets

> **Q:**
> We have 50+ subprojects sharing filtered `sourceSets`. Using
>
> ```groovy
> implementation project(':common')
> ```
>
> yields “class XYZ not found” errors in unrelated modules. Switching to
>
> ```groovy
> compileOnly 'org.example:lib:1.2.3'
> runtimeOnly project(':common')
> ```
>
> “works” at runtime but confuses the team. What’s happening?
>
> **A:**
>
> * `implementation project(':common')` places `:common`’s API on compile- and runtime-classpaths **of the depending project only**; it does **not** expose `:common`’s own `compileOnly` dependencies.
> * If `:common` has a `compileOnly` jar A that it **uses** but does **not** include in its own `api` configuration, downstream projects won’t see A → “class XYZ missing.”
>
> **Real-World Fix:**
>
> ```groovy
> // In common/build.gradle
> plugins { id 'java-library' }
> dependencies {
>   api     'org.example:shared-api:2.0'        // expose at compile-time
>   implementation 'org.example:internal-util:3.1'
>   compileOnly    'org.thirdparty:optional:4.5'
> }
>
> // In consumer/build.gradle
> dependencies {
>   implementation project(path: ':common', configuration: 'api')
>   implementation project(':common')  // includes implementation too
> }
> ```
>
> * Use the **java-library** plugin so you get separate `api` vs `implementation`.
> * Depend on `project(':common', configuration: 'api')` if you *only* want its API.
>
> This enforces **MECE** boundaries between compile vs runtime vs optional code.

#### 3. Embedding POMs in JARs & Minimum POM Generation

> **Q:**
> We use the “minimum POM” snippet to publish, but the POM has **no dependencies** and isn’t packaged inside the JAR.
>
> **A:**
> Switch to `maven-publish` and hook the generated POM into your JAR:
>
> ```groovy
> plugins { id 'maven-publish' }
> publishing {
>   publications {
>     mavenJava(MavenPublication) {
>       from components.java
>       pom {
>         withXml {
>           def root = asNode()
>           dependencies.each { dep ->
>             root.appendNode('dependency').with {
>               appendNode('groupId', dep.group)
>               appendNode('artifactId', dep.name)
>               appendNode('version', dep.version)
>               appendNode('scope', dep.configuration)
>             }
>           }
>         }
>       }
>     }
>   }
> }
>
> // Bundle POM in META-INF/maven/… inside the JAR
> tasks.register('embedPom', Copy) {
>   from(publishing.publications.mavenJava.artifactId.map { "${it}.pom" })
>   into("${buildDir}/libs/META-INF/maven/${group}/${archivesBaseName}")
> }
> tasks.named('jar') {
>   dependsOn 'embedPom'
>   from("${buildDir}/libs/META-INF") { into 'META-INF' }
> }
> ```

#### 4. Taming Buildship’s Linked Sources

> **Q:**
> Buildship adds a second “src/java” linked source in Eclipse on top of `src/main/java`, developers hate it.
>
> **A:**
> Customize the Eclipse classpath merging to drop unwanted entries:
>
> ```groovy
> eclipse {
>   classpath {
>     file {
>       whenMerged { cp ->
>         cp.entries.removeAll { entry ->
>           entry.kind.name() == 'src' && entry.path.endsWith('/src/java')
>         }
>       }
>     }
>   }
> }
> ```
>
> Now `Refresh Gradle Project` will keep only `src/main/java` (and any other you explicitly declare).

#### 5. Architecting Three-Level Subprojects

> **Q:**
> We tried a 3-level hierarchy (`:api`, `:api:core`, `:api:core:impl`), but `:api:core` won’t compile—its plugin-settings and conventions don’t “inherit.”
>
> **A:**
>
> 1. **Include** every module in `settings.gradle`:
>
>    ```groovy
>    include 'api', 'api:core', 'api:core:impl'
>    ```
> 2. **Apply** your shared conventions to *all* levels:
>
>    ```groovy
>    // in buildSrc or root build.gradle
>    subprojects {
>      apply plugin: 'java-library'
>      group = 'com.example'
>      version = '1.0.0'
>      repositories { mavenCentral() }
>      // …common dependency configurations…
>    }
>    ```
> 3. **Verify** that `api:core` has its own `build.gradle` applying the `java-library` plugin (or inherits it via `subprojects {}`).
>
> Without those two pieces—**explicit include** and **shared plugin application**—middle modules remain “blank” and can’t compile.

### Generating a Companion Jupyter Notebook via GitHub

You can automate creation of a notebook containing all of the above code snippets:

1. **Create** `.github/workflows/notebook.yml`:

   ```yaml
   name: Generate Troubleshooting Notebook
   on:
     push:
       paths:
         - "chapterX/**"
   jobs:
     build:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v3
         - name: Install Python
           uses: actions/setup-python@v5
           with:
             python-version: '3.x'
         - name: Install nbformat
           run: pip install nbformat
         - name: Generate Notebook
           run: |
             python chapterX/gen_notebook.py \
               --output notebooks/Advanced_Troubleshooting.ipynb
         - name: Commit & Push
           run: |
             git config user.name "github-actions"
             git config user.email "actions@github.com"
             git add notebooks/Advanced_Troubleshooting.ipynb
             git commit -m "Auto-generate troubleshooting notebook"
             git push
   ```

2. **Write** `chapterX/gen_notebook.py` using `nbformat`:

   ```python
   import nbformat as nbf
   import argparse

   snippets = [
     ("PKIX Errors", """# PKIX Error Fix...\n<bash and ini examples>\n"""),
     ("Gradle Configs", """# implementation vs api...\n<Gradle Groovy DSL>\n"""),
     # …and so on for each section…
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
   ```

With that in place, every push to `chapterX/` automates a fresh notebook demonstrating each forum-style solution in a ready-to-run format.

### Next Steps

1. Slot **Chapter X** into your ToC and fill out any additional real-world issues.
2. Adjust your root `settings.gradle` and shared `subprojects {}` blocks to support nested modules.
3. Commit the GitHub Action and generator script to start producing the Jupyter notebook automatically.

This approach gives you:

* A clear, advanced troubleshooting chapter.
* Forum-style Q&A that your engineers will recognize.
* Automated, runnable notebooks for hands-on learning.
