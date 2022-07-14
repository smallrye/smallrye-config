# Configuration Reference

## `smallrye.config.profile`

The main [Profile](profiles.md) to activate.

> * type: String[] 
> * default:

## `smallrye.config.profile.parent`

The parent [Profile](profiles.md#parent-profile) to activate.

> * type: String[]
> * default:

## `smallrye.config.locations`

[Additional config locations](../config-sources/locations.md) to be loaded with the Config. The configuration supports 
multiple locations separated by a comma and each must represent a valid `java.net.URI`.

> * type: URI[]
> * default:

## `smallrye.config.mapping.validate-unknown`

[Validates](mappings.md#retrieval) that a `@ConfigMapping` maps every available configuration name contained in the 
mapping prefix.

> * type: boolean
> * default: false


