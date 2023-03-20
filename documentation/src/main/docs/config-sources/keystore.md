# KeyStore Config Source

This Config Source allows to use a Java `KeyStore` to load configuration values. It uses an ordinal of `100`.

The following dependency is required in the classpath to use the KeyStore Config Source:

```xml
<dependency>
    <groupId>io.smallrye.config</groupId>
    <artifactId>smallrye-config-source-keystore</artifactId>
    <version>{{attributes['version']}}</version>
</dependency>
```

## Configuration

| Configuration Property 	                                                                                            | Type 	| Default 	 |
|---------------------------------------------------------------------------------------------------------------------|---	|----|
| `smallrye.config.source.keystore."name".path`<br>The KeyStore path. 	                                            | String 	| 	  |
| `smallrye.config.source.keystore."name".password`<br>The KeyStore password. 	                                    | String 	| 	  |
| `smallrye.config.source.keystore."name".type`<br>The KeyStore type. 	                                            | String 	| `PKCS12` |
| `smallrye.config.source.keystore."name".handler`<br>An Optional secret keys handler.	                            | String 	| 	  |
| `smallrye.config.source.keystore."name".aliases."key".name`<br>An Optional aliases key name. 	                   | String 	| 	  |
| `smallrye.config.source.keystore."name".aliases."key".password`<br>An Optional aliases key password. 	           | String 	| 	  |
| `smallrye.config.source.keystore."name".aliases."key".handler`<br>An Optional aliases key secret keys handler. 	 | String 	| 	  |
