package io.smallrye.config.source.yaml;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import io.smallrye.config.source.EnabledConfigSource;
import io.smallrye.config.source.file.AbstractUrlBasedSource;

/**
 * Create a deployable unit to test against
 * 
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public class DeployableUnit {

    public static WebArchive create() {
        return create(null);
    }

    public static WebArchive create(String includeConfigFile) {

        final File[] smallryeConfig = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("io.smallrye:smallrye-config")
                .withoutTransitivity().asFile();

        WebArchive archive = ShrinkWrap.create(WebArchive.class, "YamlConfigSourceTest.war")
                .addPackage(YamlConfigSource.class.getPackage())
                .addPackage(AbstractUrlBasedSource.class.getPackage())
                .addPackage(EnabledConfigSource.class.getPackage())
                .addAsLibraries(smallryeConfig)
                .addAsResource(
                        new File("src/main/resources/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource"),
                        "META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        if (includeConfigFile != null && !includeConfigFile.isEmpty()) {
            archive = archive.addAsResource(
                    DeployableUnit.class.getClassLoader().getResource(includeConfigFile),
                    "META-INF/microprofile-config.properties");
        }

        return archive;
    }
}
