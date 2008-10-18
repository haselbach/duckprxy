package mrcode.duckprxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DuckMethod {

    String[] value() default {};
    
    boolean fallback() default false;
    
    boolean subdelegate() default false;
    
}
