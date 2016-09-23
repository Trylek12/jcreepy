
package org.apache.commons.lang3.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ClassUtils;


public class TypeUtils {
    public static boolean isAssignable(Type type, Type toType) {
        return TypeUtils.isAssignable(type, toType, null);
    }

    private static boolean isAssignable(Type type, Type toType, Map<TypeVariable<?>, Type> typeVarAssigns) {
        if (toType == null || toType instanceof Class) {
            return TypeUtils.isAssignable(type, (Class)toType);
        }
        if (toType instanceof ParameterizedType) {
            return TypeUtils.isAssignable(type, (ParameterizedType)toType, typeVarAssigns);
        }
        if (toType instanceof GenericArrayType) {
            return TypeUtils.isAssignable(type, (GenericArrayType)toType, typeVarAssigns);
        }
        if (toType instanceof WildcardType) {
            return TypeUtils.isAssignable(type, (WildcardType)toType, typeVarAssigns);
        }
        if (toType instanceof TypeVariable) {
            return TypeUtils.isAssignable(type, (TypeVariable)toType, typeVarAssigns);
        }
        throw new IllegalStateException("found an unhandled type: " + toType);
    }

    private static boolean isAssignable(Type type, Class<?> toClass) {
        if (type == null) {
            return toClass == null || !toClass.isPrimitive();
        }
        if (toClass == null) {
            return false;
        }
        if (toClass.equals(type)) {
            return true;
        }
        if (type instanceof Class) {
            return ClassUtils.isAssignable((Class)type, toClass);
        }
        if (type instanceof ParameterizedType) {
            return TypeUtils.isAssignable(TypeUtils.getRawType((ParameterizedType)type), toClass);
        }
        if (type instanceof TypeVariable) {
            for (Type bound : ((TypeVariable)type).getBounds()) {
                if (!TypeUtils.isAssignable(bound, toClass)) continue;
                return true;
            }
            return false;
        }
        if (type instanceof GenericArrayType) {
            return toClass.equals(Object.class) || toClass.isArray() && TypeUtils.isAssignable(((GenericArrayType)type).getGenericComponentType(), toClass.getComponentType());
        }
        if (type instanceof WildcardType) {
            return false;
        }
        throw new IllegalStateException("found an unhandled type: " + type);
    }

    private static boolean isAssignable(Type type, ParameterizedType toParameterizedType, Map<TypeVariable<?>, Type> typeVarAssigns) {
        if (type == null) {
            return true;
        }
        if (toParameterizedType == null) {
            return false;
        }
        if (toParameterizedType.equals(type)) {
            return true;
        }
        Class toClass = TypeUtils.getRawType(toParameterizedType);
        Map fromTypeVarAssigns = TypeUtils.getTypeArguments(type, toClass, null);
        if (fromTypeVarAssigns == null) {
            return false;
        }
        if (fromTypeVarAssigns.isEmpty()) {
            return true;
        }
        Map toTypeVarAssigns = TypeUtils.getTypeArguments(toParameterizedType, toClass, typeVarAssigns);
        for (Map.Entry entry : toTypeVarAssigns.entrySet()) {
            Type toTypeArg = entry.getValue();
            Type fromTypeArg = fromTypeVarAssigns.get(entry.getKey());
            if (fromTypeArg == null || toTypeArg.equals(fromTypeArg) || toTypeArg instanceof WildcardType && TypeUtils.isAssignable(fromTypeArg, toTypeArg, typeVarAssigns)) continue;
            return false;
        }
        return true;
    }

