package io.smallrye.config;

import static io.smallrye.config.ConfigMappingInterface.GroupProperty;
import static io.smallrye.config.ConfigMappingInterface.LeafProperty;
import static io.smallrye.config.ConfigMappingInterface.MapProperty;
import static io.smallrye.config.ConfigMappingInterface.PrimitiveProperty;
import static io.smallrye.config.ConfigMappingInterface.Property;
import static io.smallrye.config.ConfigMappingLoader.getConfigMappingClass;
import static io.smallrye.config.ConfigMappingLoader.getConfigMappingInterface;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN;
import static io.smallrye.config.common.utils.StringUtil.replaceNonAlphanumericByUnderscores;
import static java.lang.Integer.parseInt;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.function.Functions;
import io.smallrye.config.ConfigMappingInterface.CollectionProperty;
import io.smallrye.config.ConfigMappingInterface.NamingStrategy;

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

    private final Map<String, List<Class<?>>> roots;
    private final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions;
    private final Map<String, Property> properties;
    private final KeyMap<String> defaultValues;
    private final boolean validateUnknown;

    ConfigMappingProvider(final Builder builder) {
        this.roots = new HashMap<>(builder.roots);
        this.matchActions = new KeyMap<>();
        this.properties = new HashMap<>();
        this.defaultValues = new KeyMap<>();
        this.validateUnknown = builder.validateUnknown;

        final ArrayDeque<String> currentPath = new ArrayDeque<>();
        for (Map.Entry<String, List<Class<?>>> entry : roots.entrySet()) {
            NameIterator rootNi = new NameIterator(entry.getKey());
            while (rootNi.hasNext()) {
                final String nextSegment = rootNi.getNextSegment();
                if (!nextSegment.isEmpty()) {
                    currentPath.add(nextSegment);
                }
                rootNi.next();
            }
            List<Class<?>> roots = entry.getValue();
            for (Class<?> root : roots) {
                // construct the lazy match actions for each group
                BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> ef = new GetRootAction(root,
                        entry.getKey());
                ConfigMappingInterface mapping = getConfigMappingInterface(root);
                processEagerGroup(currentPath, matchActions, defaultValues, mapping.getNamingStrategy(), mapping, ef);
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
    }

    static String skewer(String camelHumps) {
        return skewer(camelHumps, '-');
    }

    static String skewer(String camelHumps, char separator) {
        return skewer(camelHumps, 0, camelHumps.length(), new StringBuilder(), separator);
    }

    private static String skewer(String camelHumps, int start, int end, StringBuilder b, char separator) {
        if (camelHumps.isEmpty()) {
            throw new IllegalArgumentException("Method seems to have an empty name");
        }
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
                    b.appendCodePoint(Character.toLowerCase(nextCp));
                    start += Character.charCount(cp);
                    cp = nextCp;
                    if (start == end) {
                        return b.toString();
                    }
                    nextCp = camelHumps.codePointAt(start);
                    // combine non-letters in with this name
                    if (Character.isLowerCase(nextCp)) {
                        b.append(separator);
                        return skewer(camelHumps, start, end, b, separator);
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
                        b.append(separator);
                        return skewer(camelHumps, start, end, b, separator);
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
                    b.append(separator);
                    return skewer(camelHumps, start, end, b, separator);
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

    private void processEagerGroup(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final KeyMap<String> defaultValues,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface group,
            final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> getEnclosingFunction) {

        Class<?> type = group.getInterfaceType();
        HashSet<String> usedProperties = new HashSet<>();
        for (int i = 0; i < group.getPropertyCount(); i++) {
            Property property = group.getProperty(i);
            String memberName = property.getMethod().getName();
            ArrayDeque<String> propertyPath = new ArrayDeque<>(currentPath);
            if (usedProperties.add(memberName)) {
                // process by property type
                if (!property.isParentPropertyName()) {
                    NameIterator ni = new NameIterator(property.hasPropertyName() ? property.getPropertyName()
                            : propertyName(property, group, namingStrategy));
                    while (ni.hasNext()) {
                        propertyPath.add(ni.getNextSegment());
                        ni.next();
                    }
                }
                processProperty(propertyPath, matchActions, defaultValues, namingStrategy, group, getEnclosingFunction, type,
                        memberName, property);
            }
        }
        int sc = group.getSuperTypeCount();
        for (int i = 0; i < sc; i++) {
            processEagerGroup(currentPath, matchActions, defaultValues, namingStrategy, group.getSuperType(i),
                    getEnclosingFunction);
        }
    }

    private void processProperty(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final KeyMap<String> defaultValues,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface group,
            final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> getEnclosingFunction,
            final Class<?> type,
            final String memberName,
            final Property property) {

        if (property.isOptional()) {
            // switch to lazy mode
            Property nestedProperty = property.asOptional().getNestedProperty();
            processOptionalProperty(currentPath, matchActions, defaultValues, namingStrategy, group, getEnclosingFunction, type,
                    memberName, nestedProperty);
        } else if (property.isGroup()) {
            processEagerGroup(currentPath, matchActions, defaultValues, namingStrategy, property.asGroup().getGroupType(),
                    new GetOrCreateEnclosingGroupInGroup(getEnclosingFunction, group, property.asGroup(), currentPath));
        } else if (property.isPrimitive()) {
            // already processed eagerly
            PrimitiveProperty primitiveProperty = property.asPrimitive();
            if (primitiveProperty.hasDefaultValue()) {
                defaultValues.findOrAdd(currentPath).putRootValue(primitiveProperty.getDefaultValue());
                // collections may also be represented without [] so we need to register both paths
                if (isCollection(currentPath)) {
                    defaultValues.findOrAdd(inlineCollectionPath(currentPath))
                            .putRootValue(primitiveProperty.getDefaultValue());
                }
            }
            addAction(currentPath, property, DO_NOTHING);
            // collections may also be represented without [] so we need to register both paths
            if (isCollection(currentPath)) {
                addAction(inlineCollectionPath(currentPath), property, DO_NOTHING);
            }
        } else if (property.isLeaf()) {
            // already processed eagerly
            LeafProperty leafProperty = property.asLeaf();
            if (leafProperty.hasDefaultValue()) {
                defaultValues.findOrAdd(currentPath).putRootValue(leafProperty.getDefaultValue());
                // collections may also be represented without [] so we need to register both paths
                if (isCollection(currentPath)) {
                    defaultValues.findOrAdd(inlineCollectionPath(currentPath)).putRootValue(leafProperty.getDefaultValue());
                }
            }
            // ignore with no error message
            addAction(currentPath, property, DO_NOTHING);
            // collections may also be represented without [] so we need to register both paths
            if (isCollection(currentPath)) {
                addAction(inlineCollectionPath(currentPath), property, DO_NOTHING);
            }
        } else if (property.isMap()) {
            // the enclosure of the map is this group
            processLazyMapInGroup(currentPath, matchActions, defaultValues, property.asMap(), getEnclosingFunction,
                    namingStrategy, group);
        } else if (property.isCollection()) {
            CollectionProperty collectionProperty = property.asCollection();
            currentPath.addLast(currentPath.removeLast() + "[*]");
            processProperty(currentPath, matchActions, defaultValues, namingStrategy, group, getEnclosingFunction, type,
                    memberName, collectionProperty.getElement());
        }
    }

    private void processOptionalProperty(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final KeyMap<String> defaultValues,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface group,
            final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> getEnclosingFunction,
            final Class<?> type, final String memberName, final Property property) {

        if (property.isGroup()) {
            GroupProperty nestedGroup = property.asGroup();
            // on match, always create the outermost group, which recursively creates inner groups
            GetOrCreateEnclosingGroupInGroup matchAction = new GetOrCreateEnclosingGroupInGroup(
                    getEnclosingFunction, group, nestedGroup, currentPath);
            GetFieldOfEnclosing ef = new GetFieldOfEnclosing(
                    nestedGroup.isParentPropertyName() ? getEnclosingFunction
                            : new ConsumeOneAndThenFn<>(getEnclosingFunction),
                    type, memberName);
            processLazyGroupInGroup(currentPath, matchActions, defaultValues, namingStrategy, nestedGroup.getGroupType(), ef,
                    matchAction,
                    new HashSet<>());
        } else if (property.isLeaf()) {
            LeafProperty leafProperty = property.asLeaf();
            if (leafProperty.hasDefaultValue()) {
                defaultValues.findOrAdd(currentPath).putRootValue(leafProperty.getDefaultValue());
                // collections may also be represented without [] so we need to register both paths
                if (isCollection(currentPath)) {
                    defaultValues.findOrAdd(inlineCollectionPath(currentPath)).putRootValue(leafProperty.getDefaultValue());
                }
            }
            addAction(currentPath, property, DO_NOTHING);
            // collections may also be represented without [] so we need to register both paths
            if (isCollection(currentPath)) {
                addAction(inlineCollectionPath(currentPath), property, DO_NOTHING);
            }
        } else if (property.isCollection()) {
            CollectionProperty collectionProperty = property.asCollection();
            currentPath.addLast(currentPath.removeLast() + "[*]");
            processProperty(currentPath, matchActions, defaultValues, namingStrategy, group, getEnclosingFunction, type,
                    memberName, collectionProperty.getElement());
        }
    }

    private void processLazyGroupInGroup(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final KeyMap<String> defaultValues,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface group,
            final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> getEnclosingFunction,
            final BiConsumer<ConfigMappingContext, NameIterator> matchAction,
            final HashSet<String> usedProperties) {

        int pc = group.getPropertyCount();
        int pathLen = currentPath.size();
        for (int i = 0; i < pc; i++) {
            Property property = group.getProperty(i);
            if (!property.isParentPropertyName()) {
                NameIterator ni = new NameIterator(property.hasPropertyName() ? property.getPropertyName()
                        : propertyName(property, group, namingStrategy));
                while (ni.hasNext()) {
                    currentPath.add(ni.getNextSegment());
                    ni.next();
                }
            }
            if (matchActions.hasRootValue(currentPath)) {
                while (currentPath.size() > pathLen) {
                    currentPath.removeLast();
                }
                continue;
            }
            if (usedProperties.add(String.join(".", String.join(".", currentPath), property.getMethod().getName()))) {
                boolean optional = property.isOptional();
                processLazyPropertyInGroup(currentPath, matchActions, defaultValues, getEnclosingFunction, matchAction,
                        usedProperties, namingStrategy, group, optional, property);
            }
            while (currentPath.size() > pathLen) {
                currentPath.removeLast();
            }
        }
        int sc = group.getSuperTypeCount();
        for (int i = 0; i < sc; i++) {
            processLazyGroupInGroup(currentPath, matchActions, defaultValues, namingStrategy, group.getSuperType(i),
                    getEnclosingFunction, matchAction, usedProperties);
        }
    }

    private void processLazyPropertyInGroup(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final KeyMap<String> defaultValues,
            final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> getEnclosingFunction,
            final BiConsumer<ConfigMappingContext, NameIterator> matchAction,
            final HashSet<String> usedProperties,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface group,
            final boolean optional,
            final Property property) {

        if (optional && property.asOptional().getNestedProperty().isGroup()) {
            GroupProperty nestedGroup = property.asOptional().getNestedProperty().asGroup();
            GetOrCreateEnclosingGroupInGroup nestedMatchAction = new GetOrCreateEnclosingGroupInGroup(
                    property.isParentPropertyName() ? getEnclosingFunction
                            : new ConsumeOneAndThenFn<>(getEnclosingFunction),
                    group, nestedGroup, currentPath);
            processLazyGroupInGroup(currentPath, matchActions, defaultValues, namingStrategy, nestedGroup.getGroupType(),
                    nestedMatchAction,
                    nestedMatchAction, new HashSet<>());
        } else if (property.isGroup()) {
            GroupProperty asGroup = property.asGroup();
            GetOrCreateEnclosingGroupInGroup nestedEnclosingFunction = new GetOrCreateEnclosingGroupInGroup(
                    property.isParentPropertyName() ? getEnclosingFunction
                            : new ConsumeOneAndThenFn<>(getEnclosingFunction),
                    group, asGroup, currentPath);
            BiConsumer<ConfigMappingContext, NameIterator> nestedMatchAction;
            nestedMatchAction = matchAction;
            if (!property.isParentPropertyName()) {
                nestedMatchAction = new ConsumeOneAndThen(nestedMatchAction);
            }
            processLazyGroupInGroup(currentPath, matchActions, defaultValues, namingStrategy, asGroup.getGroupType(),
                    nestedEnclosingFunction,
                    nestedMatchAction, usedProperties);
        } else if (property.isLeaf() || property.isPrimitive()
                || optional && property.asOptional().getNestedProperty().isLeaf()) {
            BiConsumer<ConfigMappingContext, NameIterator> actualAction;
            if (!property.isParentPropertyName()) {
                actualAction = new ConsumeOneAndThen(matchAction);
            } else {
                actualAction = matchAction;
            }
            addAction(currentPath, property, actualAction);
            // collections may also be represented without [] so we need to register both paths
            if (isCollection(currentPath)) {
                addAction(inlineCollectionPath(currentPath), property, actualAction);
            }
            if (property.isPrimitive()) {
                PrimitiveProperty primitiveProperty = property.asPrimitive();
                if (primitiveProperty.hasDefaultValue()) {
                    defaultValues.findOrAdd(currentPath).putRootValue(primitiveProperty.getDefaultValue());
                    // collections may also be represented without [] so we need to register both paths
                    if (isCollection(currentPath)) {
                        defaultValues.findOrAdd(inlineCollectionPath(currentPath))
                                .putRootValue(primitiveProperty.getDefaultValue());
                    }
                }
            } else if (property.isLeaf() && optional) {
                LeafProperty leafProperty = property.asOptional().getNestedProperty().asLeaf();
                if (leafProperty.hasDefaultValue()) {
                    defaultValues.findOrAdd(currentPath).putRootValue(leafProperty.getDefaultValue());
                    // collections may also be represented without [] so we need to register both paths
                    if (isCollection(currentPath)) {
                        defaultValues.findOrAdd(inlineCollectionPath(currentPath)).putRootValue(leafProperty.getDefaultValue());
                    }
                }
            } else {
                LeafProperty leafProperty = property.asLeaf();
                if (leafProperty.hasDefaultValue()) {
                    defaultValues.findOrAdd(currentPath).putRootValue(leafProperty.getDefaultValue());
                    // collections may also be represented without [] so we need to register both paths
                    if (isCollection(currentPath)) {
                        defaultValues.findOrAdd(inlineCollectionPath(currentPath)).putRootValue(leafProperty.getDefaultValue());
                    }
                }
            }
        } else if (property.isMap()) {
            processLazyMapInGroup(currentPath, matchActions, defaultValues, property.asMap(), getEnclosingFunction,
                    namingStrategy, group);
        } else if (property.isCollection() || optional && property.asOptional().getNestedProperty().isCollection()) {
            CollectionProperty collectionProperty = optional ? property.asOptional().getNestedProperty().asCollection()
                    : property.asCollection();
            currentPath.addLast(currentPath.removeLast() + "[*]");
            processLazyPropertyInGroup(currentPath, matchActions, defaultValues, getEnclosingFunction, matchAction,
                    usedProperties, namingStrategy, group, false, collectionProperty.getElement());
        }
    }

    private void processLazyMapInGroup(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final KeyMap<String> defaultValues,
            final MapProperty property, BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> getEnclosingGroup,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface enclosingGroup) {

        GetOrCreateEnclosingMapInGroup getEnclosingMap = new GetOrCreateEnclosingMapInGroup(getEnclosingGroup, enclosingGroup,
                property, currentPath);
        processLazyMap(currentPath, matchActions, defaultValues, property, getEnclosingMap, namingStrategy, enclosingGroup);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void processLazyMap(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final KeyMap<String> defaultValues,
            final MapProperty property, BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> getEnclosingMap,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface enclosingGroup) {

        Property valueProperty = property.getValueProperty();
        Class<? extends Converter<?>> keyConvertWith = property.hasKeyConvertWith() ? property.getKeyConvertWith() : null;
        Class<?> keyRawType = property.getKeyRawType();

        if (valueProperty.isLeaf()) {
            currentPath.addLast("*");
            if (matchActions.hasRootValue(currentPath)) {
                currentPath.removeLast();
                return;
            }

            LeafProperty leafProperty = valueProperty.asLeaf();
            Class<? extends Converter<?>> valConvertWith = leafProperty.getConvertWith();
            Class<?> valueRawType = leafProperty.getValueRawType();

            addAction(currentPath, property, (mc, ni) -> {
                StringBuilder sb = mc.getStringBuilder();
                sb.setLength(0);
                sb.append(ni.getAllPreviousSegments());
                String configKey = sb.toString();
                String rawMapKey = ni.getPreviousSegment();
                Map<?, ?> map = getEnclosingMap.apply(mc, ni);
                Converter<?> keyConv;
                SmallRyeConfig config = mc.getConfig();
                if (keyConvertWith != null) {
                    keyConv = mc.getConverterInstance(keyConvertWith);
                } else {
                    keyConv = config.requireConverter(keyRawType);
                }
                Object key = keyConv.convert(rawMapKey);
                Converter<?> valueConv;
                if (valConvertWith != null) {
                    valueConv = mc.getConverterInstance(valConvertWith);
                } else {
                    valueConv = config.requireConverter(valueRawType);
                }
                ((Map) map).put(key, config.getValue(configKey, valueConv));
            });
        } else if (valueProperty.isMap()) {
            currentPath.addLast("*");
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
                    keyConv = config.requireConverter(keyRawType);
                }
                Object key = keyConv.convert(rawMapKey);
                return (Map) ((Map) enclosingMap).computeIfAbsent(key, x -> new HashMap<>());
            }, namingStrategy, enclosingGroup);
        } else {
            assert valueProperty.isGroup();
            GetOrCreateEnclosingGroupInMap ef = new GetOrCreateEnclosingGroupInMap(getEnclosingMap, property, enclosingGroup,
                    valueProperty.asGroup(), String.join(".", currentPath));
            currentPath.addLast("*");
            processLazyGroupInGroup(currentPath, matchActions, defaultValues, namingStrategy,
                    valueProperty.asGroup().getGroupType(),
                    ef, ef, new HashSet<>());
        }
        currentPath.removeLast();
    }

    private void addAction(
            final ArrayDeque<String> currentPath,
            final Property property,
            final BiConsumer<ConfigMappingContext, NameIterator> action) {
        matchActions.findOrAdd(currentPath).putRootValue(action);
        properties.put(String.join(".", currentPath), property);
    }

    private static boolean isCollection(final ArrayDeque<String> currentPath) {
        return currentPath.getLast().endsWith("[*]");
    }

    private static ArrayDeque<String> inlineCollectionPath(final ArrayDeque<String> currentPath) {
        ArrayDeque<String> inlineCollectionPath = new ArrayDeque<>(currentPath);
        String last = inlineCollectionPath.removeLast();
        inlineCollectionPath.addLast(last.substring(0, last.length() - 3));
        return inlineCollectionPath;
    }

    // This will add the property index (if exists) to the name
    private static String indexName(final String name, final ArrayDeque<String> groupPath, final NameIterator nameIterator) {
        if (groupPath.isEmpty()) {
            return name;
        }

        String property = nameIterator.getAllPreviousSegments();
        int start = property.lastIndexOf(normalizeIfIndexed(groupPath.getLast()));
        if (start != -1) {
            int i = start + normalizeIfIndexed(groupPath.getLast()).length();
            if (i < property.length() && property.charAt(i) == '[') {
                for (;;) {
                    if (property.charAt(i) == ']') {
                        try {
                            int index = parseInt(
                                    property.substring(start + normalizeIfIndexed(groupPath.getLast()).length() + 1, i));
                            return name + "[" + index + "]";
                        } catch (NumberFormatException e) {
                            //NOOP
                        }
                        break;
                    } else if (i < property.length() - 1) {
                        i++;
                    } else {
                        break;
                    }
                }
            }
        }
        return name;
    }

    private static String propertyName(final Property property, final ConfigMappingInterface group,
            final NamingStrategy namingStrategy) {
        return namingStrategy(namingStrategy, group.getNamingStrategy()).apply(property.getPropertyName());
    }

    private static NamingStrategy namingStrategy(NamingStrategy parent, NamingStrategy current) {
        if (!current.isDefault()) {
            return current;
        } else {
            return parent;
        }
    }

    static class GetRootAction implements BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> {
        private final Class<?> root;
        private final String rootPath;

        GetRootAction(final Class<?> root, final String rootPath) {
            this.root = root;
            this.rootPath = rootPath;
        }

        public ConfigMappingObject apply(final ConfigMappingContext mc, final NameIterator ni) {
            return mc.getRoot(root, rootPath);
        }
    }

    static class GetOrCreateEnclosingGroupInGroup
            implements BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject>,
            BiConsumer<ConfigMappingContext, NameIterator> {
        private final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> delegate;
        private final ConfigMappingInterface enclosingGroup;
        private final GroupProperty enclosedGroup;
        private final ArrayDeque<String> path;

        GetOrCreateEnclosingGroupInGroup(
                final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> delegate,
                final ConfigMappingInterface enclosingGroup,
                final GroupProperty enclosedGroup,
                final ArrayDeque<String> path) {
            this.delegate = delegate;
            this.enclosingGroup = enclosingGroup;
            this.enclosedGroup = enclosedGroup;
            this.path = new ArrayDeque<>(path);
        }

        public ConfigMappingObject apply(final ConfigMappingContext context, final NameIterator ni) {
            ConfigMappingObject ourEnclosing = delegate.apply(context, ni);
            Class<?> enclosingType = enclosingGroup.getInterfaceType();
            String key = indexName(enclosedGroup.getMethod().getName(), path, ni);
            ConfigMappingObject val = (ConfigMappingObject) context.getEnclosedField(enclosingType, key, ourEnclosing);
            context.applyNamingStrategy(
                    namingStrategy(enclosedGroup.getGroupType().getNamingStrategy(), enclosingGroup.getNamingStrategy()));
            if (val == null) {
                // it must be an optional group
                StringBuilder sb = context.getStringBuilder();
                sb.replace(0, sb.length(), ni.getAllPreviousSegments());
                val = (ConfigMappingObject) context.constructGroup(enclosedGroup.getGroupType().getInterfaceType());
                context.registerEnclosedField(enclosingType, key, ourEnclosing, val);
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
        final GroupProperty enclosedGroup;
        final String mapPath;

        GetOrCreateEnclosingGroupInMap(final BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> getEnclosingMap,
                final MapProperty enclosingMap, final ConfigMappingInterface enclosingGroup,
                final GroupProperty enclosedGroup, final String mapPath) {
            this.getEnclosingMap = getEnclosingMap;
            this.enclosingMap = enclosingMap;
            this.enclosingGroup = enclosingGroup;
            this.enclosedGroup = enclosedGroup;
            this.mapPath = mapPath;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ConfigMappingObject apply(final ConfigMappingContext context, final NameIterator ni) {
            ni.previous();
            Map<?, ?> ourEnclosing = getEnclosingMap.apply(context, ni);
            ni.next();
            String mapKey = mapKey(ni);
            Converter<?> keyConverter = context.getKeyConverter(enclosingGroup.getInterfaceType(),
                    enclosingMap.getMethod().getName(), enclosingMap.getLevels() - 1);
            Object convertedKey = keyConverter.convert(mapKey);
            ConfigMappingObject val = (ConfigMappingObject) ourEnclosing.get(convertedKey);
            context.applyNamingStrategy(
                    namingStrategy(enclosedGroup.getGroupType().getNamingStrategy(), enclosingGroup.getNamingStrategy()));
            if (val == null) {
                StringBuilder sb = context.getStringBuilder();
                while (ni.hasPrevious()) {
                    if (!ni.previousSegmentEquals(mapKey)) {
                        ni.previous();
                        continue;
                    }
                    break;
                }
                sb.replace(0, sb.length(), ni.getAllPreviousSegments());
                ((Map) ourEnclosing).put(convertedKey,
                        val = (ConfigMappingObject) context.constructGroup(enclosedGroup.getGroupType().getInterfaceType()));
            }
            return val;
        }

        public void accept(final ConfigMappingContext context, final NameIterator ni) {
            apply(context, ni);
        }

        private String mapKey(final NameIterator ni) {
            NameIterator mapPath = new NameIterator(normalizeIfIndexed(this.mapPath));
            NameIterator mapKey = new NameIterator(normalizeIfIndexed(ni.getName()));
            while (mapPath.hasNext() && mapKey.hasNext()) {
                if (mapPath.getNextSegment().equals(mapKey.getNextSegment()) || mapPath.getNextSegment().equals("*")) {
                    mapPath.next();
                    mapKey.next();
                } else {
                    break;
                }
            }
            return mapKey.hasNext() ? mapKey.getNextSegment() : ni.getPreviousSegment();
        }
    }

    static class GetOrCreateEnclosingMapInGroup implements BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>>,
            BiConsumer<ConfigMappingContext, NameIterator> {
        final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> delegate;
        final ConfigMappingInterface enclosingGroup;
        final MapProperty enclosedGroup;
        final ArrayDeque<String> path;

        GetOrCreateEnclosingMapInGroup(
                final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> delegate,
                final ConfigMappingInterface enclosingGroup,
                final MapProperty enclosedGroup,
                final ArrayDeque<String> path) {
            this.delegate = delegate;
            this.enclosingGroup = enclosingGroup;
            this.enclosedGroup = enclosedGroup;
            this.path = new ArrayDeque<>(path);
        }

        public Map<?, ?> apply(final ConfigMappingContext context, final NameIterator ni) {
            boolean consumeName = !enclosedGroup.isParentPropertyName();
            if (consumeName)
                ni.previous();
            ConfigMappingObject ourEnclosing = delegate.apply(context, ni);
            if (consumeName)
                ni.next();
            Class<?> enclosingType = enclosingGroup.getInterfaceType();
            String key = indexName(enclosedGroup.getMethod().getName(), path, ni);
            Map<?, ?> val = (Map<?, ?>) context.getEnclosedField(enclosingType, key, ourEnclosing);
            context.applyNamingStrategy(enclosingGroup.getNamingStrategy());
            if (val == null) {
                // map is not yet constructed
                val = new HashMap<>();
                context.registerEnclosedField(enclosingType, key, ourEnclosing, val);
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

    public static Builder builder() {
        return new Builder();
    }

    KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> getMatchActions() {
        return matchActions;
    }

    Map<String, Property> getProperties() {
        return properties;
    }

    KeyMap<String> getDefaultValues() {
        return defaultValues;
    }

    void mapConfiguration(SmallRyeConfig config) throws ConfigValidationException {
        for (ConfigSource configSource : config.getConfigSources()) {
            if (configSource instanceof DefaultValuesConfigSource) {
                final DefaultValuesConfigSource defaultValuesConfigSource = (DefaultValuesConfigSource) configSource;
                defaultValuesConfigSource.registerDefaults(this.getDefaultValues());
            }
        }

        mapConfiguration(config, config.getConfigMappings());
    }

    private void mapConfiguration(SmallRyeConfig config, ConfigMappings mappings) throws ConfigValidationException {
        if (roots.isEmpty()) {
            return;
        }

        Assert.checkNotNullParam("config", config);
        ConfigMappingContext context = new ConfigMappingContext(config);
        // eagerly populate roots
        for (Map.Entry<String, List<Class<?>>> entry : roots.entrySet()) {
            String path = entry.getKey();
            List<Class<?>> roots = entry.getValue();
            for (Class<?> root : roots) {
                StringBuilder sb = context.getStringBuilder();
                sb.replace(0, sb.length(), path);
                ConfigMappingObject group = (ConfigMappingObject) context.constructRoot(root);
                context.registerRoot(root, path, group);
            }
        }

        // lazily sweep
        Set<String> unknownProperties = new HashSet<>();
        for (String name : config.getPropertyNames()) {
            NameIterator ni = new NameIterator(name);
            // filter properties in root
            if (!isPropertyInRoot(ni)) {
                continue;
            }

            BiConsumer<ConfigMappingContext, NameIterator> action = matchActions.findRootValue(ni);
            if (action != null) {
                action.accept(context, ni);
            } else {
                if (validateUnknown(validateUnknown, config)) {
                    unknownProperties.add(name);
                }
            }
        }

        unknownProperties(unknownProperties, context);
        ArrayList<ConfigValidationException.Problem> problems = context.getProblems();
        if (!problems.isEmpty()) {
            throw new ConfigValidationException(problems.toArray(ConfigValidationException.Problem.NO_PROBLEMS));
        }
        context.fillInOptionals();

        mappings.registerConfigMappings(context.getRootsMap());
    }

    private boolean isPropertyInRoot(NameIterator propertyName) {
        final Set<String> registeredRoots = roots.keySet();
        for (String registeredRoot : registeredRoots) {
            // match everything
            if (registeredRoot.length() == 0) {
                return true;
            }

            // A sub property from a namespace is always bigger in length
            if (propertyName.getName().length() <= registeredRoot.length()) {
                continue;
            }

            final NameIterator root = new NameIterator(registeredRoot);
            // compare segments
            while (root.hasNext()) {
                String segment = root.getNextSegment();
                if (!propertyName.hasNext()) {
                    propertyName.goToStart();
                    break;
                }

                final String nextSegment = propertyName.getNextSegment();
                if (!segment.equals(normalizeIfIndexed(nextSegment))) {
                    propertyName.goToStart();
                    break;
                }

                root.next();
                propertyName.next();

                // root has no more segments and we reached this far so everything matched.
                // on top, property still has more segments to do the mapping.
                if (!root.hasNext() && propertyName.hasNext()) {
                    propertyName.goToStart();
                    return true;
                }
            }
        }

        return false;
    }

    private static String normalizeIfIndexed(final String propertyName) {
        int indexStart = propertyName.indexOf("[");
        int indexEnd = propertyName.indexOf("]");
        if (indexStart != -1 && indexEnd != -1) {
            return propertyName.substring(0, indexStart);
        }
        return propertyName;
    }

    private static boolean validateUnknown(final boolean validateUnknown, final SmallRyeConfig config) {
        return config.getOptionalValue(SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN, Boolean.class)
                .orElse(validateUnknown);
    }

    private static void unknownProperties(Set<String> properties, ConfigMappingContext context) {
        Set<String> usedProperties = new HashSet<>();
        for (String property : context.getConfig().getPropertyNames()) {
            if (properties.contains(property)) {
                continue;
            }

            usedProperties.add(replaceNonAlphanumericByUnderscores(property));
        }
        usedProperties.removeAll(properties);

        for (String property : properties) {
            boolean found = false;
            for (String usedProperty : usedProperties) {
                if (usedProperty.equalsIgnoreCase(replaceNonAlphanumericByUnderscores(property))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                context.unknownConfigElement(property);
            }
        }
    }

    public static final class Builder {
        final Set<Class<?>> types = new HashSet<>();
        final Map<String, List<Class<?>>> roots = new HashMap<>();
        final List<String[]> ignored = new ArrayList<>();
        boolean validateUnknown = true;

        Builder() {
        }

        public Builder addRoot(String path, Class<?> type) {
            Assert.checkNotNullParam("path", path);
            Assert.checkNotNullParam("type", type);
            types.add(type);
            roots.computeIfAbsent(path, k -> new ArrayList<>(4)).add(getConfigMappingClass(type));
            return this;
        }

        public Builder addIgnored(String path) {
            Assert.checkNotNullParam("path", path);
            ignored.add(path.split("\\."));
            return this;
        }

        public Builder validateUnknown(boolean validateUnknown) {
            this.validateUnknown = validateUnknown;
            return this;
        }

        public ConfigMappingProvider build() {
            // We don't validate for MP ConfigProperties, so if all classes are MP ConfigProperties disable validation.
            boolean allConfigurationProperties = true;
            for (Class<?> type : types) {
                if (ConfigMappingClass.getConfigurationClass(type) == null) {
                    allConfigurationProperties = false;
                    break;
                }
            }

            if (allConfigurationProperties) {
                this.validateUnknown = false;
            }

            return new ConfigMappingProvider(this);
        }
    }
}
