# Profiles

Applications often require different configurations depending on the target environment. For example, the local 
development environment may be different from the production environment. Profiles allow for multiple configurations 
in the same file or separate files and select between them via a profile name.

## Profile aware properties

To be able to set properties with the same name, each property needs to be prefixed with a percentage sign `%` followed 
by the profile name and a dot `.` in the syntax `%{profile-name}.config.name`:

!!! example

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

!!! example 

    
    ```properties title="microprofile-config.properties"
    http.port=8080
    ```

    ```properties title="microprofile-config-dev.properties"
    http.port=8181
    ```

In this style, the property names in the profile aware file do not need to be prefixed with the profile name.

!!! note

    Properties in the profile aware file have priority over profile aware properties defined in the main file.

!!! attention

    Do not use Profile aware files to set `smallrye.config.profile`. This will not work because the 
    the profile is required in advance to load the profile aware files.

## Priority

Profile lookups are only valid if the `ConfigSource` has a higher ordinal than a lookup to the regular configuration 
name. Consider:

!!! example

    ```properties title="main.properties"
    config_ordinal=1000
    http.port=8080
    ```

    ```properties title="profile.properties"
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

!!! example
    
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

A Parent Profile adds multiple levels of hierarchy to the current profile. The configuration 
`smallrye.config.profile.parent` also acccepts a comma-separated list of profile names.

When the Parent Profile is active, if a property cannot be found in the current active Profile, the config lookup 
fallbacks to the Parent Profile. Consider:

!!! example

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

!!! attention

    Do not use Profile aware files to set smallrye.config.profile.parent`. This will not work because the 
    the profile is required in advance to load the profile aware files.

### Multi-level Hierarchy

The Parent Profile also supports multiple levels of hierarchies:

!!! example

    ```properties
    smallrye.config.profile=child
    %child.smallrye.config.profile.parent=parent
    %parent.smallrye.config.profile.parent=grandparent
    %grandparent.smallrye.config.profile.parent=greatgrandparent
    %greatgrandparent.smallrye.config.profile.parent=end
    ```

Will load the following profiles in order: `child`, `parent`, `grandparent`, `greatgrandparent`, `end` 

