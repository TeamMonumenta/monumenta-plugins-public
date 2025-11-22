package com.playmonumenta.plugins.depths.abilities.curses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class CurseOfSloth extends DepthsAbility {
	public static final String ABILITY_NAME = "Curse of Sloth";
	public static final String MODIFIER = "CurseOfSlothSpeedModifier";
	public static final double SLOWNESS = 0.15;

	public static final DepthsAbilityInfo<CurseOfSloth> INFO =
		new DepthsAbilityInfo<>(CurseOfSloth.class, ABILITY_NAME, CurseOfSloth::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.MOSSY_COBBLESTONE)
			.descriptions(CurseOfSloth::getDescription)
			.remove(CurseOfSloth::removeModifier);

	public CurseOfSloth(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		addModifier(mPlayer);
		for (PercentSpeed speed : mPlugin.mEffectManager.getEffects(mPlayer, PercentSpeed.class)) {
			if (speed.isBuff()) {
				speed.setDuration(0);
			}
		}
	}

	@Override
	public void customEffectApplyEvent(CustomEffectApplyEvent event) {
		if (event.getEffect() instanceof PercentSpeed speed && speed.isBuff()) {
			event.setCancelled(true);
		}
	}

	private static void addModifier(Player player) {
		EntityUtils.addAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED,
			new AttributeModifier(MODIFIER, -SLOWNESS, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
	}

	private static void removeModifier(Player player) {
		EntityUtils.removeAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, MODIFIER);
	}

	public static Description<CurseOfSloth> getDescription() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Lose ")
			.addPercent(SLOWNESS)
			.add(" speed. You cannot gain any positive speed effects.");
	}
}
