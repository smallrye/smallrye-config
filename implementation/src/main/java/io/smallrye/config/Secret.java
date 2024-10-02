package io.smallrye.config;

import org.eclipse.microprofile.config.spi.Converter;

public interface Secret<T> {
    T get();

    class SecretConverter<T> implements Converter<Secret<T>> {
        private static final long serialVersionUID = -4624156385855243648L;
        private final Converter<T> delegate;

        public SecretConverter(final Converter<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Secret<T> convert(final String value) throws IllegalArgumentException, NullPointerException {
            return new Secret<T>() {
                @Override
                public T get() {
                    return delegate.convert(value);
                }
            };
        }
    }
}
