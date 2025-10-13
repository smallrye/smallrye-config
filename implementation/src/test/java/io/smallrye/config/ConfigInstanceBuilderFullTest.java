package io.smallrye.config;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigInstanceBuilderFullTest.HttpConfig.AuthRuntimeConfig.FormAuthConfig.CookieSameSite;
import io.smallrye.config.ConfigInstanceBuilderFullTest.HttpConfig.AuthRuntimeConfig.InclusiveMode;
import io.smallrye.config.ConfigInstanceBuilderFullTest.HttpConfig.CharsetConverter;
import io.smallrye.config.ConfigInstanceBuilderFullTest.HttpConfig.DurationConverter;
import io.smallrye.config.ConfigInstanceBuilderFullTest.HttpConfig.MemorySize;
import io.smallrye.config.ConfigInstanceBuilderFullTest.HttpConfig.MemorySizeConverter;
import io.smallrye.config.ConfigInstanceBuilderFullTest.HttpConfig.ProxyConfig.ForwardedPrecedence;

class ConfigInstanceBuilderFullTest {
    @BeforeAll
    static void beforeAll() {
        ConfigInstanceBuilder.registerConverter(Duration.class, new DurationConverter());
        ConfigInstanceBuilder.registerConverter(MemorySize.class, new MemorySizeConverter());
        ConfigInstanceBuilder.registerConverter(Charset.class, new CharsetConverter());
    }

