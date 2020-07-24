package io.smallrye.config;

import static io.smallrye.config.ConfigMappingInterface.GroupProperty;
import static io.smallrye.config.ConfigMappingInterface.LeafProperty;
import static io.smallrye.config.ConfigMappingInterface.MapProperty;
import static io.smallrye.config.ConfigMappingInterface.MayBeOptionalProperty;
import static io.smallrye.config.ConfigMappingInterface.PrimitiveProperty;
import static io.smallrye.config.ConfigMappingInterface.Property;
import static io.smallrye.config.ConfigMappingInterface.getConfigurationInterface;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.function.Functions;

/**
 *
 */
final class ConfigMappingProvider implements Serializable {
    private static final long serialVersionUID = 3977667610888849912L;

    /**
     * The do-nothing action is used when the matched property is eager.
     */
    private static final BiConsumer<ConfigMappingContext, NameIterator> DO_NOTHING = Functions.discardingBiConsumer();
    private static final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> IGNORE_EVERYTHING;

    static {
        final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> map = new KeyMap<>();
        map.putRootValue(DO_NOTHING);
        IGNORE_EVERYTHING = map;
    }

    private final Map<String, List<ConfigMappingInterface>> roots;
    private final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions;
    private final KeyMap<String> defaultValues;

    ConfigMappingProvider(final Builder builder) {
        roots = new HashMap<>(builder.roots);
        final ArrayDeque<String> currentPath = new ArrayDeque<>();
        KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions = new KeyMap<>();
        KeyMap<String> defaultValues = new KeyMap<>();
        for (Map.Entry<String, List<ConfigMappingInterface>> entry : roots.entrySet()) {
            NameIterator rootNi = new NameIterator(entry.getKey());
            while (rootNi.hasNext()) {
                final String nextSegment = rootNi.getNextSegment();
                if (!nextSegment.isEmpty()) {
                    currentPath.add(nextSegment);
                }
                rootNi.next();
            }
            List<ConfigMappingInterface> roots = entry.getValue();
            for (ConfigMappingInterface root : roots) {
                // construct the lazy match actions for each group
                BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> ef = new GetRootAction(root, entry);
                processEagerGroup(currentPath, matchActions, defaultValues, root, ef);
            }
            currentPath.clear();
        }
        for (String[] ignoredPath : builder.ignored) {
            int len = ignoredPath.length;
            KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> found;
            if (ignoredPath[len - 1].equals("**")) {
                found = matchActions.findOrAdd(ignoredPath, 0, len - 1);
                found.putRootValue(DO_NOTHING);
                found.putAny(IGNORE_EVERYTHING);
            } else {
                found = matchActions.findOrAdd(ignoredPath);
                found.putRootValue(DO_NOTHING);
            }
        }
        this.matchActions = matchActions;
        this.defaultValues = defaultValues;
    }

    static String skewer(Method method) {
        return skewer(method.getName());
    }

    static String skewer(String camelHumps) {
        return skewer(camelHumps, 0, camelHumps.length(), new StringBuilder());
    }

