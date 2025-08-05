### Chapter 3: Debug or Die – Why IDE Developer Experience Still Matters

The productivity of developers is often dictated not only by the build system but by the tools they use every day.  After migrating the build, we turned our attention to smoothing the developer experience.  This chapter describes how to integrate Gradle deeply into Eclipse and IntelliJ, leverage build scans, configure run and test tasks, and provide a supportive environment for current and future team members.  A smooth workflow encourages adoption and ensures that the benefits of modularisation are realised.

#### Section 1: The Developer Experience – Feedback and Flow

Modern software development hinges on rapid feedback.  If a change breaks the build or introduces a bug, developers need to know quickly so they can adjust course.  Continuous integration practices like daily merges and automated builds reduce the risk of delivery delays and wasted integration effort【109171532237068†L115-L120】.  A responsive build pipeline frees developers to stay in a state of “flow,” moving between coding, testing and debugging without friction.  Conversely, a clumsy toolchain forces context switches and hampers productivity.

In our project, moving to Gradle eliminated many manual steps, but to fully benefit we had to ensure that IDE tasks, debuggers and tests worked seamlessly.  We set a goal that developers should be able to clone the repository, run a single command to import the project into their favourite IDE, and start coding without editing configuration files.  Achieving this goal required understanding the capabilities of each IDE and configuring Gradle to cooperate with them.  Throughout this chapter we will highlight how to preserve the intuitive flow of development while improving build fidelity.

#### Section 2: IDE Landscape – Eclipse, IntelliJ and NetBeans

Java developers typically rely on rich integrated development environments such as Eclipse, IntelliJ IDEA and NetBeans.  Each IDE offers features like code completion, refactoring tools and test runners, but their Gradle integration varies.  Eclipse uses the **Buildship** plug‑in to integrate Gradle projects; Buildship is a collection of Eclipse plug‑ins that provides deep integration with Gradle, allowing you to execute tasks, inspect dependencies and synchronise the IDE model with the build【56816376473026†L239-L244】.  IntelliJ IDEA ships with its own Gradle importer and run configuration interface that reads the build script and generates appropriate project settings.  NetBeans also includes a Gradle support plug‑in that recognises multi‑project builds and displays tasks in a sidebar.

While migrating, we tested all three IDEs to ensure that the build worked consistently.  Eclipse and IntelliJ proved the most robust; Buildship’s ability to work directly with the Gradle Tooling API means that no manual generation of `.classpath` files is required.  IntelliJ’s importer similarly recognises `build.gradle` and `settings.gradle` and provides contextual actions for running tasks.  Regardless of IDE, our priority was to avoid any IDE‑specific hacks in the build scripts.  By targeting Gradle’s standard configuration, we allowed developers to choose their preferred tool while maintaining a single source of truth for dependencies and tasks.

#### Section 3: Gradle Buildship Integration

Eclipse users previously relied on the `eclipse` Gradle plug‑in to generate `.project` and `.classpath` files.  In the Gradle era we switched to Buildship, which interacts with Gradle through the Tooling API.  Buildship is described as a set of Eclipse plug‑ins that provides deep integration with Gradle【56816376473026†L239-L244】.  The Gradle user manual echoes this, calling Buildship a plug‑in collection that embeds Gradle into Eclipse【657788656503172†L330-L341】.  Once installed, Buildship offers a “Gradle Tasks” view where you can browse tasks for each subproject, run them directly and view console output.

To import a project, choose **File → Import → Gradle → Existing Gradle Project** and select the root directory.  Buildship reads `settings.gradle`, detects all subprojects and configures the workspace accordingly.  When you change `build.gradle` or `settings.gradle`, Buildship prompts you to synchronise the project; this reloads the build model and updates classpaths and project settings.  Unlike the old `eclipse` tasks, Buildship does not persist generated files in version control.  Instead it treats Gradle as the canonical source of configuration.  For teams migrating from Ant this is a major improvement: there is no need to commit `.classpath` tweaks, and the IDE reflects the build exactly.

#### Section 4: Synchronising Sources and Classpaths

During the migration we encountered issues with stale classpaths and missing source folders when developers manually edited `.classpath` files.  Buildship resolves this by deriving the classpath directly from Gradle’s dependency graph.  When you refresh the project, Buildship analyses `sourceSets`, identifies `src/main/java`, `src/main/resources` and any custom directories, and updates the Eclipse project accordingly.  Similarly, IntelliJ re‑reads the Gradle model and updates module settings.  To synchronise the project, either accept Buildship’s prompt after editing build files or right‑click the project and choose **Gradle → Refresh Gradle Project**.  This ensures that new modules, dependencies and source sets appear in the IDE without manual changes.

