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

public abstract class AbstractPrxyInvocationHandler
implements InvocationHandler {
    
    public abstract Method getSubDelegateGetter();

    protected final Object delegate;

    public AbstractPrxyInvocationHandler(Object delegate) {
        this.delegate = delegate;
    }

    protected Object getSubDelegate(Object proxy) {
        try {
            return getSubDelegateGetter().invoke(delegate, (Object[])null);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Got unexpected exception in sub delegate getter",
                    e);
        }
    }

    protected List<MethodRetrieveStrategy> makeStrategies(
            final Class<?> delegateClass,
            final DelegateClassInformation info) {
        MethodRetrieveStrategy fallbackStrategy;
        if (info.subDelegateGetter != null) {
            fallbackStrategy = MethodUtils.defaultMethodStrategy(null);
        } else if (info.fallbackMethod != null) {
            fallbackStrategy =
                MethodUtils.defaultMethodStrategy(info.fallbackMethod);
        } else {
            fallbackStrategy = MethodUtils.defaultMethodStrategy();
        }
        List<MethodRetrieveStrategy> strategies =
            Arrays.asList(new MethodRetrieveStrategy[] {
                    MethodUtils.methodByNameAndArgsStrategy(delegateClass),
                    MethodUtils.methodByNameWithoutArgsStrategy(
                            delegateClass),
                    MethodUtils.methodByPatternStrategy(
                            delegateClass, info.patternMap),
                    fallbackStrategy});
        return strategies;
    }
    
    protected Object[] getDelegateArguments(
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

    public DuckArg getDuckArg(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (DuckArg.class.equals(annotation.annotationType())) {
                return (DuckArg) annotation;
            }
        }
        return null;
    }
    
    protected DelegateClassInformation getDelegateClassInformation(
            final Class<?> delegateClass) {
        Map<Pattern, Method> patternMap = new HashMap<Pattern, Method>();
        Method fallbackMethod = null;
        Method subDelegateGetter = null;
        for (final Method method : delegateClass.getMethods()) {
            final DuckMethod duckMethod =
                method.getAnnotation(DuckMethod.class);
            if (duckMethod != null) {
                for (final String value : duckMethod.value()) {
                    patternMap.put(Pattern.compile(value), method);
                }
                if (duckMethod.fallback()) {
                    fallbackMethod = method;
                }
                if (duckMethod.subdelegate()) {
                    subDelegateGetter = method;
                }
            }
        }
        return new DelegateClassInformation(
                fallbackMethod, subDelegateGetter, patternMap);
    }

    protected static class DelegateClassInformation {
        protected final Method fallbackMethod;
        protected final Method subDelegateGetter;
        protected final Map<Pattern, Method> patternMap;
        public DelegateClassInformation(
                Method fallbackMethod,
                Method subDelegateGetter,
                Map<Pattern, Method> patternMap) {
            this.fallbackMethod = fallbackMethod;
            this.subDelegateGetter = subDelegateGetter;
            this.patternMap = patternMap;
        }
    }
}