    @Test
    void emptyWithDefaults() {
        HttpConfig httpConfig = ConfigInstanceBuilder.forInterface(HttpConfig.class)
                .with(HttpConfig::host, "localhost")
                .build();

        assertEquals(8080, httpConfig.port());
        assertEquals(8081, httpConfig.testPort());
        assertEquals("localhost", httpConfig.host());
        assertFalse(httpConfig.testHost().isPresent());
        assertTrue(httpConfig.hostEnabled());
        assertEquals(8443, httpConfig.sslPort());
        assertEquals(8444, httpConfig.testSslPort());
        assertFalse(httpConfig.testSslEnabled().isPresent());
        assertFalse(httpConfig.insecureRequests().isPresent());
        assertTrue(httpConfig.http2());
        assertTrue(httpConfig.http2PushEnabled());
        assertFalse(httpConfig.tlsConfigurationName().isPresent());
        assertFalse(httpConfig.handle100ContinueAutomatically());
        assertFalse(httpConfig.ioThreads().isPresent());
        assertEquals(Duration.of(30, MINUTES), httpConfig.idleTimeout());
        assertEquals(Duration.of(60, SECONDS), httpConfig.readTimeout());
        assertFalse(httpConfig.encryptionKey().isPresent());
        assertFalse(httpConfig.soReusePort());
        assertFalse(httpConfig.tcpQuickAck());
        assertFalse(httpConfig.tcpCork());
        assertFalse(httpConfig.tcpFastOpen());
        assertEquals(-1, httpConfig.acceptBacklog());
        assertFalse(httpConfig.initialWindowSize().isPresent());
        assertEquals("/var/run/io.quarkus.app.socket", httpConfig.domainSocket());
        assertFalse(httpConfig.domainSocketEnabled());
        assertFalse(httpConfig.recordRequestStartTime());
        assertFalse(httpConfig.unhandledErrorContentTypeDefault().isPresent());

        assertNotNull(httpConfig.auth());
        assertNotNull(httpConfig.auth().permissions());
        assertTrue(httpConfig.auth().permissions().isEmpty());
        assertNotNull(httpConfig.auth().rolePolicy());
        assertTrue(httpConfig.auth().rolePolicy().isEmpty());
        assertNotNull(httpConfig.auth().rolesMapping());
        assertTrue(httpConfig.auth().rolesMapping().isEmpty());
        assertEquals("CN", httpConfig.auth().certificateRoleAttribute());
        assertFalse(httpConfig.auth().certificateRoleProperties().isPresent());
        assertFalse(httpConfig.auth().realm().isPresent());
        assertNotNull(httpConfig.auth().form());
        assertTrue(httpConfig.auth().form().loginPage().isPresent());
        assertEquals("/login.html", httpConfig.auth().form().loginPage().get());
        assertEquals("j_username", httpConfig.auth().form().usernameParameter());
        assertEquals("j_password", httpConfig.auth().form().passwordParameter());
        assertTrue(httpConfig.auth().form().errorPage().isPresent());
        assertEquals("/error.html", httpConfig.auth().form().errorPage().get());
        assertTrue(httpConfig.auth().form().landingPage().isPresent());
        assertEquals("/index.html", httpConfig.auth().form().landingPage().get());
        assertEquals("quarkus-redirect-location", httpConfig.auth().form().locationCookie());
        assertEquals(Duration.of(30, MINUTES), httpConfig.auth().form().timeout());
        assertEquals(Duration.of(1, MINUTES), httpConfig.auth().form().newCookieInterval());
        assertEquals("quarkus-credential", httpConfig.auth().form().cookieName());
        assertTrue(httpConfig.auth().form().cookiePath().isPresent());
        assertEquals("/", httpConfig.auth().form().cookiePath().get());
        assertFalse(httpConfig.auth().form().cookieDomain().isPresent());
        assertFalse(httpConfig.auth().form().httpOnlyCookie());
        assertEquals(CookieSameSite.STRICT, httpConfig.auth().form().cookieSameSite());
        assertFalse(httpConfig.auth().form().cookieMaxAge().isPresent());
        assertEquals("/j_security_check", httpConfig.auth().form().postLocation());
        assertFalse(httpConfig.auth().inclusive());
        assertEquals(InclusiveMode.STRICT, httpConfig.auth().inclusiveMode());

        assertNotNull(httpConfig.cors());
        assertFalse(httpConfig.cors().enabled());
        assertFalse(httpConfig.cors().origins().isPresent());
        assertFalse(httpConfig.cors().methods().isPresent());
        assertFalse(httpConfig.cors().headers().isPresent());
        assertFalse(httpConfig.cors().exposedHeaders().isPresent());
        assertFalse(httpConfig.cors().accessControlMaxAge().isPresent());
        assertFalse(httpConfig.cors().accessControlAllowCredentials().isPresent());

        assertNotNull(httpConfig.ssl());
        assertFalse(httpConfig.ssl().certificate().credentialsProvider().isPresent());
        assertFalse(httpConfig.ssl().certificate().credentialsProviderName().isPresent());
        assertFalse(httpConfig.ssl().certificate().files().isPresent());
        assertFalse(httpConfig.ssl().certificate().keyFiles().isPresent());
        assertFalse(httpConfig.ssl().certificate().keyStoreFile().isPresent());
        assertFalse(httpConfig.ssl().certificate().keyStoreFileType().isPresent());
        assertFalse(httpConfig.ssl().certificate().keyStoreProvider().isPresent());
        assertFalse(httpConfig.ssl().certificate().keyStorePassword().isPresent());
        assertFalse(httpConfig.ssl().certificate().keyStorePasswordKey().isPresent());
        assertFalse(httpConfig.ssl().certificate().keyStoreAlias().isPresent());
        assertFalse(httpConfig.ssl().certificate().keyStoreAliasPassword().isPresent());
        assertFalse(httpConfig.ssl().certificate().keyStoreAliasPasswordKey().isPresent());
        assertFalse(httpConfig.ssl().certificate().trustStoreFile().isPresent());
        assertFalse(httpConfig.ssl().certificate().trustStoreFiles().isPresent());
        assertFalse(httpConfig.ssl().certificate().trustStoreFileType().isPresent());
        assertFalse(httpConfig.ssl().certificate().trustStoreProvider().isPresent());
        assertFalse(httpConfig.ssl().certificate().trustStorePassword().isPresent());
        assertFalse(httpConfig.ssl().certificate().trustStorePasswordKey().isPresent());
        assertFalse(httpConfig.ssl().certificate().trustStoreCertAlias().isPresent());
        assertFalse(httpConfig.ssl().certificate().reloadPeriod().isPresent());
        assertFalse(httpConfig.ssl().cipherSuites().isPresent());
        assertTrue(httpConfig.ssl().protocols().contains("TLSv1.2"));
        assertTrue(httpConfig.ssl().protocols().contains("TLSv1.3"));
        assertFalse(httpConfig.ssl().sni());

        assertNotNull(httpConfig.staticResources());
        assertEquals("index.html", httpConfig.staticResources().indexPage());
        assertTrue(httpConfig.staticResources().includeHidden());
        assertTrue(httpConfig.staticResources().enableRangeSupport());
        assertTrue(httpConfig.staticResources().cachingEnabled());
        assertEquals(Duration.of(30, SECONDS), httpConfig.staticResources().cacheEntryTimeout());
        assertEquals(Duration.of(24, HOURS), httpConfig.staticResources().maxAge());
        assertEquals(10000, httpConfig.staticResources().maxCacheSize());
        assertEquals(StandardCharsets.UTF_8, httpConfig.staticResources().contentEncoding());

        assertNotNull(httpConfig.limits());
        assertEquals(new MemorySizeConverter().convert("20K"), httpConfig.limits().maxHeaderSize());
        assertTrue(httpConfig.limits().maxBodySize().isPresent());
        assertEquals(new MemorySizeConverter().convert("10240K"), httpConfig.limits().maxBodySize().get());
        assertEquals(new MemorySizeConverter().convert("8192"), httpConfig.limits().maxChunkSize());
        assertEquals(4096, httpConfig.limits().maxInitialLineLength());
        assertEquals(new MemorySizeConverter().convert("2048"), httpConfig.limits().maxFormAttributeSize());
        assertEquals(256, httpConfig.limits().maxFormFields());
        assertEquals(new MemorySizeConverter().convert("1K"), httpConfig.limits().maxFormBufferedBytes());
        assertEquals(1000, httpConfig.limits().maxParameters());
        assertFalse(httpConfig.limits().maxConnections().isPresent());
        assertFalse(httpConfig.limits().headerTableSize().isPresent());
        assertFalse(httpConfig.limits().maxConcurrentStreams().isPresent());
        assertFalse(httpConfig.limits().maxFrameSize().isPresent());
        assertFalse(httpConfig.limits().maxHeaderListSize().isPresent());
        assertFalse(httpConfig.limits().rstFloodMaxRstFramePerWindow().isPresent());
        assertFalse(httpConfig.limits().rstFloodWindowDuration().isPresent());

        assertNotNull(httpConfig.body());
        assertTrue(httpConfig.body().handleFileUploads());
        // TODO - how to handle expressions?
        assertEquals("${java.io.tmpdir}/uploads", httpConfig.body().uploadsDirectory());
        assertTrue(httpConfig.body().mergeFormAttributes());
        assertTrue(httpConfig.body().deleteUploadedFilesOnEnd());
        assertFalse(httpConfig.body().preallocateBodyBuffer());
        assertNotNull(httpConfig.body().multipart());
        assertFalse(httpConfig.body().multipart().fileContentTypes().isPresent());

        assertNotNull(httpConfig.accessLog());
        assertFalse(httpConfig.accessLog().enabled());
        assertFalse(httpConfig.accessLog().excludePattern().isPresent());
        assertEquals("common", httpConfig.accessLog().pattern());
        assertFalse(httpConfig.accessLog().logToFile());
        assertEquals("quarkus", httpConfig.accessLog().baseFileName());
        assertFalse(httpConfig.accessLog().logDirectory().isPresent());
        assertEquals(".log", httpConfig.accessLog().logSuffix());
        assertEquals("io.quarkus.http.access-log", httpConfig.accessLog().category());
        assertTrue(httpConfig.accessLog().rotate());
        assertFalse(httpConfig.accessLog().consolidateReroutedRequests());

        assertNotNull(httpConfig.trafficShaping());
        assertFalse(httpConfig.trafficShaping().enabled());
        assertFalse(httpConfig.trafficShaping().inboundGlobalBandwidth().isPresent());
        assertFalse(httpConfig.trafficShaping().outboundGlobalBandwidth().isPresent());
        assertFalse(httpConfig.trafficShaping().maxDelay().isPresent());
        assertFalse(httpConfig.trafficShaping().checkInterval().isPresent());
        assertFalse(httpConfig.trafficShaping().peakOutboundGlobalBandwidth().isPresent());

        assertNotNull(httpConfig.sameSiteCookie());
        assertTrue(httpConfig.sameSiteCookie().isEmpty());
        assertNotNull(httpConfig.header());
        assertTrue(httpConfig.header().isEmpty());
        assertNotNull(httpConfig.filter());
        assertTrue(httpConfig.filter().isEmpty());

        assertNotNull(httpConfig.proxy());
        assertFalse(httpConfig.proxy().useProxyProtocol());
        assertFalse(httpConfig.proxy().proxyAddressForwarding());
        assertFalse(httpConfig.proxy().allowForwarded());
        assertFalse(httpConfig.proxy().allowXForwarded().isPresent());
        assertTrue(httpConfig.proxy().strictForwardedControl());
        assertEquals(ForwardedPrecedence.FORWARDED, httpConfig.proxy().forwardedPrecedence());
        assertFalse(httpConfig.proxy().enableForwardedHost());
        assertEquals("X-Forwarded-Host", httpConfig.proxy().forwardedHostHeader());
        assertFalse(httpConfig.proxy().enableForwardedPrefix());
        assertEquals("X-Forwarded-Prefix", httpConfig.proxy().forwardedPrefixHeader());
        assertFalse(httpConfig.proxy().enableTrustedProxyHeader());

        assertNotNull(httpConfig.websocketServer());
        assertFalse(httpConfig.websocketServer().maxFrameSize().isPresent());
        assertFalse(httpConfig.websocketServer().maxMessageSize().isPresent());
    }

