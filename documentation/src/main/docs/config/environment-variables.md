# Environment Variables

Environment variable names follow the conversion rules specified by [MicroProfile Config](https://github.com/eclipse/microprofile-config/blob/master/spec/src/main/asciidoc/configsources.asciidoc#default-configsources).

SmallRye Config specifies additional conversion rules:

- A property with double quotes `foo."bar".baz`, replace each character that is neither alphanumeric nor `_` 
with `_`: `FOO__BAR__BAZ`
- A property with dashes `foo.bar-baz`, replace each character that is neither alphanumeric nor `_`
  with `_`: `FOO_BAR_BAZ`
- An indexed property `foo.bar[0]`, replace each character that is neither alphanumeric nor `_`
  with `_`: `FOO_BAR_0_`

!!! danger

    Environment variables format cannot represent the entire spectrum of common property names.

By default, the underscore `_` of an environment variable name always maps to a dot `.`. If the property name
contains a dash or some other special character, that property name needs to be specified in one of the Config 
sources. It will provide additional information to SmallRye Config on converting the environment variable name.

For instance:

_Config A_
```console
FOO_BAR_BAZ=VALUE
```

Will map to `foo.bar.baz` and value `value`.

_Config B_
```console
FOO_BAR_BAZ=VALUE
```
```properties
foo.bar-baz=default
```

Will map to `foo.bar-baz` and value `value`.
