# Gradle Cookbook by Example

This sample demonstrates a multi-project build with Eclipse support.
It now implements a simple **Goat Management** application to showcase
cross-project dependencies.

### Modules

- `common` – basic utilities shared across the repo
- `library` – domain classes such as `Goat`
- `data` – in-memory repository using `commons-lang3`
- `service` – service layer combining data and library
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