    @ConfigMapping
    interface HttpConfig {
        AuthRuntimeConfig auth();

        @WithDefault("8080")
        int port();

        @WithDefault("8081")
        int testPort();

        String host();

        Optional<String> testHost();

        @WithDefault("true")
        boolean hostEnabled();

        @WithDefault("8443")
        int sslPort();

        @WithDefault("8444")
        int testSslPort();

        Optional<Boolean> testSslEnabled();

        Optional<InsecureRequests> insecureRequests();

        @WithDefault("true")
        boolean http2();

        @WithDefault("true")
        boolean http2PushEnabled();

        CORSConfig cors();

        ServerSslConfig ssl();

        Optional<String> tlsConfigurationName();

        StaticResourcesConfig staticResources();

        @WithName("handle-100-continue-automatically")
        @WithDefault("false")
        boolean handle100ContinueAutomatically();

        OptionalInt ioThreads();

        ServerLimitsConfig limits();

        @WithDefault("30M")
        Duration idleTimeout();

        @WithDefault("60s")
        Duration readTimeout();

        BodyConfig body();

        @WithName("auth.session.encryption-key")
        Optional<String> encryptionKey();

        @WithDefault("false")
        boolean soReusePort();

        @WithDefault("false")
        boolean tcpQuickAck();

