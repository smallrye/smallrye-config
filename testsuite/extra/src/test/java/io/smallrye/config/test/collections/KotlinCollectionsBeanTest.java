package io.smallrye.config.test.collections;

import static org.testng.Assert.assertEquals;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

public class KotlinCollectionsBeanTest extends Arquillian {
    @Deployment
    public static WebArchive deploy() {
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
    public void kotlinCollections() {
        assertEquals(kotlinCollectionsBean.typeList.get(0).getValue(), "1");
        assertEquals(kotlinCollectionsBean.typeList.get(1).getValue(), "2");
        assertEquals(kotlinCollectionsBean.typeList.get(2).getValue(), "3");
        assertEquals(kotlinCollectionsBean.singleType.getValue(), "1234");
    }
}
