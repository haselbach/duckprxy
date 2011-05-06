package mrcode.duckprxy.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mrcode.duckprxy.impl.MethodUtils.MethodRetrieveStrategy;

public class PrxyPreCompInvocationHandler
extends AbstractPrxyInvocationHandler {

    private final Map<Method, Method> methodMap;
    private final InvocationHandler subDelegate;
    private final Method subDelegateGetter;
    
    public PrxyPreCompInvocationHandler(
            final Object delegate,
            final Class<?>[] interfaces) {
        super(delegate);
        final Class<?> delegateClass = delegate.getClass();
        DelegateClassInformation info =
            getDelegateClassInformation(delegateClass);

        List<MethodRetrieveStrategy> strategies =
            makeStrategies(delegateClass, info);
        
        methodMap = new HashMap<Method, Method>();
        for (final Class<?> iface : interfaces) {
            for (final Method method : iface.getMethods()) {
                methodMap.put(
                        method,
                        createDelegateMethod(strategies, method));
            }
        }
        
        this.subDelegateGetter = info.subDelegateGetter;
        this.subDelegate = subDelegateGetter == null ? null :
            new PrxyInvocationHanlder(getSubDelegate(delegate));
    }

    private static Method createDelegateMethod(
            final List<MethodRetrieveStrategy> strategies,
            final Method method) {
        return getDelegateMethod(
                strategies,
                method.getName(),
                method.getParameterTypes());
    }

    private static Method getDelegateMethod(
            final List<MethodRetrieveStrategy> strategies,
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
    
    public Object invoke(
            final Object proxy,
            final Method method,
            final Object[] args)
            throws Throwable {
        final String name = method.getName();
        final Method delegateMethod = methodMap.get(method);
        if (delegateMethod != null) {
            final Object[] delegateArgs =
                getDelegateArguments(name, delegateMethod, args);
            return delegateMethod.invoke(delegate, delegateArgs);
        }
        return subDelegate.invoke(getSubDelegate(delegate), method, args);
    }

    public Method getSubDelegateGetter() {
        return subDelegateGetter;
    }
    
}
