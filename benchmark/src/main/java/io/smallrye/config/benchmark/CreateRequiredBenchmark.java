package io.smallrye.config.benchmark;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import io.smallrye.config.PropertyName;

/**
 * Benchmarks different implementations of the createRequired() logic
 * from ConfigMappingContext.ObjectCreator.
 *
 * Each benchmark measures the full startup cost: building data structures
 * + running all 39 createRequired queries. Property names and query paths
 * are captured from a real Quarkus simple-rest app startup (598 properties).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
@State(Scope.Thread)
public class CreateRequiredBenchmark {

    private static final QueryPath[] QUERY_PATHS = {
            qp("quarkus.http.auth", new String[] { "permission.*.paths", "permission.*.policy", "roles-mapping.*",
                    "basic", "form", "proactive", "certificate-role-attribute", "propagate-security-identity",
                    "inclusive", "inclusive-mode", "realm", "mtls.priority", "basic.priority", "form.priority",
                    "permission.*.enabled", "permission.*.methods", "permission.*.auth-mechanism",
                    "permission.*.shared", "permission.*.applies-to", "policy.*.roles-allowed",
                    "policy.*.permission-class", "policy.*.roles.*", "policy.*.permissions.*",
                    "form.login-page", "form.error-page", "form.landing-page", "form.post-location",
                    "form.location-cookie", "form.timeout", "form.new-cookie-interval",
                    "form.cookie-name", "form.cookie-path", "form.cookie-domain",
                    "form.cookie-max-age", "form.http-only-cookie", "form.cookie-same-site",
                    "form.username-parameter", "form.password-parameter",
                    "certificate-role-properties" }),
            qp("quarkus.http.auth.inclusive", new String[] { "permission.*.paths", "permission.*.policy" }),
            qp("quarkus.http.auth.form", new String[] { "login-page", "error-page", "landing-page", "post-location",
                    "location-cookie", "timeout", "new-cookie-interval", "cookie-name", "cookie-path",
                    "cookie-domain", "cookie-max-age", "http-only-cookie", "cookie-same-site",
                    "username-parameter", "password-parameter", "enabled", "priority" }),
            qp("quarkus.http.auth.inclusive-mode", new String[] { "enabled" }),
            qp("quarkus.http.auth.certificate-role-attribute", new String[] { "enabled" }),
            qp("quarkus.http.auth.propagate-security-identity", new String[] { "enabled" }),
            qp("quarkus.http.auth.proactive", new String[] { "enabled" }),
            qp("quarkus.http.auth.basic", new String[] { "enabled", "priority" }),
            qp("quarkus.http.cors", new String[] { "origins", "methods", "headers", "exposed-headers",
                    "access-control-max-age", "access-control-allow-credentials", "enabled",
                    "return-exact-origins", "vary-origin" }),
            qp("quarkus.http.cors.return-exact-origins", new String[] { "enabled" }),
            qp("quarkus.http.cors.enabled", new String[] { "enabled" }),
            qp("quarkus.http.cors.vary-origin", new String[] { "enabled" }),
            qp("quarkus.log.category.\"io.vertx.core.http.impl.http1.Http1ServerRequest\"",
                    new String[] { "level", "handlers", "use-parent-handlers", "min-level" }),
            qp("quarkus.log.category.\"io.vertx.core.impl.ContextImpl\"",
                    new String[] { "level", "handlers", "use-parent-handlers", "min-level" }),
            qp("quarkus.log.filter.\"org.jboss.threads\"",
                    new String[] { "if-starts-with", "target-level" }),
            qp("quarkus.log.filter.\"io.netty.resolver.HostsFileParser\"",
                    new String[] { "if-starts-with", "target-level" }),
            qp("quarkus.log.filter.\"io.netty.resolver.dns.DnsServerAddressStreamProviders\"",
                    new String[] { "if-starts-with", "target-level" }),
            qp("quarkus.log.filter.\"io.vertx.core.impl.ContextImpl\"",
                    new String[] { "if-starts-with", "target-level" }),
            qp("quarkus.log.filter.\"io.netty.util.internal.PlatformDependent0\"",
                    new String[] { "if-starts-with", "target-level" }),
            qp("quarkus.tls",
                    new String[] { "key-store", "trust-store", "cipher-suites", "protocols", "handshake-timeout",
                            "alpn", "trust-all", "pqc-enforcement-policy", "hostname-verification-algorithm",
                            "reload-period", "certificate-revocation-list", "key-exchange-groups", "ssl-engine" }),
            qp("quarkus.tls.key-store",
                    new String[] { "pem", "jks", "p12", "other", "sni", "credentials-provider" }),
            qp("quarkus.tls.key-store.jks", new String[] { "path", "password", "alias", "alias-password", "provider" }),
            qp("quarkus.tls.key-store.other", new String[] { "path", "password", "alias", "alias-password", "provider", "type",
                    "params.*" }),
            qp("quarkus.tls.key-store.p12", new String[] { "path", "password", "alias", "alias-password", "provider" }),
            qp("quarkus.tls.key-store.pem", new String[] { "order", "*.cert", "*.key", "*.password" }),
            qp("quarkus.tls.trust-store",
                    new String[] { "pem", "jks", "p12", "other", "credentials-provider", "certificate-expiration-policy" }),
            qp("quarkus.tls.trust-store.jks", new String[] { "path", "password", "alias", "provider" }),
            qp("quarkus.tls.trust-store.other", new String[] { "path", "password", "alias", "provider", "type", "params.*" }),
            qp("quarkus.tls.trust-store.p12", new String[] { "path", "password", "alias", "provider" }),
            qp("quarkus.tls.trust-store.pem", new String[] { "certs", "cert-dirs" }),
            qp("quarkus.tls.*.key-store",
                    new String[] { "pem", "jks", "p12", "other", "sni", "credentials-provider" }),
            qp("quarkus.tls.*.trust-store",
                    new String[] { "pem", "jks", "p12", "other", "credentials-provider", "certificate-expiration-policy" }),
            qp("quarkus.tls.alpn", new String[] { "enabled" }),
            qp("quarkus.tls.trust-store", new String[] { "pem", "jks", "p12", "other" }),
            qp("quarkus.tls.pqc-enforcement-policy", new String[] { "enabled" }),
            qp("quarkus.tls.key-store", new String[] { "pem", "jks", "p12", "other" }),
            qp("quarkus.tls.trust-all", new String[] { "enabled" }),
            qp("quarkus.tls.protocols", new String[] { "enabled" }),
            qp("quarkus.tls.handshake-timeout", new String[] { "enabled" }),
    };

    private List<String> propertyNames;

    @Setup
    public void setup() throws IOException {
        propertyNames = loadPropertyNames();
    }

    // -----------------------------------------------------------------------
    // Approach 1: Current — linear scan, no data structure (baseline)
    // -----------------------------------------------------------------------
    @Benchmark
    public void baseline_linearScan(Blackhole bh) {
        for (QueryPath qp : QUERY_PATHS) {
            bh.consume(createRequired_linear(propertyNames, qp.path, qp.mappedProperties));
        }
    }

    // -----------------------------------------------------------------------
    // Approach 2: NavigableSet — build TreeSet + query via tailSet
    // -----------------------------------------------------------------------
    @Benchmark
    public void navigableSet(Blackhole bh) {
        NavigableSet<String> sorted = new TreeSet<>(propertyNames);
        for (QueryPath qp : QUERY_PATHS) {
            bh.consume(createRequired_navigableSet(sorted, qp.path, qp.mappedProperties));
        }
    }

    // -----------------------------------------------------------------------
    // Approach 3: Prefix HashSet — build prefix index + linear gather on hit
    // -----------------------------------------------------------------------
    @Benchmark
    public void prefixHashSet(Blackhole bh) {
        Set<String> prefixes = buildPrefixIndex(propertyNames);
        for (QueryPath qp : QUERY_PATHS) {
            bh.consume(createRequired_prefixSet(propertyNames, prefixes, qp.path, qp.mappedProperties));
        }
    }

    // -----------------------------------------------------------------------
    // Approach 4: Sorted List — build sorted ArrayList + query via binarySearch
    // -----------------------------------------------------------------------
    @Benchmark
    public void sortedList(Blackhole bh) {
        List<String> sorted = new ArrayList<>(propertyNames);
        Collections.sort(sorted);
        for (QueryPath qp : QUERY_PATHS) {
            bh.consume(createRequired_sortedList(sorted, qp.path, qp.mappedProperties));
        }
    }

    // -----------------------------------------------------------------------
    // Approach 5: Prefix HashSet + NavigableSet
    // -----------------------------------------------------------------------
    @Benchmark
    public void prefixHashSet_plus_navigableSet(Blackhole bh) {
        Set<String> prefixes = buildPrefixIndex(propertyNames);
        NavigableSet<String> sorted = new TreeSet<>(propertyNames);
        for (QueryPath qp : QUERY_PATHS) {
            bh.consume(createRequired_prefixSetNav(sorted, prefixes, qp.path, qp.mappedProperties));
        }
    }

    // -----------------------------------------------------------------------
    // Implementations
    // -----------------------------------------------------------------------

    private static boolean createRequired_linear(List<String> propertyNames, String path, String[] mappedProperties) {
        List<String> candidates = new ArrayList<>();
        for (String name : propertyNames) {
            if (name.startsWith(path)) {
                String candidate = name.length() > path.length() && name.charAt(path.length()) == '.'
                        ? name.substring(path.length() + 1)
                        : name.substring(path.length());
                candidates.add(candidate);
            }
        }
        if (!candidates.isEmpty()) {
            for (String mappedProperty : mappedProperties) {
                for (String candidate : candidates) {
                    if (PropertyName.equals(candidate, mappedProperty)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean createRequired_navigableSet(NavigableSet<String> sorted, String path,
            String[] mappedProperties) {
        String ceiling = sorted.ceiling(path);
        if (ceiling == null || !ceiling.startsWith(path)) {
            return false;
        }
        List<String> candidates = new ArrayList<>();
        for (String name : sorted.tailSet(path, true)) {
            if (!name.startsWith(path)) {
                break;
            }
            String candidate = name.length() > path.length() && name.charAt(path.length()) == '.'
                    ? name.substring(path.length() + 1)
                    : name.substring(path.length());
            candidates.add(candidate);
        }
        if (!candidates.isEmpty()) {
            for (String mappedProperty : mappedProperties) {
                for (String candidate : candidates) {
                    if (PropertyName.equals(candidate, mappedProperty)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean createRequired_sortedList(List<String> sorted, String path, String[] mappedProperties) {
        int idx = Collections.binarySearch(sorted, path);
        int startIdx = idx >= 0 ? idx : -(idx + 1);
        if (startIdx >= sorted.size() || !sorted.get(startIdx).startsWith(path)) {
            return false;
        }
        List<String> candidates = new ArrayList<>();
        for (int i = startIdx; i < sorted.size(); i++) {
            String name = sorted.get(i);
            if (!name.startsWith(path)) {
                break;
            }
            String candidate = name.length() > path.length() && name.charAt(path.length()) == '.'
                    ? name.substring(path.length() + 1)
                    : name.substring(path.length());
            candidates.add(candidate);
        }
        if (!candidates.isEmpty()) {
            for (String mappedProperty : mappedProperties) {
                for (String candidate : candidates) {
                    if (PropertyName.equals(candidate, mappedProperty)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean createRequired_prefixSet(List<String> propertyNames, Set<String> prefixes, String path,
            String[] mappedProperties) {
        if (!prefixes.contains(path)) {
            return false;
        }
        List<String> candidates = new ArrayList<>();
        for (String name : propertyNames) {
            if (name.startsWith(path)) {
                String candidate = name.length() > path.length() && name.charAt(path.length()) == '.'
                        ? name.substring(path.length() + 1)
                        : name.substring(path.length());
                candidates.add(candidate);
            }
        }
        if (!candidates.isEmpty()) {
            for (String mappedProperty : mappedProperties) {
                for (String candidate : candidates) {
                    if (PropertyName.equals(candidate, mappedProperty)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean createRequired_prefixSetNav(NavigableSet<String> sorted, Set<String> prefixes, String path,
            String[] mappedProperties) {
        if (!prefixes.contains(path)) {
            return false;
        }
        List<String> candidates = new ArrayList<>();
        for (String name : sorted.tailSet(path, true)) {
            if (!name.startsWith(path)) {
                break;
            }
            String candidate = name.length() > path.length() && name.charAt(path.length()) == '.'
                    ? name.substring(path.length() + 1)
                    : name.substring(path.length());
            candidates.add(candidate);
        }
        if (!candidates.isEmpty()) {
            for (String mappedProperty : mappedProperties) {
                for (String candidate : candidates) {
                    if (PropertyName.equals(candidate, mappedProperty)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Set<String> buildPrefixIndex(List<String> propertyNames) {
        Set<String> prefixes = new HashSet<>();
        for (String name : propertyNames) {
            int idx = 0;
            while ((idx = name.indexOf('.', idx)) != -1) {
                prefixes.add(name.substring(0, idx));
                idx++;
            }
            prefixes.add(name);
        }
        return prefixes;
    }

    private static List<String> loadPropertyNames() throws IOException {
        List<String> names = new ArrayList<>(640);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                CreateRequiredBenchmark.class.getResourceAsStream("/quarkus-runtime-property-names.txt"),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    names.add(line.trim());
                }
            }
        }
        return names;
    }

    private static QueryPath qp(String path, String[] mappedProperties) {
        return new QueryPath(path, mappedProperties);
    }

    static final class QueryPath {
        final String path;
        final String[] mappedProperties;

        QueryPath(String path, String[] mappedProperties) {
            this.path = path;
            this.mappedProperties = mappedProperties;
        }
    }
}
