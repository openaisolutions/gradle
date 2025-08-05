## Part VII – Maven Lies and Gradle Truths – Generating POMs That Actually Work

A modular Gradle build is worthless if it cannot be consumed by others.  Publishing modules to Maven repositories is how you distribute libraries and applications to the wider world.  However, Gradle and Maven have different models for dependency management.  Gradle’s variant‑aware engine and rich metadata can be flattened into a Maven POM, but this process requires care.  Many teams trip over mismatched scopes, missing dependencies and misleading optional flags.  In this chapter we demystify Gradle’s publishing machinery, show how to map configurations to Maven scopes, customise generated POM files, and verify that published artifacts behave as expected.  By the end you will understand why Gradle’s module metadata is more expressive than Maven’s and how to bridge the gap without lying to your consumers.

### Chapter 8: Maven Lies and Gradle Truths – Generating POMs That Actually Work

#### Section 1: Gradle Module Metadata vs Maven POM

Gradle and Maven describe dependencies differently.  Gradle uses variant‑aware metadata that records each outgoing variant (for example, API and runtime) and the attributes required to select it.  Maven, in contrast, uses a single POM file with scopes like `compile`, `runtime`, `provided` and `test`.  When Gradle publishes to a Maven repository, it generates both Gradle module metadata and a POM.  The POM is a lossy representation: it cannot express variant attributes, capabilities or rich version constraints.  Consequently, when consumers use the POM with Maven, they may see different dependency graphs than Gradle consumers.  The Maven POM should therefore be treated as a compatibility layer, and you should strive to make it as accurate as possible.

#### Section 2: Publishing with the Maven Publish Plug‑in

Gradle’s `maven-publish` plug‑in adds a `publishing` block and tasks for generating and uploading artifacts.  The documentation lists tasks such as `generatePomFileFor<Publication>`, `publish<Publication>PublicationTo<Repository>`, `publishToMavenLocal` and `publish`【800041472511575†L318-L322】.  To use the plug‑in, declare a `MavenPublication` and specify which component to publish:

```groovy
publishing {
    publications {
        mavenJava(MavenPublication) {
            from components.java
        }
    }
    repositories {
        maven {
            url = uri("https://repo.example.com/releases")
        }
    }
}
```

Running `./gradlew publish` will generate the POM and upload the JAR to the configured repository.  You can also publish to the local Maven repository with `./gradlew publishToMavenLocal` for quick testing.

#### Section 3: Mapping Configurations to Maven Scopes

Gradle’s Java library plug‑in introduces `api` and `implementation` configurations.  `api` dependencies are part of the public API and are exported to consumers, while `implementation` dependencies are internal and should not leak outside the module【21875673602686†L325-L361】.  When publishing to Maven, these configurations map to POM scopes: `api` dependencies become `compile` scope and `implementation` dependencies become `runtime` scope.  This mapping reduces the compile classpath for consumers and improves build performance【21875673602686†L349-L361】.  Use `compileOnly` for dependencies required only at compile time and `runtimeOnly` for those needed only at runtime【904821515729589†L344-L360】; these map to the `provided` and `runtime` scopes in the POM respectively.  Proper configuration ensures that consumers pull in the correct transitive dependencies and avoid classpath pollution.

#### Section 4: Customising Group, Artifact and Version

When you publish a module, Gradle uses the project’s `group`, `name` and `version` as the Maven coordinates.  You can override these per publication by setting `groupId`, `artifactId` and `version` inside the `MavenPublication`.  The Maven publish documentation shows how to customise the identity and metadata of a publication【800041472511575†L324-L356】.  For example:

```groovy
publishing {
    publications {
        mavenJava(MavenPublication) {
            from components.java
            groupId = 'com.example.billing'
            artifactId = 'billing-api'
            version = '1.0.0'
        }
    }
}
```

Choose group IDs following Java package rules【477992074245371†L113-L119】 and artifact IDs that are lowercase with hyphens【477992074245371†L146-L152】.  Align versions across modules using semantic versioning【477992074245371†L156-L170】 to indicate compatibility.

#### Section 5: Handling Optional Dependencies

Maven supports an `<optional>true</optional>` flag to indicate that a dependency is not required by default.  Gradle does not have a direct equivalent and generally discourages optional dependencies.  As the Gradle blog notes, “there are no optional dependencies: there are dependencies which are required if you use a specific feature”【928492847469635†L119-L122】.  Declaring a dependency as optional in Gradle has no effect on resolution; it simply adds documentation.  If you truly have feature‑specific dependencies, model them as separate variants or modules.  Alternatively, publish a feature module with its own artifacts and let consumers depend on it when needed.  This approach avoids hidden classpath requirements and makes dependencies explicit.

