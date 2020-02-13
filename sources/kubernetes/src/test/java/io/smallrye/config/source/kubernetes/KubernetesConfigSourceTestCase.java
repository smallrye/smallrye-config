/*
 * Copyright 2020 Red Hat, Inc.
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

package io.smallrye.config.source.kubernetes;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class KubernetesConfigSourceTestCase {

    private static final String CONFIG_SOURCE_KUBERNETES = "io/smallrye/config/source/kubernetes/config";

    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    @Test
    public void testReadPropertiesFiles() throws URISyntaxException {
        final URL configDirURL = getClass().getClassLoader().getResource(CONFIG_SOURCE_KUBERNETES);
        final File dir = new File(configDirURL.toURI());

        final KubernetesConfigSource kubernetesConfigSource = new KubernetesConfigSource(dir.toPath());
        final String aValue = kubernetesConfigSource.getValue("a");
        final String fValue = kubernetesConfigSource.getValue("f");

        assertThat(aValue).isEqualTo("b");
        assertThat(fValue).isEqualTo("g");
    }

    @Test
    public void testReadYamlFiles() throws URISyntaxException {
        final URL configDirURL = getClass().getClassLoader().getResource(CONFIG_SOURCE_KUBERNETES);
        final File dir = new File(configDirURL.toURI());

        final KubernetesConfigSource kubernetesConfigSource = new KubernetesConfigSource(dir.toPath());
        final String cValue = kubernetesConfigSource.getValue("c");

        assertThat(cValue).isEqualTo("d");
    }

    @Test
    public void testReadFileSystemFiles() throws URISyntaxException {
        final URL configDirURL = getClass().getClassLoader().getResource(CONFIG_SOURCE_KUBERNETES);
        final File dir = new File(configDirURL.toURI());

        final KubernetesConfigSource kubernetesConfigSource = new KubernetesConfigSource(dir.toPath());
        final String myKeyValue = kubernetesConfigSource.getValue("myKey");

        assertThat(myKeyValue).isEqualTo("e");
    }

    @Test
    public void testWatchChanges() throws IOException {

        final Path configFile = folder.newFile("conf.properties").toPath();
        Files.write(configFile, "a=b".getBytes());

        final KubernetesConfigSource kubernetesConfigSource = new KubernetesConfigSource(configFile.getParent());
        String aValue = kubernetesConfigSource.getValue("a");
        assertThat(aValue).isEqualTo("b");

        final ExecutorService executorService = Executors.newFixedThreadPool(1);
        kubernetesConfigSource.startMonitoringChanges(executorService);

        Files.write(configFile, "a=d".getBytes());

        await()
            .atMost(15, TimeUnit.SECONDS)
            .until(() -> kubernetesConfigSource.getValue("a").equals("d"));

        aValue = kubernetesConfigSource.getValue("a");
        assertThat(aValue).isEqualTo("d");

        executorService.shutdown();
    }

}
