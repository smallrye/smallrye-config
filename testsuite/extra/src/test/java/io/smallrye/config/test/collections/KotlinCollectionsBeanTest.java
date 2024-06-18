package io.smallrye.config.test.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
class KotlinCollectionsBeanTest {
    @Deployment
    static WebArchive deploy() {
        return ShrinkWrap
                .create(WebArchive.class)
                .addClasses(CollectionBean.class)
                .addClasses(KotlinCollectionsBean.class, KotlinCollectionsBeanTest.class)
                .addAsManifestResource("beans.xml")
                .addAsResource(new StringAsset("property.list=1,2,3\n" +
                        "property.single=1234\n"),
                        "META-INF/microprofile-config.properties");
    }

    @Inject
    KotlinCollectionsBean kotlinCollectionsBean;

    @Test
    void kotlinCollections() {
        assertEquals("1", kotlinCollectionsBean.typeList.get(0).getValue());
        assertEquals("2", kotlinCollectionsBean.typeList.get(1).getValue());
        assertEquals("3", kotlinCollectionsBean.typeList.get(2).getValue());
        assertEquals("1234", kotlinCollectionsBean.singleType.getValue());
    }
}