#### Section 6: Customising the Generated POM

Sometimes you need to add or remove information from the generated POM.  Gradle exposes a `pom.withXml { }` hook that allows you to manipulate the XML before it is written.  You can, for example, add a `<description>` element, attach license information or mark a dependency as optional.  The API documentation shows that `withXml` accepts an action that operates on an `XmlProvider`【743689685373435†L23-L48】.  Inside the closure you can call `asNode()` to obtain the underlying DOM and use Groovy’s XML builder to modify it.  For example:

```groovy
mavenJava {
    pom.withXml { xml ->
        def root = xml.asNode()
        root.appendNode('description', 'Billing API module')
        // mark a dependency as optional
        root.dependencies.dependency.find { it.artifactId.text() == 'spring-boot-starter-web' }?.appendNode('optional', 'true')
    }
}
```

Be cautious: modifications via `withXml` only affect the POM, not the Gradle module metadata.  Use this hook sparingly to add metadata that Maven consumers expect.

#### Section 7: Publishing a BOM

To ensure consistent versions across modules, publish a Bill of Materials (BOM).  Apply the `java-platform` plug‑in and define a `constraints` block listing the minimum or preferred versions of each dependency【613331523443752†L298-L339】.  Then publish the platform as its own `MavenPublication`.  Consumers can import the BOM in their POM or Gradle build to align versions.  Remember that Maven does not recognise Gradle module metadata constraints; only the BOM can convey version recommendations【613331523443752†L474-L477】.  Use semantic versioning for the BOM and update it whenever you upgrade dependencies.

#### Section 8: Verifying Published Artifacts

Publishing a module is only half the work; you must verify that it works when consumed.  Before releasing, publish to a staging repository and create a small consumer project that depends on your module.  Build the consumer with both Gradle and Maven.  In Gradle, verify that the API and runtime classpaths contain the expected dependencies and that there are no leakage of `implementation` dependencies.  In Maven, run `mvn dependency:tree` to inspect scopes.  If the graphs differ, adjust your `api`/`implementation` declarations or `pom.withXml` customisations.  Gradle’s guidelines stress the importance of consuming your own published artifacts to catch POM issues before they reach users【434703612789846†L950-L1094】.

#### Section 9: Declaring Optional Modules

Large libraries often offer additional features via optional modules.  Instead of marking dependencies as optional, create a separate Gradle project for each feature.  For example, a billing API might have a core module and an optional `billing-webmvc` module.  Consumers who need web integration can depend on `billing-webmvc`; others can ignore it.  In the BOM, constrain the optional module version to match the core.  This strategy keeps the core module lean and avoids pulling unnecessary frameworks into all consumers.

#### Section 10: Marking Provided Dependencies

Sometimes a dependency is provided by the runtime environment and should not be included in your artifact.  Use `compileOnly` to declare such dependencies; Gradle will include them on the compile classpath but not on the runtime classpath or in the published metadata【904821515729589†L344-L360】.  Examples include servlet APIs or container‑provided libraries.  When publishing, verify that these dependencies appear with `<scope>provided</scope>` in the POM.  For test‑only dependencies, use `testImplementation` and `testRuntimeOnly` so they don’t leak into the main POM.

#### Section 11: Attaching Sources and Javadoc JARs

Publishing sources and Javadoc helps consumers understand your library.  Apply `withSourcesJar()` and `withJavadocJar()` in the `java` block, then include these variants in your publication.  Gradle automatically generates `*-sources.jar` and `*-javadoc.jar` and publishes them alongside the main JAR.  Consumers’ IDEs will download them when available, enabling code navigation and documentation.  In Maven Central, having source and Javadoc JARs is often a requirement.

#### Section 12: Publishing Plugins

Publishing Gradle plug‑ins has its own conventions.  Use the `java-gradle-plugin` and `plugin-publish` plug‑ins to define the plug‑in ID, implementation class and metadata.  When publishing, Gradle generates a plug‑in marker artifact and a POM that declares the plug‑in’s implementation dependencies.  The artifact ID should be the plug‑in ID with dots replaced by hyphens and suffixed with `-plugin`, e.g. `com.example.billing-app-plugin`.  Publishing plug‑ins to the Gradle Plug‑in Portal makes them discoverable, but you can also host them in your own Maven repository.

