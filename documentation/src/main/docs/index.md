---
hide:
- navigation
- toc
---

# SmallRye Config

**SmallRye Config** is a library that provides a way to configure applications, frameworks and containers. It is used 
in applications servers like [WildFly](https://wildfly.org/), [Open Liberty](https://openliberty.io) and 
[TomEE](https://tomee.apache.org) or frameworks like [Quarkus](https://quarkus.io). It can also be used completely 
standalone in any Java application, which makes it a very flexible library. 

It follows the [MicroProfile Config](https://github.com/eclipse/microprofile-config/) specification to provide 
the initial config foundations and expands with it own concepts to cover a wide range of use cases observed in the 
configuration space.   

## Use SmallRye Config in a Java application

Add the _dependency_ to your project using your preferred build tool:

=== "Maven"

    ```xml
    <dependency>
        <groupId>io.smallrye.config</groupId>
        <artifactId>smallrye-config</artifactId>
        <version>{{attributes['version']}}</version>
    </dependency>
    ```

=== "Gradle (Groovy)"

    ```groovy
    implementation 'io.smallrye.config:smallrye-config:{{attributes['version']}}'
    ```

=== "Gradle (Kotlin)"

    ```kotlin
    implementation("io.smallrye.config:smallrye-config:{{attributes['version']}}")
    ```

=== "JBang"

    ```java
    //DEPS io.smallrye.config:smallrye-config:{{attributes['version']}}
    ```

And retrieve a `SmallRyeConfig` instance with:

```java
SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
```

!!! info

    The `SmallRyeConfig` instance will be created and registered to the context class loader if no such configuration 
    is already created and registered. 

Or build your own:

```java
SmallRyeConfig config = new SmallRyeConfigBuilder().build();
```

!!! info

    `SmallRyeConfig` is the entry point to all the config capabilities provided by SmallRye Config. 
