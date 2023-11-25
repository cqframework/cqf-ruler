# Plugin

This is a cqf-ruler plugin which takes an eRSD V2 Bundle and converts it to an eRSD V1 Bundle.

## Build

Use `mvn package` to build the jar files

## Docker

The Dockerfile builds on top of the base cqf-ruler image and simply copies the jar into the `plugin` directory of the image.

## Setup

A V1 PlanDefinition skeleton must be uploaded with ID : `plandefinition-ersd-skeleton`