If your build uses unconventional source directories, specify them in `sourceSets` in `build.gradle`.  For example:

```groovy
sourceSets {
    main {
        java.srcDirs = ['src']
        resources.srcDirs = ['resources']
    }
}
```

Defining source sets declaratively allows Buildship and IntelliJ to configure the IDE correctly.  As you move modules from Ant layouts to Gradle conventions, update these declarations to reflect the new structure.

#### Section 5: Debugging Across Modules

Legacy systems often consist of multiple modules that must be launched together for debugging.  IntelliJ IDEA offers a Gradle run configuration that can execute tasks across modules and attach the debugger.  A useful option is **Debug all tasks on the execution graph**, which instructs the IDE to start the JVM in debug mode for every Gradle task【86014017228282†L326-L361】.  This allows you to set breakpoints in any module and inspect variables as the application starts.  Alternatively, you can uncheck this option and debug only tasks that run tests or your application, reducing overhead【86014017228282†L326-L361】.

For Eclipse users, Buildship exposes the `run` task of the application plug‑in and any custom tasks.  You can right‑click the `run` task and choose **Debug** to start the application with a debugger attached.  If a module uses the Java Library plug‑in rather than the application plug‑in, create a custom task that invokes `JavaExec` and configure its `debug` property.  When modules depend on each other, ensure that their `implementation` relationships are correctly defined so that the debugger can resolve symbols across boundaries.  By standardising on Gradle tasks for launching the application, we avoided maintaining separate IDE launchers and ensured consistent behaviour across development and CI.

#### Section 6: Running Gradle Tasks from the IDE

IntelliJ and Eclipse both support running arbitrary Gradle tasks from within the IDE.  In IntelliJ you can create a **Run/Debug configuration** of type **Gradle** and specify the tasks and arguments to execute【86014017228282†L12-L50】.  You might set the task field to `clean build` or `:moduleA:test` and pass JVM arguments under the **VM options** field.  Configurations can be stored in the `.idea` directory or shared via version control.  For repetitive tasks like assembling the distribution or generating documentation, save separate configurations to avoid typing long commands.

Eclipse’s Buildship provides a similar “Run Configurations” dialog.  Selecting a Gradle project allows you to choose tasks, specify command‑line options and set environment variables.  The configuration persists within the workspace but is not checked into the repository.  Buildship also includes a **Tasks** view where double‑clicking a task executes it with default arguments.  This integration reduces the need to switch to a terminal and ensures that tasks are executed with the correct wrapper version and project directory.

#### Section 7: Configuring Source Sets for IDEs

As you modularise the application, some modules may use non‑standard source layouts.  For example, a module migrated from Ant might place Java code under `src` rather than `src/main/java`.  Gradle’s `sourceSets` mechanism allows you to map these directories so that the IDE recognises them.  Declare custom source sets in your `build.gradle` file and ensure that each has a unique `name`.  Buildship and IntelliJ read these definitions and create corresponding folders in the project explorer.  If your module contains generated sources, specify them in `sourceSets.main.java.srcDirs` and mark them as generated sources in IntelliJ to avoid editing warnings.

Over time you should move toward Gradle’s conventional layout, but during migration, explicit `sourceSets` declarations preserve the existing structure.  Remember to update integration tests or integration resources directories as well.  Consistently defining source sets prevents confusion about where code should live and ensures that IDE features such as search, refactoring and code generation work reliably.

#### Section 8: Managing Duplicate Classes and Classpaths

One frequent problem when migrating from Ant to Gradle is duplicated entries in the IDE classpath.  In a forum thread a user discovered that running `gradlew cleanEclipse eclipse` caused duplicate dependency entries to appear in the `.classpath` file and break compilation【929742176678335†L58-L71】.  The recommended workaround was to remove duplicates during the `.classpath` generation.  The script uses the `whenMerged` hook to group classpath entries by their path and then re‑insert only unique entries【929742176678335†L158-L179】.  In Gradle you can add this logic to the `eclipseClasspath` configuration to eliminate duplicates automatically.

