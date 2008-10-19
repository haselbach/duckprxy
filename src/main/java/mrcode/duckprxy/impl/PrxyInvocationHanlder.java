package mrcode.duckprxy.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import mrcode.duckprxy.impl.MethodUtils.MethodRetrieveStrategy;

/**
 * The heart of the the {@link DuckPrxyImpl} implementation.
 * 
 * @author Christian Haselbach
 */
public class PrxyInvocationHanlder extends AbstractPrxyInvocationHandler {
    
    private final Class<?> delegateClass;
    private final List<MethodRetrieveStrategy> strategies;
    private final InvocationHandler subDelegate;
    private final Method subDelegateGetter;
    
    public PrxyInvocationHanlder(final Object delegate) {
        super(delegate);
        this.delegateClass = delegate.getClass();
        DelegateClassInformation info =
            getDelegateClassInformation(delegateClass);
        this.strategies = makeStrategies(delegateClass, info);
        this.subDelegateGetter = info.subDelegateGetter;
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

    public Method getSubDelegateGetter() {
        return subDelegateGetter;
    }
    
}
