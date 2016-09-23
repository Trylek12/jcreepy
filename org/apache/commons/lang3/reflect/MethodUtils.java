
package org.apache.commons.lang3.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MemberUtils;


public class MethodUtils {
    public static /* varargs */ Object invokeMethod(Object object, String methodName, Object ... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (args == null) {
            args = ArrayUtils.EMPTY_OBJECT_ARRAY;
        }
        int arguments = args.length;
        Class[] parameterTypes = new Class[arguments];
        for (int i = 0; i < arguments; ++i) {
            parameterTypes[i] = args[i].getClass();
        }
        return MethodUtils.invokeMethod(object, methodName, args, parameterTypes);
    }

    public static Object invokeMethod(Object object, String methodName, Object[] args, Class<?>[] parameterTypes) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method;
        if (parameterTypes == null) {
            parameterTypes = ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        if (args == null) {
            args = ArrayUtils.EMPTY_OBJECT_ARRAY;
        }
        if ((method = MethodUtils.getMatchingAccessibleMethod(object.getClass(), methodName, parameterTypes)) == null) {
            throw new NoSuchMethodException("No such accessible method: " + methodName + "() on object: " + object.getClass().getName());
        }
        return method.invoke(object, args);
    }

    public static /* varargs */ Object invokeExactMethod(Object object, String methodName, Object ... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (args == null) {
            args = ArrayUtils.EMPTY_OBJECT_ARRAY;
        }
        int arguments = args.length;
        Class[] parameterTypes = new Class[arguments];
        for (int i = 0; i < arguments; ++i) {
            parameterTypes[i] = args[i].getClass();
        }
        return MethodUtils.invokeExactMethod(object, methodName, args, parameterTypes);
    }

    public static Object invokeExactMethod(Object object, String methodName, Object[] args, Class<?>[] parameterTypes) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method;
        if (args == null) {
            args = ArrayUtils.EMPTY_OBJECT_ARRAY;
        }
        if (parameterTypes == null) {
            parameterTypes = ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        if ((method = MethodUtils.getAccessibleMethod(object.getClass(), methodName, parameterTypes)) == null) {
            throw new NoSuchMethodException("No such accessible method: " + methodName + "() on object: " + object.getClass().getName());
        }
        return method.invoke(object, args);
    }

    public static Object invokeExactStaticMethod(Class<?> cls, String methodName, Object[] args, Class<?>[] parameterTypes) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method;
        if (args == null) {
            args = ArrayUtils.EMPTY_OBJECT_ARRAY;
        }
        if (parameterTypes == null) {
            parameterTypes = ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        if ((method = MethodUtils.getAccessibleMethod(cls, methodName, parameterTypes)) == null) {
            throw new NoSuchMethodException("No such accessible method: " + methodName + "() on class: " + cls.getName());
        }
        return method.invoke(null, args);
    }

    public static /* varargs */ Object invokeStaticMethod(Class<?> cls, String methodName, Object ... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (args == null) {
            args = ArrayUtils.EMPTY_OBJECT_ARRAY;
        }
        int arguments = args.length;
        Class[] parameterTypes = new Class[arguments];
        for (int i = 0; i < arguments; ++i) {
            parameterTypes[i] = args[i].getClass();
        }
        return MethodUtils.invokeStaticMethod(cls, methodName, args, parameterTypes);
    }

    public static Object invokeStaticMethod(Class<?> cls, String methodName, Object[] args, Class<?>[] parameterTypes) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method;
        if (parameterTypes == null) {
            parameterTypes = ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        if (args == null) {
            args = ArrayUtils.EMPTY_OBJECT_ARRAY;
        }
        if ((method = MethodUtils.getMatchingAccessibleMethod(cls, methodName, parameterTypes)) == null) {
            throw new NoSuchMethodException("No such accessible method: " + methodName + "() on class: " + cls.getName());
        }
        return method.invoke(null, args);
    }

    public static /* varargs */ Object invokeExactStaticMethod(Class<?> cls, String methodName, Object ... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (args == null) {
            args = ArrayUtils.EMPTY_OBJECT_ARRAY;
        }
        int arguments = args.length;
        Class[] parameterTypes = new Class[arguments];
        for (int i = 0; i < arguments; ++i) {
            parameterTypes[i] = args[i].getClass();
        }
        return MethodUtils.invokeExactStaticMethod(cls, methodName, args, parameterTypes);
    }

    public static /* varargs */ Method getAccessibleMethod(Class<?> cls, String methodName, Class<?> ... parameterTypes) {
        try {
            return MethodUtils.getAccessibleMethod(cls.getMethod(methodName, parameterTypes));
        }
        catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static Method getAccessibleMethod(Method method) {
        if (!MemberUtils.isAccessible(method)) {
            return null;
        }
        Class cls = method.getDeclaringClass();
        if (Modifier.isPublic(cls.getModifiers())) {
            return method;
        }
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        method = MethodUtils.getAccessibleMethodFromInterfaceNest(cls, methodName, parameterTypes);
        if (method == null) {
            method = MethodUtils.getAccessibleMethodFromSuperclass(cls, methodName, parameterTypes);
        }
        return method;
    }

    private static /* varargs */ Method getAccessibleMethodFromSuperclass(Class<?> cls, String methodName, Class<?> ... parameterTypes) {
        for (Class parentClass = cls.getSuperclass(); parentClass != null; parentClass = parentClass.getSuperclass()) {
            if (!Modifier.isPublic(parentClass.getModifiers())) continue;
            try {
                return parentClass.getMethod(methodName, parameterTypes);
            }
            catch (NoSuchMethodException e) {
                return null;
            }
        }
        return null;
    }

    private static /* varargs */ Method getAccessibleMethodFromInterfaceNest(Class<?> cls, String methodName, Class<?> ... parameterTypes) {
        Method method = null;
        while (cls != null) {
            Class<?>[] interfaces = cls.getInterfaces();
            for (int i = 0; i < interfaces.length; ++i) {
                if (!Modifier.isPublic(interfaces[i].getModifiers())) continue;
                try {
                    method = interfaces[i].getDeclaredMethod(methodName, parameterTypes);
                }
                catch (NoSuchMethodException e) {
                    // empty catch block
                }
                if (method != null || (method = MethodUtils.getAccessibleMethodFromInterfaceNest(interfaces[i], methodName, parameterTypes)) != null) break;
            }
            cls = cls.getSuperclass();
        }
        return method;
    }

    public static /* varargs */ Method getMatchingAccessibleMethod(Class<?> cls, String methodName, Class<?> ... parameterTypes) {
        try {
            Method method = cls.getMethod(methodName, parameterTypes);
            MemberUtils.setAccessibleWorkaround(method);
            return method;
        }
        catch (NoSuchMethodException e) {
            Method[] methods;
            Method bestMatch = null;
            for (Method method : methods = cls.getMethods()) {
                Method accessibleMethod;
                if (!method.getName().equals(methodName) || !ClassUtils.isAssignable(parameterTypes, method.getParameterTypes(), true) || (accessibleMethod = MethodUtils.getAccessibleMethod(method)) == null || bestMatch != null && MemberUtils.compareParameterTypes(accessibleMethod.getParameterTypes(), bestMatch.getParameterTypes(), parameterTypes) >= 0) continue;
                bestMatch = accessibleMethod;
            }
            if (bestMatch != null) {
                MemberUtils.setAccessibleWorkaround(bestMatch);
            }
            return bestMatch;
        }
    }
}

