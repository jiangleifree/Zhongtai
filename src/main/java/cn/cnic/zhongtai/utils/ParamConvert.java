package cn.cnic.zhongtai.utils;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.*;
import java.util.Map;

public class ParamConvert {
    private static final Map<Class<?>, Member> conversionMemberCache = new ConcurrentReferenceHashMap(32);
    private static boolean isApplicable(Member member, Class<?> sourceClass) {
        if (member instanceof Method) {
            Method method = (Method)member;
            return !Modifier.isStatic(method.getModifiers()) ? ClassUtils.isAssignable(method.getDeclaringClass(), sourceClass) : method.getParameterTypes()[0] == sourceClass;
        } else if (member instanceof Constructor) {
            Constructor<?> ctor = (Constructor)member;
            return ctor.getParameterTypes()[0] == sourceClass;
        } else {
            return false;
        }
    }

    private static Method determineToMethod(Class<?> targetClass, Class<?> sourceClass) {
        if (String.class != targetClass && String.class != sourceClass) {
            Method method = ClassUtils.getMethodIfAvailable(sourceClass, "to" + targetClass.getSimpleName(), new Class[0]);
            return method != null && !Modifier.isStatic(method.getModifiers()) && ClassUtils.isAssignable(targetClass, method.getReturnType()) ? method : null;
        } else {
            return null;
        }
    }

    private static Constructor<?> determineFactoryConstructor(Class<?> targetClass, Class<?> sourceClass) {
        return ClassUtils.getConstructorIfAvailable(targetClass, new Class[]{sourceClass});
    }

    private static Method determineFactoryMethod(Class<?> targetClass, Class<?> sourceClass) {
        if (String.class == targetClass) {
            return null;
        } else {
            Method method = ClassUtils.getStaticMethod(targetClass, "valueOf", new Class[]{sourceClass});
            if (method == null) {
                method = ClassUtils.getStaticMethod(targetClass, "of", new Class[]{sourceClass});
                if (method == null) {
                    method = ClassUtils.getStaticMethod(targetClass, "from", new Class[]{sourceClass});
                }
            }

            return method;
        }
    }

    public static Object convert(@Nullable Object source, Class targetType){
        TypeDescriptor targetDes = TypeDescriptor.valueOf(targetType);
        TypeDescriptor sourceDes = TypeDescriptor.forObject(source);
        return convert(source, sourceDes, targetDes);
    }


    public static Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        } else {
            Class<?> sourceClass = sourceType.getType();
            Class<?> targetClass = targetType.getType();
            Member member = getValidatedMember(targetClass, sourceClass);

            try {
                if (member instanceof Method) {
                    Method method = (Method)member;
                    ReflectionUtils.makeAccessible(method);
                    if (!Modifier.isStatic(method.getModifiers())) {
                        return method.invoke(source);
                    }

                    return method.invoke((Object)null, source);
                }

                if (member instanceof Constructor) {
                    Constructor<?> ctor = (Constructor)member;
                    ReflectionUtils.makeAccessible(ctor);
                    return ctor.newInstance(source);
                }
            } catch (InvocationTargetException var8) {
                throw new ConversionFailedException(sourceType, targetType, source, var8.getTargetException());
            } catch (Throwable var9) {
                throw new ConversionFailedException(sourceType, targetType, source, var9);
            }

            throw new IllegalStateException(String.format("No to%3$s() method exists on %1$s, and no static valueOf/of/from(%1$s) method or %3$s(%1$s) constructor exists on %2$s.", sourceClass.getName(), targetClass.getName(), targetClass.getSimpleName()));
        }
    }

    @Nullable
    private static Member getValidatedMember(Class<?> targetClass, Class<?> sourceClass) {
        Member member = (Member)conversionMemberCache.get(targetClass);
        if (isApplicable(member, sourceClass)) {
            return member;
        } else {
            member = determineToMethod(targetClass, sourceClass);
            if (member == null) {
                member = determineFactoryMethod(targetClass, sourceClass);
                if (member == null) {
                    member = determineFactoryConstructor(targetClass, sourceClass);
                    if (member == null) {
                        return null;
                    }
                }
            }

            conversionMemberCache.put(targetClass, member);
            return (Member)member;
        }
    }
}
