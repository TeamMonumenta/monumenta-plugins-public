package com.playmonumenta.plugins.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.entity.LivingEntity;

public class AbsorptionUtils {

	private static Method handleMethod;
	private static Method getAbsorptionMethod;
	private static Method setAbsorptionMethod;

	// Doesn't work for subtracting absorption because newAbsorption makes sure it never drops (in case absorption is higher than maxAmount)
	public static void addAbsorption(LivingEntity entity, float amount, float maxAmount) {
		float absorption = getAbsorption(entity);
		float newAbsorption = Math.max(absorption, Math.min(absorption + amount, maxAmount));
		if (newAbsorption != absorption) {
			setAbsorption(entity, newAbsorption);
		}
	}

	public static void subtractAbsorption(LivingEntity entity, float amount) {
		float absorption = getAbsorption(entity);
		float newAbsorption = Math.max(absorption - amount, 0);
		if (newAbsorption != absorption) {
			setAbsorption(entity, newAbsorption);
		}
	}

	public static void setAbsorption(LivingEntity entity, float amount) {
		cacheReflectionMethods(entity);

		try {
			setAbsorptionMethod.invoke(handleMethod.invoke(entity), amount);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public static float getAbsorption(LivingEntity entity) {
		cacheReflectionMethods(entity);

		try {
			return (Float) getAbsorptionMethod.invoke(handleMethod.invoke(entity));
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
			return 0;
		}
	}

	private static void cacheReflectionMethods(LivingEntity entity) {
		if (handleMethod == null || getAbsorptionMethod == null || setAbsorptionMethod == null) {
			try {
				handleMethod = entity.getClass().getMethod("getHandle");
				Object handle = handleMethod.invoke(entity);
				getAbsorptionMethod = handle.getClass().getMethod("getAbsorptionHearts");
				setAbsorptionMethod = handle.getClass().getMethod("setAbsorptionHearts", float.class);
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

}
