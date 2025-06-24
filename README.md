# Gradle Cookbook by Example

This sample demonstrates a multi-project build with Eclipse support.
It includes multiple layers of modules that depend on each other
and a simple Swing UI for a thick client application.

### Modules

- `common` – basic utilities shared across the repo
- `library` – library logic depending on `common`
- `data` – data access using `commons-io` and `commons-lang3`
- `service` – service layer combining `data` and `library`
- `ui` – Swing user interface depending on `service`
- `app` – runnable entry point assembling everything

## Building

```bash
gradle build eclipse
```

Import the project into Eclipse via Buildship after running the `eclipse` task.
If you prefer to use the Gradle wrapper, first run `gradle wrapper` to
generate the wrapper JAR files.

## Docker

A simple Dockerfile is provided to run the app in a container:

```bash
docker build -t cookbook-app .
docker run --rm cookbook-app
```

## Packaging

Scripts under `scripts/` illustrate how you might package the application
with install4j for Windows. Example:

```bash
./scripts/package-install4j.sh
```

## Eclipse import and hot reload

1. Run `./gradlew eclipse` and import the project via **File ▸ Import ▸ Existing Gradle Project**.
2. In **Preferences ▸ Gradle ▸ Synchronization** enable *Synchronize automatically after builds*.
3. Launch the `app` run configuration from the Gradle Tasks view. While it runs under the debugger, editing a method signature in `library` and saving will trigger HotswapAgent to reload the class.
4. For instant recompiles run `./gradlew -t classes` in the background so Eclipse picks up new class files immediately.