    static String skewer(String camelHumps, int start, int end, StringBuilder b) {
        assert !camelHumps.isEmpty() : "Method seems to have an empty name";
        int cp = camelHumps.codePointAt(start);
        b.appendCodePoint(Character.toLowerCase(cp));
        start += Character.charCount(cp);
        if (start == end) {
            // a lonely character at the end of the string
            return b.toString();
        }
        if (Character.isUpperCase(cp)) {
            // all-uppercase words need one code point of lookahead
            int nextCp = camelHumps.codePointAt(start);
            if (Character.isUpperCase(nextCp)) {
                // it's some kind of `WORD`
                for (;;) {
                    b.appendCodePoint(Character.toLowerCase(cp));
                    start += Character.charCount(cp);
                    cp = nextCp;
                    if (start == end) {
                        return b.toString();
                    }
                    nextCp = camelHumps.codePointAt(start);
                    // combine non-letters in with this name
                    if (Character.isLowerCase(nextCp)) {
                        b.append('-');
                        return skewer(camelHumps, start, end, b);
                    }
                }
                // unreachable
            } else {
                // it was the start of a `Word`; continue until we hit the end or an uppercase.
                b.appendCodePoint(nextCp);
                start += Character.charCount(nextCp);
                for (;;) {
                    if (start == end) {
                        return b.toString();
                    }
                    cp = camelHumps.codePointAt(start);
                    // combine non-letters in with this name
                    if (Character.isUpperCase(cp)) {
                        b.append('-');
                        return skewer(camelHumps, start, end, b);
                    }
                    b.appendCodePoint(cp);
                    start += Character.charCount(cp);
                }
                // unreachable
            }
            // unreachable
        } else {
            // it's some kind of `word`
            for (;;) {
                cp = camelHumps.codePointAt(start);
                // combine non-letters in with this name
                if (Character.isUpperCase(cp)) {
                    b.append('-');
                    return skewer(camelHumps, start, end, b);
                }
                b.appendCodePoint(cp);
                start += Character.charCount(cp);
                if (start == end) {
                    return b.toString();
                }
            }
            // unreachable
        }
        // unreachable
    }

    static final class ConsumeOneAndThen implements BiConsumer<ConfigMappingContext, NameIterator> {
        private final BiConsumer<ConfigMappingContext, NameIterator> delegate;

        ConsumeOneAndThen(final BiConsumer<ConfigMappingContext, NameIterator> delegate) {
            this.delegate = delegate;
        }

        public void accept(final ConfigMappingContext context, final NameIterator nameIterator) {
            nameIterator.previous();
            delegate.accept(context, nameIterator);
            nameIterator.next();
        }
    }

    static final class ConsumeOneAndThenFn<T> implements BiFunction<ConfigMappingContext, NameIterator, T> {
        private final BiFunction<ConfigMappingContext, NameIterator, T> delegate;

        ConsumeOneAndThenFn(final BiFunction<ConfigMappingContext, NameIterator, T> delegate) {
            this.delegate = delegate;
        }

        public T apply(final ConfigMappingContext context, final NameIterator nameIterator) {
            nameIterator.previous();
            T result = delegate.apply(context, nameIterator);
            nameIterator.next();
            return result;
        }
    }

