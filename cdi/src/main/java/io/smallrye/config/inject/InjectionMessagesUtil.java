package io.smallrye.config.inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.InjectionPoint;

public class InjectionMessagesUtil {

    /**
     * 
     * Formats InjectPoint information for Exception messages.<br>
     * <br>
     * 
     * 3 possible InjectionPoint types are considered:<br>
     * <br>
     * 
     * <b>Fields</b><br>
     * Given: java.lang.String
     * io.smallrye.config.inject.ValidateInjectionTest$SkipPropertiesTest$SkipPropertiesBean.missingProp<br>
     * Returns: io.smallrye.config.inject.ValidateInjectionTest$SkipPropertiesTest$SkipPropertiesBean.missingProp<br>
     * <br>
     * 
     * <b>Method parameters</b><br>
     * Given: private void
     * io.smallrye.config.inject.ValidateInjectionTest$MethodUnnamedPropertyTest$MethodUnnamedPropertyBean.methodUnnamedProperty(java.lang.String)<br>
     * Returns:
     * io.smallrye.config.inject.ValidateInjectionTest$MethodUnnamedPropertyTest$MethodUnnamedPropertyBean.methodUnnamedProperty(String)<br>
     * <br>
     * 
     * <b>Constructor parameters</b><br>
     * Given: public
     * io.smallrye.config.inject.ValidateInjectionTest$ConstructorUnnamedPropertyTest$ConstructorUnnamedPropertyBean(java.lang.String)<br>
     * Returns:
     * io.smallrye.config.inject.ValidateInjectionTest$ConstructorUnnamedPropertyTest$ConstructorUnnamedPropertyBean(String)
     * 
     */
    public static String formatInjectionPoint(InjectionPoint injectionPoint) {

        Member member = injectionPoint.getMember();

        StringBuilder sb = new StringBuilder();
        sb.append(member.getDeclaringClass().getName());

        if (member instanceof Field) {
            sb.append("." + member.getName());
        } else if (member instanceof Method) {
            sb.append("." + member.getName());
            appendParameterTypes(sb, (Method) member);
        } else if (member instanceof Constructor) {
            appendParameterTypes(sb, (Constructor) member);
        }
        return sb.toString();
    }

    private static void appendParameterTypes(StringBuilder sb, Executable executable) {
        sb.append("(" +
                Arrays.stream(executable.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(", "))
                + ")");
    }

}