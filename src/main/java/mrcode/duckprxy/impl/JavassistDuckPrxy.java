package mrcode.duckprxy.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import mrcode.duckprxy.DuckArg;
import mrcode.duckprxy.DuckMethod;
import mrcode.duckprxy.DuckPrxy;
import mrcode.duckprxy.impl.AbstractPrxyInvocationHandler.DelegateClassInformation;
import mrcode.duckprxy.impl.MethodUtils.MethodRetrieveStrategy;
import mrcode.duckprxy.util.ObjectUtil;

/**
 * Javassist implementation of DuckPrxy.
 * A new class is generated for a proxy which will delegate
 * calls to the provided delegate object.
 * No runtime reflection is used, making it very efficient.
 * Also, it can implement classes, not just interfaces.
 * However, there are some down-sides:
 * <ul>
 * <li> Some JVMs cannot garbage collect classes. In such JVM every
 * javassist duck proxy instance will leak memory.
 * <li> Javassist cannot handle inner interfaces and classes. Hence,
 * javassist duck proxies cannot implement inner interfaces
 * (and inner classes, but the reflection based implementation
 * cannot implement classes at all).
 * </ul>
 * There are some rough edges: The proxy creator assumes that the delegate
 * methods have a appropriate parameter and return types. This is not checked.
 * You will probably get strange javassist or class loading exceptions
 * when this assumption is not met.
 */
public class JavassistDuckPrxy implements DuckPrxy {
    
    /**
     * Method missing body simulates the behavior of throwing
     * NoSuchMethodExcpetion in an invocation handler.
     */
    private static final String METHOD_MISSING_BODY =
        "throw new java.lang.reflect.UndeclaredThrowableException(" +
        "new java.lang.reflect.InvocationTargetException(" +
        "new NoSuchMethodException()));";
    
    /**
     * Mapping to hold classes associated with their name.
     */
    private Map<String, Class<?>> classMap = new HashMap<String, Class<?>>();
    
    private final String classNamePrefix;

    public JavassistDuckPrxy() {
        this("duckprxy:");
    }
    
    public JavassistDuckPrxy(String classNamePrefix) {
        this.classNamePrefix = classNamePrefix;
    }

