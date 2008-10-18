package mrcode.duckprxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds details to a method of a duck delegate.
 * 
 * @author Christian Haselbach
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DuckMethod {

    /**
     * @return An array of patterns to match method names.
     */
    String[] value() default {};
    
    /**
     * @return true iff this method shall be used as a fallback.
     */
    boolean fallback() default false;

    /**
     * @return true iff this method is a getter for a subdelegate.
     */
    boolean subdelegate() default false;
    
}
