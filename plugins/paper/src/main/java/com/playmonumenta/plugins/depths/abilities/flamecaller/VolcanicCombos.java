package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class VolcanicCombos extends DepthsAbility {
	public static final String ABILITY_NAME = "Volcanic Combos";
	public static final int[] DAMAGE = {6, 7, 8, 9, 10, 12};
	public static final int RADIUS = 4;
	public static final int FIRE_TICKS = 3 * 20;

	public static final DepthsAbilityInfo<VolcanicCombos> INFO =
		new DepthsAbilityInfo<>(VolcanicCombos.class, ABILITY_NAME, VolcanicCombos::new, DepthsTree.FLAMECALLER, DepthsTrigger.COMBO)
			.displayItem(new ItemStack(Material.BLAZE_ROD))
			.descriptions(VolcanicCombos::getDescription, MAX_RARITY);

	private int mComboCount = 0;

	public VolcanicCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (DepthsUtils.isValidComboAttack(event, mPlayer)) {
			mComboCount++;

			if (mComboCount >= 3 && mRarity > 0) {
				Location location = enemy.getLocation();
				for (LivingEntity mob : EntityUtils.getNearbyMobs(location, RADIUS)) {
					EntityUtils.applyFire(mPlugin, FIRE_TICKS, mob, mPlayer);
					DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, DAMAGE[mRarity - 1], mInfo.getLinkedSpell(), true);
				}
				World world = mPlayer.getWorld();
				for (int i = 0; i < 360; i += 45) {
					double rad = Math.toRadians(i);
					Location locationDelta = new Location(world, RADIUS / 2 * FastUtils.cos(rad), 0.5, RADIUS / 2 * FastUtils.sin(rad));
					location.add(locationDelta);
					world.spawnParticle(Particle.FLAME, location, 1);
					location.subtract(locationDelta);
				}
				world.playSound(location, Sound.ITEM_FIRECHARGE_USE, 0.5f, 1);
				mComboCount = 0;
			}
			return true;
		}
		return false;
	}

	private static String getDescription(int rarity) {
		return "Every third melee attack deals " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " magic damage to enemies in a " + RADIUS + " block radius and sets those enemies on fire for " + FIRE_TICKS / 20 + " seconds.";
	}


}