        @WithDefault("false")
        boolean tcpCork();

        @WithDefault("false")
        boolean tcpFastOpen();

        @WithDefault("-1")
        int acceptBacklog();

        OptionalInt initialWindowSize();

        @WithDefault("/var/run/io.quarkus.app.socket")
        String domainSocket();

        @WithDefault("false")
        boolean domainSocketEnabled();

        @WithDefault("false")
        boolean recordRequestStartTime();

        AccessLogConfig accessLog();

        TrafficShapingConfig trafficShaping();

        Map<String, SameSiteCookieConfig> sameSiteCookie();

        Optional<PayloadHint> unhandledErrorContentTypeDefault();

        Map<String, HeaderConfig> header();

        Map<String, FilterConfig> filter();

        ProxyConfig proxy();

        WebsocketServerConfig websocketServer();

        interface AuthRuntimeConfig {
            @WithName("permission")
            Map<String, PolicyMappingConfig> permissions();

            @WithName("policy")
            Map<String, PolicyConfig> rolePolicy();

            Map<String, List<String>> rolesMapping();

            @WithDefault("CN")
            String certificateRoleAttribute();

            Optional<Path> certificateRoleProperties();

            Optional<String> realm();

            FormAuthConfig form();

            @WithDefault("false")
            boolean inclusive();

            @WithDefault("strict")
            InclusiveMode inclusiveMode();

            interface PolicyMappingConfig {
                Optional<Boolean> enabled();

                String policy();

                Optional<List<String>> methods();

                Optional<List<String>> paths();

                Optional<String> authMechanism();

