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
	public static final double SLOWNESS = 0.25;

	public static final DepthsAbilityInfo<CurseOfSloth> INFO =
		new DepthsAbilityInfo<>(CurseOfSloth.class, ABILITY_NAME, CurseOfSloth::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.MOSSY_COBBLESTONE) //idk probably there's a better choice
			.descriptions(CurseOfSloth::getDescription)
			.remove(CurseOfSloth::removeModifier);

	public CurseOfSloth(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		addModifier(mPlayer);
		mPlugin.mEffectManager.getEffects(mPlayer, PercentSpeed.class).forEach(this::invertSpeedEffect);
	}

	@Override
	public void customEffectApplyEvent(CustomEffectApplyEvent event) {
		if (event.getEffect() instanceof PercentSpeed speed) {
			invertSpeedEffect(speed);
		}
	}

	private void invertSpeedEffect(PercentSpeed effect) {
		if (effect.isBuff()) {
			effect.setAmount(-effect.getMagnitude(), mPlayer);
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
		return new DescriptionBuilder<CurseOfSloth>()
			.add("Lose ")
			.addPercent(SLOWNESS)
			.add(" speed. Any speed effects you have are inverted to slowness effects.");
	}
}
