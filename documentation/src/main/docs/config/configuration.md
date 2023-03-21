# Configuration Reference

| Configuration Property 	                                                                                                                                                                                                                       | Type 	     | Default 	 |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|-----------|
| `smallrye.config.profile`<br>The main [Profile](profiles.md) to activate. 	                                                                                                                                                                    | String[] 	 | 	         |
| `smallrye.config.profile.parent`<br>The parent [Profile](profiles.md#parent-profile) to activate.	                                                                                                                                             | String 	   | 	         |
| `smallrye.config.locations`<br>[Additional config locations](../config-sources/locations.md) to be loaded with the Config. The configuration supports multiple locations separated by a comma and each must represent a valid `java.net.URI`.	 | URI[] 	    | 	         |
| `smallrye.config.mapping.validate-unknown`<br>[Validates](mappings.md#retrieval) that a `@ConfigMapping` maps every available configuration name contained in the      mapping prefix.	                                                        | boolean 	  | false	    |

