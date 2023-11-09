package io.smallrye.config;

import static io.smallrye.config.ConfigMappingContext.createCollectionFactory;
import static io.smallrye.config.ConfigMappingInterface.GroupProperty;
import static io.smallrye.config.ConfigMappingInterface.LeafProperty;
import static io.smallrye.config.ConfigMappingInterface.MapProperty;
import static io.smallrye.config.ConfigMappingInterface.PrimitiveProperty;
import static io.smallrye.config.ConfigMappingInterface.Property;
import static io.smallrye.config.ConfigMappingLoader.getConfigMapping;
import static io.smallrye.config.ConfigMappingLoader.getConfigMappingClass;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN;
import static io.smallrye.config.common.utils.StringUtil.replaceNonAlphanumericByUnderscores;
import static io.smallrye.config.common.utils.StringUtil.toLowerCaseAndDotted;
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
import io.smallrye.config._private.ConfigMessages;
import io.smallrye.config.common.utils.StringUtil;

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
    private final Map<String, String> defaultValues;
    private final boolean validateUnknown;

    ConfigMappingProvider(final Builder builder) {
        this.roots = new HashMap<>(builder.roots);
        this.matchActions = new KeyMap<>();
        this.properties = new HashMap<>();
        this.defaultValues = new HashMap<>();
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
                ConfigMappingInterface mapping = getConfigMapping(root);
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
        if (root.getRootValue() == null) {
            root.putRootValue(DO_NOTHING);
        }

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
            final Map<String, String> defaultValues,
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
        for (int i = 0; i < group.getPropertyCount(); i++) {
            Property property = group.getProperty(i);
            ArrayDeque<String> propertyPath = new ArrayDeque<>(currentPath);
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
                    property);
        }
    }

    private void processProperty(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final Map<String, String> defaultValues,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface group,
            final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> getEnclosingFunction,
            final Class<?> type,
            final Property property) {

        if (property.isOptional()) {
            // switch to lazy mode
            Property nestedProperty = property.asOptional().getNestedProperty();
            processOptionalProperty(currentPath, matchActions, defaultValues, namingStrategy, group, getEnclosingFunction, type,
                    nestedProperty);
        } else if (property.isGroup()) {
            GroupProperty nestedGroup = property.asGroup();
            GetOrCreateEnclosingGroupInGroup enclosingFunction = new GetOrCreateEnclosingGroupInGroup(getEnclosingFunction,
                    nestedGroup.getGroupType().getInterfaceType(),
                    nestedGroup.getMemberName(),
                    currentPath,
                    nestedGroup.getGroupType().getNamingStrategy(),
                    group.getInterfaceType(),
                    namingStrategy);
            processEagerGroup(currentPath, matchActions, defaultValues, namingStrategy, property.asGroup().getGroupType(),
                    enclosingFunction);
        } else if (property.isPrimitive()) {
            // already processed eagerly
            PrimitiveProperty primitiveProperty = property.asPrimitive();
            if (primitiveProperty.hasDefaultValue()) {
                addDefault(currentPath, primitiveProperty.getDefaultValue());
                // collections may also be represented without [] so we need to register both paths
                if (isCollection(currentPath)) {
                    addDefault(inlineCollectionPath(currentPath), primitiveProperty.getDefaultValue());
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
                addDefault(currentPath, leafProperty.getDefaultValue());
                // collections may also be represented without [] so we need to register both paths
                if (isCollection(currentPath)) {
                    addDefault(inlineCollectionPath(currentPath), leafProperty.getDefaultValue());
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
                    collectionProperty.getElement());
        }
    }

    private void processOptionalProperty(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final Map<String, String> defaultValues,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface group,
            final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> getEnclosingFunction,
            final Class<?> type,
            final Property property) {

        if (property.isGroup()) {
            GroupProperty nestedGroup = property.asGroup();
            // on match, always create the outermost group, which recursively creates inner groups
            GetOrCreateEnclosingGroupInGroup enclosingFunction = new GetOrCreateEnclosingGroupInGroup(getEnclosingFunction,
                    nestedGroup.getGroupType().getInterfaceType(),
                    nestedGroup.getMemberName(),
                    currentPath,
                    nestedGroup.getGroupType().getNamingStrategy(),
                    group.getInterfaceType(),
                    namingStrategy);
            processLazyGroupInGroup(currentPath, matchActions, defaultValues, namingStrategy, nestedGroup.getGroupType(),
                    enclosingFunction);
        } else if (property.isLeaf()) {
            LeafProperty leafProperty = property.asLeaf();
            if (leafProperty.hasDefaultValue()) {
                addDefault(currentPath, leafProperty.getDefaultValue());
                // collections may also be represented without [] so we need to register both paths
                if (isCollection(currentPath)) {
                    addDefault(inlineCollectionPath(currentPath), leafProperty.getDefaultValue());
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
                    collectionProperty.getElement());
        }
    }

    private void processLazyGroupInGroup(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final Map<String, String> defaultValues,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface group,
            final BiConsumer<ConfigMappingContext, NameIterator> matchAction) {
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
            boolean optional = property.isOptional();
            processLazyPropertyInGroup(currentPath, matchActions, defaultValues, matchAction, namingStrategy, group, optional,
                    property);
            while (currentPath.size() > pathLen) {
                currentPath.removeLast();
            }
        }
        int sc = group.getSuperTypeCount();
        for (int i = 0; i < sc; i++) {
            processLazyGroupInGroup(currentPath, matchActions, defaultValues, namingStrategy, group.getSuperType(i),
                    matchAction);
        }
    }

    private void processLazyPropertyInGroup(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final Map<String, String> defaultValues,
            final BiConsumer<ConfigMappingContext, NameIterator> matchAction,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface group,
            final boolean optional,
            final Property property) {

        if (property.isGroup() || optional && property.asOptional().getNestedProperty().isGroup()) {
            BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> delegate = property.isParentPropertyName()
                    ? new GetNestedEnclosing(matchAction)
                    : new ConsumeOneAndThenFn<>(new GetNestedEnclosing(matchAction));
            GroupProperty nestedGroup = property.isGroup() ? property.asGroup()
                    : property.asOptional().getNestedProperty().asGroup();
            GetOrCreateEnclosingGroupInGroup enclosingFunction = new GetOrCreateEnclosingGroupInGroup(delegate,
                    nestedGroup.getGroupType().getInterfaceType(),
                    nestedGroup.getMemberName(),
                    currentPath,
                    nestedGroup.getGroupType().getNamingStrategy(),
                    group.getInterfaceType(),
                    namingStrategy);
            processLazyGroupInGroup(currentPath, matchActions, defaultValues, namingStrategy, nestedGroup.getGroupType(),
                    enclosingFunction);
        } else if (property.isGroup()) {
            GroupProperty nestedGroup = property.asGroup();
            BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> delegate = property.isParentPropertyName()
                    ? new GetNestedEnclosing(matchAction)
                    : new ConsumeOneAndThenFn<>(new GetNestedEnclosing(matchAction));
            GetOrCreateEnclosingGroupInGroup enclosingFunction = new GetOrCreateEnclosingGroupInGroup(delegate,
                    nestedGroup.getGroupType().getInterfaceType(),
                    nestedGroup.getMemberName(),
                    currentPath,
                    nestedGroup.getGroupType().getNamingStrategy(),
                    group.getInterfaceType(),
                    namingStrategy);
            processLazyGroupInGroup(currentPath, matchActions, defaultValues, namingStrategy, nestedGroup.getGroupType(),
                    enclosingFunction);
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
                    addDefault(currentPath, primitiveProperty.getDefaultValue());
                    // collections may also be represented without [] so we need to register both paths
                    if (isCollection(currentPath)) {
                        addDefault(inlineCollectionPath(currentPath), primitiveProperty.getDefaultValue());
                    }
                }
            } else if (property.isLeaf() && optional) {
                LeafProperty leafProperty = property.asOptional().getNestedProperty().asLeaf();
                if (leafProperty.hasDefaultValue()) {
                    addDefault(currentPath, leafProperty.getDefaultValue());
                    // collections may also be represented without [] so we need to register both paths
                    if (isCollection(currentPath)) {
                        addDefault(inlineCollectionPath(currentPath), leafProperty.getDefaultValue());
                    }
                }
            } else {
                LeafProperty leafProperty = property.asLeaf();
                if (leafProperty.hasDefaultValue()) {
                    addDefault(currentPath, leafProperty.getDefaultValue());
                    // collections may also be represented without [] so we need to register both paths
                    if (isCollection(currentPath)) {
                        addDefault(inlineCollectionPath(currentPath), leafProperty.getDefaultValue());
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
            processLazyPropertyInGroup(currentPath, matchActions, defaultValues, matchAction, namingStrategy, group, false,
                    collectionProperty.getElement());
        }
    }

    private void processLazyMapInGroup(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final Map<String, String> defaultValues,
            final MapProperty mapProperty,
            final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> getEnclosingGroup,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface enclosingGroup) {

        BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> delegate = mapProperty.isParentPropertyName()
                ? getEnclosingGroup
                : new ConsumeOneAndThenFn<>(getEnclosingGroup);
        GetOrCreateEnclosingMapInGroup getEnclosingMap = new GetOrCreateEnclosingMapInGroup(delegate,
                mapProperty.getMemberName(), currentPath, enclosingGroup.getInterfaceType(),
                enclosingGroup.getNamingStrategy());
        processLazyMap(currentPath, matchActions, defaultValues, mapProperty, getEnclosingMap, namingStrategy, enclosingGroup);
    }

    private void processLazyMap(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final Map<String, String> defaultValues,
            final MapProperty property,
            final BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> getEnclosingMap,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface enclosingGroup) {

        Property valueProperty = property.getValueProperty();
        ArrayDeque<String> unnamedPath = new ArrayDeque<>(currentPath);
        currentPath.addLast("*");
        processLazyMapValue(currentPath, matchActions, defaultValues, property, valueProperty, false, getEnclosingMap,
                namingStrategy, enclosingGroup);
        if (property.hasKeyUnnamed()) {
            processLazyMapValue(unnamedPath, matchActions, defaultValues, property, valueProperty, true, getEnclosingMap,
                    namingStrategy, enclosingGroup);
        }
    }

    private void processLazyMapValue(
            final ArrayDeque<String> currentPath,
            final KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> matchActions,
            final Map<String, String> defaultValues,
            final MapProperty mapProperty,
            final Property mapValueProperty,
            final boolean keyUnnamed,
            final BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> getEnclosingMap,
            final NamingStrategy namingStrategy,
            final ConfigMappingInterface enclosingGroup) {

        if (mapValueProperty.isLeaf()) {
            LeafProperty leafProperty = mapValueProperty.asLeaf();
            String mapPath = String.join(".", currentPath);

            CreateValueInMap matchAction = new CreateValueInMap(
                    getEnclosingMap,
                    mapPath,
                    mapProperty.getKeyRawType(), mapProperty.hasKeyConvertWith() ? mapProperty.getKeyConvertWith() : null,
                    leafProperty.getValueRawType(),
                    leafProperty.getConvertWith(),
                    mapProperty.getValueProperty().isCollection(),
                    mapProperty.getValueProperty().isCollection()
                            ? mapProperty.getValueProperty().asCollection().getCollectionRawType()
                            : null);

            addAction(currentPath, mapProperty, matchAction);

            // action to match all segments of a key after the map path
            KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> mapAction = matchActions.find(mapPath);
            if (mapAction != null) {
                mapAction.putAny(matchActions.find(mapPath));
            }
            // collections may also be represented without [] so we need to register both paths
            if (isCollection(currentPath)) {
                addAction(inlineCollectionPath(currentPath), leafProperty, DO_NOTHING);
            }
        } else if (mapValueProperty.isMap()) {
            GetOrCreateEnclosingMapInMap getNestedEnclosingMap = new GetOrCreateEnclosingMapInMap(
                    getEnclosingMap,
                    mapProperty.getKeyRawType(),
                    keyUnnamed,
                    mapProperty.getKeyUnnamed(),
                    mapProperty.hasKeyConvertWith() ? mapProperty.getKeyConvertWith() : null);
            processLazyMap(currentPath, matchActions, defaultValues, mapValueProperty.asMap(), getNestedEnclosingMap,
                    namingStrategy, enclosingGroup);
        } else if (mapValueProperty.isGroup()) {
            GetOrCreateEnclosingGroupInMap ef = new GetOrCreateEnclosingGroupInMap(
                    getEnclosingMap,
                    currentPath,
                    mapProperty.getKeyRawType(),
                    keyUnnamed,
                    mapProperty.getKeyUnnamed(),
                    mapProperty.hasKeyConvertWith() ? mapProperty.getKeyConvertWith() : null,
                    mapProperty.getValueProperty().isCollection()
                            ? mapProperty.getValueProperty().asCollection().getCollectionRawType()
                            : null,
                    mapProperty.getValueProperty().isCollection(),
                    mapValueProperty.asGroup().getGroupType().getInterfaceType(),
                    mapValueProperty.asGroup().getGroupType().getNamingStrategy(),
                    enclosingGroup.getInterfaceType(),
                    enclosingGroup.getNamingStrategy());
            processLazyGroupInGroup(currentPath, matchActions, defaultValues, namingStrategy,
                    mapValueProperty.asGroup().getGroupType(), ef);
        } else if (mapValueProperty.isCollection()) {
            CollectionProperty collectionProperty = mapValueProperty.asCollection();
            Property element = collectionProperty.getElement();
            if (!element.hasConvertWith() && !keyUnnamed && !element.isLeaf()) {
                currentPath.addLast(currentPath.removeLast() + "[*]");
            }
            processLazyMapValue(currentPath, matchActions, defaultValues, mapProperty, element, keyUnnamed, getEnclosingMap,
                    namingStrategy, enclosingGroup);
        }
    }

    private void addAction(
            final ArrayDeque<String> currentPath,
            final Property property,
            final BiConsumer<ConfigMappingContext, NameIterator> action) {
        KeyMap<BiConsumer<ConfigMappingContext, NameIterator>> current = matchActions.findOrAdd(currentPath);
        Property previous = properties.put(String.join(".", currentPath), property);
        if (current.hasRootValue() && current.getRootValue() != action && previous != null && !previous.equals(property)) {
            throw ConfigMessages.msg.ambiguousMapping(String.join(".", currentPath), property.getMemberName(),
                    previous.getMemberName());
        }
        current.putRootValue(action);
    }

    private static boolean isCollection(final ArrayDeque<String> currentPath) {
        return !currentPath.isEmpty() && currentPath.getLast().endsWith("[*]");
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

    private static NameIterator mapPath(final String mapPath, final NameIterator propertyName) {
        int segments = 0;
        NameIterator countSegments = new NameIterator(mapPath);
        while (countSegments.hasNext()) {
            segments++;
            countSegments.next();
        }

        // We don't want the key; keys only exist when the map ends with '*'; else it is an unnamed key
        if (mapPath.endsWith("*") || mapPath.endsWith("*[*]")) {
            segments = segments - 1;
        }

        NameIterator propertyMap = new NameIterator(propertyName.getName());
        propertyMap.next(segments);
        return propertyMap;
    }

    private static String propertyName(final Property property, final ConfigMappingInterface group,
            final NamingStrategy namingStrategy) {
        return namingStrategy(namingStrategy, group.getNamingStrategy()).apply(property.getPropertyName());
    }

    private static NamingStrategy namingStrategy(NamingStrategy enclosing, NamingStrategy enclosed) {
        // if enclosed element is using default, then use the enclosing naming strategy
        if (enclosed.isDefault()) {
            return enclosing;
        } else {
            return enclosed;
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
        private final Class<?> groupType;
        private final String groupName;
        private final String groupPath;
        private final int groupDepth;
        private final NamingStrategy groupNaming;
        private final Class<?> enclosingGroupType;
        private final NamingStrategy enclosingGroupNaming;

        public GetOrCreateEnclosingGroupInGroup(
                final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> delegate,
                final Class<?> groupType,
                final String groupName,
                final ArrayDeque<String> groupPath,
                final NamingStrategy groupNaming,
                final Class<?> enclosingGroupType,
                final NamingStrategy enclosingGroupNaming) {

            this.delegate = delegate;
            this.groupType = groupType;
            this.groupName = groupName;
            this.groupPath = String.join(".", groupPath);
            this.groupDepth = groupPath.size();
            this.groupNaming = groupNaming;
            this.enclosingGroupType = enclosingGroupType;
            this.enclosingGroupNaming = enclosingGroupNaming;
        }

        @Override
        public ConfigMappingObject apply(final ConfigMappingContext context, final NameIterator ni) {
            ConfigMappingObject ourEnclosing = delegate.apply(context, ni);
            String key = indexName(groupName, groupPath, ni);
            ConfigMappingObject val = (ConfigMappingObject) context.getEnclosedField(enclosingGroupType, key, ourEnclosing);
            context.applyNamingStrategy(namingStrategy(enclosingGroupNaming, groupNaming));
            if (val == null) {
                NameIterator groupNi = new NameIterator(ni.getName());
                groupNi.next(groupDepth);
                StringBuilder sb = context.getStringBuilder();
                sb.replace(0, sb.length(), groupNi.getAllPreviousSegments());
                val = (ConfigMappingObject) context.constructGroup(groupType);
                context.registerEnclosedField(enclosingGroupType, key, ourEnclosing, val);
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
        private final BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> delegate;

        private final String mapPath;
        private final Class<?> mapKeyRawType;
        private final boolean mapKeyUnnamed;
        private final String mapKeyUnnamedKey;
        private final Class<? extends Converter<?>> mapKeyConverter;
        private final Class<?> mapValueRawType;
        private final boolean mapValueCollection;
        private final Class<?> groupType;
        private final NamingStrategy groupNaming;
        private final Class<?> enclosingGroupType;
        private final NamingStrategy enclosingGroupNaming;

        public GetOrCreateEnclosingGroupInMap(
                final BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> delegate,
                final ArrayDeque<String> mapPath,
                final Class<?> mapKeyRawType,
                final boolean mapKeyUnnamed,
                final String mapKeyUnnamedKey,
                final Class<? extends Converter<?>> mapKeyConverter,
                final Class<?> mapValueRawType,
                final boolean mapValueCollection,
                final Class<?> groupType,
                final NamingStrategy groupNaming,
                final Class<?> enclosingGroupType,
                final NamingStrategy enclosingGroupNaming) {

            this.delegate = delegate;
            this.mapPath = String.join(".", mapPath);
            this.mapKeyRawType = mapKeyRawType;
            this.mapKeyUnnamed = mapKeyUnnamed;
            this.mapKeyUnnamedKey = mapKeyUnnamedKey;
            this.mapKeyConverter = mapKeyConverter;
            this.mapValueRawType = mapValueRawType;
            this.mapValueCollection = mapValueCollection;
            this.groupType = groupType;
            this.groupNaming = groupNaming;
            this.enclosingGroupType = enclosingGroupType;
            this.enclosingGroupNaming = enclosingGroupNaming;
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ConfigMappingObject apply(final ConfigMappingContext context, final NameIterator ni) {
            NameIterator niMapPath = mapPath(mapPath, ni);
            MapKey mapKey = mapKey(context, ni, niMapPath);
            Map<?, ?> ourEnclosing = delegate.apply(context, niMapPath);
            ConfigMappingObject val = (ConfigMappingObject) context.getEnclosedField(enclosingGroupType, mapKey.getKey(),
                    ourEnclosing);
            if (val == null) {
                StringBuilder sb = context.getStringBuilder();
                sb.replace(0, sb.length(), mapKeyUnnamed ? niMapPath.getAllPreviousSegments()
                        : niMapPath.getAllPreviousSegmentsWith(mapKey.getKey()));
                context.applyNamingStrategy(namingStrategy(enclosingGroupNaming, groupNaming));
                val = (ConfigMappingObject) context.constructGroup(groupType);
                context.registerEnclosedField(enclosingGroupType, mapKey.getKey(), ourEnclosing, val);
                if (mapValueCollection) {
                    Collection collection = (Collection) ourEnclosing.get(mapKey.getConvertedKey());
                    if (collection == null) {
                        // Create the Collection in the Map does not have it
                        IntFunction<Collection<?>> collectionFactory = createCollectionFactory(mapValueRawType);
                        // Get all the available indexes
                        List<Integer> indexes = mapKeyUnnamed ? List.of(0)
                                : context.getConfig()
                                        .getIndexedPropertiesIndexes(niMapPath.getAllPreviousSegmentsWith(mapKey.getNameKey()));
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

        private MapKey mapKey(final ConfigMappingContext context, final NameIterator ni, final NameIterator mapPath) {
            if (mapKeyUnnamed && mapKeyUnnamedKey == null) {
                return new MapKey(null, null, null, 0);
            }

            String rawKey = mapKeyUnnamed ? mapKeyUnnamedKey : mapPath.getNextSegment();
            mapPath.next();
            String pathKey = mapPath.getAllPreviousSegments();
            mapPath.previous();
            Converter<?> converterKey;
            if (mapKeyConverter != null) {
                converterKey = context.getConverterInstance(mapKeyConverter);
            } else {
                converterKey = context.getConfig().requireConverter(mapKeyRawType);
            }

            // This will be the key to use to store the value in the map
            String nameKey = normalizeIfIndexed(rawKey);
            Object convertedKey = converterKey.convert(rawKey);
            if (convertedKey.equals(rawKey)) {
                convertedKey = nameKey;
            }

            int index = -1;
            if (mapValueCollection) {
                index = mapKeyUnnamed ? 0 : getIndex(rawKey);
            }

            // NameIterator#getNextSegment() returns the name without quotes. We need add them if they exist for lookups to work properly
            if (pathKey.charAt(pathKey.length() - 1 - rawKey.length() + nameKey.length()) == '"'
                    && pathKey.charAt(pathKey.length() - 1 - rawKey.length() - 1) == '"') {
                nameKey = "\"" + nameKey + "\"";
                rawKey = mapValueCollection ? nameKey + "[" + index + "]" : nameKey;
            }

            if (!mapKeyUnnamed && (index >= 0 ? rawKey : nameKey).equals(mapKeyUnnamedKey)) {
                throw ConfigMessages.msg.explicitNameInUnnamed(ni.getName(), rawKey);
            }

            return new MapKey(rawKey, nameKey, convertedKey, index);
        }

        static class MapKey {
            private final String rawKey;
            private final String nameKey;
            private final Object convertedKey;
            private final int index;

            public MapKey(final String rawKey, final String nameKey, final Object convertedKey, final int index) {
                this.rawKey = rawKey;
                this.nameKey = nameKey;
                this.convertedKey = convertedKey;
                this.index = index;
            }

            public String getKey() {
                return index >= 0 ? rawKey : nameKey;
            }

            public String getNameKey() {
                return nameKey;
            }

            public Object getConvertedKey() {
                return convertedKey;
            }

            public int getIndex() {
                return index;
            }
        }
    }

    static class GetOrCreateEnclosingMapInGroup implements BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>>,
            BiConsumer<ConfigMappingContext, NameIterator> {
        private final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> delegate;
        private final String mapName;
        private final String mapPath;
        private final Class<?> enclosingGroupType;
        private final NamingStrategy enclosingGroupNaming;

        public GetOrCreateEnclosingMapInGroup(
                final BiFunction<ConfigMappingContext, NameIterator, ConfigMappingObject> delegate,
                final String mapName,
                final ArrayDeque<String> mapPath,
                final Class<?> enclosingGroupType,
                final NamingStrategy enclosingGroupNaming) {

            this.delegate = delegate;
            this.mapName = mapName;
            this.mapPath = String.join(".", mapPath);
            this.enclosingGroupType = enclosingGroupType;
            this.enclosingGroupNaming = enclosingGroupNaming;
        }

        @Override
        public Map<?, ?> apply(final ConfigMappingContext context, final NameIterator ni) {
            ConfigMappingObject ourEnclosing = delegate.apply(context, ni);
            String key = indexName(mapName, mapPath, ni);
            Map<?, ?> val = (Map<?, ?>) context.getEnclosedField(enclosingGroupType, key, ourEnclosing);
            context.applyNamingStrategy(enclosingGroupNaming);
            if (val == null) {
                // map is not yet constructed
                val = new HashMap<>();
                context.registerEnclosedField(enclosingGroupType, key, ourEnclosing, val);
            }
            return val;
        }

        @Override
        public void accept(final ConfigMappingContext context, final NameIterator ni) {
            apply(context, ni);
        }
    }

    static class CreateValueInMap implements BiConsumer<ConfigMappingContext, NameIterator> {
        private final BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> delegate;
        private final String mapPath;
        private final Class<?> mapKeyRawType;
        private final Class<? extends Converter<?>> mapKeyConverter;
        private final Class<?> mapValueRawType;
        private final Class<? extends Converter<?>> mapValueConverter;
        private final boolean mapValueCollection;
        private final Class<?> mapValueCollectionRawType;

        public CreateValueInMap(
                final BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> delegate,
                final String mapPath,
                final Class<?> mapKeyRawType,
                final Class<? extends Converter<?>> mapKeyConverter,
                final Class<?> mapValueRawType,
                final Class<? extends Converter<?>> mapValueConverter,
                final boolean mapValueCollection,
                final Class<?> mapValueCollectionRawType) {

            this.delegate = delegate;
            this.mapPath = mapPath;
            this.mapKeyRawType = mapKeyRawType;
            this.mapKeyConverter = mapKeyConverter;
            this.mapValueRawType = mapValueRawType;
            this.mapValueConverter = mapValueConverter;
            this.mapValueCollection = mapValueCollection;
            this.mapValueCollectionRawType = mapValueCollectionRawType;
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void accept(final ConfigMappingContext context, final NameIterator ni) {
            // Place the cursor at the map path
            NameIterator niMapPath = mapPath(mapPath, ni);
            Map<?, ?> map = delegate.apply(context, niMapPath);

            String rawMapKey;
            String configKey;
            boolean indexed = isIndexed(ni.getPreviousSegment());
            if (indexed && ni.hasPrevious()) {
                rawMapKey = normalizeIfIndexed(niMapPath.getName().substring(niMapPath.getPosition() + 1));
                configKey = niMapPath.getAllPreviousSegmentsWith(rawMapKey);
            } else {
                rawMapKey = niMapPath.getName().substring(niMapPath.getPosition() + 1);
                configKey = ni.getAllPreviousSegments();
            }

            // Remove quotes if exists
            if (rawMapKey.length() > 1 && rawMapKey.charAt(0) == '"' && rawMapKey.charAt(rawMapKey.length() - 1) == '"') {
                rawMapKey = rawMapKey.substring(1, rawMapKey.length() - 1);
            }

            Converter<?> keyConv;
            SmallRyeConfig config = context.getConfig();
            if (mapKeyConverter != null) {
                keyConv = context.getConverterInstance(mapKeyConverter);
            } else {
                keyConv = config.requireConverter(mapKeyRawType);
            }
            Converter<?> valueConv;
            if (mapValueConverter != null) {
                valueConv = context.getConverterInstance(mapValueConverter);
            } else {
                valueConv = config.requireConverter(mapValueRawType);
            }

            if (mapValueCollection && mapValueConverter == null) {
                IntFunction collectionFactory = createCollectionFactory(mapValueCollectionRawType);
                ((Map) map).put(keyConv.convert(rawMapKey), config.getValues(configKey, valueConv, collectionFactory));
            } else {
                ((Map) map).put(keyConv.convert(rawMapKey), config.getValue(configKey, valueConv));
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static class GetOrCreateEnclosingMapInMap implements BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>>,
            BiConsumer<ConfigMappingContext, NameIterator> {
        private final BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> delegate;
        private final Class<?> mapKeyRawType;
        private final boolean mapKeyUnnamed;
        private final String mapKeyUnnamedKey;
        private final Class<? extends Converter<?>> mapKeyConverter;

        public GetOrCreateEnclosingMapInMap(
                final BiFunction<ConfigMappingContext, NameIterator, Map<?, ?>> delegate,
                final Class<?> mapKeyRawType,
                final boolean mapKeyUnnamed,
                final String mapKeyUnnamedKey,
                final Class<? extends Converter<?>> mapKeyConverter) {

            this.delegate = delegate;
            this.mapKeyRawType = mapKeyRawType;
            this.mapKeyUnnamed = mapKeyUnnamed;
            this.mapKeyUnnamedKey = mapKeyUnnamedKey;
            this.mapKeyConverter = mapKeyConverter;
        }

        @Override
        public Map<?, ?> apply(final ConfigMappingContext context, final NameIterator ni) {
            if (!mapKeyUnnamed) {
                ni.previous();
            }
            Map<?, ?> enclosingMap = delegate.apply(context, ni);
            if (!mapKeyUnnamed) {
                ni.next();
            }
            String rawMapKey = mapKeyUnnamed ? mapKeyUnnamedKey : ni.getPreviousSegment();
            Converter<?> keyConv;
            SmallRyeConfig config = context.getConfig();
            if (mapKeyConverter != null) {
                keyConv = context.getConverterInstance(mapKeyConverter);
            } else {
                keyConv = config.requireConverter(mapKeyRawType);
            }
            Object key = rawMapKey != null ? keyConv.convert(rawMapKey) : null;
            return (Map) ((Map) enclosingMap).computeIfAbsent(key, map -> new HashMap<>());
        }

        @Override
        public void accept(final ConfigMappingContext context, final NameIterator ni) {
            apply(context, ni);
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

    Map<String, String> getDefaultValues() {
        return defaultValues;
    }

    private void addDefault(ArrayDeque<String> path, String value) {
        defaultValues.put(String.join(".", path), value);
    }

    ConfigMappingContext mapConfiguration(SmallRyeConfig config) throws ConfigValidationException {
        // We need to set defaults from mappings here, because in a CDI environment mappings are added on an existent Config instance
        ConfigSource configSource = config.getDefaultValues();
        if (configSource instanceof DefaultValuesConfigSource) {
            DefaultValuesConfigSource defaultValuesConfigSource = (DefaultValuesConfigSource) configSource;
            defaultValuesConfigSource.addDefaults(defaultValues);
        }
        matchPropertiesWithEnv(config, roots.keySet(), getProperties().keySet());
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
        for (String name : filterPropertiesInRoots(config.getPropertyNames(), roots.keySet())) {
            NameIterator ni = new NameIterator(name);
            BiConsumer<ConfigMappingContext, NameIterator> action = matchActions.findRootValue(ni);
            if (action != null) {
                action.accept(context, ni);
            } else {
                context.unknownProperty(name);
            }
        }

        boolean validateUnknown = config.getOptionalValue(SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN, boolean.class)
                .orElse(this.validateUnknown);
        if (validateUnknown) {
            context.reportUnknown();
        }

        List<ConfigValidationException.Problem> problems = context.getProblems();
        if (!problems.isEmpty()) {
            throw new ConfigValidationException(problems.toArray(ConfigValidationException.Problem.NO_PROBLEMS));
        }
        context.fillInOptionals();

        return context;
    }

    /**
     * Filters the full list of properties names in Config to only the property names that can match any of the
     * prefixes (namespaces) registered in mappings.
     *
     * @param properties the available property names in Config.
     * @param roots the registered mapping roots.
     *
     * @return the property names that match to at least one root.
     */
    private static Iterable<String> filterPropertiesInRoots(final Iterable<String> properties, final Set<String> roots) {
        if (roots.isEmpty()) {
            return properties;
        }

        // Will match everything, so no point in filtering
        if (roots.contains("")) {
            return properties;
        }

        List<String> matchedProperties = new ArrayList<>();
        for (String property : properties) {
            for (String root : roots) {
                if (isPropertyInRoot(property, root)) {
                    matchedProperties.add(property);
                    break;
                }
            }
        }
        return matchedProperties;
    }

    private static void matchPropertiesWithEnv(final SmallRyeConfig config, final Set<String> roots,
            final Set<String> mappedProperties) {
        // TODO - We shouldn't be mutating the EnvSource.
        // We should do the calculation when creating the EnvSource, but right mappings and sources are not well integrated.

        // Collect properties from all sources except Env
        List<String> configuredProperties = new ArrayList<>();
        for (ConfigSource configSource : config.getConfigSources()) {
            if (!(configSource instanceof EnvConfigSource)) {
                Set<String> propertyNames = configSource.getPropertyNames();
                if (propertyNames != null) {
                    configuredProperties.addAll(propertyNames);
                }
            }
        }

        // Check Env properties
        StringBuilder sb = new StringBuilder();
        for (ConfigSource configSource : config.getConfigSources(EnvConfigSource.class)) {
            EnvConfigSource envConfigSource = (EnvConfigSource) configSource;
            // Filter Envs with roots
            List<String> envProperties = new ArrayList<>();
            if (roots.contains("")) {
                envProperties.addAll(envConfigSource.getPropertyNames());
            } else {
                for (String envProperty : envConfigSource.getPropertyNames()) {
                    for (String root : roots) {
                        if (isEnvPropertyInRoot(envProperty, root)) {
                            envProperties.add(envProperty);
                            break;
                        }
                    }
                }
            }

            // Try to match Env with Root mapped property and generate the expected format
            for (String envProperty : envProperties) {
                // We improve matching here by filtering only mapped properties from the matched root
                for (String mappedProperty : mappedProperties) {
                    List<Integer> indexOfDashes = indexOfDashes(mappedProperty, envProperty);
                    if (indexOfDashes != null) {
                        sb.append(envProperty);
                        for (Integer dash : indexOfDashes) {
                            sb.setCharAt(dash, '-');
                        }
                        String expectedEnvProperty = sb.toString();
                        if (!envProperty.equals(expectedEnvProperty)) {
                            envConfigSource.getPropertyNames().add(sb.toString());
                            envConfigSource.getPropertyNames().remove(envProperty);
                        }
                        sb.setLength(0);
                        break;
                    }
                }
            }

            // Match configured properties with Env with the same semantic meaning and use that one
            for (String configuredProperty : configuredProperties) {
                Set<String> envNames = envConfigSource.getPropertyNames();
                if (envConfigSource.hasPropertyName(configuredProperty)) {
                    if (!envNames.contains(configuredProperty)) {
                        // this may be expensive, but it shouldn't happend that often
                        envNames.remove(toLowerCaseAndDotted(replaceNonAlphanumericByUnderscores(configuredProperty)));
                        envNames.add(configuredProperty);
                    }
                }
            }
        }
    }

    private static boolean isPropertyInRoot(final String property, final String root) {
        if (property.equals(root)) {
            return true;
        }

        // if property is less than the root no way to match
        if (property.length() <= root.length()) {
            return false;
        }

        // foo.bar
        // foo.bar."baz"
        // foo.bar[0]
        char c = property.charAt(root.length());
        if ((c == '.') || c == '[') {
            return property.startsWith(root);
        }

        return false;
    }

    /**
     * Matches if a dotted Environment property name is part of a registered root.
     *
     * @param envProperty a generated dotted property from the {@link EnvConfigSource}.
     * @param root the root name
     * @return <code>true</code> if the env property ir part of the root, or <code>false</code> otherwise.
     */
    private static boolean isEnvPropertyInRoot(final String envProperty, final String root) {
        if (envProperty.equals(root)) {
            return true;
        }

        // if property is less than the root no way to match
        if (envProperty.length() <= root.length()) {
            return false;
        }

        // foo.bar
        // foo.bar."baz"
        // foo.bar[0]
        char e = envProperty.charAt(root.length());
        if ((e == '.') || e == '[') {
            for (int i = 0; i < root.length(); i++) {
                char r = root.charAt(i);
                e = envProperty.charAt(i);
                if (r == '-') {
                    if (e != '.' && e != '-') {
                        return false;
                    }
                } else if (r != e) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Finds and returns all indexes from a dotted Environment property name, related to its matched mapped
     * property name that must be replaced with a dash. This allows to set single environment variables as
     * <code>FOO_BAR_BAZ</code> and match them to mappeds properties like <code>foo.*.baz</code>,
     * <code>foo-bar.baz</code> or any other combinations find in mappings, without the need of additional metadata.
     *
     * @param mappedProperty the mapping property name.
     * @param envProperty a generated dotted property from the {@link EnvConfigSource}.
     * @return a List of indexes from the env property name to replace with a dash.
     */
    private static List<Integer> indexOfDashes(final String mappedProperty, final String envProperty) {
        if (mappedProperty.length() > envProperty.length()) {
            return null;
        }

        List<Integer> dashesPosition = null;
        int matchPosition = envProperty.length() - 1;
        for (int i = mappedProperty.length() - 1; i >= 0; i--) {
            if (matchPosition == -1) {
                return null;
            }

            char c = mappedProperty.charAt(i);
            if (c == '.' || c == '-') {
                char p = envProperty.charAt(matchPosition);
                if (p != '.' && p != '-') { // a property coming from env can either be . or -
                    return null;
                }
                if (c == '-') {
                    if (dashesPosition == null) {
                        dashesPosition = new ArrayList<>();
                    }
                    dashesPosition.add(matchPosition);
                }
                matchPosition--;
            } else if (c == '*') { // it's a map - skip to next separator
                char p = envProperty.charAt(matchPosition);
                if (p == '"') {
                    matchPosition = envProperty.lastIndexOf('"', matchPosition - 1);
                    if (matchPosition != -1) {
                        matchPosition = envProperty.lastIndexOf('.', matchPosition);
                    }
                }
                matchPosition = envProperty.lastIndexOf('.', matchPosition);
            } else if (c == ']') { // it's a collection - skip to next separator
                i = i - 2;
                matchPosition = envProperty.lastIndexOf('[', matchPosition);
                if (matchPosition != -1) {
                    matchPosition--;
                }
            } else if (c != envProperty.charAt(matchPosition)) {
                return null;
            } else {
                matchPosition--;
            }
        }
        return dashesPosition;
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
        int indexStart = propertyName.indexOf('[');
        int indexEnd = propertyName.indexOf(']');
        if (indexStart != -1 && indexEnd != -1) {
            int indexLength = indexEnd - (indexStart + 1);
            if (indexLength == 1 && ((CharSequence) propertyName).charAt(indexStart + 1) == '*') {
                return true;
            }
            return StringUtil.isNumeric(propertyName, indexStart + 1, indexEnd);
        }
        return false;
    }

    private static int getIndex(final String propertyName) {
        int indexStart = propertyName.indexOf('[');
        int indexEnd = propertyName.indexOf(']');
        if (indexStart != -1 && indexEnd != -1) {
            try {
                return Integer.parseInt(propertyName.substring(indexStart + 1, indexEnd));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException();
            }
        }
        throw new IllegalArgumentException();
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
