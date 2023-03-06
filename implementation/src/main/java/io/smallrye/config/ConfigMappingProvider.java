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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.IntFunction;

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
        KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> map = new KeyMap<>();
        map.putRootValue(DO_NOTHING);
        //noinspection CollectionAddedToSelf
        map.putAny(map);
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
                ignoreRecursively(found);
            } else {
                found = matchActions.findOrAdd(ignoredPath);
                found.putRootValue(DO_NOTHING);
            }
        }
    }

    static void ignoreRecursively(KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> root) {
        if (root.getAny() == null) {
            root.putAny(IGNORE_EVERYTHING);
        } else {
            var any = root.getAny();
            if (root != any) {
                ignoreRecursively(any);
            }
        }

        for (var value : root.values()) {
            ignoreRecursively(value);
        }
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

        // Register super types first. The main mapping will override methods from the super types
        int sc = group.getSuperTypeCount();
        for (int i = 0; i < sc; i++) {
            processEagerGroup(currentPath, matchActions, defaultValues, namingStrategy, group.getSuperType(i),
                    getEnclosingFunction);
        }

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
            final Class<?> type,
            final String memberName,
            final Property property) {

        if (property.isGroup()) {
            GroupProperty nestedGroup = property.asGroup();
            // on match, always create the outermost group, which recursively creates inner groups
            GetOrCreateEnclosingGroupInGroup matchAction = new GetOrCreateEnclosingGroupInGroup(
                    getEnclosingFunction, group, nestedGroup, currentPath);
            processLazyGroupInGroup(currentPath, matchActions, defaultValues, namingStrategy, nestedGroup.getGroupType(),
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
            if (usedProperties.add(String.join(".", String.join(".", currentPath), property.getMethod().getName()))) {
                boolean optional = property.isOptional();
                processLazyPropertyInGroup(currentPath, matchActions, defaultValues, matchAction, usedProperties,
                        namingStrategy, group, optional, property);
            }
            while (currentPath.size() > pathLen) {
                currentPath.removeLast();
            }
        }
        int sc = group.getSuperTypeCount();
        for (int i = 0; i < sc; i++) {
            processLazyGroupInGroup(currentPath, matchActions, defaultValues, namingStrategy, group.getSuperType(i),
                    matchAction, usedProperties);
        }
    }

    private void processLazyPropertyInGroup(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final KeyMap<String> defaultValues,
            final BiConsumer<ConfigMappingContext, NameIterator> matchAction,
            final HashSet<String> usedProperties,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface group,
            final boolean optional,
            final Property property) {

        if (optional && property.asOptional().getNestedProperty().isGroup()) {
            GroupProperty nestedGroup = property.asOptional().getNestedProperty().asGroup();
            GetOrCreateEnclosingGroupInGroup nestedEnclosingFunction = new GetOrCreateEnclosingGroupInGroup(
                    property.isParentPropertyName() ? new GetNestedEnclosing(matchAction)
                            : new ConsumeOneAndThenFn<>(new GetNestedEnclosing(matchAction)),
                    group, nestedGroup, currentPath);
            processLazyGroupInGroup(currentPath, matchActions, defaultValues, namingStrategy, nestedGroup.getGroupType(),
                    nestedEnclosingFunction, new HashSet<>());
        } else if (property.isGroup()) {
            GroupProperty asGroup = property.asGroup();
            GetOrCreateEnclosingGroupInGroup nestedEnclosingFunction = new GetOrCreateEnclosingGroupInGroup(
                    property.isParentPropertyName() ? new GetNestedEnclosing(matchAction)
                            : new ConsumeOneAndThenFn<>(new GetNestedEnclosing(matchAction)),
                    group, asGroup, currentPath);
            processLazyGroupInGroup(currentPath, matchActions, defaultValues, namingStrategy, asGroup.getGroupType(),
                    nestedEnclosingFunction, usedProperties);
        } else if (property.isLeaf() || property.isPrimitive()
                || optional && property.asOptional().getNestedProperty().isLeaf()) {
            BiConsumer<ConfigMappingContext, NameIterator> actualAction = property.isParentPropertyName() ? matchAction
                    : new ConsumeOneAndThen(matchAction);
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
            GetNestedEnclosing nestedMatchAction = new GetNestedEnclosing(matchAction);
            processLazyMapInGroup(currentPath, matchActions, defaultValues, property.asMap(), nestedMatchAction, namingStrategy,
                    group);
        } else if (property.isCollection() || optional && property.asOptional().getNestedProperty().isCollection()) {
            CollectionProperty collectionProperty = optional ? property.asOptional().getNestedProperty().asCollection()
                    : property.asCollection();
            currentPath.addLast(currentPath.removeLast() + "[*]");
            processLazyPropertyInGroup(currentPath, matchActions, defaultValues, matchAction, usedProperties, namingStrategy,
                    group, false, collectionProperty.getElement());
        }
    }

    private void processLazyMapInGroup(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final KeyMap<String> defaultValues,
            final MapProperty property,
            final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> getEnclosingGroup,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface enclosingGroup) {

        GetOrCreateEnclosingMapInGroup getEnclosingMap = new GetOrCreateEnclosingMapInGroup(getEnclosingGroup, enclosingGroup,
                property, currentPath);
        processLazyMap(currentPath, matchActions, defaultValues, property, getEnclosingMap, namingStrategy, enclosingGroup);
    }

    private void processLazyMap(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final KeyMap<String> defaultValues,
            final MapProperty property,
            final BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> getEnclosingMap,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface enclosingGroup) {

        Property valueProperty = property.getValueProperty();
        Class<? extends Converter<?>> keyConvertWith = property.hasKeyConvertWith() ? property.getKeyConvertWith() : null;
        Class<?> keyRawType = property.getKeyRawType();
        currentPath.addLast("*");
        processLazyMapValue(currentPath, matchActions, defaultValues, property, valueProperty, keyConvertWith, keyRawType,
                getEnclosingMap, namingStrategy, enclosingGroup);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void processLazyMapValue(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final KeyMap<String> defaultValues,
            final MapProperty mapProperty,
            final Property property,
            final Class<? extends Converter<?>> keyConvertWith,
            final Class<?> keyRawType,
            final BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> getEnclosingMap,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface enclosingGroup) {

        if (property.isLeaf()) {
            LeafProperty leafProperty = property.asLeaf();
            Class<? extends Converter<?>> valConvertWith = leafProperty.getConvertWith();
            Class<?> valueRawType = leafProperty.getValueRawType();

            String mapPath = String.join(".", currentPath);
            addAction(currentPath, mapProperty, (mc, ni) -> {
                // Place the cursor at the map path
                NameIterator niAtMapPath = atMapPath(mapPath, ni);
                Map<?, ?> map = getEnclosingMap.apply(mc, niAtMapPath);

                String rawMapKey;
                String configKey;
                boolean indexed = isIndexed(ni.getPreviousSegment());
                if (indexed && ni.hasPrevious()) {
                    rawMapKey = normalizeIfIndexed(niAtMapPath.getName().substring(niAtMapPath.getPosition() + 1));
                    configKey = niAtMapPath.getAllPreviousSegmentsWith(rawMapKey);
                } else {
                    rawMapKey = niAtMapPath.getName().substring(niAtMapPath.getPosition() + 1);
                    configKey = ni.getAllPreviousSegments();
                }

                // Remove quotes if exists
                if (rawMapKey.charAt(0) == '"' && rawMapKey.charAt(rawMapKey.length() - 1) == '"') {
                    rawMapKey = rawMapKey.substring(1, rawMapKey.length() - 1);
                }

                Converter<?> keyConv;
                SmallRyeConfig config = mc.getConfig();
                if (keyConvertWith != null) {
                    keyConv = mc.getConverterInstance(keyConvertWith);
                } else {
                    keyConv = config.requireConverter(keyRawType);
                }
                Converter<?> valueConv;
                if (valConvertWith != null) {
                    valueConv = mc.getConverterInstance(valConvertWith);
                } else {
                    valueConv = config.requireConverter(valueRawType);
                }

                if (indexed) {
                    CollectionProperty collectionProperty = mapProperty.getValueProperty().asCollection();
                    Class<?> collectionRawType = collectionProperty.getCollectionRawType();
                    IntFunction collectionFactory = ConfigMappingContext.createCollectionFactory(collectionRawType);
                    ((Map) map).put(keyConv.convert(rawMapKey), config.getValues(configKey, valueConv, collectionFactory));
                } else {
                    ((Map) map).put(keyConv.convert(rawMapKey), config.getValue(configKey, valueConv));
                }
            });
            // action to match all segments of a key after the map path
            KeyMap mapAction = matchActions.find(currentPath);
            if (mapAction != null) {
                mapAction.putAny(matchActions.find(currentPath));
            }

            // collections may also be represented without [] so we need to register both paths
            if (isCollection(currentPath)) {
                addAction(inlineCollectionPath(currentPath), leafProperty, DO_NOTHING);
            }

        } else if (property.isMap()) {
            processLazyMap(currentPath, matchActions, defaultValues, property.asMap(), (mc, ni) -> {
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
        } else if (property.isGroup()) {
            GetOrCreateEnclosingGroupInMap ef = new GetOrCreateEnclosingGroupInMap(getEnclosingMap, mapProperty, enclosingGroup,
                    property.asGroup(), currentPath);
            processLazyGroupInGroup(currentPath, matchActions, defaultValues, namingStrategy,
                    property.asGroup().getGroupType(), ef, new HashSet<>());
        } else if (property.isCollection()) {
            CollectionProperty collectionProperty = property.asCollection();
            Property element = collectionProperty.getElement();
            if (!element.hasConvertWith()) {
                currentPath.addLast(currentPath.removeLast() + "[*]");
            }
            processLazyMapValue(currentPath, matchActions, defaultValues, mapProperty, element, keyConvertWith, keyRawType,
                    getEnclosingMap, namingStrategy, enclosingGroup);
        } else {
            throw new UnsupportedOperationException();
        }
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
    private static String indexName(final String name, final String groupPath, final NameIterator nameIterator) {
        String group = new NameIterator(groupPath, true).getPreviousSegment();
        String property = nameIterator.getAllPreviousSegments();
        int start = property.lastIndexOf(normalizeIfIndexed(group));
        if (start != -1) {
            int i = start + normalizeIfIndexed(group).length();
            if (i < property.length() && property.charAt(i) == '[') {
                for (;;) {
                    if (property.charAt(i) == ']') {
                        try {
                            int index = parseInt(
                                    property.substring(start + normalizeIfIndexed(group).length() + 1, i));
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

    private static NameIterator atMapPath(final String mapPath, final NameIterator propertyName) {
        int segments = 0;
        NameIterator countSegments = new NameIterator(mapPath);
        while (countSegments.hasNext()) {
            segments++;
            countSegments.next();
        }

        // We don't want the key;
        segments = segments - 1;

        NameIterator propertyMap = new NameIterator(propertyName.getName());
        for (int i = 0; i < segments; i++) {
            propertyMap.next();
        }

        return propertyMap;
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

        @Override
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
        private final String groupPath;

        GetOrCreateEnclosingGroupInGroup(
                final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> delegate,
                final ConfigMappingInterface enclosingGroup,
                final GroupProperty enclosedGroup,
                final ArrayDeque<String> path) {
            this.delegate = delegate;
            this.enclosingGroup = enclosingGroup;
            this.enclosedGroup = enclosedGroup;
            this.groupPath = String.join(".", path);
        }

        @Override
        public ConfigMappingObject apply(final ConfigMappingContext context, final NameIterator ni) {
            ConfigMappingObject ourEnclosing = delegate.apply(context, ni);
            Class<?> enclosingType = enclosingGroup.getInterfaceType();
            String key = indexName(enclosedGroup.getMethod().getName(), groupPath, ni);
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

        @Override
        public void accept(final ConfigMappingContext context, final NameIterator nameIterator) {
            apply(context, nameIterator);
        }
    }

    static class GetOrCreateEnclosingGroupInMap implements BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject>,
            BiConsumer<ConfigMappingContext, NameIterator> {
        private final BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> getEnclosingMap;
        private final MapProperty enclosingMap;
        private final ConfigMappingInterface enclosingGroup;
        private final GroupProperty enclosedGroup;
        private final String mapPath;

        GetOrCreateEnclosingGroupInMap(
                final BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> getEnclosingMap,
                final MapProperty enclosingMap,
                final ConfigMappingInterface enclosingGroup,
                final GroupProperty enclosedGroup,
                final ArrayDeque<String> path) {
            this.getEnclosingMap = getEnclosingMap;
            this.enclosingMap = enclosingMap;
            this.enclosingGroup = enclosingGroup;
            this.enclosedGroup = enclosedGroup;
            this.mapPath = String.join(".", path);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ConfigMappingObject apply(final ConfigMappingContext context, final NameIterator ni) {
            NameIterator atMapPath = atMapPath(mapPath, ni);
            Map<?, ?> ourEnclosing = getEnclosingMap.apply(context, atMapPath);

            Converter<?> keyConverter = context.getKeyConverter(enclosingGroup.getInterfaceType(),
                    enclosingMap.getMethod().getName(), enclosingMap.getLevels() - 1);
            MapKey mapKey;
            if (enclosingMap.getValueProperty().isCollection()) {
                mapKey = MapKey.collectionKey(atMapPath.getNextSegment(), keyConverter);
            } else {
                mapKey = MapKey.key(atMapPath.getNextSegment(), ni.getAllPreviousSegments(), keyConverter);
            }
            ConfigMappingObject val = (ConfigMappingObject) context.getEnclosedField(enclosingGroup.getInterfaceType(),
                    mapKey.getKey(),
                    ourEnclosing);
            if (val == null) {
                StringBuilder sb = context.getStringBuilder();
                sb.replace(0, sb.length(), atMapPath.getAllPreviousSegmentsWith(mapKey.getKey()));

                context.applyNamingStrategy(
                        namingStrategy(enclosedGroup.getGroupType().getNamingStrategy(), enclosingGroup.getNamingStrategy()));
                val = (ConfigMappingObject) context.constructGroup(enclosedGroup.getGroupType().getInterfaceType());
                context.registerEnclosedField(enclosingGroup.getInterfaceType(), mapKey.getKey(), ourEnclosing, val);

                if (enclosingMap.getValueProperty().isCollection()) {
                    CollectionProperty collectionProperty = enclosingMap.getValueProperty().asCollection();
                    Collection collection = (Collection) ourEnclosing.get(mapKey.getConvertedKey());
                    if (collection == null) {
                        // Create the Collection in the Map does not have it
                        Class<?> collectionRawType = collectionProperty.getCollectionRawType();
                        IntFunction<Collection<?>> collectionFactory = ConfigMappingContext
                                .createCollectionFactory(collectionRawType);
                        // Get all the available indexes
                        List<Integer> indexes = context.getConfig().getIndexedPropertiesIndexes(
                                atMapPath.getAllPreviousSegmentsWith(normalizeIfIndexed(mapKey.getKey())));
                        collection = collectionFactory.apply(indexes.size());
                        // Initialize all expected elements in the list
                        if (collection instanceof List) {
                            for (Integer index : indexes) {
                                ((List<?>) collection).add(index, null);
                            }
                        }
                        ((Map) ourEnclosing).put(mapKey.getConvertedKey(), collection);
                    }

                    if (collection instanceof List) {
                        // We don't know the order in which the properties will be processed, so we set it manually
                        ((List) collection).set(mapKey.getIndex(), val);
                    } else {
                        collection.add(val);
                    }
                } else {
                    ((Map) ourEnclosing).put(mapKey.getConvertedKey(), val);
                }
            }
            return val;
        }

        @Override
        public void accept(final ConfigMappingContext context, final NameIterator ni) {
            apply(context, ni);
        }

        static class MapKey {
            private final String key;
            private final Object convertedKey;

            public MapKey(final String key, final Object convertedKey) {
                this.key = key;
                this.convertedKey = convertedKey;
            }

            public String getKey() {
                return key;
            }

            public Object getConvertedKey() {
                return convertedKey;
            }

            public int getIndex() {
                int indexStart = key.indexOf("[");
                int indexEnd = key.indexOf("]");
                if (indexStart != -1 && indexEnd != -1) {
                    String index = key.substring(indexStart + 1, indexEnd);
                    try {
                        return Integer.parseInt(index);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException();
                    }
                }
                throw new IllegalArgumentException();
            }

            // NameIterator#getPreviousSegment() returns the name without quotes.
            // We need add them if they exist, so we cannot reuse mapKey, or we are going to get NoSuchElement
            static MapKey key(final String mapKey, final String mapPath, final Converter<?> keyConverter) {
                String key = mapKey;
                if (mapPath.charAt(mapPath.length() - 1) == '"'
                        && mapPath.charAt(mapPath.length() - 1 - mapKey.length() - 1) == '"') {
                    key = "\"" + mapKey + "\"";
                }
                // Remove indexes to construct the group. The group constructor handles the indexes

                Object convertedKey = keyConverter.convert(mapKey);
                if (convertedKey.equals(mapKey)) {
                    convertedKey = normalizeIfIndexed(mapKey);
                }

                return new MapKey(normalizeIfIndexed(key), convertedKey);
            }

            static MapKey collectionKey(final String mapKey, final Converter<?> keyConverter) {
                Object convertedKey = keyConverter.convert(mapKey);
                if (convertedKey.equals(mapKey)) {
                    convertedKey = normalizeIfIndexed(mapKey);
                }

                return new MapKey(mapKey, convertedKey);
            }
        }
    }

    static class GetOrCreateEnclosingMapInGroup implements BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>>,
            BiConsumer<ConfigMappingContext, NameIterator> {
        private final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> delegate;
        private final ConfigMappingInterface enclosingGroup;
        private final MapProperty enclosedGroup;
        private final String groupPath;

        GetOrCreateEnclosingMapInGroup(
                final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> delegate,
                final ConfigMappingInterface enclosingGroup,
                final MapProperty enclosedGroup,
                final ArrayDeque<String> path) {
            this.delegate = delegate;
            this.enclosingGroup = enclosingGroup;
            this.enclosedGroup = enclosedGroup;
            this.groupPath = String.join(".", path);
        }

        @Override
        public Map<?, ?> apply(final ConfigMappingContext context, final NameIterator ni) {
            boolean consumeName = !enclosedGroup.isParentPropertyName();
            if (consumeName)
                ni.previous();
            ConfigMappingObject ourEnclosing = delegate.apply(context, ni);
            if (consumeName)
                ni.next();
            Class<?> enclosingType = enclosingGroup.getInterfaceType();
            String key = indexName(enclosedGroup.getMethod().getName(), groupPath, ni);
            Map<?, ?> val = (Map<?, ?>) context.getEnclosedField(enclosingType, key, ourEnclosing);
            context.applyNamingStrategy(enclosingGroup.getNamingStrategy());
            if (val == null) {
                // map is not yet constructed
                val = new HashMap<>();
                context.registerEnclosedField(enclosingType, key, ourEnclosing, val);
            }
            return val;
        }

        @Override
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

        @Override
        public ConfigMappingObject apply(final ConfigMappingContext context, final NameIterator ni) {
            ConfigMappingObject outer = getEnclosingFunction.apply(context, ni);
            // eagerly populated groups will always exist
            return (ConfigMappingObject) context.getEnclosedField(type, memberName, outer);
        }
    }

    // To recursively create Optional nested groups
    static class GetNestedEnclosing implements BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> {
        private final BiConsumer<ConfigMappingContext, NameIterator> matchAction;

        public GetNestedEnclosing(final BiConsumer<ConfigMappingContext, NameIterator> matchAction) {
            this.matchAction = matchAction;
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ConfigMappingObject apply(final ConfigMappingContext configMappingContext, final NameIterator nameIterator) {
            if (matchAction instanceof BiFunction) {
                return (ConfigMappingObject) ((BiFunction) matchAction).apply(configMappingContext, nameIterator);
            }
            return null;
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

    ConfigMappingContext mapConfiguration(SmallRyeConfig config) throws ConfigValidationException {
        for (ConfigSource configSource : config.getConfigSources()) {
            if (configSource instanceof DefaultValuesConfigSource) {
                DefaultValuesConfigSource defaultValuesConfigSource = (DefaultValuesConfigSource) configSource;
                defaultValuesConfigSource.registerDefaults(defaultValues);
            }
        }

        config.addPropertyNames(additionalMappedProperties(new HashSet<>(getProperties().keySet()), config));
        return SecretKeys.doUnlocked(() -> mapConfigurationInternal(config));
    }

    private ConfigMappingContext mapConfigurationInternal(SmallRyeConfig config) throws ConfigValidationException {
        Assert.checkNotNullParam("config", config);
        ConfigMappingContext context = new ConfigMappingContext(config);

        if (roots.isEmpty()) {
            return context;
        }

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
                context.unknownProperty(name);
            }
        }

        context.validateUnknown(
                config.getOptionalValue(SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN, boolean.class).orElse(this.validateUnknown));

        List<ConfigValidationException.Problem> problems = context.getProblems();
        if (!problems.isEmpty()) {
            throw new ConfigValidationException(problems.toArray(ConfigValidationException.Problem.NO_PROBLEMS));
        }
        context.fillInOptionals();

        return context;
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

    private static Set<String> additionalMappedProperties(final Set<String> mappedProperties, final SmallRyeConfig config) {
        // Collect EnvSource properties
        Set<String> envProperties = new HashSet<>();
        for (ConfigSource source : config.getConfigSources(EnvConfigSource.class)) {
            envProperties.addAll(source.getPropertyNames());
        }

        // Remove properties that we know that already exist
        for (String propertyName : config.getPropertyNames()) {
            mappedProperties.remove(propertyName);
        }

        Set<String> additionalMappedProperties = new HashSet<>();
        // Look for unmatched properties if we can find one in the Env ones and add it
        for (String mappedProperty : mappedProperties) {
            Set<String> matchedEnvProperties = new HashSet<>();
            String endMappedProperty = replaceNonAlphanumericByUnderscores(mappedProperty);
            for (String envProperty : envProperties) {
                if (envProperty.equalsIgnoreCase(endMappedProperty)) {
                    additionalMappedProperties.add(mappedProperty);
                    matchedEnvProperties.add(envProperty);
                    break;
                }

                NameIterator ni = new NameIterator(mappedProperty);
                StringBuilder sb = new StringBuilder();
                while (ni.hasNext()) {
                    String propertySegment = ni.getNextSegment();
                    if (isIndexed(propertySegment)) {
                        // A mapped index property is represented as foo.bar[*] or foo.bar[*].baz
                        // The env property is represented as FOO_BAR_0_ or FOO_BAR_0__BAZ
                        // We need to match these somehow
                        int position = ni.getPosition();
                        int indexStart = propertySegment.indexOf("[") + position + 1;
                        // If the segment is indexed, we try to match all previous segments with the env candidates
                        if (envProperty.length() >= indexStart
                                && envProperty.toLowerCase().startsWith(replaceNonAlphanumericByUnderscores(
                                        sb + propertySegment.substring(0, indexStart - position - 1) + "_"))) {

                            // Search for the ending _ to retrieve the possible index
                            int indexEnd = -1;
                            for (int i = indexStart + 1; i < envProperty.length(); i++) {
                                if (envProperty.charAt(i) == '_') {
                                    indexEnd = i;
                                    break;
                                }
                            }

                            // Extract the index from the env property
                            // We don't care if this is numeric, it will be validated on the mapping retrieval
                            String index = envProperty.substring(indexStart + 1, indexEnd);
                            sb.append(propertySegment, 0, propertySegment.indexOf("[") + 1)
                                    .append(index)
                                    .append("]");
                        }
                    } else {
                        sb.append(propertySegment);
                    }

                    ni.next();

                    if (ni.hasNext()) {
                        sb.append(".");
                    }
                }

                String mappedPropertyToMatch = sb.toString();
                if (envProperty.equalsIgnoreCase(replaceNonAlphanumericByUnderscores(mappedPropertyToMatch))) {
                    additionalMappedProperties.add(mappedPropertyToMatch);
                    matchedEnvProperties.add(envProperty);
                    // We cannot break here because if there are indexed properties they may match multiple envs
                }
            }
            envProperties.removeAll(matchedEnvProperties);
        }

        return additionalMappedProperties;
    }

    private static String normalizeIfIndexed(final String propertyName) {
        int indexStart = propertyName.indexOf("[");
        int indexEnd = propertyName.indexOf("]");
        if (indexStart != -1 && indexEnd != -1) {
            String index = propertyName.substring(indexStart + 1, indexEnd);
            if (index.equals("*")) {
                return propertyName.substring(0, indexStart);
            }
            try {
                Integer.parseInt(index);
                return propertyName.substring(0, indexStart);
            } catch (NumberFormatException e) {
                return propertyName;
            }
        }
        return propertyName;
    }

    private static boolean isIndexed(final String propertyName) {
        int indexStart = propertyName.indexOf("[");
        int indexEnd = propertyName.indexOf("]");
        if (indexStart != -1 && indexEnd != -1) {
            String index = propertyName.substring(indexStart + 1, indexEnd);
            if (index.equals("*")) {
                return true;
            }
            try {
                Integer.parseInt(index);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
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
