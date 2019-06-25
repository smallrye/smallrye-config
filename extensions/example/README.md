# Example Application

This is a very basic example application to demonstrate the Config extensions.

## Running the example.

Using maven, you can start this application using Quarkus, Thorntail, Payara or OpenLiberty:
   
```
    mvn -Pquarkus clean install
```
or
```
    mvn -Pthorntail clean install
```
or
```
    mvn -Ppayara clean install
```
or
```
    mvn -Popenliberty clean install
```

You can then go to http://localhost:8080/api/openapi-ui 

### See the events in action

If you want to see the events in action, look at the log file file changing a value in `src/main/resources/examples/config.yaml`

You should see the event printing out, example:

```
SEVERE [io.sma.con.exa.EventChangeListener] (pool-2-thread-1) UPDATE: Received a config change event: ChangeEvent{type=UPDATE, key=ylocation.port, oldValue=Optional[8080], newValue=8081, fromSource=YamlConfigSource}
SEVERE [io.sma.con.exa.EventChangeListener] (pool-2-thread-1) ALL: Received a config change event: ChangeEvent{type=UPDATE, key=ylocation.port, oldValue=Optional[8080], newValue=8081, fromSource=YamlConfigSource}
```

That comes from `io.smallrye.config.example.EventChangeListener`:


``` 
    
    public void all(@Observes ChangeEvent changeEvent){
        log.log(Level.SEVERE, "ALL: Received a config change event: {0}", changeEvent);
    }

    public void overrideValue(@Observes @TypeFilter(Type.UPDATE) ChangeEvent changeEvent){
        log.log(Level.SEVERE, "UPDATE: Received a config change event: {0}", changeEvent);
    }
```