                @WithDefault("false")
                boolean shared();

                @WithDefault("ALL")
                AppliesTo appliesTo();

                enum AppliesTo {
                    ALL,
                    JAXRS
                }
            }

            interface PolicyConfig {
                @WithDefault("**")
                List<String> rolesAllowed();

                Map<String, List<String>> roles();

                Map<String, List<String>> permissions();

                @WithDefault("io.quarkus.security.StringPermission")
                String permissionClass();
            }

            interface FormAuthConfig {
                enum CookieSameSite {
                    STRICT,
                    LAX,
                    NONE
                }

                @WithDefault("/login.html")
                Optional<String> loginPage();

                @WithDefault("j_username")
                String usernameParameter();

                @WithDefault("j_password")
                String passwordParameter();

                @WithDefault("/error.html")
                Optional<String> errorPage();

                @WithDefault("/index.html")
                Optional<String> landingPage();

                @WithDefault("quarkus-redirect-location")
                String locationCookie();

                @WithDefault("PT30M")
                Duration timeout();

                @WithDefault("PT1M")
                Duration newCookieInterval();

                @WithDefault("quarkus-credential")
                String cookieName();

                @WithDefault("/")
                Optional<String> cookiePath();

                Optional<String> cookieDomain();

                @WithDefault("false")
                boolean httpOnlyCookie();

                @WithDefault("strict")
                CookieSameSite cookieSameSite();

                Optional<Duration> cookieMaxAge();

                @WithDefault("/j_security_check")
                String postLocation();
            }

            enum InclusiveMode {
                LAX,
                STRICT
            }
        }

        interface CORSConfig {
            @WithDefault("false")
            boolean enabled();

            Optional<List<String>> origins();

            Optional<List<String>> methods();

            Optional<List<String>> headers();

            Optional<List<String>> exposedHeaders();

            Optional<Duration> accessControlMaxAge();

            Optional<Boolean> accessControlAllowCredentials();
        }

        interface ServerSslConfig {
            CertificateConfig certificate();

            Optional<List<String>> cipherSuites();

            @WithDefault("TLSv1.3,TLSv1.2")
            Set<String> protocols();

            @WithDefault("false")
            boolean sni();

            interface CertificateConfig {
                Optional<String> credentialsProvider();

                Optional<String> credentialsProviderName();

                Optional<List<Path>> files();

                Optional<List<Path>> keyFiles();

                Optional<Path> keyStoreFile();

                Optional<String> keyStoreFileType();

                Optional<String> keyStoreProvider();

                Optional<String> keyStorePassword();

                Optional<String> keyStorePasswordKey();

                Optional<String> keyStoreAlias();

                Optional<String> keyStoreAliasPassword();

                Optional<String> keyStoreAliasPasswordKey();

                Optional<Path> trustStoreFile();

                Optional<List<Path>> trustStoreFiles();

                Optional<String> trustStoreFileType();

                Optional<String> trustStoreProvider();

                Optional<String> trustStorePassword();

                Optional<String> trustStorePasswordKey();

                Optional<String> trustStoreCertAlias();

                Optional<Duration> reloadPeriod();
            }
        }

        interface StaticResourcesConfig {
            @WithDefault("index.html")
            String indexPage();

            @WithDefault("true")
            boolean includeHidden();

            @WithDefault("true")
            boolean enableRangeSupport();

            @WithDefault("true")
            boolean cachingEnabled();

            @WithDefault("30S")
            Duration cacheEntryTimeout();

            @WithDefault("24H")
            Duration maxAge();

            @WithDefault("10000")
            int maxCacheSize();

            @WithDefault("UTF-8")
            Charset contentEncoding();
        }

        interface ServerLimitsConfig {
            @WithDefault("20K")
            MemorySize maxHeaderSize();

            @WithDefault("10240K")
            Optional<MemorySize> maxBodySize();

            @WithDefault("8192")
            MemorySize maxChunkSize();

            @WithDefault("4096")
            int maxInitialLineLength();

            @WithDefault("2048")
            MemorySize maxFormAttributeSize();

            @WithDefault("256")
            int maxFormFields();

            @WithDefault("1K")
            MemorySize maxFormBufferedBytes();

