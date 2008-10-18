package mrcode.duckprxy;

/**
 * An interface for a duck typing proxy creator service.
 * <p>
 * A duck proxy delegates to its delegate by using the looking for a
 * method, using the first strategy that yields a result:
 * <ol>
 * <li> Search for a method with the same name and signature.
 * <li> Search for a method with the same name, ignoring the signature.
 * <li> Looking through the defined name patterns, checking whether a method
 *      has a matching pattern definition (more on that later). 
 * </ol>
 * 
 * If no method yields a method, the duck proxy checks for a subdelegate
 * (defined via {@link DuckMethod}). If such a subdelegate is found,
 * the invocation will be forwarded to the subdelegate.
 * <p>
 * Once a method is found, the arguments are constructed to call this method.
 * Let {@code arg_1,...,arg_n} be the arguments for the call and
 * {@code par_1,...,par_k} the parameters of the duck delegate method.
 * A {@code par_i} is filled with a value according to its DuckArg definition
 * if such a definition exists. Parameter with no such definition are filled
 * up with the arguments of the call. I.e., if {@code k} parameters
 * {@code par_j} with {@code j &lt; i} and {@code par_j} duck arg definition
 * exists, that {@code par_i} is filled with {@code arg_(i-k)}. 
 * <p>
 * To give a more practical example: If we want to map call
 * {@code foo(String s, String t)} to a call {@code bar("foo", s, t)}, we can
 * do this with the following definition:
 * <code style="white-space:pre">
 *   &#64;DuckMethod("foo")
 *   public void bar(
 *       &#64;DuckArg(DuckArgType.NAME) String name,
 *       String s, String t) {
 *   ...}
 * </code>
 * 
 * @author Christian Haselbach
 */
public interface DuckPrxy {

    /**
     * Creates an object that implements the defined interfaces
     * delegating to a given object, mainly using duck typing.
     * @param <T>            The class of the main interface.
     * @param mainInterface  The main interface that the resulting object
     *                       shall implement.
     * @param delegate       The object to delegate the calls to.
     * @param interfaces     Secondary interfaces to implement.
     * @return               An object implementing the main interface and
     *                       secondary interfaces, delegating to the delegate
     *                       object.
     */
    <T> T makeProxy(
            final Class<T> mainInterface,
            final Object delegate,
            final Class<?> ... interfaces);
}
