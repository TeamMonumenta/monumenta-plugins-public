package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class FlameSpirit extends DepthsAbility {

	public static final String ABILITY_NAME = "Flame Spirit";

	public static final int[] DAMAGE = {5, 6, 7, 8, 9, 13};
	public static final int DAMAGE_COUNT = 3;
	public static final int FIRE_TICKS = 2 * 20;
	public static final int RADIUS = 4;

	public static final DepthsAbilityInfo<FlameSpirit> INFO =
		new DepthsAbilityInfo<>(FlameSpirit.class, ABILITY_NAME, FlameSpirit::new, DepthsTree.FLAMECALLER, DepthsTrigger.SPAWNER)
			.displayItem(new ItemStack(Material.SOUL_CAMPFIRE))
			.descriptions(FlameSpirit::getDescription, MAX_RARITY);

	public FlameSpirit(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean blockBreakEvent(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (ItemUtils.isPickaxe(event.getPlayer().getInventory().getItemInMainHand()) && block.getType() == Material.SPAWNER) {
			Location centerLoc = block.getLocation().add(0.5, 0, 0.5);
			new BukkitRunnable() {
				int mTickCount = 0;

				@Override
				public void run() {
					if (mTickCount >= DAMAGE_COUNT) {
						this.cancel();
					}

					for (LivingEntity mob : EntityUtils.getNearbyMobs(centerLoc, RADIUS)) {
						if (!(mob == null)) {
							EntityUtils.applyFire(mPlugin, FIRE_TICKS, mob, mPlayer);
							DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, DAMAGE[mRarity - 1], mInfo.getLinkedSpell(), true, false);
						}
					}

					new BukkitRunnable() {
						double mVerticalAngle = 0;
						double mRotationAngle = 0;
						int mTicksElapsed = 0;

						@Override
						public void run() {
							if (mTicksElapsed >= 20) {
								this.cancel();
							}

							mVerticalAngle += 5.5;
							mRotationAngle += 20;
							mVerticalAngle %= 360;
							mRotationAngle %= 360;

							new PartialParticle(
									Particle.FLAME,
									centerLoc
										.add(
											FastUtils.cos(Math.toRadians(mRotationAngle)) * 2,
											FastUtils.sin(Math.toRadians(mVerticalAngle)) * 0.02,
											FastUtils.sin(Math.toRadians(mRotationAngle)) * 2
									), 1, 0, 0.01
							).spawnAsPlayerActive(mPlayer);

							new PartialParticle(
									Particle.FLAME,
									centerLoc
										.add(
											FastUtils.cos(Math.toRadians(mRotationAngle)) * -2,
											FastUtils.sin(Math.toRadians(mVerticalAngle)) * 0.02,
											FastUtils.sin(Math.toRadians(mRotationAngle)) * -2
									), 1, 0, 0.01
							).spawnAsPlayerActive(mPlayer);

							mTicksElapsed++;
						}
					}.runTaskTimer(mPlugin, 0, 1);
					mTickCount++;
				}
			}.runTaskTimer(mPlugin, 0, 20);
		}
		return true;
	}

	private static String getDescription(int rarity) {
		return "Breaking a spawner summons a spirit that deals " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " magic damage in a " + RADIUS + " block radius every second for " + DAMAGE_COUNT + " seconds and sets affected mobs on fire for " + FIRE_TICKS / 20 + " seconds.";
	}


}
