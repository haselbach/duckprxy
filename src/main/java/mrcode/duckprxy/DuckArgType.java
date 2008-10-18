package mrcode.duckprxy;

public enum DuckArgType {
    
    /**
     * An argument that is to be filled with null.
     */
    NULL,
    
    /**
     * An argument that is to be filled with the name of the invoked method.
     * The argument must accept strings. Otherwise the behavior is
     * unspecified.
     */
    NAME,
    
    /**
     * An argument that is to be filled with the arguments.
     * The argument must accept Object[]. Otherwise the behavior is
     * unspecified.
     */
    ARGS,
    
    /**
     * An argument that is to be filled with the n-th argument, where
     * n is defined by the pos value of {@link DuckArg}. n must be
     * non-negative and smaller than the number of arguments provided by the
     * call. Otherwise the behavior is unspecified.
     */
    ARGN

}
