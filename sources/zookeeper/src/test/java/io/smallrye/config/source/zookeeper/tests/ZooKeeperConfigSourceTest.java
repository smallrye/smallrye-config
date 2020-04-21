/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smallrye.config.source.zookeeper.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.weld.junit4.WeldInitiator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.smallrye.config.inject.ConfigProducer;

/**
 * Test the ConfigSource
 */
public class ZooKeeperConfigSourceTest {

    private static final Logger logger = Logger.getLogger(ZooKeeperConfigSourceTest.class.getName());

    private CuratorFramework curatorClient;

    private final String APPLICATION_ID = "test1";
    private final String PROPERTY_NAME = "some.property";
    private final String ZK_KEY = "/" + APPLICATION_ID + "/" + PROPERTY_NAME;
    private final String PROPERTY_VALUE = "some.value";

    @Rule
    public WeldInitiator weld = WeldInitiator.from(ConfigProducer.class)
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    @ConfigProperty(name = "injected.property", defaultValue = "injected.property.default")
    private String injectedProperty;

    @Inject
    @ConfigProperty(name = "injected.int.property", defaultValue = "13")
    private int injectedIntProperty;

    @Before
    public void setUpClass() {
        //Connection to ZK so that we can add in a property
        curatorClient = CuratorFrameworkFactory.newClient("localhost:2181", new ExponentialBackoffRetry(1000, 3));
        curatorClient.start();
    }

    @After
    public void tearDownClass() {
        logger.info("Teardown ");
        curatorClient.close();
    }

    @Test
    public void testGettingProperty() {
        logger.info("ZooKeeperConfigSourceTest.testGettingProperty");

        Config cfg = ConfigProvider.getConfig();

        //Check that the ZK ConfigSource will work
        assertNotNull(cfg.getValue("io.smallrye.configsource.zookeeper.url", String.class));

        //Check that a property doesn't exist yet
        try {
            cfg.getValue(PROPERTY_NAME, String.class);
            fail("Property " + PROPERTY_NAME + " should not exist");
        } catch (NoSuchElementException ignored) {
        }

        //Check that the optional version of the property is not present
        assertFalse(cfg.getOptionalValue(PROPERTY_NAME, String.class).isPresent());
        //setup the property in ZK
        try {
            curatorClient.createContainers(ZK_KEY);
            curatorClient.setData().forPath(ZK_KEY, PROPERTY_VALUE.getBytes());
        } catch (Exception e) {
            fail("Cannot set property PROPERTY_VALUE directly in Zookeeper");
        }

        //check the property can be optained by a property
        assertEquals(PROPERTY_VALUE, cfg.getValue(PROPERTY_NAME, String.class));

        Set<String> propertyNames = new HashSet<>();
        cfg.getPropertyNames().forEach(propertyNames::add);
        assertTrue(propertyNames.contains(PROPERTY_NAME));
    }

    @Test
    public void testInjection() {
        assertEquals("injected.property.value", injectedProperty);
        assertEquals(17, injectedIntProperty);
    }
}