Switching to Buildship largely sidesteps this issue because the IDE derives the classpath directly from the dependency graph.  However, if you still need to generate Eclipse files—for example, for teams not using Buildship—apply the duplicate removal pattern.  Also check for duplicate classes within your modules.  When two modules define the same package and class name, the IDE might compile one and run another, causing unpredictable behaviour.  Use your dependency graph visualisation and static analysis tools to detect and eliminate duplicates early.

#### Section 9: Resolving Source Lookup Failures

Another issue encountered during migration is the “source not found” error when debugging third‑party libraries.  A Gradle forum discussion explained that using a `flatDir` repository prevents Buildship from finding source JARs because the repository has no group or artifact coordinates【268312600825314†L60-L72】.  The advice is to avoid `flatDir` repositories and instead structure local repositories in a Maven‑like layout (`group/artifact/version`), enabling the IDE to download sources and javadocs automatically【268312600825314†L169-L172】.  If you must use local JARs temporarily, add them via `implementation files('libs/foo.jar')` and accept that source lookup will not work until the dependency is published properly.

To ensure good debugging support, prefer declaring dependencies via Maven Central or your internal repository.  When migrating, plan to migrate proprietary libraries into a repository so that Gradle and the IDE can locate sources.  For external libraries, enable the “Download Sources” option in IntelliJ’s Gradle settings.  Buildship fetches sources automatically when they are available.  Removing `flatDir` dependencies was one of the first tasks we undertook to improve developer experience.

#### Section 10: Adjusting Build Directories

In legacy builds, compiled classes and scripts often live in a `bin` directory.  Gradle uses a different convention: compiled classes are placed under `build/classes` and distribution scripts are generated under `build/distributions`.  If your application depends on a particular directory structure—perhaps because an Ant script expects to find executables in `bin`—you can customise Gradle’s output.  The Application plug‑in exposes an `executableDir` property that controls where start scripts are generated【97055485946526†L353-L377】.  For example, setting `applicationDistribution.from installDist.executableDir = new File(project.buildDir, 'bin')` will produce scripts in the `build/bin` directory.

Another approach is to update downstream scripts to look in Gradle’s default locations.  In our migration we first customised the distribution directory to maintain compatibility with existing launchers, then gradually removed those launchers as we replaced them with `gradlew run` commands.  This allowed the team to continue working while we refactored the deployment pipeline.  When adjusting build directories, document your changes and ensure that continuous integration scripts and packaging tasks refer to the new paths.

#### Section 11: Custom Run Configurations

Developers often need to pass additional arguments or environment variables when running a module.  Both IntelliJ and Eclipse allow you to create custom run configurations.  In IntelliJ’s **Run/Debug** dialog you can specify tasks such as `run` and add program arguments or JVM options in the respective fields【86014017228282†L12-L50】.  You can also choose whether to run tasks incrementally or always run them, and save the configuration so that it can be shared across the team.  For instance, a configuration might run `:moduleA:run` with `--args='--profile=dev'` and a custom heap size.

Eclipse’s run configuration interface for Gradle provides similar options.  You can select multiple tasks, specify `--debug-jvm` to run the application in debug mode and set environment variables.  If you need to debug build scripts themselves, Buildship offers a setting to enable script debugging, but this can slow down execution.  Disabling script debugging will debug only the executed Java application or tests【86014017228282†L326-L361】.  For reproducibility, avoid embedding absolute paths or secrets in run configurations—use project properties or environment variables instead.

#### Section 12: Using the Application Plugin

When your project contains an application, Gradle’s application plug‑in simplifies running and packaging it.  The plug‑in implicitly applies the Java and distribution plug‑ins and adds tasks such as `run` and `distZip`【97055485946526†L291-L350】.  You define the fully qualified name of your main class using the `mainClass` property, and Gradle generates scripts under `build/install/<project>` to start the application.  Running `./gradlew run` executes your application locally, and you can pass arguments using `--args`, for example `./gradlew run --args='--port=8080'`【97055485946526†L291-L350】.

The plug‑in also supports debugging.  Passing `--debug-jvm` to the `run` task instructs Gradle to start the JVM in debug mode, allowing you to attach a debugger from your IDE【97055485946526†L291-L350】.  You can customise the default JVM arguments by configuring the `applicationDefaultJvmArgs` property or modify the generated start scripts via `distributions { main { contents.from(...) } }`.  By relying on the application plug‑in, we replaced custom Ant launchers with a standard approach that works consistently across environments.

#### Section 13: Testing Frameworks in the IDE