#### Section 13: Variant‑Aware Publishing

Gradle’s variant‑aware model allows modules to provide multiple variants such as JAR, sources JAR and shaded JARs.  To publish variants, use `addVariantsFromConfiguration` on the component.  The publishing customisation guide shows how to create an adhoc component and map its variants to Maven scopes【839199595060898†L320-L341】.  For example, you can publish a shaded JAR as a classifier `-all` while keeping the regular JAR as the default.  This advanced technique enables complex artifacts to be consumed correctly by both Gradle and Maven.

#### Section 14: Setting Up Maven Local and Continuous Integration

During development, publish modules to your local Maven repository with `publishToMavenLocal`.  This populates `~/.m2/repository` and allows you to test consumption without pushing to a shared repository.  In CI, configure credentials for your release repository and set the repository URLs via project properties or environment variables.  Use separate repositories for snapshots and releases and enforce that release versions are immutable.  Automate the publication step in your pipeline after running tests and quality checks.

#### Section 15: Signing Published Artifacts

When publishing to public repositories like Maven Central, artifacts must be signed.  Apply the `signing` plug‑in and configure it with your GPG key.  Gradle will automatically sign each artifact and attach `.asc` files to the publication.  Signing ensures that consumers can verify authenticity and integrity.  Remember to distribute your public key via a key server and specify the key ID in your build script.  Keep private keys secure and rotate them periodically.

#### Section 16: Multi‑Module Publishing Strategies

In a multi‑project build, you may choose to publish all modules individually or aggregate them under a parent POM.  Publishing individually allows consumers to pick only the modules they need and keeps dependencies clear.  However, some tooling expects a parent POM with module declarations.  If you choose a parent POM, use a separate project to generate it and include `<modules>` entries for each published module.  Avoid including dependency management in the parent; instead, rely on a BOM.  Use Gradle’s `project.version` to align versions across modules, and configure the `maven-publish` plug‑in consistently in each subproject via a common script.

#### Section 17: Handling Repository Credentials and Credentials Rotation

Publishing requires credentials for your repository manager.  Store these in environment variables or a credentials file outside of version control and read them in the build script.  For example, define `mavenRepoUser` and `mavenRepoPassword` in your CI system and configure the repository as follows:

```groovy
repositories {
    maven {
        name = 'releases'
        url = uri("https://repo.example.com/releases")
        credentials {
            username = project.findProperty('mavenRepoUser') ?: System.getenv('MAVEN_REPO_USER')
            password = project.findProperty('mavenRepoPassword') ?: System.getenv('MAVEN_REPO_PASSWORD')
        }
    }
}
```

Rotate credentials periodically and use fine‑grained access tokens to limit damage if credentials leak.  For shared builds, prefer repository credentials tied to a service account rather than a personal user.

#### Section 18: Lessons Learned from Publishing

Publishing seems simple until a consumer raises an issue.  Our team learned that misaligned scopes cause classpath conflicts, missing dependencies cause runtime errors and optional flags mislead users.  We resolved these by strictly separating `api` and `implementation` dependencies, modelling optional features as separate modules and verifying our POMs by consuming them in both Gradle and Maven.  We also learned that Gradle module metadata is richer and should be preserved for Gradle consumers; publishing a BOM helps bridge the gap for Maven users.  Finally, we automated our publishing process and integrated tests into CI to catch problems early.

#### Section 19: Toward a Truthful POM

Gradle’s goal is to represent your build truthfully.  A correct POM aligns Maven scopes with your internal configurations, accurately describes optional modules and omits internal details.  Where Maven falls short, Gradle module metadata provides the missing information.  Educate your consumers about the difference and encourage them to use Gradle when possible.  When they must use Maven, give them a well‑tuned POM and a BOM to avoid surprises.  Transparency builds trust and reduces support overhead.

#### Section 20: Summary and Next Steps

Publishing modules is the final leg of your migration journey.  By understanding how Gradle maps its rich model to Maven, you can avoid broken POMs and confused consumers.  Use the `maven-publish` plug‑in and configure your publications with clear coordinates【800041472511575†L292-L321】.  Map `api` and `implementation` correctly【21875673602686†L325-L361】, avoid optional dependencies【928492847469635†L119-L122】, and customise the POM only when necessary【743689685373435†L23-L48】.  Publish BOMs to centralise version management【613331523443752†L298-L339】【613331523443752†L474-L477】 and verify your artifacts by consuming them.  Signed, documented, and accurately scoped releases will pave the way for the final stages of your modularisation project.