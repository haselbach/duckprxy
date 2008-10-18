package mrcode.duckprxy.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import mrcode.duckprxy.DuckPrxy;
import mrcode.duckprxy.impl.PrxyInvocationHanlder;

public class DuckPrxyImpl implements DuckPrxy {
    
    public <T> T makeProxy(
            final Class<T> mainInterface,
            final Object delegate,
            final Class<?> ... interfaces) {
        final Class<?>[] allInterfaces = new Class<?>[interfaces.length + 1];
        allInterfaces[0] = mainInterface;
        for (int i=0; i<interfaces.length; i++) {
            allInterfaces[i+1] = interfaces[i];
        }
        final InvocationHandler invocationHandler =
            new PrxyInvocationHanlder(delegate);
        @SuppressWarnings("unchecked")
        final T proxy = (T) Proxy.newProxyInstance(
                delegate.getClass().getClassLoader(),
                allInterfaces,
                invocationHandler);
        return proxy;
    }

}