    private void processEagerGroup(final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions, final KeyMap<String> defaultValues,
            final ConfigMappingInterface group,
            final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> getEnclosingFunction) {
        Class<?> type = group.getInterfaceType();
        int pc = group.getPropertyCount();
        int pathLen = currentPath.size();
        HashSet<String> usedProperties = new HashSet<>();
        for (int i = 0; i < pc; i++) {
            Property property = group.getProperty(i);
            String memberName = property.getMethod().getName();
            if (usedProperties.add(memberName)) {
                // process by property type
                if (!property.isParentPropertyName()) {
                    String propertyName = property.hasPropertyName() ? property.getPropertyName()
                            : skewer(property.getMethod());
                    NameIterator ni = new NameIterator(propertyName);
                    while (ni.hasNext()) {
                        currentPath.add(ni.getNextSegment());
                        ni.next();
                    }
                }
                if (property.isOptional()) {
                    // switch to lazy mode
                    MayBeOptionalProperty nestedProperty = property.asOptional().getNestedProperty();
                    if (nestedProperty.isGroup()) {
                        GroupProperty nestedGroup = nestedProperty.asGroup();
                        // on match, always create the outermost group, which recursively creates inner groups
                        GetOrCreateEnclosingGroupInGroup matchAction = new GetOrCreateEnclosingGroupInGroup(
                                getEnclosingFunction, group, nestedGroup);
                        GetFieldOfEnclosing ef = new GetFieldOfEnclosing(
                                nestedGroup.isParentPropertyName() ? getEnclosingFunction
                                        : new ConsumeOneAndThenFn<>(getEnclosingFunction),
                                type, memberName);
                        processLazyGroupInGroup(currentPath, matchActions, defaultValues, nestedGroup, ef, matchAction,
                                new HashSet<>());
                    } else if (nestedProperty.isLeaf()) {
                        LeafProperty leafProperty = nestedProperty.asLeaf();
                        if (leafProperty.hasDefaultValue()) {
                            defaultValues.findOrAdd(currentPath).putRootValue(leafProperty.getDefaultValue());
                        }
                        matchActions.findOrAdd(currentPath).putRootValue(DO_NOTHING);
                    }
                } else if (property.isGroup()) {

                    processEagerGroup(currentPath, matchActions, defaultValues, property.asGroup().getGroupType(),
                            new GetOrCreateEnclosingGroupInGroup(getEnclosingFunction, group, property.asGroup()));
                } else if (property.isPrimitive()) {
                    // already processed eagerly
                    PrimitiveProperty primitiveProperty = property.asPrimitive();
                    if (primitiveProperty.hasDefaultValue()) {
                        defaultValues.findOrAdd(currentPath).putRootValue(primitiveProperty.getDefaultValue());
                    }
                    matchActions.findOrAdd(currentPath).putRootValue(DO_NOTHING);
                } else if (property.isLeaf()) {
                    // already processed eagerly
                    LeafProperty leafProperty = property.asLeaf();
                    if (leafProperty.hasDefaultValue()) {
                        defaultValues.findOrAdd(currentPath).putRootValue(leafProperty.getDefaultValue());
                    }
                    // ignore with no error message
                    matchActions.findOrAdd(currentPath).putRootValue(DO_NOTHING);
                } else if (property.isMap()) {
                    // the enclosure of the map is this group
                    processLazyMapInGroup(currentPath, matchActions, defaultValues, property.asMap(), getEnclosingFunction,
                            group);
                }
                while (currentPath.size() > pathLen) {
                    currentPath.removeLast();
                }
            }
        }
        int sc = group.getSuperTypeCount();
        for (int i = 0; i < sc; i++) {
            processEagerGroup(currentPath, matchActions, defaultValues, group.getSuperType(i), getEnclosingFunction);
        }
    }

