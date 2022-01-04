# Profiles

Applications often require different configurations depending on the target environment. For example, the local 
development environment may be different from the production environment. Profiles allow for multiple configurations 
in the same file or separate files and select between them via a profile name.

## Profile aware properties

To be able to set properties with the same name, each property needs to be prefixed with a percentage sign `%` followed 
by the profile name and a dot `.` in the syntax `%{profile-name}.config.name`:

```properties
http.port=8080
%dev.http.port=8181
```

To activate the profile `dev`, the configuration `smallrye.config.profile=dev` has to be set into any valid 
`ConfigSource`.

Any lookup to the `http.port` property name will first search by the active profile name `%dev.http.port` and then 
fallback to `http.port` if no value is present. In this case a lookup to the property `http.port` with the `dev` profile 
active, yields the value `8181`.

## Profile aware files

Properties for a specific profile may reside in a `microprofile-config-{profile}.properties` named file. The previous 
example can be expressed as:

_microprofile-config.properties_
```properties
http.port=8080
```

_microprofile-config-dev.properties_
```properties
http.port=8181
```

In this style, the property names in the profile aware file do not need to be prefixed with the profile name.

!!!caution

    Properties in the profile aware file have priority over profile aware properties defined in the main file.

## Priority

Profile lookups are only valid if the `ConfigSource` has a higher ordinal than a lookup to the regular configuration 
name. Consider:

_main.properties_
```properties
config_ordinal=1000
http.port=8080
```

_profile.properties_
```properties
config_ordinal=100
%dev.http.port=8181
```

Even with the profile `dev` active, the lookup value for `my.prop` is `1234`. This prevents lower ordinal sources to 
set a profile property value that cannot be overridden unless the profile property is also overridden.

## Multiple Profiles

Multiple Profiles may be active at the same time. The configuration `smallrye.config.profile` accepts a comma-separated 
list of profile names: `smallrye.config.profile=common,dev`. Both `common` and `dev` are separate profiles.

When multiple profiles are active, the rules for profile configuration are the same. If two profiles define the same 
configuration, then the last listed profile has priority. Consider:

```properties
smallrye.config.profile=common,dev

my.prop=1234
%common.my.prop=0
%dev.my.prop=5678

%common.commom.prop=common
%dev.dev.prop=dev
%test.test.prop=test
```

Then

- `common.prop` value is `common`
- `dev.prop` value is `dev`
- `my.prop` value is `5678`
- `test.prop` does not have a value

## Parent Profile

A Parent Profile adds one level of hierarchy to the current profile. The configuration `smallrye.config.profile.parent` 
accepts a single profile name.

When the Parent Profile is active, if a property cannot be found in the current active Profile, the config lookup 
fallbacks to the Parent Profile. Consider:

```properties
smallrye.config.profile=dev
smallrye.config.profile.parent=common

my.prop=1234
%common.my.prop=0
%dev.my.prop=5678

%common.commom.prop=common
%dev.dev.prop=dev
%test.test.prop=test
```

Then

- `common.prop` value is `common`
- `dev.prop` value is `dev`
- `my.prop` value is `0`
- `test.prop` does not have a value
