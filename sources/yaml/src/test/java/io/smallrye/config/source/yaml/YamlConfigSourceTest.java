package io.smallrye.config.source.yaml;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

public class YamlConfigSourceTest {
    @Test
    void flatten() throws Exception {
        YamlConfigSource yaml = new YamlConfigSource("yaml",
                YamlConfigSourceTest.class.getResourceAsStream("/example-216.yml"));
        String value = yaml.getValue("admin.users");
        Users users = new UserConverter().convert(value);
        assertEquals(2, users.getUsers().size());
        assertEquals(users.users.get(0).getEmail(), "joe@gmail.com");
        assertEquals(users.users.get(0).getRoles(), Stream.of("Moderator", "Admin").collect(toList()));

        assertEquals("joe@gmail.com", yaml.getValue("admin.users.[0].email"));
    }

    @Test
    void profiles() throws Exception {
        YamlConfigSource yaml = new YamlConfigSource("yaml",
                YamlConfigSourceTest.class.getResourceAsStream("/example-profiles.yml"));

        assertEquals("default", yaml.getValue("foo.bar"));
        assertEquals("dev", yaml.getValue("%dev.foo.bar"));
        assertEquals("prod", yaml.getValue("%prod.foo.bar"));
    }

    public static class Users {
        List<User> users;

        public List<User> getUsers() {
            return users;
        }

        public void setUsers(final List<User> users) {
            this.users = users;
        }
    }

    public static class User {
        String email;
        String username;
        String password;
        List<String> roles;

        public String getEmail() {
            return email;
        }

        public void setEmail(final String email) {
            this.email = email;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(final String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(final String password) {
            this.password = password;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(final List<String> roles) {
            this.roles = roles;
        }
    }

    static class UserConverter implements Converter<Users> {
        @Override
        public Users convert(final String value) {
            return new Yaml().loadAs(value, Users.class);
        }
    }
}
