package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PrimordialMastery extends DepthsAbility {

	public static final String ABILITY_NAME = "Primordial Mastery";
	public static final double[] SPELL_MOD = {1.08, 1.095, 1.11, 1.125, 1.14, 1.18};

	public static final DepthsAbilityInfo<PrimordialMastery> INFO =
		new DepthsAbilityInfo<>(PrimordialMastery.class, ABILITY_NAME, PrimordialMastery::new, DepthsTree.FLAMECALLER, DepthsTrigger.PASSIVE)
			.displayItem(new ItemStack(Material.FIRE_CORAL_FAN))
			.descriptions(PrimordialMastery::getDescription);

	public PrimordialMastery(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getAbility() != null && !event.getAbility().isFake()) {
			event.setDamage(event.getDamage() * SPELL_MOD[mRarity - 1]);
		}
		return false; // only changes event damage
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("All ability damage is multiplied by ")
			.append(Component.text(SPELL_MOD[rarity - 1], color))
			.append(Component.text("."));
	}
}

