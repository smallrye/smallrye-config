package io.smallrye.config.examples.configmap;

import java.net.InetSocketAddress;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.sun.net.httpserver.HttpServer;

import io.smallrye.config.Config;

public class ConfigMapApp {
    public static void main(String[] args) throws Exception {
        Config config = Config.getOrCreate();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/configMap", exchange -> {
            boolean responseSent = false;

            final Iterable<ConfigSource> configSources = config.getConfigSources();
            for (ConfigSource configSource : configSources) {
                if (configSource.getName().startsWith("FileSystemConfig")) {
                    final Map<String, String> properties = configSource.getProperties();
                    final byte[] bytes = properties.toString().getBytes();
                    exchange.sendResponseHeaders(200, properties.toString().length());
                    exchange.getResponseBody().write(bytes);
                    exchange.getResponseBody().flush();
                    exchange.getResponseBody().close();
                    responseSent = true;
                    break;
                }
            }

            if (!responseSent) {
                exchange.sendResponseHeaders(404, 0);
                exchange.getResponseBody().write(new byte[0]);
                exchange.getResponseBody().flush();
                exchange.getResponseBody().close();
            }
        });

        server.start();
    }
}
