
package org.apache.commons.lang3.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MemberUtils;


public class ConstructorUtils {
    public static /* varargs */ <T> T invokeConstructor(Class<T> cls, Object ... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (args == null) {
            args = ArrayUtils.EMPTY_OBJECT_ARRAY;
        }
        Class[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; ++i) {
            parameterTypes[i] = args[i].getClass();
        }
        return ConstructorUtils.invokeConstructor(cls, args, parameterTypes);
    }

    public static <T> T invokeConstructor(Class<T> cls, Object[] args, Class<?>[] parameterTypes) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<T> ctor;
        if (parameterTypes == null) {
            parameterTypes = ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        if (args == null) {
            args = ArrayUtils.EMPTY_OBJECT_ARRAY;
        }
        if ((ctor = ConstructorUtils.getMatchingAccessibleConstructor(cls, parameterTypes)) == null) {
            throw new NoSuchMethodException("No such accessible constructor on object: " + cls.getName());
        }
        return ctor.newInstance(args);
    }

    public static /* varargs */ <T> T invokeExactConstructor(Class<T> cls, Object ... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (args == null) {
            args = ArrayUtils.EMPTY_OBJECT_ARRAY;
        }
        int arguments = args.length;
        Class[] parameterTypes = new Class[arguments];
        for (int i = 0; i < arguments; ++i) {
            parameterTypes[i] = args[i].getClass();
        }
        return ConstructorUtils.invokeExactConstructor(cls, args, parameterTypes);
    }

    public static <T> T invokeExactConstructor(Class<T> cls, Object[] args, Class<?>[] parameterTypes) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<T> ctor;
        if (args == null) {
            args = ArrayUtils.EMPTY_OBJECT_ARRAY;
        }
        if (parameterTypes == null) {
            parameterTypes = ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        if ((ctor = ConstructorUtils.getAccessibleConstructor(cls, parameterTypes)) == null) {
            throw new NoSuchMethodException("No such accessible constructor on object: " + cls.getName());
        }
        return ctor.newInstance(args);
    }

    public static /* varargs */ <T> Constructor<T> getAccessibleConstructor(Class<T> cls, Class<?> ... parameterTypes) {
        try {
            return ConstructorUtils.getAccessibleConstructor(cls.getConstructor(parameterTypes));
        }
        catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static <T> Constructor<T> getAccessibleConstructor(Constructor<T> ctor) {
        return MemberUtils.isAccessible(ctor) && Modifier.isPublic(ctor.getDeclaringClass().getModifiers()) ? ctor : null;
    }

    public static /* varargs */ <T> Constructor<T> getMatchingAccessibleConstructor(Class<T> cls, Class<?> ... parameterTypes) {
        try {
            Constructor<T> ctor = cls.getConstructor(parameterTypes);
            MemberUtils.setAccessibleWorkaround(ctor);
            return ctor;
        }
        catch (NoSuchMethodException e) {
            Constructor<?>[] ctors;
            Constructor result = null;
            for (Constructor ctor : ctors = cls.getConstructors()) {
                Constructor constructor;
                if (!ClassUtils.isAssignable(parameterTypes, ctor.getParameterTypes(), true) || (ctor = ConstructorUtils.getAccessibleConstructor(ctor)) == null) continue;
                MemberUtils.setAccessibleWorkaround(ctor);
                if (result != null && MemberUtils.compareParameterTypes(ctor.getParameterTypes(), result.getParameterTypes(), parameterTypes) >= 0) continue;
                result = constructor = ctor;
            }
            return result;
        }
    }
}

