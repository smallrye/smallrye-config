---
hide:
- navigation
- toc
---

# SmallRye Config

**SmallRye Config** is a library that provides a way to configure applications, frameworks and containers. It is used 
in applications servers like [WildFly](https://wildfly.org/) and [Open Liberty](https://openliberty.io), or frameworks 
like [Quarkus](https://quarkus.io). It can also be used completely standalone in any Java application, which makes it a 
very flexible library. 

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
    implementation 'io.smallrye.config:smallrye-config:{{ attributes.versions }}'
    ```

=== "Gradle (Kotlin)"

    ```kotlin
    implementation("io.smallrye.config:smallrye-config:{{ attributes.versions }}")
    ```

=== "JBang"

    ```java
    //DEPS io.smallrye.config:smallrye-config:{{ attributes.versions }}
    ```

And retrieve a `SmallRyeConfig` instance with:

```java
SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
```

!!! info

    `SmallRyeConfig` is the entry point to all the config capabilities provided by SmallRye Config. 
