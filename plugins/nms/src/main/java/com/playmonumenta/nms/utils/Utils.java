package com.playmonumenta.nms.utils;

import java.lang.reflect.Field;

public class Utils {
	public static Object getPrivateField(String fieldName, Class<?> clazz, Object object) throws NoSuchFieldException, IllegalAccessException {
		Field field;
		Object o = null;

		field = clazz.getDeclaredField(fieldName);

		field.setAccessible(true);

		o = field.get(object);

		return o;
	}
}
