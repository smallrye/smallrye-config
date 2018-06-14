package io.streamzi.config.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MicroProfile Config Source that is backed by Zookeeper.
 * <p>
 * The Config Source itself needs configuration which is handled by other Config Sources.
 * Properties prefixed with io.streamzi.zk will be ignored by this Config Source.
 * <p>
 * author: Simon Woodman <swoodman@redhat.com>
 */
public class ZkConfigSource implements ConfigSource {

    private static final Logger logger = Logger.getLogger(ZkConfigSource.class.getName());

    //Apache Curator framework used to access Zookeeper
    private CuratorFramework curatorClient;

    //Root node of an application's configuration
    private String applicationId;

    //Prefix of ignored properties
    private final String ignoredPrefix = "io.streamzi.zk";

    //Property the URL of the Zookeeper instance will be read from
    private final String zkUrkKey = "io.streamzi.zk.zkUrl";

    //Property of the Application Id. This will be the root znode for an application's properties
    private final String applicationIdKey = "io.streamzi.zk.applicationId";

    public final String ZK_CONFIG_NAME = "io.streamzi.zk.ZkConfigSource";

    public ZkConfigSource() {
    }

    @Override
    public int getOrdinal() {
        return 150;
    }

    @Override
    public Set<String> getPropertyNames() {

        final Set<String> propertyNames = new HashSet();

        try {
            final List<String> children = getCuratorClient().getChildren().forPath(applicationId);
            propertyNames.addAll(children);
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            e.printStackTrace();
        }

        return propertyNames;
    }

    @Override
    public Map<String, String> getProperties() {

        final Map<String, String> props = new HashMap();

        try {
            final List<String> children = getCuratorClient().getChildren().forPath(applicationId);
            for (final String key : children) {
                final String value = new String(getCuratorClient().getData().forPath(applicationId + "/" + key));
                props.put(key, value);
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }

        return props;
    }

    @Override
    public String getValue(final String key) {

        /*
         * Explicitly ignore all keys that are prefixed with the prefix used to configure the Zookeeper connection.
         * Other wise a stack overflow obviously happens.
         */
        if (key.startsWith(ignoredPrefix)) {
            return null;
        }
        try {
            final Stat stat = getCuratorClient().checkExists().forPath(applicationId + "/" + key);

            if (stat != null) {
                return new String(getCuratorClient().getData().forPath(applicationId + "/" + key));
            } else {
                return null;
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
        return null;
    }


    @Override
    public String getName() {
        return ZK_CONFIG_NAME;
    }

    private CuratorFramework getCuratorClient() {
        if (curatorClient == null) {

            final Config cfg = ConfigProvider.getConfig();
            final String zkUrl = cfg.getValue(zkUrkKey, String.class);

            applicationId = cfg.getValue(applicationIdKey, String.class);

            curatorClient = CuratorFrameworkFactory.newClient(zkUrl, new ExponentialBackoffRetry(1000, 3));
            curatorClient.start();
        }
        return curatorClient;
    }

}

