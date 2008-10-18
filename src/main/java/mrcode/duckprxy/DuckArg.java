package mrcode.duckprxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for duck delegate method paramaeters.
 * 
 * @author Christian Haselbach
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface DuckArg {

    /**
     * @return How to fill the argument. See {@link DuckArgType} for details.
     */
    DuckArgType value() default DuckArgType.NULL;
    
    int pos() default -1;
    
}
