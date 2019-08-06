# Config events

Util library for config sources that fire events on changes.

## Usage

```xml

    <dependency>
        <groupId>io.smallrye.config</groupId>
        <artifactId>smallrye-config-events-1.3</artifactId>
        <version>XXXX</version>
    </dependency>

```

## The event

The CDI Event is a `ChangeEvent` and contains the following fields: 

* String key
* Optional (String) oldValue
* String newValue 
* Type type
* String fromSource

There are 3 types: 

* NEW - When you create a new key and value (i.e. the key does not exist anywhere in any config source)
* UPDATE - When you update a value of an existing key (i.e. the key and value exist somewhere in a config source)
* REMOVE - When you remove the value from the source (and that changed the overall config)

### Observing events:

You can listen to all or some of these events, filtering by `type` and/or `key` and/or `source`, example:

```java

    // Getting all config event
    public void all(@Observes ChangeEvent changeEvent){
        log.log(Level.SEVERE, "ALL: Received a config change event: {0}", changeEvent);
    }
    
    // Get only new values
    public void newValue(@Observes @TypeFilter(Type.NEW) ChangeEvent changeEvent){
        log.log(Level.SEVERE, "NEW: Received a config change event: {0}", changeEvent);
    }
    
    // Get only override values
    public void overrideValue(@Observes @TypeFilter(Type.UPDATE) ChangeEvent changeEvent){
        log.log(Level.SEVERE, "UPDATE: Received a config change event: {0}", changeEvent);
    }
    
    // Get only revert values
    public void revertValue(@Observes @TypeFilter(Type.REMOVE) ChangeEvent changeEvent){
        log.log(Level.SEVERE, "REMOVE: Received a config change event: {0}", changeEvent);
    }
    
    // Getting all config event when key is some.key
    public void allForKey(@Observes @KeyFilter("some.key") ChangeEvent changeEvent){
        log.log(Level.SEVERE, "ALL for key [some.key]: Received a config change event: {0}", changeEvent);
    }
    
    // Getting all config event when key is some.key for new events
    public void newForKey(@Observes @TypeFilter(Type.NEW) @KeyFilter("some.key") ChangeEvent changeEvent){
        log.log(Level.SEVERE, "NEW for key [some.key]: Received a config change event: {0}", changeEvent);
    }
    
    // Getting all config event when key is some.key for override events
    public void overrideForKey(@Observes @TypeFilter(Type.UPDATE) @KeyFilter("some.key") ChangeEvent changeEvent){
        log.log(Level.SEVERE, "UPDATE for key [some.key]: Received a config change event: {0}", changeEvent);
    }
    
    // Getting all config event when key is some.key for revert events
    public void revertForKey(@Observes @TypeFilter(Type.REMOVE) @KeyFilter("some.key") ChangeEvent changeEvent){
        log.log(Level.SEVERE, "REMOVE for key [some.key]: Received a config change event: {0}", changeEvent);
    }
    
    // Getting all config events for a certain source
    public void allForSource(@Observes @SourceFilter("MemoryConfigSource") ChangeEvent changeEvent){
        log.log(Level.SEVERE, "ALL for source [MemoryConfigSource]: Received a config change event: {0}", changeEvent);
    }
    
    // Getting all config events for a certain source
    public void allForSourceAndKey(@Observes @SourceFilter("MemoryConfigSource") @KeyFilter("some.key")  ChangeEvent changeEvent){
        log.log(Level.SEVERE, "ALL for source [MemoryConfigSource] and for key [some.key]: Received a config change event: {0}", changeEvent);
    }
    
    // Getting all config events for a certain source
    public void overrideForSourceAndKey(@Observes @TypeFilter(Type.UPDATE) @SourceFilter("MemoryConfigSource") @KeyFilter("some.key")  ChangeEvent changeEvent){
        log.log(Level.SEVERE, "UPDATE for source [MemoryConfigSource] and for key [some.key]: Received a config change event: {0}", changeEvent);
    }

```

Note: You can filter by including the `@TypeFilter` and/or the `@KeyFilter` and/or the `@SourceFilter`.


### Pattern matching on field.

You might want to listen for fields that match a certain regex.

Example, listen to all keys that starts with `some.`:

```java

    @RegexFilter("^some\\..+") 
    public void allForPatternMatchOnKey(@Observes ChangeEvent changeEvent){
        log.log(Level.SEVERE, "Pattern match on key: Received a config change event: {0}", changeEvent);
    }

```

By default, it will match on `key`, however you also listen on another field, 
for example, listen to all `oldValue` that starts with `some.`:

```java

    @RegexFilter(onField = Field.oldValue, value = "^some\\..+")
    public void allForPatternMatchOnOldValue(@Observes ChangeEvent changeEvent){
        log.log(Level.SEVERE, "Pattern match on old value: Received a config change event: {0}", changeEvent);
    }

```

You can Match on the following fields of the `ChangeEvent` object:

* key
* oldValue
* newValue
* fromSource

## Implementing this for your own Config source

An example of a source that uses this is [Memory Config source](https://github.com/smallrye/smallrye-config/tree/master/extensions/sources/memory)

`io.smallrye.config.events.ChangeEventNotifier` is a bean that makes it easy to detect changes and fire the appropriate events. 

To use it in your own source:

* Get a snapshot of the properties before the change.
* Get a snapshot of the properties after the change.
* Call `detectChangesAndFire` method:

Example: 

```java
    
    Map<String,String> before = new HashMap<>(memoryConfigSource.getProperties());
    memoryConfigSource.getProperties().remove(key);
    Map<String,String> after = new HashMap<>(memoryConfigSource.getProperties());
    ChangeEventNotifier.getInstance().detectChangesAndFire(before, after,MemoryConfigSource.NAME)

```

or if you know the change and do not need detection:

```java

    memoryConfigSource.getProperties().remove(key);
    ChangeEventNotifier.getInstance().fire(new ChangeEvent(Type.REMOVE,key,getOptionalOldValue(oldValue),null,MemoryConfigSource.NAME));

```