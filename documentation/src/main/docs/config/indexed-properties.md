# Indexed Properties

In [MicroProfile Config](https://github.com/eclipse/microprofile-config/), a config value with unescaped commas may be 
converted to `Collection`. It works for simple cases, but it becomes cumbersome and limited for more advanced use cases.

Indexed Properties provide a way to use indexes in config property names to map specific elements in a `Collection` 
type. Since the indexed element is part of the property name, it can also map complex object types. Consider:

```properties
# MicroProfile Config - Collection Values
my.collection=dog,cat,turtle

# SmallRye Config - Indexed Property
my.indexed.collection[0]=dog
my.indexed.collection[1]=cat
my.indexed.collection[2]=turtle
```

The indexed property syntax uses the property name and square brackets with an index in between.

A call to `Config#getValues("my.collection", String.class)`, will automatically create and convert a `List<String>` 
that contains the values `dog`, `cat` and `turtle`. A call to `Config#getValues("my.indexed.collection", String.class)` 
returns the exact same result. The indexed property format is prioritized when both styles are found in the same 
configuration source. When available in multiple sources, the higher ordinal source wins, like any other configuration 
lookup.

The indexed property is sorted by its index before being added to the target `Collection`. Any gaps in the indexes do 
not resolve to the target `Collection`, which means that the `Collection` result will store all values without empty 
elements.
