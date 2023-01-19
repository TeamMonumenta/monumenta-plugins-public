package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Pyromania extends DepthsAbility {

	public static final String ABILITY_NAME = "Pyromania";
	public static final double[] DAMAGE = {0.02, 0.025, 0.03, 0.035, 0.04, 0.05};
	public static final int RADIUS = 6;
	public static final int TWISTED_RADIUS = 8;

	public static final DepthsAbilityInfo<Pyromania> INFO =
		new DepthsAbilityInfo<>(Pyromania.class, ABILITY_NAME, Pyromania::new, DepthsTree.FLAMECALLER, DepthsTrigger.PASSIVE)
			.displayItem(new ItemStack(Material.CAMPFIRE))
			.descriptions(Pyromania::getDescription);

	public Pyromania(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {

		int radius = mRarity < 6 ? RADIUS : TWISTED_RADIUS;
		int fireCount = 0;
		for (LivingEntity e : EntityUtils.getNearbyMobs(mPlayer.getLocation(), radius)) {
			if (e.getFireTicks() > 0) {
				fireCount++;
			}
		}
		if (fireCount > 0) {
			event.setDamage(event.getDamage() * (1 + (DAMAGE[mRarity - 1] * fireCount)));
		}
		return false;
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		Component radius = rarity == 6 ? Component.text(TWISTED_RADIUS, color) : Component.text(RADIUS);
		return Component.text("For every mob on fire within ")
			.append(radius)
			.append(Component.text(" blocks of you, gain "))
			.append(Component.text(StringUtils.multiplierToPercentage(DAMAGE[rarity - 1]) + "%", color))
			.append(Component.text(" increased damage."));
	}
}

