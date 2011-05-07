# duckprxy

## What is it
Duck typing is a style of dynamic typing where the type of an object
is determined by its set of methods rather than by a defined class
it belongs to.

Java does not allow duck typing out of the box. This library helps
to overcome this limitation. Given an arbitrary object and an interface,
this library lets you get an object that implements the interface
by delegating to the object.

## Project Site
More information can be found in the
[duckprxy project's site](http://mr-co.de/projects/duckprxy/site/) (generated
with [Maven](http://maven.apache.org/). Inter alia
[duckprxy's api doc](http://mr-co.de/projects/duckprxy/site/apidocs/).

## Example
Let us assume we have to implement an interface (`MyInterface`),
but only want to implement the method `bar()`, because
the other methods will not be called. This can be done like this:

    interface MyInterface {
        void foo();
        int bar(int x, int y);
        int baz(int x);
    }

    public class Delegate {
        public int bar() {
        return 42;
        }
    }

Now we can create an object that implements `MyInterface`:

    DuckPrxy duckProxy = new DuckPrxyImpl();
    Delegate delegate = new Delegate();
    MyInterface prxy = duckProxy.makeProxy(MyInterface.class, delegate);
    prxy.bar(2, 3); // Will return 42.

Note: the `DuckPrxy` implementations are thread safe, so
one instance can be safely shared / injected. The created proxies
are thread safe iff the delegate is thread safe.

There is one limitation though: You can use anonymous classes for
the delegate, but the methods called via duck typing must be made
visible by implementing an interface or by extending a class that
has these methods. Otherwise Java's reflection API will not be able
to see the methods.