            @WithDefault("1000")
            int maxParameters();

            OptionalInt maxConnections();

            OptionalLong headerTableSize();

            OptionalLong maxConcurrentStreams();

            OptionalInt maxFrameSize();

            OptionalLong maxHeaderListSize();

            OptionalInt rstFloodMaxRstFramePerWindow();

            Optional<Duration> rstFloodWindowDuration();
        }

        interface BodyConfig {
            @WithDefault("true")
            boolean handleFileUploads();

            @WithDefault("${java.io.tmpdir}/uploads")
            String uploadsDirectory();

            @WithDefault("true")
            boolean mergeFormAttributes();

            @WithDefault("true")
            boolean deleteUploadedFilesOnEnd();

            @WithDefault("false")
            boolean preallocateBodyBuffer();

            MultiPartConfig multipart();

            interface MultiPartConfig {
                Optional<List<String>> fileContentTypes();
            }
        }

        interface AccessLogConfig {
            @WithDefault("false")
            boolean enabled();

            Optional<String> excludePattern();

            @WithDefault("common")
            String pattern();

            @WithDefault("false")
            boolean logToFile();

            @WithDefault("quarkus")
            String baseFileName();

            Optional<String> logDirectory();

            @WithDefault(".log")
            String logSuffix();

            @WithDefault("io.quarkus.http.access-log")
            String category();

            @WithDefault("true")
            boolean rotate();

            @WithDefault("false")
            boolean consolidateReroutedRequests();
        }

        interface TrafficShapingConfig {
            @WithDefault("false")
            boolean enabled();

            Optional<MemorySize> inboundGlobalBandwidth();

            Optional<MemorySize> outboundGlobalBandwidth();

            Optional<Duration> maxDelay();

            Optional<Duration> checkInterval();

            Optional<MemorySize> peakOutboundGlobalBandwidth();
        }

        interface SameSiteCookieConfig {
            @WithDefault("false")
            boolean caseSensitive();

            CookieSameSite value();

            @WithDefault("true")
            boolean enableClientChecker();

            @WithDefault("true")
            boolean addSecureForNone();
        }

        interface HeaderConfig {
            @WithDefault("/*")
            String path();

            String value();

            Optional<List<String>> methods();
        }

        interface FilterConfig {
            String matches();

            Map<String, String> header();

            Optional<List<String>> methods();

            OptionalInt order();
        }

        interface ProxyConfig {
            @WithDefault("false")
            boolean useProxyProtocol();

            @WithDefault("false")
            boolean proxyAddressForwarding();

            @WithDefault("false")
            boolean allowForwarded();

            Optional<Boolean> allowXForwarded();

            @WithDefault("true")
            boolean strictForwardedControl();

            enum ForwardedPrecedence {
                FORWARDED,
                X_FORWARDED
            }

            @WithDefault("forwarded")
            ForwardedPrecedence forwardedPrecedence();

            @WithDefault("false")
            boolean enableForwardedHost();

            @WithDefault("X-Forwarded-Host")
            String forwardedHostHeader();

            @WithDefault("false")
            boolean enableForwardedPrefix();

            @WithDefault("X-Forwarded-Prefix")
            String forwardedPrefixHeader();

            @WithDefault("false")
            boolean enableTrustedProxyHeader();
        }

        interface WebsocketServerConfig {
            Optional<Integer> maxFrameSize();

            Optional<Integer> maxMessageSize();
        }

        enum InsecureRequests {
            ENABLED,
            REDIRECT,
            DISABLED;
        }

        enum PayloadHint {
            JSON,
            HTML,
            TEXT
        }

        enum CookieSameSite {
            NONE("None"),
            STRICT("Strict"),
            LAX("Lax");

            private final String label;

            private CookieSameSite(String label) {
                this.label = label;
            }

            public String toString() {
                return this.label;
            }
        }

        final class MemorySize {
            private final BigInteger value;

            public MemorySize(BigInteger value) {
                this.value = value;
            }

            public long asLongValue() {
                return value.longValueExact();
            }

            public BigInteger asBigInteger() {
                return value;
            }

            @Override
            public boolean equals(Object object) {
                if (this == object)
                    return true;
                if (object == null || getClass() != object.getClass())
                    return false;
                MemorySize that = (MemorySize) object;
                return Objects.equals(value, that.value);
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(value);
            }
        }