Gradle supports various testing frameworks such as JUnit, TestNG and Spock.  In IntelliJ you can choose whether to use the IDE’s built‑in test runner or Gradle’s test runner.  JetBrains documentation explains that you can set the **Test Runner** to Gradle, IntelliJ or select per test class【758756572275466†L20-L69】.  Running tests with the Gradle test runner ensures that the behaviour matches the command line and continuous integration.  The IDE displays test results in a dedicated tool window, showing passed, failed and skipped tests.  You can right‑click a test class or method in the editor and choose **Run Gradle Test** to execute it.

Debugging tests is equally straightforward.  Choosing **Debug Gradle Test** attaches the debugger to the test process.  If you prefer the IntelliJ test runner, you can run tests without Gradle, but the results may differ from CI if build scripts define custom test tasks.  In Eclipse, Buildship surfaces the `test` task, and running it opens the JUnit view.  To run a single test class, use `gradlew :module:test --tests com.example.MyClass`.  Adopting consistent test runners across IDE and CI avoids surprise failures.

#### Section 14: Hot Reload and Continuous Build

Developers appreciate immediate feedback when editing code.  Gradle offers **continuous build** mode, which re‑executes tasks automatically when their inputs change.  The manual describes running tasks with the `--continuous` flag; Gradle starts watching the file system and reruns the task whenever inputs change【908571624651963†L287-L387】.  This feature is invaluable for tasks such as compiling classes or regenerating documentation.  However, the documentation warns that continuous build is not integrated into IDE workflows—you must run `./gradlew <task> --continuous` in a separate terminal window alongside your IDE【908571624651963†L287-L387】.  IDEs will reflect changes once the task completes.

Some frameworks offer true hot reload, where the running application automatically reloads classes without restarting the JVM.  Combining Gradle’s continuous build with the application plug‑in can approximate this: one process rebuilds the classes, while another monitors the output and reloads classes.  For example, Spring Boot’s devtools watch for changes in `build/classes` and restart the application.  When using continuous build, ensure that tasks are idempotent and quick to run; long‑running tasks can consume CPU and hinder performance.  Over time we expect IDEs to integrate file watching more tightly, but until then, continuous build remains a valuable tool.

#### Section 15: Leveraging Build Scans

Despite best efforts, builds sometimes fail unexpectedly.  Gradle’s build scan service provides a powerful way to diagnose such failures.  The features page explains that a build scan captures a wealth of information about your build and publishes it to a web application【856209582615570†L156-L168】.  Build scans include details such as build environment, task execution order, dependency resolution, and test results.  You can share the scan URL with colleagues or support teams, enabling collaborative debugging【856209582615570†L156-L168】.  Build scans also allow you to compare two builds to identify differences, making it easier to pinpoint regressions.

To generate a build scan, add the `com.gradle.build-scan` plug‑in to your build and run `./gradlew build --scan`.  Accept the terms of service when prompted, and Gradle will upload the scan to the Gradle Enterprise server.  In many cases, the free public server is sufficient for open source projects.  For commercial projects, consider hosting your own server to retain control of build data.  Build scans became part of our migration toolkit; when a developer reported a mysterious failure, we asked for a scan and quickly identified issues such as incompatible Java versions or misconfigured proxies.  The ability to visualise the dependency graph and task execution timeline in the browser accelerated troubleshooting.

#### Section 16: Developer Feedback Loops

Establishing a tight feedback loop is essential for maintaining momentum during long migrations.  Martin Fowler notes that continuous integration reduces the risk of delivery delays and wasted integration effort by merging changes frequently and running automated tests【109171532237068†L115-L120】.  A healthy feedback loop involves quick local builds, prompt test results and fast debugging cycles.  To achieve this, we invested in a powerful CI server that executed the build on every push and provided notifications via chat.  Developers could run the same tasks locally with `./gradlew build` or via their IDE run configurations.  For integration tests that took longer, we separated them into dedicated tasks so that the fast feedback loop remained unblocked.

In the IDE, Buildship and IntelliJ’s Gradle integration allowed developers to run tasks incrementally and observe results in context.  We encouraged developers to commit small changes frequently and rely on the build to catch regressions.  Combined with build scans, this practice built confidence that the system remained stable as modules were extracted and refactored.  Continuous integration thus served both as a safety net and as a forcing function for modularity: you cannot have a green build if cycles and missing dependencies linger.

