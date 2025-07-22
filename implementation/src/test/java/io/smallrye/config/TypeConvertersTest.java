package io.smallrye.config;

import static io.smallrye.config.Converters.newCollectionConverter;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

public class TypeConvertersTest {
    private final Map<TypeConverter<?>, Converter<?>> converters = new ConcurrentHashMap<>();
    private final Map<Type, Function<Converter<?>, Converter<?>>> typeConverters = new ConcurrentHashMap<>();

    @Test
    void converter() {
        converters.put(TypeConverter.of(String.class), Converters.STRING_CONVERTER);
        typeConverters.put(Optional.class, new Function<Converter<?>, Converter<?>>() {
            @Override
            public Converter<?> apply(Converter<?> converter) {
                return Converters.newOptionalConverter(converter);
            }
        });
        typeConverters.put(Supplier.class, new Function<Converter<?>, Converter<?>>() {
            @Override
            public Converter<?> apply(Converter<?> converter) {
                return new Converter<Supplier<?>>() {
                    @Override
                    public Supplier<?> convert(String value) throws IllegalArgumentException, NullPointerException {
                        return new Supplier<Object>() {
                            @Override
                            public Object get() {
                                return value;
                            }
                        };
                    }
                };
            }
        });
        typeConverters.put(List.class, new Function<Converter<?>, Converter<?>>() {
            @Override
            public Converter<?> apply(Converter<?> converter) {
                return newCollectionConverter(converter, ArrayList::new);
            }
        });

        assertNotNull(requireConverter(TypeConverter.of(String.class)));
        assertNotNull(requireConverter(new TypeConverter<Optional<String>>() {
        }));
        assertNotNull(requireConverter(new TypeConverter<Supplier<String>>() {
        }));
        assertNotNull(requireConverter(new TypeConverter<List<String>>() {
        }));

        Converter<List<String>> listConverter = requireConverter(new TypeConverter<>() {
        });
        assertIterableEquals(List.of("a", "b", "c"), listConverter.convert("a,b,c"));
    }

    @SuppressWarnings("unchecked")
    <T> Converter<T> requireConverter(TypeConverter<T> type) {
        if (type.isResolvable()) {
            Converter<?> converter = converters.get(type);
            if (converter != null) {
                return (Converter<T>) converter;
            }
        } else {
            Function<Converter<?>, Converter<?>> rawTypeConverter = typeConverters.get(type.getRawType());
            if (rawTypeConverter != null) {
                if (type.getType() instanceof ParameterizedType paramType) {
                    Type[] actualTypes = paramType.getActualTypeArguments();
                    if (actualTypes.length == 1) {
                        return (Converter<T>) rawTypeConverter.apply(requireConverter(TypeConverter.of(actualTypes[0])));
                    }
                }
            }
        }
        return null;
    }

    public static abstract class TypeConverter<T> {
        private final Type type;

        private TypeConverter() {
            Type superclass = getClass().getGenericSuperclass();
            if (superclass instanceof Class) {
                throw new RuntimeException("Missing type parameter.");
            }
            this.type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
        }

        private TypeConverter(Type type) {
            this.type = type;
        }

        boolean isResolvable() {
            return type instanceof Class<?>;
        }

        Type getType() {
            return type;
        }

        Class<?> getRawType() {
            return rawTypeOf(type);
        }

        static <T> TypeConverter<T> of(Type type) {
            return new TypeConverter<>(type) {
            };
        }

        static <T> TypeConverter<T> of(Class<T> type) {
            return new TypeConverter<>(type) {
            };
        }

        @SuppressWarnings("unchecked")
        private static <T> Class<T> rawTypeOf(Type type) {
            if (type instanceof Class<?>) {
                return (Class<T>) type;
            } else if (type instanceof ParameterizedType) {
                return rawTypeOf(((ParameterizedType) type).getRawType());
            } else {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TypeConverter<?> that))
                return false;
            return Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(type);
        }
    }
}
