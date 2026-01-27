# Keyple Plugin CNA Arrive Android Java Library

## Overview

The **Keyple Plugin CNA Arrive Android Java Library** is an add-on to allow an application using Keyple to interact with Arrive Android terminals.

## Requirements

- **Android API Level**: 26 or higher
- **Arrive SDK**: Version 2.0.9

## Build Prerequisites

The compilation of the plugin and the example application requires the presence of the following file in the `plugin/libs/` folder:

- `AndroidParkeonCommon-release.aar`

Ensure this file is available before building the project.

## Project Structure

This project is organized into several modules:

- **plugin** - Main plugin module implementing the Keyple plugin for Arrive Android terminals
- **plugin-mock** - Mock implementation for testing purposes
- **example-app** - Sample Android application demonstrating the plugin usage

## Examples

An example of implementation is available in the **example-app** folder.

## Documentation

API documentation is generated using Dokka and can be built with:

```bash
./gradlew dokkaGeneratePublicationHtml
```

The generated documentation will be available in the `build/dokka/html` directory.

## About the source code

The code is built with **Gradle** and written in **Kotlin**, with compatibility targeting **Java 1.8** to address a wide range of applications.

## Continuous Integration

This project uses **GitHub Actions** for continuous integration. Every push and pull request triggers automated builds
and checks to ensure code quality and maintain compatibility with the defined specifications.