    private static boolean isAssignable(Type type, GenericArrayType toGenericArrayType, Map<TypeVariable<?>, Type> typeVarAssigns) {
        if (type == null) {
            return true;
        }
        if (toGenericArrayType == null) {
            return false;
        }
        if (toGenericArrayType.equals(type)) {
            return true;
        }
        Type toComponentType = toGenericArrayType.getGenericComponentType();
        if (type instanceof Class) {
            Class cls = (Class)type;
            return cls.isArray() && TypeUtils.isAssignable(cls.getComponentType(), toComponentType, typeVarAssigns);
        }
        if (type instanceof GenericArrayType) {
            return TypeUtils.isAssignable(((GenericArrayType)type).getGenericComponentType(), toComponentType, typeVarAssigns);
        }
        if (type instanceof WildcardType) {
            for (Type bound : TypeUtils.getImplicitUpperBounds((WildcardType)type)) {
                if (!TypeUtils.isAssignable(bound, toGenericArrayType)) continue;
                return true;
            }
            return false;
        }
        if (type instanceof TypeVariable) {
            for (Type bound : TypeUtils.getImplicitBounds((TypeVariable)type)) {
                if (!TypeUtils.isAssignable(bound, toGenericArrayType)) continue;
                return true;
            }
            return false;
        }
        if (type instanceof ParameterizedType) {
            return false;
        }
        throw new IllegalStateException("found an unhandled type: " + type);
    }

