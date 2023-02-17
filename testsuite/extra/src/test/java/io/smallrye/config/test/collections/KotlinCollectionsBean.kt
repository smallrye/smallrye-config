package io.smallrye.config.test.collections

import org.eclipse.microprofile.config.inject.ConfigProperty
import jakarta.enterprise.context.Dependent
import jakarta.inject.Inject

@Dependent
class KotlinCollectionsBean {
    @Inject
    @ConfigProperty(name = "property.list")
    lateinit var typeList: List<MyType>

    @Inject
    @ConfigProperty(name = "property.single")
    lateinit var singleType: MyType
}

class MyType(val value: String) {

}
