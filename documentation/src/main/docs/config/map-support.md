# Map Support

SmallRye Config allows injecting multiple configuration parameters as a `Map`. The configuration value syntax is 
represented by `property.name.map-key=value` Consider:

```properties
server.reasons.200=OK
server.reasons.201=Created
```

The previous configuration could be injected directly in a CDI Bean:

With `@ConfigProperty`

```java
@ApplicationScoped
public class ConfigBean {
    @Inject
    @ConfigProperty(name = "server.reasons") 
    Map<Integer, String> reasons;
}
```

With `@ConfigProperties`

```java
@ConfigProperties(prefix = "server") 
public class Config {
    Map<Integer, String> reasons; 
}
```

The `Map` will contains the keys `200` and `201`, which map to the values `OK` and `Created`.

!!!note

    Only the direct sub properties will be converted into a `Map` and
    injected into the target bean, the rest will be ignored. In other words,
    in the previous example, a property whose name is `reasons.200.a` would
    be ignored as not considered as a direct sub property.

!!!note

    The property will be considered as missing if no direct sub properties
    could be found.

It is also possible to retrieve the `Map` programmatically by calling the methods 
`SmallRyeConfig#getValues("server.reasons", Integer.class, String.class)` or 
`SmallRyeConfig#getOptionalValues("server.reasons", Integer.class, String.class)`.
