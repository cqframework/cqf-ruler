# Plugin

This is a sample project demonstrating how to create a cqf-ruler plugin.

## Setup

cqf-ruler plugins rely heavily on Spring auto-configuration. On startup the server looks for the `resources/META_INF/spring.factories` file in all the jars on the classpath. The contents of that file point to a root Spring configuration class that defines all the beans for the plugin. For example:

```ini
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.HelloWorldConfig
```

This tells Spring to load the config described in the HelloWorldConfig class. The cqf-ruler then loads and registers Providers, Interceptors, and Metadata extenders as built by the Spring config. It's possible to create other Beans as well.

The `pom.xml` file in this project describes the minimal set of dependencies to get a plugin going. Any dependency provided by the base server needs to be scoped `provided` so that it's not duplicated.

This system is very basic. It does not support runtime addition or removal of plugins, or plugin versions, or any sort of conflict resolution. Plugins are expected to be well-behaved in that they will only create Beans appropriate for the server configuration that is running. For example, a plugin will not create a DSTU3-only bean when the server is running in R4 mode. Please use conditional Bean creation to ensure this behavior. There is no guaranteed order of loading plugins, so they should not overlap or conflict in functionality or errors may result.

NOTE: This plugin is for demonstration purposes only. It's never intended to be published

## Docker

The Dockerfile builds on top of the base cqf-ruler image and simply copies the jar into the `plugin` directory of the image.
