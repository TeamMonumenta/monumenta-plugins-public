package com.playmonumenta.plugins.effects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
			player.getLocation().getNearbyPlayers(100).forEach(p -> p.sendMessage(Component.text(player.getName() + " has picked up the Dagger!", NamedTextColor.YELLOW)));
			player.sendMessage(Component.text("As you pick up the dagger, it fades and infuses with your weapon... You feel empowered!", NamedTextColor.AQUA, TextDecoration.ITALIC));
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Player player) {
			player.sendMessage(Component.text("You feel the effect of the dagger slowly fades away...", NamedTextColor.AQUA, TextDecoration.ITALIC));
		}
	}

	@Override public String toString() {
		return String.format("SamwellBlackbloodDagger duration:%d", this.getDuration());
	}
}
