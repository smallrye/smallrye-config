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

## Create a KeyStore

The following command creates a simple KeyStore

```bash
keytool -importpass -alias my.secret -keystore keystore -storepass secret -storetype PKCS12 -v
```

The `-alias my.secret` stores the configuration property name `my.secret` in the KeyStore. The command will 
interactively ask for the value to be stored in the KeyStore.

## Read the KeyStore

The KeyStore Config Source supports reading multiple keystore files:

```properties
smallrye.config.source.keystore.one.path=keystore-one
smallrye.config.source.keystore.one.password=password

smallrye.config.source.keystore.two.path=keystore-two
smallrye.config.source.keystore.two.password=password
```

The names are arbitrary and can be any name. The name `one` and `two` are used to distinguish both KeyStores.  

If a stored configuration property requires a [Secret Handler](../config/secret-keys.md) to decode a value, set 
the handler name with `smallrye.config.source.keystore."name".handler`.

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