        class DurationConverter implements Converter<Duration>, Serializable {
            @Serial
            private static final long serialVersionUID = 7499347081928776532L;
            private static final String PERIOD = "P";
            private static final String PERIOD_OF_TIME = "PT";
            public static final Pattern DIGITS = Pattern.compile("^[-+]?\\d+$");
            private static final Pattern DIGITS_AND_UNIT = Pattern.compile("^(?:[-+]?\\d+(?:\\.\\d+)?(?i)[hms])+$");
            private static final Pattern DAYS = Pattern.compile("^[-+]?\\d+(?i)d$");
            private static final Pattern MILLIS = Pattern.compile("^[-+]?\\d+(?i)ms$");

            public DurationConverter() {
            }

            @Override
            public Duration convert(String value) {
                return parseDuration(value);
            }

            public static Duration parseDuration(String value) {
                value = value.trim();
                if (value.isEmpty()) {
                    return null;
                }
                if (DIGITS.asPredicate().test(value)) {
                    return Duration.ofSeconds(Long.parseLong(value));
                } else if (MILLIS.asPredicate().test(value)) {
                    return Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2)));
                }

                try {
                    if (DIGITS_AND_UNIT.asPredicate().test(value)) {
                        return Duration.parse(PERIOD_OF_TIME + value);
                    } else if (DAYS.asPredicate().test(value)) {
                        return Duration.parse(PERIOD + value);
                    }

                    return Duration.parse(value);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }

        class MemorySizeConverter implements Converter<MemorySize>, Serializable {
            @Serial
            private static final long serialVersionUID = -1988485929047973068L;
            private static final Pattern MEMORY_SIZE_PATTERN = Pattern.compile("^(\\d+)([BbKkMmGgTtPpEeZzYy]?)$");
            static final BigInteger KILO_BYTES = BigInteger.valueOf(1024);
            private static final Map<String, BigInteger> MEMORY_SIZE_MULTIPLIERS;

            static {
                MEMORY_SIZE_MULTIPLIERS = new HashMap<>();
                MEMORY_SIZE_MULTIPLIERS.put("K", KILO_BYTES);
                MEMORY_SIZE_MULTIPLIERS.put("M", KILO_BYTES.pow(2));
                MEMORY_SIZE_MULTIPLIERS.put("G", KILO_BYTES.pow(3));
                MEMORY_SIZE_MULTIPLIERS.put("T", KILO_BYTES.pow(4));
                MEMORY_SIZE_MULTIPLIERS.put("P", KILO_BYTES.pow(5));
                MEMORY_SIZE_MULTIPLIERS.put("E", KILO_BYTES.pow(6));
                MEMORY_SIZE_MULTIPLIERS.put("Z", KILO_BYTES.pow(7));
                MEMORY_SIZE_MULTIPLIERS.put("Y", KILO_BYTES.pow(8));
            }

            public MemorySize convert(String value) {
                value = value.trim();
                if (value.isEmpty()) {
                    return null;
                }
                Matcher matcher = MEMORY_SIZE_PATTERN.matcher(value);
                if (matcher.find()) {
                    BigInteger number = new BigInteger(matcher.group(1));
                    String scale = matcher.group(2).toUpperCase();
                    BigInteger multiplier = MEMORY_SIZE_MULTIPLIERS.get(scale);
                    return multiplier == null ? new MemorySize(number) : new MemorySize(number.multiply(multiplier));
                }

                throw new IllegalArgumentException(
                        String.format("value %s not in correct format (regular expression): [0-9]+[BbKkMmGgTtPpEeZzYy]?",
                                value));
            }
        }

        class CharsetConverter implements Converter<Charset>, Serializable {
            @Serial
            private static final long serialVersionUID = 2320905063828247874L;

            @Override
            public Charset convert(String value) {
                if (value == null) {
                    return null;
                }

                String trimmedCharset = value.trim();

                if (trimmedCharset.isEmpty()) {
                    return null;
                }

                try {
                    return Charset.forName(trimmedCharset);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Unable to create Charset from: '" + trimmedCharset + "'", e);
                }
            }
        }
    }
}
