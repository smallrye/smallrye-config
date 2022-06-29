# Environment Variables

Environment Variable names follow the conversion rules specified by [MicroProfile Config](https://github.com/eclipse/microprofile-config/blob/master/spec/src/main/asciidoc/configsources.asciidoc#default-configsources).

SmallRye Config specifies additional conversion rules:

- A property with double quotes `foo."bar".baz`, replace each character that is neither alphanumeric nor `_` 
with `_`: `FOO__BAR__BAZ`
- A property with dashes `foo.bar-baz`, replace each character that is neither alphanumeric nor `_`
  with `_`: `FOO_BAR_BAZ`
- An indexed property `foo.bar[0]`, replace each character that is neither alphanumeric nor `_`
  with `_`: `FOO_BAR_0_`

!!! danger

    Environment Variables format cannot represent the entire spectrum of common property names.

The lookup of configuration values from Environment Variables will always use the dotted format name. For 
instance, the lookup of the Environment Variable `FOO_BAR` value, requires the property name `foo.bar`:

```java
ConfigProvider.getConfig().getValue("foo.bar", String.class);
```

When SmallRyeConfig performs the lookup on the Environment Variables Config Source, it applies the conversion rules to 
find the matching property name and retrieve the value. 

In some situations, looking up the exact property name is impossible. For instance, when SmallRye Config has to look up 
a configuration that is part of a `Map`, and the property name contains a dynamic segment (a `Map` key). In this case, 
SmallRye Config relies upon each source's list of property names. These must be converted back to their most likely 
dotted format for Environment Variables.

By default, the underscore `_` of an Environment Variable name always maps to a dot `.`. If the property name
contains a dash or some other special character, that property name can be specified in another Config 
Source, with the expected dotted format. It will provide additional information to SmallRye Config to perform a 
two-way conversion and match the property names.

For instance:

**Config A**
```console
FOO_BAR_BAZ=VALUE
```

Will map to `foo.bar.baz` and value `value`.

**Config B**
```console
FOO_BAR_BAZ=VALUE
```
```properties
foo.bar-baz=default
```

Will map to `foo.bar-baz` and value `value`.

!!! note

The property name in dotted format needs to exist somewhere to provide this additional information. It can be set in a 
low ordinal source, even without value. The Environment Variables source will override the value and map the correct 
configuration name.
