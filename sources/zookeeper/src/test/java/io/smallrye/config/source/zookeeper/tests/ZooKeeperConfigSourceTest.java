/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smallrye.config.source.zookeeper.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.io.File;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.smallrye.config.common.AbstractConfigSource;
import io.smallrye.config.source.zookeeper.ZooKeeperConfigSource;

/**
 * Test the ConfigSource
 */
@RunWith(Arquillian.class)
public class ZooKeeperConfigSourceTest {

    private static final Logger logger = Logger.getLogger(ZooKeeperConfigSourceTest.class.getName());

    private CuratorFramework curatorClient;

    private final String APPLICATION_ID = "test1";
    private final String PROPERTY_NAME = "some.property";
    private final String ZK_KEY = "/" + APPLICATION_ID + "/" + PROPERTY_NAME;
    private final String PROPERTY_VALUE = "some.value";

    public ZooKeeperConfigSourceTest() {
    }

    @Inject
    @ConfigProperty(name = "injected.property", defaultValue = "injected.property.default")
    private String injectedProperty;

    @Inject
    @ConfigProperty(name = "injected.int.property", defaultValue = "13")
    private int injectedIntProperty;

    @Deployment
    public static WebArchive createDeployment() {

        //Add the Curator and Microprofile Config libraries
        final File[] curatorFiles = Maven.resolver().resolve("org.apache.curator:curator-recipes:2.12.0").withTransitivity()
                .asFile();
        final File[] curatorTestFiles = Maven.resolver().resolve("org.apache.curator:curator-test:2.12.0").withTransitivity()
                .asFile();
        final File[] guavaFiles = Maven.resolver().resolve("com.google.guava:guava:25.1-jre").withTransitivity().asFile();
        final File[] swarmMPCFiles = Maven.resolver().resolve("org.wildfly.swarm:microprofile-config:1.0.1")
                .withoutTransitivity().asFile();
        final File[] assertJFiles = Maven.resolver().resolve("org.assertj:assertj-core:3.10.0").withoutTransitivity().asFile();

        return ShrinkWrap.create(WebArchive.class, "ZkMicroProfileConfigTest.war")
                .addPackage(ZooKeeperConfigSource.class.getPackage())
                .addPackage(AbstractConfigSource.class.getPackage())
                .addAsLibraries(curatorFiles)
                .addAsLibraries(swarmMPCFiles)
                .addAsLibraries(curatorTestFiles)
                .addAsLibraries(guavaFiles)
                .addAsLibraries(assertJFiles)
                .addAsResource(
                        new File("src/main/resources/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource"),
                        "META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource")
                .addAsResource(new File("src/test/resources/META-INF/microprofile-config.properties"),
                        "META-INF/microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

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
        assertThat(cfg.getValue("io.smallrye.configsource.zookeeper.url", String.class)).isNotNull();

        //Check that a property doesn't exist yet
        try {
            cfg.getValue(PROPERTY_NAME, String.class);
            fail("Property " + PROPERTY_NAME + " should not exist");
        } catch (NoSuchElementException ignored) {
        }

        //Check that the optional version of the property is not present
        assertThat(cfg.getOptionalValue(PROPERTY_NAME, String.class)).isNotPresent();
        //setup the property in ZK
        try {
            curatorClient.createContainers(ZK_KEY);
            curatorClient.setData().forPath(ZK_KEY, PROPERTY_VALUE.getBytes());
        } catch (Exception e) {
            fail("Cannot set property PROPERTY_VALUE directly in Zookeeper");
        }

        //check the property can be optained by a property
        assertThat(cfg.getValue(PROPERTY_NAME, String.class)).isEqualTo(PROPERTY_VALUE);

        Set<String> propertyNames = new HashSet<>();
        cfg.getPropertyNames().forEach(propertyNames::add);
        assertThat(propertyNames).contains(PROPERTY_NAME);
    }

    @Test
    public void testInjection() {
        assertThat(injectedProperty).isEqualTo("injected.property.value");
        assertThat(injectedIntProperty).isEqualTo(17);
    }

}
