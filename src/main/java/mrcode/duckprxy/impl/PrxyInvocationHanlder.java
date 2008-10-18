package mrcode.duckprxy.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import mrcode.duckprxy.DuckArg;
import mrcode.duckprxy.DuckMethod;
import mrcode.duckprxy.impl.MethodUtils.MethodRetrieveStrategy;

/**
 * The heart of the the {@link DuckPrxyImpl} implementation.
 * 
 * @author Christian Haselbach
 */
public class PrxyInvocationHanlder implements InvocationHandler {
    
    private final Object delegate;
    private final Class<?> delegateClass;
    private final List<MethodRetrieveStrategy> strategies;
    private final InvocationHandler subDelegate;
    private final Method subDelegateGetter;
    
    public PrxyInvocationHanlder(final Object delegate) {
        this.delegate = delegate;
        this.delegateClass = delegate.getClass();
        Map<Pattern, Method> methodMap = new HashMap<Pattern, Method>();
        Method fallbackMethod = null;
        Method subDelegateGetter = null;
        for (final Method method : delegateClass.getMethods()) {
            final DuckMethod duckMethod =
                method.getAnnotation(DuckMethod.class);
            if (duckMethod != null) {
                for (final String value : duckMethod.value()) {
                    methodMap.put(Pattern.compile(value), method);
                }
                if (duckMethod.fallback()) {
                    fallbackMethod = method;
                }
                if (duckMethod.subdelegate()) {
                    subDelegateGetter = method;
                }
            }
        }
        MethodRetrieveStrategy fallbackStrategy;
        if (subDelegateGetter != null) {
            fallbackStrategy = MethodUtils.defaultMethodStrategy(null);
        } else if (fallbackMethod != null) {
            fallbackStrategy =
                MethodUtils.defaultMethodStrategy(fallbackMethod);
        } else {
            fallbackStrategy = MethodUtils.defaultMethodStrategy();
        }
        this.strategies = Arrays.asList(new MethodRetrieveStrategy[] {
                MethodUtils.methodByNameAndArgsStrategy(delegateClass),
                MethodUtils.methodByNameWithoutArgsStrategy(delegateClass),
                MethodUtils.methodByPatternStrategy(delegateClass, methodMap),
                fallbackStrategy});
        this.subDelegateGetter = subDelegateGetter;
        this.subDelegate = subDelegateGetter == null ? null :
            new PrxyInvocationHanlder(getSubDelegate(delegate));
    }

    public Object invoke(
            final Object proxy,
            final Method method,
            final Object[] args)
            throws Throwable {
        final String name = method.getName();
        final Class<?>[] parameterTypes = method.getParameterTypes();
        final Method delegateMethod = getDelegateMethod(name, parameterTypes);
        if (delegateMethod != null) {
            return delegateMethod.invoke(
                    delegate,
                    getDelegateArguments(name, delegateMethod, args));
        }
        return subDelegate.invoke(getSubDelegate(delegate), method, args);
    }

    private Object getSubDelegate(Object proxy) {
        try {
            return subDelegateGetter.invoke(delegate, (Object[])null);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Got unexpected exception in sub delegate getter",
                    e);
        }
    }

    private Object[] getDelegateArguments(
            final String name,
            final Method delegateMethod,
            final Object[] args) {
        final Annotation[][] annotations =
            delegateMethod.getParameterAnnotations();
        final int len = annotations.length; 
        int argIndex = 0;
        final Object[] delegateArgs = new Object[len];
        for (int annoIndex = 0; annoIndex < len; annoIndex++) {
            DuckArg duckArg = getDuckArg(annotations[annoIndex]);
            if (duckArg == null) {
                if (argIndex < args.length) {
                    delegateArgs[annoIndex] = args[argIndex];
                    argIndex++;
                }
            } else {
                switch (duckArg.value()) {
                    case NULL:
                        break;
                    case NAME:
                        delegateArgs[annoIndex] = name;
                        break;
                    case ARGS:
                        delegateArgs[annoIndex] = args;
                        break;
                    case ARGN:
                        if (duckArg.pos() < args.length) {
                            delegateArgs[annoIndex] = args[duckArg.pos()];
                        }
                        break;
                }
            }
        }
        return delegateArgs;
    }

    private DuckArg getDuckArg(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (DuckArg.class.equals(annotation.annotationType())) {
                return (DuckArg) annotation;
            }
        }
        return null;
    }

    private Method getDelegateMethod(
            final String name,
            final Class<?>[] parameterTypes) {
        Method delegateMethod = null;
        for(MethodRetrieveStrategy strategy : strategies) {
            try {
                delegateMethod = strategy.getMethod(name, parameterTypes);
            } catch (Exception e) {
                delegateMethod = null;
            }
            if (delegateMethod != null) {
                break;
            }
        }
        return delegateMethod;
    }
    
}