    private void processLazyGroupInGroup(ArrayDeque<String> currentPath,
            KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            KeyMap<String> defaultValues,
            GroupProperty groupProperty,
            BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> getEnclosingFunction,
            BiConsumer<ConfigMappingContext, NameIterator> matchAction, HashSet<String> usedProperties) {
        ConfigMappingInterface group = groupProperty.getGroupType();
        int pc = group.getPropertyCount();
        int pathLen = currentPath.size();
        for (int i = 0; i < pc; i++) {
            Property property = group.getProperty(i);
            if (!property.isParentPropertyName()) {
                String propertyName = property.hasPropertyName() ? property.getPropertyName()
                        : skewer(property.getMethod());
                NameIterator ni = new NameIterator(propertyName);
                while (ni.hasNext()) {
                    currentPath.add(ni.getNextSegment());
                    ni.next();
                }
            }
            if (usedProperties.add(property.getMethod().getName())) {
                boolean optional = property.isOptional();
                if (optional && property.asOptional().getNestedProperty().isGroup()) {
                    GroupProperty nestedGroup = property.asOptional().getNestedProperty().asGroup();
                    GetOrCreateEnclosingGroupInGroup nestedMatchAction = new GetOrCreateEnclosingGroupInGroup(
                            property.isParentPropertyName() ? getEnclosingFunction
                                    : new ConsumeOneAndThenFn<>(getEnclosingFunction),
                            group, nestedGroup);
                    processLazyGroupInGroup(currentPath, matchActions, defaultValues, nestedGroup, nestedMatchAction,
                            nestedMatchAction, new HashSet<>());
                } else if (property.isGroup()) {
                    GroupProperty asGroup = property.asGroup();
                    GetOrCreateEnclosingGroupInGroup nestedEnclosingFunction = new GetOrCreateEnclosingGroupInGroup(
                            property.isParentPropertyName() ? getEnclosingFunction
                                    : new ConsumeOneAndThenFn<>(getEnclosingFunction),
                            group, asGroup);
                    BiConsumer<ConfigMappingContext, NameIterator> nestedMatchAction;
                    nestedMatchAction = matchAction;
                    if (!property.isParentPropertyName()) {
                        nestedMatchAction = new ConsumeOneAndThen(nestedMatchAction);
                    }
                    processLazyGroupInGroup(currentPath, matchActions, defaultValues, asGroup, nestedEnclosingFunction,
                            nestedMatchAction, usedProperties);
                } else if (property.isLeaf() || property.isPrimitive()
                        || optional && property.asOptional().getNestedProperty().isLeaf()) {
                    BiConsumer<ConfigMappingContext, NameIterator> actualAction;
                    if (!property.isParentPropertyName()) {
                        actualAction = new ConsumeOneAndThen(matchAction);
                    } else {
                        actualAction = matchAction;
                    }
                    matchActions.findOrAdd(currentPath).putRootValue(actualAction);
                    if (property.isPrimitive()) {
                        PrimitiveProperty primitiveProperty = property.asPrimitive();
                        if (primitiveProperty.hasDefaultValue()) {
                            defaultValues.findOrAdd(currentPath).putRootValue(primitiveProperty.getDefaultValue());
                        }
                    } else if (property.isLeaf()) {
                        LeafProperty leafProperty = property.asLeaf();
                        if (leafProperty.hasDefaultValue()) {
                            defaultValues.findOrAdd(currentPath).putRootValue(leafProperty.getDefaultValue());
                        }
                    } else {
                        LeafProperty leafProperty = property.asOptional().getNestedProperty().asLeaf();
                        if (leafProperty.hasDefaultValue()) {
                            defaultValues.findOrAdd(currentPath).putRootValue(leafProperty.getDefaultValue());
                        }
                    }
                } else if (property.isMap()) {
                    processLazyMapInGroup(currentPath, matchActions, defaultValues, property.asMap(), getEnclosingFunction,
                            group);
                }
            }
            while (currentPath.size() > pathLen) {
                currentPath.removeLast();
            }
        }
        int sc = group.getSuperTypeCount();
        for (int i = 0; i < sc; i++) {
            processLazyGroupInGroup(currentPath, matchActions, defaultValues, groupProperty, getEnclosingFunction,
                    matchAction, usedProperties);
        }
    }

