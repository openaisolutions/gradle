## Part VIII – The Install4j Factor – Deploying Legacy Apps in Modern Pipelines

Building and packaging are only part of the journey.  To deliver software to end users you need a reliable installation process that works across platforms, bundles the right Java runtime and meets security requirements.  For desktop applications, this often means generating native installers such as MSI, DMG and DEB files.  In the Ant era, teams wrote custom scripts or used third‑party installers that were disconnected from the build.  Gradle and tools like Install4j and jpackage provide a more integrated path.  This chapter explains how to use Install4j’s Gradle plug‑in, compares it to open‑source alternatives, covers automation, signing and testing, and offers strategies for integrating installation into a modern CI/CD pipeline.

### Chapter 9: The Install4j Factor – Deploying Legacy Apps in Modern Pipelines

#### Section 1: Why Installers Matter

Even in a world of web services and cloud deployments, many enterprise Java applications are delivered as desktop or client‑server software.  For these apps, a proper installer shapes the user’s first impression.  A one‑click installation that bundles the required JRE, sets up start menu shortcuts and integrates with the operating system reduces support costs and increases adoption.  Conversely, a manual installation process invites misconfiguration and frustration.  Our legacy system shipped as a ZIP file with instructions to set `JAVA_HOME` and run a script; this caused frequent errors.  During the migration, we aimed to produce native installers that embed the correct runtime and configure the application consistently across Windows, macOS and Linux.  To achieve this we evaluated several tools and settled on Install4j for its maturity, cross‑platform support and Gradle integration.

#### Section 2: Choosing an Installation Technology

Several options exist for packaging Java applications.  **Install4j** is a commercial tool that produces native installers for multiple platforms and supports rich features such as auto‑update, code signing and custom installation screens.  Its Gradle plug‑in integrates seamlessly with the build, letting you run the install4j compiler as part of your pipeline【495266242642713†L153-L170】.  **jpackage**, introduced in JDK 14, is an open‑source tool that creates native packages by bundling a modular JRE with your application.  **jlink** can generate a trimmed runtime image but does not produce an installer.  Other alternatives like **Conveyor** and **jDeploy** offer cross‑platform packaging with automatic updates.  When selecting a tool, consider licensing (Install4j is commercial but offers a free trial), feature requirements (auto‑update, custom UI), and build integration.  We chose Install4j because it produced polished installers and had a Gradle plug‑in, but we also experimented with jpackage for lightweight prototypes.

#### Section 3: Applying the Install4j Gradle Plug‑in

The first step is to add the Install4j plug‑in to your build script.  The official documentation shows that you apply the plug‑in using a standard `plugins` block【495266242642713†L153-L160】:

```
plugins {
    id "com.install4j.gradle" version "11.0.4"
}
```

This plug‑in provides a top‑level `install4j {}` configuration block and tasks of type `com.install4j.gradle.Install4jTask`【495266242642713†L164-L170】.  In the configuration block you can specify global defaults such as the installation directory of Install4j.  If you omit `installDir`, the plug‑in will auto‑provision a matching distribution and cache it under `<Gradle user home>/install4j/dist`【495266242642713†L184-L187】.  For example:

```
install4j {
    installDir = file("/opt/install4j")
}
```

This ensures that the correct version of Install4j is available during the build.

#### Section 4: Defining Installer Tasks

Install4j uses a project file (`.install4j`) that describes the installer screens, file sets, actions and variables.  In Gradle, define tasks of type `Install4jTask` to build these projects.  The plug‑in documentation lists many parameters you can configure on these tasks—such as `projectFile`, `release`, `destination`, `variables` and `buildIds`【495266242642713†L205-L227】.  A minimal task might look like this:

```
task createInstaller(type: com.install4j.gradle.Install4jTask) {
    projectFile = file("installer/billing.install4j")
    release = version
    destination = file("build/installers")
    variables = [appName: 'Billing', company: 'Example Corp']
}
```

The `variables` map overrides compiler variables defined in the Install4j project【495266242642713†L209-L217】.  You can also specify `buildIds` if you only want to build certain media types.

#### Section 5: Automating Release Workflows

