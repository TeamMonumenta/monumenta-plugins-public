package com.playmonumenta.plugins.utils;

import java.lang.NoSuchMethodException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.bukkit.entity.Player;

public class AbsorptionUtils {

	private static Method handleMethod;
	private static Method getAbsorptionMethod;
	private static Method setAbsorptionMethod;

	// Doesn't work for subtracting absorption because newAbsorption makes sure it never drops (in case absorption is higher than maxAmount)
	public static void addAbsorption(Player player, float amount, float maxAmount) {
		float absorption = getAbsorption(player);
		float newAbsorption = Math.max(absorption, Math.min(absorption + amount, maxAmount));
		if (newAbsorption != absorption) {
			setAbsorption(player, newAbsorption);
		}
	}

	public static void subtractAbsorption(Player player, float amount) {
		float absorption = getAbsorption(player);
		float newAbsorption = Math.max(absorption - amount, 0);
		if (newAbsorption != absorption) {
			setAbsorption(player, newAbsorption);
		}
	}

	public static void setAbsorption(Player player, float amount) {
		cacheReflectionMethods(player);

		try {
			setAbsorptionMethod.invoke(handleMethod.invoke(player), amount);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public static float getAbsorption(Player player) {
		cacheReflectionMethods(player);

		try {
			return (Float) getAbsorptionMethod.invoke(handleMethod.invoke(player));
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
			return 0;
		}
	}

	private static void cacheReflectionMethods(Player player) {
		if (handleMethod == null || getAbsorptionMethod == null || setAbsorptionMethod == null) {
			try {
				handleMethod = player.getClass().getMethod("getHandle");
				Object handle = handleMethod.invoke(player);
				getAbsorptionMethod = handle.getClass().getMethod("getAbsorptionHearts");
				setAbsorptionMethod = handle.getClass().getMethod("setAbsorptionHearts", float.class);
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

}