    private void processLazyMapInGroup(final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions, final KeyMap<String> defaultValues,
            final MapProperty property, BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> getEnclosingGroup,
            ConfigMappingInterface enclosingGroup) {
        GetOrCreateEnclosingMapInGroup getEnclosingMap = new GetOrCreateEnclosingMapInGroup(getEnclosingGroup, enclosingGroup,
                property);
        processLazyMap(currentPath, matchActions, defaultValues, property, getEnclosingMap, enclosingGroup);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void processLazyMap(final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions, final KeyMap<String> defaultValues,
            final MapProperty property, BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> getEnclosingMap,
            ConfigMappingInterface enclosingGroup) {
        Property valueProperty = property.getValueProperty();
        Class<? extends Converter<?>> keyConvertWith = property.hasKeyConvertWith() ? property.getKeyConvertWith() : null;
        Class<?> keyRawType = property.getKeyRawType();

        currentPath.addLast("*");
        if (valueProperty.isLeaf()) {
            LeafProperty leafProperty = valueProperty.asLeaf();
            Class<? extends Converter<?>> valConvertWith = leafProperty.getConvertWith();
            Class<?> valueRawType = leafProperty.getValueRawType();

            matchActions.findOrAdd(currentPath).putRootValue((mc, ni) -> {
                StringBuilder sb = mc.getStringBuilder();
                sb.setLength(0);
                sb.append(ni.getAllPreviousSegments());
                String configKey = sb.toString();
                Map<?, ?> map = getEnclosingMap.apply(mc, ni);
                String rawMapKey = ni.getPreviousSegment();
                Converter<?> keyConv;
                SmallRyeConfig config = mc.getConfig();
                if (keyConvertWith != null) {
                    keyConv = mc.getConverterInstance(keyConvertWith);
                } else {
                    keyConv = config.getConverter(keyRawType);
                }
                Object key = keyConv.convert(rawMapKey);
                Converter<?> valueConv;
                if (valConvertWith != null) {
                    valueConv = mc.getConverterInstance(valConvertWith);
                } else {
                    valueConv = config.getConverter(valueRawType);
                }
                ((Map) map).put(key, config.getValue(configKey, valueConv));
            });
        } else if (valueProperty.isMap()) {
            processLazyMap(currentPath, matchActions, defaultValues, valueProperty.asMap(), (mc, ni) -> {
                ni.previous();
                Map<?, ?> enclosingMap = getEnclosingMap.apply(mc, ni);
                ni.next();
                String rawMapKey = ni.getPreviousSegment();
                Converter<?> keyConv;
                SmallRyeConfig config = mc.getConfig();
                if (keyConvertWith != null) {
                    keyConv = mc.getConverterInstance(keyConvertWith);
                } else {
                    keyConv = config.getConverter(keyRawType);
                }
                Object key = keyConv.convert(rawMapKey);
                return (Map) ((Map) enclosingMap).computeIfAbsent(key, x -> new HashMap<>());
            }, enclosingGroup);
        } else {
            assert valueProperty.isGroup();
            final GetOrCreateEnclosingGroupInMap ef = new GetOrCreateEnclosingGroupInMap(getEnclosingMap, property,
                    enclosingGroup, valueProperty.asGroup());
            processLazyGroupInGroup(currentPath, matchActions, defaultValues, valueProperty.asGroup(),
                    ef, ef, new HashSet<>());
        }
        currentPath.removeLast();
    }

    static class GetRootAction implements BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> {
        private final ConfigMappingInterface root;
        private final Map.Entry<String, List<ConfigMappingInterface>> entry;

        GetRootAction(final ConfigMappingInterface root, final Map.Entry<String, List<ConfigMappingInterface>> entry) {
            this.root = root;
            this.entry = entry;
        }

        public ConfigMappingObject apply(final ConfigMappingContext mc, final NameIterator ni) {
            return mc
                    .getRoot(root.getInterfaceType(), entry.getKey());
        }
    }

    static class GetOrCreateEnclosingGroupInGroup
            implements BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject>,
            BiConsumer<ConfigMappingContext, NameIterator> {
        private final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> delegate;
        private final ConfigMappingInterface enclosingGroup;
        private final GroupProperty enclosedGroup;

        GetOrCreateEnclosingGroupInGroup(final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> delegate,
                final ConfigMappingInterface enclosingGroup, final GroupProperty enclosedGroup) {
            this.delegate = delegate;
            this.enclosingGroup = enclosingGroup;
            this.enclosedGroup = enclosedGroup;
        }

        public ConfigMappingObject apply(final ConfigMappingContext context, final NameIterator ni) {
            ConfigMappingObject ourEnclosing = delegate.apply(context, ni);
            Class<?> enclosingType = enclosingGroup.getInterfaceType();
            String methodName = enclosedGroup.getMethod().getName();
            ConfigMappingObject val = (ConfigMappingObject) context.getEnclosedField(enclosingType, methodName, ourEnclosing);
            if (val == null) {
                // it must be an optional group
                StringBuilder sb = context.getStringBuilder();
                sb.replace(0, sb.length(), ni.getAllPreviousSegments());
                val = (ConfigMappingObject) context.constructGroup(enclosedGroup.getGroupType().getInterfaceType());
                context.registerEnclosedField(enclosingType, methodName, ourEnclosing, val);
            }
            return val;
        }

        public void accept(final ConfigMappingContext context, final NameIterator nameIterator) {
            apply(context, nameIterator);
        }
    }

    static class GetOrCreateEnclosingGroupInMap implements BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject>,
            BiConsumer<ConfigMappingContext, NameIterator> {
        final BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> getEnclosingMap;
        final MapProperty enclosingMap;
        final ConfigMappingInterface enclosingGroup;
        private final GroupProperty enclosedGroup;

        GetOrCreateEnclosingGroupInMap(final BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> getEnclosingMap,
                final MapProperty enclosingMap, final ConfigMappingInterface enclosingGroup,
                final GroupProperty enclosedGroup) {
            this.getEnclosingMap = getEnclosingMap;
            this.enclosingMap = enclosingMap;
            this.enclosingGroup = enclosingGroup;
            this.enclosedGroup = enclosedGroup;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ConfigMappingObject apply(final ConfigMappingContext context, final NameIterator ni) {
            ni.previous();
            Map<?, ?> ourEnclosing = getEnclosingMap.apply(context, ni);
            ni.next();
            String mapKey = ni.getPreviousSegment();
            Converter<?> keyConverter = context.getKeyConverter(enclosingGroup.getInterfaceType(),
                    enclosingMap.getMethod().getName(), enclosingMap.getLevels() - 1);
            ConfigMappingObject val = (ConfigMappingObject) ourEnclosing.get(mapKey);
            if (val == null) {
                StringBuilder sb = context.getStringBuilder();
                sb.replace(0, sb.length(), ni.getAllPreviousSegments());
                Object convertedKey = keyConverter.convert(mapKey);
                ((Map) ourEnclosing).put(convertedKey,
                        val = (ConfigMappingObject) context.constructGroup(enclosedGroup.getGroupType().getInterfaceType()));
            }
            return val;
        }

        public void accept(final ConfigMappingContext context, final NameIterator ni) {
            apply(context, ni);
        }
    }

    static class GetOrCreateEnclosingMapInGroup implements BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>>,
            BiConsumer<ConfigMappingContext, NameIterator> {
        final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> getEnclosingGroup;
        final ConfigMappingInterface enclosingGroup;
        final MapProperty property;

        GetOrCreateEnclosingMapInGroup(
                final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> getEnclosingGroup,
                final ConfigMappingInterface enclosingGroup, final MapProperty property) {
            this.getEnclosingGroup = getEnclosingGroup;
            this.enclosingGroup = enclosingGroup;
            this.property = property;
        }

        public Map<?, ?> apply(final ConfigMappingContext context, final NameIterator ni) {
            boolean consumeName = !property.isParentPropertyName();
            if (consumeName)
                ni.previous();
            ConfigMappingObject ourEnclosing = getEnclosingGroup.apply(context, ni);
            if (consumeName)
                ni.next();
            Class<?> enclosingType = enclosingGroup.getInterfaceType();
            String methodName = property.getMethod().getName();
            Map<?, ?> val = (Map<?, ?>) context.getEnclosedField(enclosingType, methodName, ourEnclosing);
            if (val == null) {
                // map is not yet constructed
                val = new HashMap<>();
                context.registerEnclosedField(enclosingType, methodName, ourEnclosing, val);
            }
            return val;
        }

        public void accept(final ConfigMappingContext context, final NameIterator ni) {
            apply(context, ni);
        }
    }

    static class GetFieldOfEnclosing implements BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> {
        private final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> getEnclosingFunction;
        private final Class<?> type;
        private final String memberName;

        GetFieldOfEnclosing(final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> getEnclosingFunction,
                final Class<?> type, final String memberName) {
            this.getEnclosingFunction = getEnclosingFunction;
            this.type = type;
            this.memberName = memberName;
        }

        public ConfigMappingObject apply(final ConfigMappingContext mc, final NameIterator ni) {
            ConfigMappingObject outer = getEnclosingFunction.apply(mc, ni);
            // eagerly populated groups will always exist
            return (ConfigMappingObject) mc.getEnclosedField(type, memberName, outer);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static Map<?, ?> getOrCreateEnclosingMapInMap(
            ConfigMappingContext context, NameIterator ni,
            BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> getEnclosingMap, ConfigMappingInterface enclosingGroup,
            MapProperty property) {
        ni.previous();
        Map<?, ?> ourEnclosing = getEnclosingMap.apply(context, ni);
        String mapKey = ni.getNextSegment();
        Converter<?> keyConverter = context.getKeyConverter(enclosingGroup.getInterfaceType(), property.getMethod().getName(),
                property.getLevels() - 1);
        Object realKey = keyConverter.convert(mapKey);
        Map<?, ?> map = (Map<?, ?>) ourEnclosing.get(realKey);
        if (map == null) {
            map = new HashMap<>();
            ((Map) ourEnclosing).put(realKey, map);
        }
        ni.next();
        return map;
    }

    public static Builder builder() {
        return new Builder();
    }

    KeyMap<String> getDefaultValues() {
        return defaultValues;
    }

    ConfigMappings mapConfiguration(SmallRyeConfig config) throws ConfigValidationException {
        return mapConfiguration(config, new ConfigMappings());
    }

    ConfigMappings mapConfiguration(SmallRyeConfig config, ConfigMappings mappings) throws ConfigValidationException {
        if (roots.isEmpty()) {
            return new ConfigMappings(new HashMap<>());
        }

        Assert.checkNotNullParam("config", config);
        final ConfigMappingContext context = new ConfigMappingContext(config);
        // eagerly populate roots
        for (Map.Entry<String, List<ConfigMappingInterface>> entry : roots.entrySet()) {
            String path = entry.getKey();
            List<ConfigMappingInterface> roots = entry.getValue();
            for (ConfigMappingInterface root : roots) {
                StringBuilder sb = context.getStringBuilder();
                sb.replace(0, sb.length(), path);
                Class<?> type = root.getInterfaceType();
                ConfigMappingObject group = (ConfigMappingObject) context.constructGroup(type);
                context.registerRoot(type, path, group);
            }
        }
        // lazily sweep
        for (String name : config.getPropertyNames()) {
            // filter properties in root
            if (!isPropertyInRoot(name)) {
                continue;
            }

            NameIterator ni = new NameIterator(name);
            BiConsumer<ConfigMappingContext, NameIterator> action = matchActions.findRootValue(ni);
            if (action != null) {
                action.accept(context, ni);
            } else {
                context.unknownConfigElement(name);
            }
        }
        ArrayList<ConfigValidationException.Problem> problems = context.getProblems();
        if (!problems.isEmpty()) {
            throw new ConfigValidationException(
                    problems.toArray(ConfigValidationException.Problem.NO_PROBLEMS));
        }
        context.fillInOptionals();

        mappings.registerConfigMappings(context.getRootsMap());
        return mappings;
    }

    private boolean isPropertyInRoot(String propertyName) {
        final Set<String> registeredRoots = roots.keySet();
        for (String registeredRoot : registeredRoots) {
            if (propertyName.startsWith(registeredRoot)) {
                return true;
            }
        }
        return false;
    }

    public static final class Builder {
        final Map<String, List<ConfigMappingInterface>> roots = new HashMap<>();
        final List<String[]> ignored = new ArrayList<>();

        Builder() {
        }

        public Builder addRoot(String path, Class<?> type) {
            Assert.checkNotNullParam("type", type);
            return addRoot(path, getConfigurationInterface(ConfigMappingClass.toInterface(type)));
        }

        public Builder addRoot(String path, ConfigMappingInterface info) {
            Assert.checkNotNullParam("path", path);
            Assert.checkNotNullParam("info", info);
            roots.computeIfAbsent(path, k -> new ArrayList<>(4)).add(info);
            return this;
        }

        public Builder addIgnored(String path) {
            Assert.checkNotNullParam("path", path);
            ignored.add(path.split("\\."));
            return this;
        }

        public ConfigMappingProvider build() {
            return new ConfigMappingProvider(this);
        }
    }
}
