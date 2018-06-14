package io.streamzi.config.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.*;
import java.util.logging.Logger;

/**
 * MicroProfile Config Source that is backed by Zookeeper.
 *
 * The Config Source itself needs configuration which is handled by other Config Sources.
 * Properties prefixed with io.streamzi.zk will be ignored by this Config Source.
 *
 * author: Simon Woodman <swoodman@redhat.com>
 */
public class ZkConfigSource implements ConfigSource {

    private static final Logger logger = Logger.getLogger(ZkConfigSource.class.getName());

    //Apache Curator framework used to access Zookeeper
    private CuratorFramework curatorClient;

    //Root node of an application's configuration
    private String applicationId;

    //Prefix of ignored properties
    private String ignoredPrefix = "io.streamzi.zk";

    //Property the URL of the Zookeeper instance will be read from
    private String zkUrkKey = "io.streamzi.zk.zkUrl";

    //Property of the Application Id. This will be the root znode for an application's properties
    private String applicationIdKey = "io.streamzi.zk.applicationId";

    public ZkConfigSource() {
    }

    @Override
    public int getOrdinal() {
        return 150;
    }

    @Override
    public Set<String> getPropertyNames() {

        Set<String> propertyNames = new HashSet<>();

        try {
            List<String> children = getCuratorClient().getChildren().forPath(applicationId);
            propertyNames.addAll(children);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return propertyNames;
    }

    @Override
    public Map<String, String> getProperties() {

        Map<String, String> props = new HashMap<>();

        try {
            List<String> children = getCuratorClient().getChildren().forPath(applicationId);
            for (String key : children) {
                String value = new String(getCuratorClient().getData().forPath(applicationId + "/" + key));
                props.put(key, value);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return props;
    }

    @Override
    public String getValue(String key) {

        /*
         * Explicitly ignore all keys that are prefixed with the prefix used to configure the Zookeeper connection.
         * Other wise a stack overflow obviously happens.
         */
        if(key.startsWith(ignoredPrefix))
        {
            return null;
        }
        try {
            Stat stat = getCuratorClient().checkExists().forPath(applicationId + "/" + key);

            if (stat != null) {
                return new String(getCuratorClient().getData().forPath(applicationId + "/" + key));
            } else {
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public String getName() {
        return "io.streamzi.xk.ZkConfigSource";
    }

    private CuratorFramework getCuratorClient() {
        if (curatorClient == null) {

            Config cfg = ConfigProvider.getConfig();
            String zkUrl = cfg.getValue(zkUrkKey, String.class);

            applicationId = cfg.getValue(applicationIdKey, String.class);

            curatorClient = CuratorFrameworkFactory.newClient(zkUrl, new ExponentialBackoffRetry(1000, 3));
            curatorClient.start();
        }
        return curatorClient;
    }

}

