:microprofile-config: https://github.com/eclipse/microprofile-config/
:ci: https://github.com/smallrye/smallrye-config/actions?query=workflow%3A%22SmallRye+Build%22
:sonar: https://sonarcloud.io/dashboard?id=smallrye_smallrye-config

image:https://github.com/smallrye/smallrye-config/workflows/SmallRye%20Build/badge.svg?branch=main[link={ci}]
image:https://sonarcloud.io/api/project_badges/measure?project=smallrye_smallrye-config&metric=alert_status["Quality Gate Status", link={sonar}]
image:https://img.shields.io/github/license/smallrye/smallrye-config.svg["License", link="http://www.apache.org/licenses/LICENSE-2.0"]
image:https://img.shields.io/maven-central/v/io.smallrye.config/smallrye-config?color=green["Maven", link="https://search.maven.org/search?q=g:io.smallrye.config%20AND%20a:smallrye-config"]

= SmallRye Config

SmallRye Config is an implementation of {microprofile-config}[Eclipse MicroProfile Config].

== Instructions

Compile and test the project:

[source,bash]
----
mvn verify
----

Generate the documentation (from the documentation folder):

[source,bash]
----
mvn package
mkdocs serve
----

=== Project structure

* link:cdi[] - CDI Extension
* link:common[] - A set of reusable components to extend SmallRye Config
* link:converters[] - Additional Converters
* link:documentation[] - Project documentation
* link:examples[] - Examples projects to demonstrate SmallRye Config features
* link:implementation[] - Implementation of the MicroProfile Config API
* link:sources[] - Implementation of custom Config Sources
* link:testsuite[] - Test suite to run the implementation against the MicroProfile Config TCK
* link:utils[] - A set of additional extensions to enhance MicroProfile Config
* link:validator[] - Bean Validation integration

=== Contributing

Please refer to our Wiki for the https://github.com/smallrye/smallrye/[Contribution Guidelines].

=== Links

* http://github.com/smallrye/smallrye-config/[Project Homepage]
* {microprofile-config}[Eclipse MicroProfile Config]
* https://smallrye.io/smallrye-config[Documentation]
