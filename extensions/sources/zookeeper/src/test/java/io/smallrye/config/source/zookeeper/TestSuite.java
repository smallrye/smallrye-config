/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.config.source.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.util.logging.Logger;

/**
 * Start a Zookeeper TestServer and run the tests
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        ZooKeeperConfigSourceTest.class
})
public class TestSuite {
    private static final Logger logger = Logger.getLogger(TestSuite.class.getName());

    private static TestingServer testServer;

    @BeforeClass
    public static void setUpClass() throws Exception {
        logger.info("Setup Class");
        testServer = new TestingServer(2181, true);

        //Add a property that's going to be injected
        CuratorFramework curatorClient = CuratorFrameworkFactory.newClient("localhost:2181", new ExponentialBackoffRetry(1000, 3));
        curatorClient.start();
        curatorClient.createContainers("/test1/injected.property");
        curatorClient.setData().forPath("/test1/injected.property", "injected.property.value".getBytes());

        curatorClient.createContainers("/test1/injected.int.property");
        curatorClient.setData().forPath("/test1/injected.int.property", "17".getBytes());

        curatorClient.close();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        logger.info("Teardown class");
        testServer.close();
        testServer.stop();
    }
}