package mrcode.duckprxy;

import junit.framework.TestCase;
import mrcode.duckprxy.impl.JavassistDuckPrxy;

public class DuckPrxyObjectTest extends TestCase {
    
    public static class MyDelegate {
        public String foo(int i) {
            return "bar" + (i+1);
        }
    }
    
    public void testObjectProxy() {
        testObjectProxy(getDuckPrxyJavassist());
    }
    
    public void testObjectProxy(DuckPrxy prxy) {
        MyDelegate delegate = new MyDelegate();
        MyObjectOne objectOne = prxy.makeProxy(MyObjectOne.class, delegate);
        assertEquals("bar42", objectOne.foo(41));
    }

    public DuckPrxy getDuckPrxyJavassist() {
        return new JavassistDuckPrxy("duckprxy" + System.currentTimeMillis() + ":");
    }
}
