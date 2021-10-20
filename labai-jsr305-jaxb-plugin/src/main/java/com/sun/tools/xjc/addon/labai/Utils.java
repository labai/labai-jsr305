package com.sun.tools.xjc.addon.labai;

import java.lang.reflect.Field;
import java.math.BigInteger;

/*
 * utils
*/
class Utils {

    static int toInt(Object maxOccurs) {
        if (maxOccurs instanceof BigInteger) {
            // xjc
            return ((BigInteger) maxOccurs).intValue();
        } else if (maxOccurs instanceof Integer) {
            // cxf-codegen
            return (Integer) maxOccurs;
        } else {
            throw new IllegalArgumentException("unknown type " + maxOccurs.getClass());
        }
    }

    static boolean toBoolean(Object field) {
        if (field != null) {
            return Boolean.parseBoolean(field.toString());
        }
        return false;
    }

    static Field getSimpleField(String fieldName, Class<?> clazz) {
        Class<?> tmpClass = clazz;
        try {
            do {
                for (Field field : tmpClass.getDeclaredFields()) {
                    String candidateName = field.getName();
                    if (!candidateName.equals(fieldName)) {
                        continue;
                    }
                    field.setAccessible(true);
                    return field;
                }
                tmpClass = tmpClass.getSuperclass();
            } while (tmpClass != null);
        } catch (Exception e) {
            System.err.println("labai-jaxb-jsr305 - Field '" + fieldName + "' not found on class " + clazz);
        }
        return null;
    }

    static Object getField(String path, Object oo) {
        try {
            if (path.contains(".")) {
                String field = path.substring(0, path.indexOf("."));
                Field declaredField = oo.getClass().getDeclaredField(field);
                declaredField.setAccessible(true);
                Object result = declaredField.get(oo);
                return getField(path.substring(path.indexOf(".") + 1), result);
            } else {
                Field simpleField = getSimpleField(path, oo.getClass());
                simpleField.setAccessible(true);
                return simpleField.get(oo);
            }
        } catch (Exception e) {
            System.err.println("labai-jaxb-jsr305 - Field " + path + " not found on " + oo.getClass().getName());
        }
        return null;
    }

}
