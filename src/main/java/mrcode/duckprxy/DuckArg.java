package mrcode.duckprxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface DuckArg {

    DuckArgType value() default DuckArgType.NULL;
    
    int pos() default -1;
    
}