    public <T> T makeProxy(
            final Class<T> mainInterface,
            final Object delegate,
            final Class<?>... interfaces) {
        final Class<?> delegateClass = delegate.getClass();
        final List<Class<?>> allInterfaces =
            new ArrayList<Class<?>>(interfaces.length + 1);
        allInterfaces.add(mainInterface);
        allInterfaces.addAll(Arrays.asList(interfaces));
        final ClassPool classPool = ClassPool.getDefault();
        final String className = createClassName(delegateClass, allInterfaces);
        try {
            @SuppressWarnings("unchecked")
            final Class<T> proxyClass = (Class<T>) getProxyClass(
                    className,
                    delegate,
                    delegateClass,
                    allInterfaces,
                    classPool);
            final T result = proxyClass.newInstance();
            result.getClass().getField("delegate").set(result, delegate);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    public Class<?> getProxyClass(
            final String className,
            final Object delegate,
            final Class<?> delegateClass,
            final List<Class<?>> allInterfaces,
            final ClassPool classPool)
    throws CannotCompileException, NotFoundException {
        if (classMap.containsKey(className)) {
            return classMap.get(className);
        }
        final CtClass ctClass = createCtClass(
                className,
                delegate,
                delegateClass,
                allInterfaces,
                classPool);
        Class<?> proxyClass = ctClass.toClass();
        classMap.put(className, proxyClass);
        return proxyClass;
    }

    private CtClass createCtClass(
            final String className,
            final Object delegate,
            final Class<?> delegateClass,
            final List<Class<?>> allInterfaces,
            final ClassPool classPool)
    throws CannotCompileException, NotFoundException {
        final DelegateClassInformation info =
            getDelegateClassInformation(delegateClass);
        final List<MethodRetrieveStrategy> strategies =
            makeStrategies(delegateClass, info);
        final CtClass ctClass = classPool.makeClass(className);
        final CtField delegateField = new CtField(classPool.get(
                delegate.getClass().getName()),
                "delegate",
                ctClass);
        delegateField.setModifiers(Modifier.PUBLIC);
        ctClass.addField(delegateField);
        for (final Class<?> interfce : allInterfaces) {
            addInterface(
                    classPool,
                    ctClass,
                    interfce,
                    delegateClass,
                    strategies,
                    info.subDelegateGetter);
        }
        final CtConstructor ctConstructor =
            new CtConstructor(new CtClass[0], ctClass);
        ctConstructor.setBody(null);
        ctClass.addConstructor(ctConstructor);
        return ctClass;
    }
    
    protected String createClassName(
            final Class<?> delegateClass,
            final Collection<Class<?>> interfaces) {
        final StringBuilder builder = new StringBuilder(classNamePrefix)
        .append(delegateClass.getName());
        for (final Class<?> intrfce : interfaces) {
            builder.append("+").append(intrfce.getName());
        }
        return builder.toString();
    }
    
    protected void addInterface(
            final ClassPool classPool,
            final CtClass ctClass,
            final Class<?> interfce,
            final Class<?> delegateClass,
            final List<MethodRetrieveStrategy> strategies,
            final Method subDelegateGetter)
    throws NotFoundException, CannotCompileException {
        ctClass.addInterface(classPool.get(interfce.getName()));
        for (final Method method : interfce.getMethods()) {
            addMethod(
                    classPool,
                    ctClass,
                    method,
                    delegateClass,
                    strategies,
                    subDelegateGetter);
        }
    }
    
    protected void addMethod(
            final ClassPool classPool,
            final CtClass ctClass,
            final Method method,
            final Class<?> delegateClass,
            final List<MethodRetrieveStrategy> strategies,
            final Method subDelegateGetter)
    throws NotFoundException, CannotCompileException {
        final String methodName = method.getName();
        CtMethod ctMethod = new CtMethod(
                getCtClass(classPool, method.getReturnType()),
                methodName,
                getCtClass(classPool, method.getParameterTypes()),
                ctClass);
        final StringBuilder delegatePath = new StringBuilder("delegate.");
        final Method delegateMethod = getMethodDelegate(
                method,
                delegateClass,
                strategies, subDelegateGetter,
                delegatePath);
        final StringBuilder body = new StringBuilder();
        if (delegateMethod != null) {
            final Class<?> returnType = method.getReturnType();
            final Class<?> delegateReturnType =
                delegateMethod.getReturnType();
            final String delegateCall = delegatePath.toString() +
            getDelegateCall(
                    methodName,
                    delegateMethod,
                    method.getParameterTypes().length);
            body.append("{\n    ");
            if ("void".equals(delegateReturnType.getName())) {
                body.append(delegateCall).append(";\n");
                if (!"void".equals(returnType.getName())) {
                    body.append("    return null;\n");
                }
            } else {
                if (!"void".equals(returnType.getName())) {
                    body.append("return ");
                }
                body.append(delegateCall).append(";\n");
            }
            body.append("}");
        } else {
            body.append(METHOD_MISSING_BODY);
        }
        ctMethod.setBody(body.toString());
        ctClass.addMethod(ctMethod);
    }
    
    protected Method getMethodDelegate(
            final Method method,
            final Class<?> delegateClass,
            final List<MethodRetrieveStrategy> strategies,
            final Method subDelegateGetter,
            final StringBuilder delegatePath) {
        final Method delegateMethod = getDelegateMethod(
                strategies,
                method.getName(),
                method.getParameterTypes());
        if (delegateMethod != null) {
            return delegateMethod;
        }
        if (subDelegateGetter == null) {
            return null;
        }
        final Class<?> subDelegateClass = subDelegateGetter.getReturnType();
        final DelegateClassInformation info =
            getDelegateClassInformation(subDelegateClass);
        delegatePath.append(subDelegateGetter.getName()).append("().");
        return getMethodDelegate(
                method,
                subDelegateClass,
                makeStrategies(subDelegateClass, info),
                info.subDelegateGetter,
                delegatePath);
    }

    protected String getDelegateCall(
            final String name,
            final Method delegateMethod,
            final int argsLen) {
        final Annotation[][] annotations =
            delegateMethod.getParameterAnnotations();
        final int len = annotations.length; 
        int argIndex = 0;
        final StringBuilder delegateArgs =
            new StringBuilder().append(delegateMethod.getName())
            .append("(");
        for (int annoIndex = 0; annoIndex < len; annoIndex++) {
            if (annoIndex > 0) {
                delegateArgs.append(", ");
            }
            final DuckArg duckArg = getDuckArg(annotations[annoIndex]);
            if (duckArg == null) {
                if (argIndex < argsLen) {
                    argIndex++;
                    delegateArgs.append("$").append(argIndex);
                }
            } else {
                switch (duckArg.value()) {
                    case NULL:
                        delegateArgs.append("null");
                        break;
                    case NAME:
                        delegateArgs.append("\"").append(name).append("\"");
                        break;
                    case ARGS:
                        appendPutArgumentsIntoArray(argsLen, delegateArgs);
                        break;
                    case ARGN:
                        if (duckArg.pos() < argsLen) {
                            delegateArgs.append("$")
                            .append(duckArg.pos() + 1);
                        }
                        break;
                }
            }
        }
        delegateArgs.append(")");
        return delegateArgs.toString();
    }

    private void appendPutArgumentsIntoArray(
            final int argsLen,
            final StringBuilder delegateArgs) {
        delegateArgs.append("new Object[] {");
        for (int i = 1; i <= argsLen; i++) {
            if (i > 1) {
                delegateArgs.append(", ");
            }
            // TODO: Converting to object is only needed for primitives.
            delegateArgs.append(ObjectUtil.class.getName())
            .append(".toObject($").append(i).append(")");
        }
        delegateArgs.append("}");
    }
    
    public DuckArg getDuckArg(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (DuckArg.class.equals(annotation.annotationType())) {
                return (DuckArg) annotation;
            }
        }
        return null;
    }
    
    protected CtClass getCtClass(
            final ClassPool classPool,
            final Class<?> clazz)
    throws NotFoundException {
        if (clazz == null) {
            return null;
        }
        return classPool.get(clazz.getName());
    }
    
    protected CtClass[] getCtClass(
            final ClassPool classPool,
            final Class<?>[] classes)
    throws NotFoundException {
        CtClass[] ctClasses = new CtClass[classes.length];
        for (int i=0; i<classes.length; i++) {
            ctClasses[i] = classPool.get(classes[i].getName());
        }
        return ctClasses;
    }

    protected List<MethodRetrieveStrategy> makeStrategies(
            final Class<?> delegateClass,
            final DelegateClassInformation info) {
        final List<MethodRetrieveStrategy> strategies =
            new ArrayList<MethodRetrieveStrategy>();
        strategies.add(MethodUtils.methodByNameAndArgsStrategy(delegateClass));
        strategies.add(MethodUtils.methodByNameWithoutArgsStrategy(
                delegateClass));
        strategies.add(MethodUtils.methodByPatternStrategy(
                delegateClass, info.patternMap));
        if (info.fallbackMethod != null) {
            strategies.add(MethodUtils.defaultMethodStrategy(
                    info.fallbackMethod));
        }
        return strategies;
    }

    protected DelegateClassInformation getDelegateClassInformation(
            final Class<?> delegateClass) {
        Map<Pattern, Method> patternMap = new HashMap<Pattern, Method>();
        Method fallbackMethod = null;
        Method subDelegateGetter = null;
        for (final Method method : delegateClass.getMethods()) {
            final DuckMethod duckMethod =
                method.getAnnotation(DuckMethod.class);
            if (duckMethod != null) {
                for (final String value : duckMethod.value()) {
                    patternMap.put(Pattern.compile(value), method);
                }
                if (duckMethod.fallback()) {
                    fallbackMethod = method;
                }
                if (duckMethod.subdelegate()) {
                    subDelegateGetter = method;
                }
            }
        }
        return new DelegateClassInformation(
                fallbackMethod, subDelegateGetter, patternMap);
    }
    
    private static Method getDelegateMethod(
            final List<MethodRetrieveStrategy> strategies,
            final String name,
            final Class<?>[] parameterTypes) {
        Method delegateMethod = null;
        for(MethodRetrieveStrategy strategy : strategies) {
            try {
                delegateMethod = strategy.getMethod(name, parameterTypes);
            } catch (Exception e) {
                delegateMethod = null;
            }
            if (delegateMethod != null) {
                break;
            }
        }
        return delegateMethod;
    }

}
