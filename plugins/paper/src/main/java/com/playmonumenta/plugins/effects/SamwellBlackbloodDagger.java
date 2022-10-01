package com.playmonumenta.plugins.effects;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class SamwellBlackbloodDagger extends ZeroArgumentEffect {
	public static final String effectID = "SamwellBlackbloodDagger";

	public SamwellBlackbloodDagger(int duration) {
		super(duration, effectID);
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Player player) {
			player.getLocation().getNearbyPlayers(100).forEach(p -> p.sendMessage(ChatColor.YELLOW + player.getName() + " has picked up the Dagger!"));
			player.sendMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "As you pick up the dagger, it fades and infuses with your weapon... You feel empowered!");
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Player player) {
			player.sendMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "You feel the effect of the dagger slowly fades away...");
		}
	}

	@Override public String toString() {
		return String.format("SamwellBlackbloodDagger duration:%d", this.getDuration());
	}
}