    private static boolean isAssignable(Type type, WildcardType toWildcardType, Map<TypeVariable<?>, Type> typeVarAssigns) {
        if (type == null) {
            return true;
        }
        if (toWildcardType == null) {
            return false;
        }
        if (toWildcardType.equals(type)) {
            return true;
        }
        Type[] toUpperBounds = TypeUtils.getImplicitUpperBounds(toWildcardType);
        Type[] toLowerBounds = TypeUtils.getImplicitLowerBounds(toWildcardType);
        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType)type;
            Type[] upperBounds = TypeUtils.getImplicitUpperBounds(wildcardType);
            Type[] lowerBounds = TypeUtils.getImplicitLowerBounds(wildcardType);
            for (Type toBound2 : toUpperBounds) {
                toBound2 = TypeUtils.substituteTypeVariables(toBound2, typeVarAssigns);
                for (Type bound : upperBounds) {
                    if (TypeUtils.isAssignable(bound, toBound2, typeVarAssigns)) continue;
                    return false;
                }
            }
            for (Type toBound2 : toLowerBounds) {
                toBound2 = TypeUtils.substituteTypeVariables(toBound2, typeVarAssigns);
                for (Type bound : lowerBounds) {
                    if (TypeUtils.isAssignable(toBound2, bound, typeVarAssigns)) continue;
                    return false;
                }
            }
            return true;
        }
        for (Type toBound3 : toUpperBounds) {
            if (TypeUtils.isAssignable(type, TypeUtils.substituteTypeVariables(toBound3, typeVarAssigns), typeVarAssigns)) continue;
            return false;
        }
        for (Type toBound3 : toLowerBounds) {
            if (TypeUtils.isAssignable(TypeUtils.substituteTypeVariables(toBound3, typeVarAssigns), type, typeVarAssigns)) continue;
            return false;
        }
        return true;
    }

    private static boolean isAssignable(Type type, TypeVariable<?> toTypeVariable, Map<TypeVariable<?>, Type> typeVarAssigns) {
        if (type == null) {
            return true;
        }
        if (toTypeVariable == null) {
            return false;
        }
        if (toTypeVariable.equals(type)) {
            return true;
        }
        if (type instanceof TypeVariable) {
            Type[] bounds;
            for (Type bound : bounds = TypeUtils.getImplicitBounds((TypeVariable)type)) {
                if (!TypeUtils.isAssignable(bound, toTypeVariable, typeVarAssigns)) continue;
                return true;
            }
        }
        if (type instanceof Class || type instanceof ParameterizedType || type instanceof GenericArrayType || type instanceof WildcardType) {
            return false;
        }
        throw new IllegalStateException("found an unhandled type: " + type);
    }

    private static Type substituteTypeVariables(Type type, Map<TypeVariable<?>, Type> typeVarAssigns) {
        if (type instanceof TypeVariable && typeVarAssigns != null) {
            Type replacementType = typeVarAssigns.get(type);
            if (replacementType == null) {
                throw new IllegalArgumentException("missing assignment type for type variable " + type);
            }
            return replacementType;
        }
        return type;
    }

    public static Map<TypeVariable<?>, Type> getTypeArguments(ParameterizedType type) {
        return TypeUtils.getTypeArguments(type, TypeUtils.getRawType(type), null);
    }

    public static Map<TypeVariable<?>, Type> getTypeArguments(Type type, Class<?> toClass) {
        return TypeUtils.getTypeArguments(type, toClass, null);
    }

    private static Map<TypeVariable<?>, Type> getTypeArguments(Type type, Class<?> toClass, Map<TypeVariable<?>, Type> subtypeVarAssigns) {
        if (type instanceof Class) {
            return TypeUtils.getTypeArguments((Class)type, toClass, subtypeVarAssigns);
        }
        if (type instanceof ParameterizedType) {
            return TypeUtils.getTypeArguments((ParameterizedType)type, toClass, subtypeVarAssigns);
        }
        if (type instanceof GenericArrayType) {
            return TypeUtils.getTypeArguments(((GenericArrayType)type).getGenericComponentType(), toClass.isArray() ? toClass.getComponentType() : toClass, subtypeVarAssigns);
        }
        if (type instanceof WildcardType) {
            for (Type bound : TypeUtils.getImplicitUpperBounds((WildcardType)type)) {
                if (!TypeUtils.isAssignable(bound, toClass)) continue;
                return TypeUtils.getTypeArguments(bound, toClass, subtypeVarAssigns);
            }
            return null;
        }
        if (type instanceof TypeVariable) {
            for (Type bound : TypeUtils.getImplicitBounds((TypeVariable)type)) {
                if (!TypeUtils.isAssignable(bound, toClass)) continue;
                return TypeUtils.getTypeArguments(bound, toClass, subtypeVarAssigns);
            }
            return null;
        }
        throw new IllegalStateException("found an unhandled type: " + type);
    }

    private static Map<TypeVariable<?>, Type> getTypeArguments(ParameterizedType parameterizedType, Class<?> toClass, Map<TypeVariable<?>, Type> subtypeVarAssigns) {
        HashMap typeVarAssigns;
        Class cls = TypeUtils.getRawType(parameterizedType);
        if (!TypeUtils.isAssignable(cls, toClass)) {
            return null;
        }
        Type ownerType = parameterizedType.getOwnerType();
        if (ownerType instanceof ParameterizedType) {
            ParameterizedType parameterizedOwnerType = (ParameterizedType)ownerType;
            typeVarAssigns = TypeUtils.getTypeArguments(parameterizedOwnerType, TypeUtils.getRawType(parameterizedOwnerType), subtypeVarAssigns);
        } else {
            typeVarAssigns = subtypeVarAssigns == null ? new HashMap() : new HashMap(subtypeVarAssigns);
        }
        Type[] typeArgs = parameterizedType.getActualTypeArguments();
        TypeVariable<Class<?>>[] typeParams = cls.getTypeParameters();
        for (int i = 0; i < typeParams.length; ++i) {
            Type typeArg = typeArgs[i];
            typeVarAssigns.put(typeParams[i], typeVarAssigns.containsKey(typeArg) ? typeVarAssigns.get(typeArg) : typeArg);
        }
        if (toClass.equals(cls)) {
            return typeVarAssigns;
        }
        return TypeUtils.getTypeArguments(TypeUtils.getClosestParentType(cls, toClass), toClass, typeVarAssigns);
    }

    private static Map<TypeVariable<?>, Type> getTypeArguments(Class<?> cls, Class<?> toClass, Map<TypeVariable<?>, Type> subtypeVarAssigns) {
        HashMap typeVarAssigns;
        if (!TypeUtils.isAssignable(cls, toClass)) {
            return null;
        }
        if (cls.isPrimitive()) {
            if (toClass.isPrimitive()) {
                return new HashMap();
            }
            cls = ClassUtils.primitiveToWrapper(cls);
        }
        HashMap hashMap = typeVarAssigns = subtypeVarAssigns == null ? new HashMap() : new HashMap(subtypeVarAssigns);
        if (cls.getTypeParameters().length > 0 || toClass.equals(cls)) {
            return typeVarAssigns;
        }
        return TypeUtils.getTypeArguments(TypeUtils.getClosestParentType(cls, toClass), toClass, typeVarAssigns);
    }

    public static Map<TypeVariable<?>, Type> determineTypeArguments(Class<?> cls, ParameterizedType superType) {
        Class superClass = TypeUtils.getRawType(superType);
        if (!TypeUtils.isAssignable(cls, superClass)) {
            return null;
        }
        if (cls.equals(superClass)) {
            return TypeUtils.getTypeArguments(superType, superClass, null);
        }
        Type midType = TypeUtils.getClosestParentType(cls, superClass);
        if (midType instanceof Class) {
            return TypeUtils.determineTypeArguments((Class)midType, superType);
        }
        ParameterizedType midParameterizedType = (ParameterizedType)midType;
        Class midClass = TypeUtils.getRawType(midParameterizedType);
        Map typeVarAssigns = TypeUtils.determineTypeArguments(midClass, superType);
        TypeUtils.mapTypeVariablesToArguments(cls, midParameterizedType, typeVarAssigns);
        return typeVarAssigns;
    }

    private static <T> void mapTypeVariablesToArguments(Class<T> cls, ParameterizedType parameterizedType, Map<TypeVariable<?>, Type> typeVarAssigns) {
        Type ownerType = parameterizedType.getOwnerType();
        if (ownerType instanceof ParameterizedType) {
            TypeUtils.mapTypeVariablesToArguments(cls, (ParameterizedType)ownerType, typeVarAssigns);
        }
        Type[] typeArgs = parameterizedType.getActualTypeArguments();
        TypeVariable<Class<?>>[] typeVars = TypeUtils.getRawType(parameterizedType).getTypeParameters();
        List<TypeVariable<Class<T>>> typeVarList = Arrays.asList(cls.getTypeParameters());
        for (int i = 0; i < typeArgs.length; ++i) {
            TypeVariable typeVar = typeVars[i];
            Type typeArg = typeArgs[i];
            if (!typeVarList.contains(typeArg) || !typeVarAssigns.containsKey(typeVar)) continue;
            typeVarAssigns.put((TypeVariable)typeArg, typeVarAssigns.get(typeVar));
        }
    }

    private static Type getClosestParentType(Class<?> cls, Class<?> superClass) {
        if (superClass.isInterface()) {
            Type[] interfaceTypes = cls.getGenericInterfaces();
            Type genericInterface = null;
            for (Type midType : interfaceTypes) {
                Class midClass = null;
                if (midType instanceof ParameterizedType) {
                    midClass = TypeUtils.getRawType((ParameterizedType)midType);
                } else if (midType instanceof Class) {
                    midClass = (Class)midType;
                } else {
                    throw new IllegalStateException("Unexpected generic interface type found: " + midType);
                }
                if (!TypeUtils.isAssignable((Type)midClass, superClass) || !TypeUtils.isAssignable(genericInterface, (Type)midClass)) continue;
                genericInterface = midType;
            }
            if (genericInterface != null) {
                return genericInterface;
            }
        }
        return cls.getGenericSuperclass();
    }

    public static boolean isInstance(Object value, Type type) {
        if (type == null) {
            return false;
        }
        return value == null ? !(type instanceof Class) || !((Class)type).isPrimitive() : TypeUtils.isAssignable(value.getClass(), type, null);
    }

    public static Type[] normalizeUpperBounds(Type[] bounds) {
        if (bounds.length < 2) {
            return bounds;
        }
        HashSet<Type> types = new HashSet<Type>(bounds.length);
        for (Type type1 : bounds) {
            boolean subtypeFound = false;
            for (Type type2 : bounds) {
                if (type1 == type2 || !TypeUtils.isAssignable(type2, type1, null)) continue;
                subtypeFound = true;
                break;
            }
            if (subtypeFound) continue;
            types.add(type1);
        }
        return types.toArray(new Type[types.size()]);
    }

    public static Type[] getImplicitBounds(TypeVariable<?> typeVariable) {
        Type[] arrtype;
        Type[] bounds = typeVariable.getBounds();
        if (bounds.length == 0) {
            Type[] arrtype2 = new Type[1];
            arrtype = arrtype2;
            arrtype2[0] = Object.class;
        } else {
            arrtype = TypeUtils.normalizeUpperBounds(bounds);
        }
        return arrtype;
    }

    public static Type[] getImplicitUpperBounds(WildcardType wildcardType) {
        Type[] arrtype;
        Type[] bounds = wildcardType.getUpperBounds();
        if (bounds.length == 0) {
            Type[] arrtype2 = new Type[1];
            arrtype = arrtype2;
            arrtype2[0] = Object.class;
        } else {
            arrtype = TypeUtils.normalizeUpperBounds(bounds);
        }
        return arrtype;
    }

    public static Type[] getImplicitLowerBounds(WildcardType wildcardType) {
        Type[] arrtype;
        Type[] bounds = wildcardType.getLowerBounds();
        if (bounds.length == 0) {
            Type[] arrtype2 = new Type[1];
            arrtype = arrtype2;
            arrtype2[0] = null;
        } else {
            arrtype = bounds;
        }
        return arrtype;
    }

    public static boolean typesSatisfyVariables(Map<TypeVariable<?>, Type> typeVarAssigns) {
        for (Map.Entry entry : typeVarAssigns.entrySet()) {
            TypeVariable typeVar = entry.getKey();
            Type type = entry.getValue();
            for (Type bound : TypeUtils.getImplicitBounds(typeVar)) {
                if (TypeUtils.isAssignable(type, TypeUtils.substituteTypeVariables(bound, typeVarAssigns), typeVarAssigns)) continue;
                return false;
            }
        }
        return true;
    }

    private static Class<?> getRawType(ParameterizedType parameterizedType) {
        Type rawType = parameterizedType.getRawType();
        if (!(rawType instanceof Class)) {
            throw new IllegalStateException("Wait... What!? Type of rawType: " + rawType);
        }
        return (Class)rawType;
    }

    public static Class<?> getRawType(Type type, Type assigningType) {
        if (type instanceof Class) {
            return (Class)type;
        }
        if (type instanceof ParameterizedType) {
            return TypeUtils.getRawType((ParameterizedType)type);
        }
        if (type instanceof TypeVariable) {
            if (assigningType == null) {
                return null;
            }
            Object genericDeclaration = ((TypeVariable)type).getGenericDeclaration();
            if (!(genericDeclaration instanceof Class)) {
                return null;
            }
            Map typeVarAssigns = TypeUtils.getTypeArguments(assigningType, (Class)genericDeclaration);
            if (typeVarAssigns == null) {
                return null;
            }
            Type typeArgument = typeVarAssigns.get(type);
            if (typeArgument == null) {
                return null;
            }
            return TypeUtils.getRawType(typeArgument, assigningType);
        }
        if (type instanceof GenericArrayType) {
            Class rawComponentType = TypeUtils.getRawType(((GenericArrayType)type).getGenericComponentType(), assigningType);
            return Array.newInstance(rawComponentType, 0).getClass();
        }
        if (type instanceof WildcardType) {
            return null;
        }
        throw new IllegalArgumentException("unknown type: " + type);
    }

    public static boolean isArrayType(Type type) {
        return type instanceof GenericArrayType || type instanceof Class && ((Class)type).isArray();
    }

    public static Type getArrayComponentType(Type type) {
        if (type instanceof Class) {
            Class clazz = (Class)type;
            return clazz.isArray() ? clazz.getComponentType() : null;
        }
        if (type instanceof GenericArrayType) {
            return ((GenericArrayType)type).getGenericComponentType();
        }
        return null;
    }
}

