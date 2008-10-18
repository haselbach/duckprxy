package mrcode.duckprxy;

public interface DuckPrxy {
    
    <T> T makeProxy(
            final Class<T> mainInterface,
            final Object delegate,
            final Class<?> ... interfaces);
}