#### Section 17: Linting and Static Analysis

Quality checks are an important part of developer experience.  Gradle’s Checkstyle plug‑in performs static analysis on Java source files according to a configurable set of rules.  The user guide notes that the plug‑in adds tasks like `checkstyleMain` and `checkstyleTest`, which are executed when you run `gradle check`【904677006638313†L292-L318】.  The plug‑in integrates with Gradle’s Java plug‑in, ensuring that checks run with the same Java version used by the project【904677006638313†L352-L365】.  The default configuration files live under `config/checkstyle`, but you can point the plug‑in at your own `google_checks.xml` or `sun_checks.xml` to enforce your coding standard.

In the IDE, Checkstyle can be enabled via plug‑ins for Eclipse and IntelliJ.  Buildship automatically adds the Checkstyle tasks to the Gradle view; right‑clicking `checkstyleMain` runs the checks and displays the results.  IntelliJ’s `Checkstyle-IDEA` plug‑in presents violations inline in the editor.  Beyond Checkstyle, we adopted additional static analysis tools such as SpotBugs and PMD, configured through their respective Gradle plug‑ins.  Running these tools in CI and locally helps catch bugs and maintain coding standards, improving the overall quality of the codebase.

#### Section 18: Troubleshooting Common IDE Issues

Even with a modern build, IDE integration can occasionally break down.  Duplicate classpath entries, discussed earlier, can cause build failures; ensure that you do not mix `eclipse` generation tasks with Buildship and remove stale `.classpath` files.  If the IDE cannot find sources for dependencies, check whether you are using `flatDir` repositories and replace them with proper Maven repositories【268312600825314†L60-L72】.  When builds fail before any tasks execute, network misconfiguration can be the culprit—Gradle daemon logs may reveal that the build cannot contact remote repositories due to firewall or proxy issues【411864422224135†L471-L496】.  Also, check that your IDE uses the same Java version as Gradle; mismatches can lead to compiler errors.

Occasionally, IntelliJ will mark code as red even though it compiles from the command line.  In such cases, invalidate the IDE caches and reimport the project.  For Eclipse, if Buildship displays stale data, delete the `.gradle` directory in the workspace and refresh the project.  Keep an eye on plug‑in versions—upgrading Buildship or the IntelliJ Gradle plug‑in can resolve integration bugs.  By documenting these troubleshooting steps and sharing them with the team, we minimized downtime and frustration.

#### Section 19: Onboarding New Developers

An often overlooked aspect of developer experience is onboarding.  Bringing new engineers up to speed quickly is crucial for retaining talent and maintaining velocity.  An article on developer onboarding emphasises that onboarding integrates new developers into the team and provides them with knowledge, resources and relationships【735336400211954†L66-L101】.  Companies with effective onboarding see 62 % greater productivity from new hires and 50 % greater retention【735336400211954†L66-L101】.  The article further explains that a structured program gives newcomers a sense of belonging and accelerates their ability to contribute【735336400211954†L96-L119】.

To support onboarding in our migration project, we created a wiki page outlining how to clone the repository, run the build and import the project into the IDE of their choice.  We included links to Buildship and IntelliJ documentation, explained common pitfalls (e.g., do not use `flatDir` repositories), and provided sample run configurations.  We also paired new developers with experienced team members and hosted regular brown bag sessions to explain the architecture and migration goals.  By investing in onboarding, we ensured that new contributors could quickly become productive and contribute to the migration effort.

#### Section 20: Summary and Insights

This chapter highlighted the importance of a positive developer experience when refactoring a legacy application.  We explored how IDE integration via Buildship and IntelliJ simplifies running, testing and debugging multi‑module Gradle projects.  We discussed configuring source sets, managing classpaths, customising run configurations and leveraging Gradle’s application plug‑in.  We emphasised the value of continuous build and build scans for fast feedback and collaborative troubleshooting【856209582615570†L156-L168】【908571624651963†L287-L387】.  We also underscored the role of quality tools like Checkstyle and the necessity of good onboarding practices【904677006638313†L292-L318】【735336400211954†L66-L101】.

Ultimately, a pleasant developer experience is not just a perk—it is a prerequisite for successful modernisation.  When tools work with you rather than against you, developers spend less time fighting the environment and more time delivering value.  With the build system stabilised and the IDE workflow streamlined, the next chapters can focus on aligning dependency configurations, enforcing architectural boundaries and packaging the application for delivery.