Manual installer creation is error‑prone.  Integrate installer tasks into your CI pipeline so that they run after tests pass.  In our pipeline, we added a stage that executes `./gradlew clean createInstaller` on tagged releases.  The task produces installers in `build/installers`, which we store as build artifacts.  We also configured the `release` property of the Install4j task to use the git tag name, ensuring that the installer version matches the application version【495266242642713†L205-L223】.  Automating this process eliminates the risk of building with the wrong version or forgetting to include files.

#### Section 6: Code Signing and Notarization

Operating systems increasingly require signed installers.  Install4j supports code signing on Windows and macOS via keystores and notarization.  The plug‑in exposes parameters such as `winKeystorePassword`, `macKeystorePassword` and `disableNotarization`【495266242642713†L256-L261】.  Provide your keystore passwords in CI via environment variables and configure the task accordingly:

```
install4j {
    winKeystorePassword = System.getenv('WIN_KEYSTORE_PWD')
    macKeystorePassword = System.getenv('MAC_KEYSTORE_PWD')
    // disable signing when building snapshot versions
    disableSigning = version.contains("SNAPSHOT")
}
```

For notarization on macOS, you must provide notarization credentials via `xcrun altool` or a notary service.  Ensure that these secrets are stored securely and rotated regularly.

#### Section 7: Bundling the Right Java Runtime

Installers need a runtime environment.  Install4j can bundle an OpenJDK from your machine or auto‑provision one.  Alternatively, you can use `jlink` or `jpackage` to create a custom JRE image and configure Install4j to use it.  To keep installers lean, generate a modular runtime with `jlink` containing only the modules your application requires.  In our project we used the `org.beryx.jlink` plug‑in to build a runtime image and referenced it in the Install4j project.  When using jpackage alone, remember that it creates installers only for the platform it is run on; cross‑platform builds require running jpackage on each OS.  Evaluate each tool’s capabilities and choose the one that fits your distribution strategy.

#### Section 8: Enabling Auto‑Update and Rollback

One advantage of Install4j is its built‑in auto‑update mechanism.  By defining an update descriptor file and enabling auto‑update in your project file, you can provide incremental updates to users.  The installer periodically checks your update server and downloads new versions.  Configure update files to include release notes, required Java versions and rollback options.  If auto‑update is not needed, you can disable it and rely on manual upgrades.  For jpackage, consider external tools like jDeploy or jPackageAppImage that add update functionality.

#### Section 9: Creating Uninstallers and Clean‑up

Good installers should also clean up.  Install4j automatically creates an uninstaller that removes installed files and registry entries.  You can customise the uninstaller screens and actions in the Install4j project file.  On Windows, ensure that services are stopped before removal and that environment variables are unset.  For macOS and Linux, the uninstaller typically deletes the installation directory.  Document how to uninstall your application and provide a `--remove` script in the distribution for headless environments.

#### Section 10: Integrating with the Build Lifecycle

Your application build and installer build should be coordinated.  Use task dependencies in Gradle to ensure the installer runs after the application has been assembled.  For example:

```
createInstaller.dependsOn assembleDist
```

This ensures that the distribution ZIP produced by the distribution plug‑in is available when the Install4j task runs.  If you are building multiple modules, configure the installer task to depend on their respective `build` tasks.  Group installer tasks under a `release` task so that running `./gradlew release` performs all steps—from compiling and testing through packaging and installer creation.

#### Section 11: Handling Platform Differences

Installers must accommodate platform‑specific conventions.  On Windows, installers often need to register services, create start menu entries and add registry keys.  On macOS, notarization and DMG styling are mandatory for user trust.  Linux packages should follow the Linux Standard Base file hierarchy and integrate with package managers like `apt` or `rpm`.  Install4j supports these differences by letting you configure separate media files for each platform and by exposing parameters such as `buildIds` to select which media types to build【495266242642713†L225-L227】.  Test installers on each platform; a Windows installer cannot be validated on Linux.

#### Section 12: Testing Installers in Continuous Integration

Testing installers is just as important as testing code.  Use containerised or virtualized environments to run your installers in CI.  For Windows, spin up a Windows Server container or a virtual machine and execute the MSI or EXE silently.  Verify that files are installed, services start and uninstall works.  On macOS, use a macOS runner to mount the DMG, install the application and verify codesigning and notarization.  For Linux, create Docker images representing target distributions and install the DEB or RPM packages.  Automating these tests prevents surprises when users run your installers.

#### Section 13: Scanning for Vulnerabilities and Compliance

