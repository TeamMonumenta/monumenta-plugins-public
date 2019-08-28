package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import java.lang.NoSuchMethodException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;

// This should probably be in a more general location, but for now only Apothecary uses it, so...

public class AbsorptionManipulator {
	private static final int MIN_ABSORPTION_DURATION = 20 * 30;

	private Plugin plugin;
	private Player player;
	private Object handle;
	private Method handleMethod;
	private Method getAbsorptionMethod;
	private Method setAbsorptionMethod;

	public AbsorptionManipulator(Plugin plugin, Player player) {
		this.plugin = plugin;
		this.player = player;
		try {
			this.handleMethod = player.getClass().getMethod("getHandle");
			this.handle = this.handleMethod.invoke(player);

			this.getAbsorptionMethod = this.handle.getClass().getMethod("getAbsorptionHearts");
			this.setAbsorptionMethod = this.handle.getClass().getMethod("setAbsorptionHearts", float.class);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public boolean isPlayerOnline() {
		return player.isOnline();
	}

	// Doesn't work for subtracting absorption because newAbsorption makes sure it never drops (in case absorption is higher than maxAmount)
	public void addAbsorption(float amount, float maxAmount) {
		float absorption = getAbsorption();
		float newAbsorption = Math.max(absorption, Math.min(absorption + amount, maxAmount));
		if (newAbsorption != absorption) {
			setAbsorption(newAbsorption);
		}
	}

	public void subtractAbsorption(float amount) {
		float absorption = getAbsorption();
		float newAbsorption = Math.max(absorption - amount, 0);
		if (newAbsorption != absorption) {
			setAbsorption(newAbsorption);
		}
	}

	public void setAbsorption(float amount) {
		try {
			setAbsorptionMethod.invoke(handle, amount);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public float getAbsorption() {
		try {
			return (Float) getAbsorptionMethod.invoke(handle);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
			return 0;
		}
	}

}
