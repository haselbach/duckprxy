package mrcode.duckprxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import mrcode.duckprxy.impl.DuckPrxyImpl;

import junit.framework.TestCase;

public class DuckPrxyTest extends TestCase{
    
    interface MyInterfaceOne {
        void foo();
        int bar(int x, int y);
        int baz();
        int myfoo();
        void mybar(String s);
    }
    
    interface MyInterfaceTwo {
        void fooTwo(String s);
        int barTwo(int x, int y);
        int bazTwo(String s, String t);
    }
    
    public static class DelegateOne {
        final List<String> callRecord = new ArrayList<String>();
        public void foo() {
            callRecord.add("Foo");
        }
        public int baz() {
            return 42;
        }
        public int fooTwo(final String s) {
            callRecord.add("Foo: " + s);
            return s.length();
        }
        public int barTwo() {
            return 23;
        }
        public List<String> getCallRecord() {
            return callRecord;
        }
    }
    
    public static class DelegateTwo {
        final List<String> callRecord = new ArrayList<String>();
        @DuckMethod("foo.*")
        public int fooMethod() {
            callRecord.add("Foo");
            return 5;
        }
        @DuckMethod(".*bar.*")
        public int barMethod(
                @DuckArg(DuckArgType.NAME) String name,
                @DuckArg(DuckArgType.ARGS) Object[] args) {
            StringBuilder builder = new StringBuilder("-").append(name)
            .append("/");
            if (args != null) {
                for(Object arg : args) {
                    builder.append(arg.toString()).append("/");
                }
            }
            callRecord.add(builder.toString());
            return 6;
        }
        public int bazTwo(
                @DuckArg(value = DuckArgType.ARGN, pos=1) String s1,
                @DuckArg(value = DuckArgType.ARGN, pos=0) String s2) {
            callRecord.add("baz(" + s1 + "," + s2 + ")");
            return 7;
        }
        public List<String> getCallRecord() {
            return callRecord;
        }
    }
    
    @Test
    public void testPrxyImpl() {
        testPrxy(getDuckPrxy());
    }
    
    @Test
    public void testPrxyImplMissingMethod() {
        testPrxyMissingMethod(getDuckPrxy());
    }
    
    @Test
    public void testPrxyWithPatterns() {
        testPrxyWithPatterns(getDuckPrxy());
    }
    
    public void testPrxy(DuckPrxy duckPrxy) {
        final DelegateOne delegate = new DelegateOne();
        final MyInterfaceOne proxy = duckPrxy.makeProxy(
                MyInterfaceOne.class, delegate, MyInterfaceTwo.class);
        proxy.foo();
        assertEquals(
                Arrays.asList(new String[] {"Foo"}),
                delegate.getCallRecord());
        assertEquals(42, proxy.baz());
        
        MyInterfaceTwo proxyAsInterfaceTwo = (MyInterfaceTwo) proxy;
        assertEquals(23, proxyAsInterfaceTwo.barTwo(4, 5));
        proxyAsInterfaceTwo.fooTwo("abc");
        assertEquals(
                Arrays.asList(new String[] {"Foo", "Foo: abc"}),
                delegate.getCallRecord());
    }
    
    public void testPrxyMissingMethod(DuckPrxy duckPrxy) {
        final DelegateOne delegate = new DelegateOne();
        final MyInterfaceOne proxy = duckPrxy.makeProxy(
                MyInterfaceOne.class, delegate, MyInterfaceTwo.class);
        try {
            proxy.bar(2, 3);
            fail();
        } catch (UndeclaredThrowableException ute) {
            InvocationTargetException ite =
                (InvocationTargetException) ute.getCause();
            assertEquals(
                    NoSuchMethodException.class,
                    ite.getCause().getClass());
        } catch (Exception e) {
            fail();
        }
    }
    
    public void testPrxyWithPatterns(DuckPrxy duckPrxy) {
        final DelegateTwo delegate = new DelegateTwo();
        final MyInterfaceOne proxy = duckPrxy.makeProxy(
                MyInterfaceOne.class, delegate, MyInterfaceTwo.class);
        proxy.foo();
        try {
            proxy.myfoo();
        } catch (UndeclaredThrowableException ute) {
            InvocationTargetException ite =
                (InvocationTargetException) ute.getCause();
            assertEquals(
                    NoSuchMethodException.class,
                    ite.getCause().getClass());
        } catch (Exception e) {
            fail();
        }
        MyInterfaceTwo proxyAsInterfaceTwo = (MyInterfaceTwo) proxy;
        proxyAsInterfaceTwo.fooTwo("bar");
        
        assertEquals(6, proxy.bar(2, 3));
        proxy.mybar("abc");
        assertEquals(6, proxyAsInterfaceTwo.barTwo(4, 2));
        
        assertEquals(7, proxyAsInterfaceTwo.bazTwo("DEF", "ABC"));
        
        assertEquals(
                Arrays.asList(new String[] {"Foo", "Foo",
                        "-bar/2/3/", "-mybar/abc/", "-barTwo/4/2/",
                        "baz(ABC,DEF)"}),
                delegate.getCallRecord());
    }
    
    public DuckPrxy getDuckPrxy() {
        return new DuckPrxyImpl();
    }

}