Security does not stop at the library level; installers must be scanned for vulnerabilities and compliance.  Use tools like **anchore**, **Snyk** or **Syft** to scan your installer binaries for known vulnerabilities.  Generate a Software Bill of Materials (SBOM) for the runtime and bundled libraries and publish it alongside the installer.  Ensure that any third‑party code included in the installer complies with licensing requirements.  For example, bundling OpenJDK is usually safe, but bundling proprietary codecs may not be.

#### Section 14: Managing Configuration and Certificates

Many applications require external configuration files or certificates.  Decide whether these should be bundled or provided externally.  Install4j allows you to include configuration files in the installer and write them to the user’s home directory at installation time.  Use the `variables` parameter to inject environment‑specific values into the installer【495266242642713†L209-L217】.  For certificates, create a secure keystore and include it in the installer; prompt the user to provide passwords during installation.  On Windows, use the `winKeystorePassword` parameter; on macOS, use `macKeystorePassword`【495266242642713†L256-L259】.  Never hard‑code secrets in the build script; read them from environment variables or secret management services.

#### Section 15: Building Distribution Packages with Install4j and Gradle

Combine Install4j with Gradle’s distribution plug‑in to create unified installers.  First, use the distribution plug‑in to assemble your application into a ZIP or TAR.  Then configure the Install4j project to include that distribution as a file set.  This decouples building the application from building the installer and allows you to verify each step separately.  In our project, we configured the Install4j project to pick up `billing-app-1.0.zip` from `build/distributions` and extract it into the installer.  This strategy simplified debugging because we could inspect the ZIP independently of the installer.

#### Section 16: Migrating from Ant to Install4j

Our legacy build used an Ant script to copy files and invoke Install4j via the command line.  Migrating to Gradle involved replacing these procedural steps with declarative tasks.  We created a Gradle task of type `Install4jTask` and copied the options from the Ant script into task properties.  We moved the installer project file into `installer/` and versioned it alongside the code.  By applying the Install4j plug‑in, we eliminated external shell scripts and integrated installation into the build.  To verify correctness, we kept the Ant script until both produced identical installers【495392568104378†L356-L371】.  Once validated, we removed the Ant script and simplified our pipeline.

#### Section 17: Evaluating Open‑Source Alternatives

Install4j is powerful but not always necessary.  If your application has simple packaging needs and budget constraints, consider open‑source alternatives.  **jpackage** creates native installers from modular applications and can be invoked via Gradle or the command line.  **jlink** produces runtime images but requires a separate installer.  **Conveyor** packages self‑updating apps and works across operating systems.  For applications distributed via web downloads rather than physical media, a ZIP produced by the distribution plug‑in may suffice.  Evaluate features like auto‑update, custom UI, signing, and runtime customization when choosing a tool.

#### Section 18: Scalability and Maintainability of the Installation Pipeline

As your product line grows, managing multiple installers can become complex.  Standardise your installer tasks across modules by extracting common configuration into a Gradle convention plug‑in.  Use parameterised tasks to build installers for different products with minimal duplication.  Store installer project files in a separate repository to allow installer specialists to iterate independently of core development.  Document the pipeline so that new team members understand how installers are built and released.  Automated tests and reproducible builds make the process sustainable.

#### Section 19: Lessons Learned

Migrating installation to Gradle and Install4j taught us several lessons.  First, treat installers as first‑class artefacts: version them, test them and run static analysis.  Second, keep secrets out of source control and provide them via environment variables.  Third, cross‑platform packaging requires building on each target OS or using remote builders; there is no one‑size‑fits‑all.  Fourth, invest in automation early; manual installer builds delay releases and introduce errors.  Finally, be prepared to iterate—installer configuration is often trial and error, and user feedback will uncover platform quirks you didn’t anticipate.

#### Section 20: Summary and Future Work

Native installers are the final touchpoint between your code and your users.  By integrating Install4j or other packaging tools into your Gradle build you create a seamless pipeline from source to installer.  Apply the Install4j plug‑in and configure global defaults【495266242642713†L164-L170】, define installer tasks with appropriate parameters【495266242642713†L205-L227】, automate release workflows and sign your installers for trust.  Consider alternatives like jpackage when requirements are simple, and always test installers in real environments.  In the next chapter we will explore patterns for refactoring without rewriting the monolith, bringing together all the techniques learned throughout the migration.