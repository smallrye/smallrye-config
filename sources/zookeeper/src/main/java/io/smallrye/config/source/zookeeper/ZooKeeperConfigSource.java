package io.smallrye.config.source.zookeeper;

import java.util.*;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;

import io.smallrye.config.common.AbstractConfigSource;

/**
 * MicroProfile Config Source that is backed by Zookeeper.
 * <p>
 * The Config Source itself needs configuration which is handled by other Config Sources.
 * Properties prefixed with io.smallrye.configsource.zookeeper will be ignored by this Config Source.
 * <p>
 * author: Simon Woodman swoodman@redhat.com
 */
public class ZooKeeperConfigSource extends AbstractConfigSource {
    private static final long serialVersionUID = 3127679154588598693L;

    public static final String NAME = "ZooKeeperConfigSource";
    public static final int ORDINAL = 150;

    //Property the URL of the Zookeeper instance will be read from
    static final String ZOOKEEPER_URL_KEY = "io.smallrye.configsource.zookeeper.url";
    //Property of the Application Id. This will be the root znode for an application's properties
    static final String APPLICATION_ID_KEY = "io.smallrye.configsource.zookeeper.applicationId";

    //Apache Curator framework used to access Zookeeper
    private final CuratorFramework curator;
    //Root node of an application's configuration
    private final String applicationId;

    public ZooKeeperConfigSource(final String zookeeperUrl, final String applicationId) {
        super(NAME, ORDINAL);

        //Only create the ZK Client if the properties exist.
        if (zookeeperUrl != null && applicationId != null) {
            ZooKeepperLogging.log.configuringZookeeper(zookeeperUrl, applicationId);

            this.applicationId = applicationId.startsWith("/") ? applicationId : "/" + applicationId;
            this.curator = CuratorFrameworkFactory.newClient(zookeeperUrl, new ExponentialBackoffRetry(1000, 3));
            this.curator.start();
        } else {
            throw ZooKeeperMessages.msg.propertiesNotSet();
        }
    }

    @Override
    public Set<String> getPropertyNames() {

        final Set<String> propertyNames = new HashSet<>();

        try {
            final List<String> children = curator.getChildren().forPath(applicationId);
            propertyNames.addAll(children);
        } catch (Exception e) {
            ZooKeepperLogging.log.failedToRetrievePropertyNames(e);
        }

        return propertyNames;
    }

    @Override
    public Map<String, String> getProperties() {

        final Map<String, String> props = new HashMap<>();

        try {
            final List<String> children = curator.getChildren().forPath(applicationId);
            for (final String key : children) {
                final String value = new String(curator.getData().forPath(applicationId + "/" + key));
                props.put(key, value);
            }
        } catch (Exception e) {
            ZooKeepperLogging.log.failedToRetrieveProperties(e);
        }

        return props;
    }

    @Override
    public String getValue(final String key) {
        try {
            final Stat stat = curator.checkExists().forPath(applicationId + "/" + key);

            if (stat != null) {
                return new String(curator.getData().forPath(applicationId + "/" + key));
            } else {
                return null;
            }
        } catch (Exception e) {
            ZooKeepperLogging.log.failedToRetrieveValue(e, key);
        }
        return null;
    }
}
