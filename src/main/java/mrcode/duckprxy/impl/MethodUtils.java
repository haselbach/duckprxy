package mrcode.duckprxy.impl;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Pattern;

public class MethodUtils {
    
    public interface MethodRetrieveStrategy {
        Method getMethod(
                String name,
                Class<?>[] parameterTypes)
        throws SecurityException, NoSuchMethodException;
    }

    public static MethodRetrieveStrategy methodByNameAndArgsStrategy(
            final Class<?> clazz) {
        return new MethodRetrieveStrategy() {
            public Method getMethod(
                    final String name,
                    final Class<?>[] parameterTypes)
            throws SecurityException, NoSuchMethodException {
                return clazz.getMethod(name, parameterTypes);
            }
        };
    }

    public static MethodRetrieveStrategy methodByNameWithoutArgsStrategy(
            final Class<?> clazz) {
        return new MethodRetrieveStrategy() {
            public Method getMethod(
                    final String name,
                    final Class<?>[] parameterTypes)
            throws SecurityException, NoSuchMethodException {
                return clazz.getMethod(name, (Class<?>[])null);
            }
        };
    }
    
    public static MethodRetrieveStrategy methodByPatternStrategy(
            final Class<?> clazz,
            final Map<Pattern, Method> methodMap) {
        return new MethodRetrieveStrategy() {
            public Method getMethod(
                    final String name,
                    final Class<?>[] parameterTypes)
            throws SecurityException, NoSuchMethodException {
                for (final Map.Entry<Pattern, Method> entry :
                    methodMap.entrySet()) {
                    if (entry.getKey().matcher(name).matches()) {
                        return entry.getValue();
                    }
                }
                return null;
            }
        };
    }
    
    public static MethodRetrieveStrategy defaultMethodStrategy() {
        final Method defaultMethod;
        try {
            defaultMethod = MethodUtils.class.getMethod(
                    "defaultMethod",
                    (Class<?>[]) null);
        } catch (Exception e) {
            throw new RuntimeException("Caught unexpected exception", e);
        }
        return defaultMethodStrategy(defaultMethod);
    }

    public static MethodRetrieveStrategy defaultMethodStrategy(
            final Method defaultMethod) {
        return new MethodRetrieveStrategy() {
            public Method getMethod(
                    final String name,
                    final Class<?>[] parameterTypes)
                    throws SecurityException, NoSuchMethodException {
                return defaultMethod;
            }
        };
    }

    public static void defaultMethod() throws Exception {
        throw new NoSuchMethodException();
    }
    
}
