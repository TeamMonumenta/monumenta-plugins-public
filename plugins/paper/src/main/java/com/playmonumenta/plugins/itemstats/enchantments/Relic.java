package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.hexfall.Ruten;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.hexfall.InfusedLife;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class Relic implements Enchantment {

	private static final Integer lineRadius = 5;
	private static final Integer lineLength = 25;
	private static final Integer pointBlankRadius = 11;

	@Override
	public String getName() {
		return "Relic";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.RELIC;
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile projectile) {
		InfusedLife effect = plugin.mEffectManager.getActiveEffect(player, InfusedLife.class);
		if (effect == null) {
			return;
		}

		String source = plugin.mEffectManager.getSource(player, effect);
		double currentStacks = effect.getMagnitude();

		if (currentStacks >= InfusedLife.MAX_ENERGY - 1) {

			projectile.remove();

			if (source != null) {
				effect.setCurrentEnergy(0);
				player.playSound(player, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1, 1);
			}

			BukkitRunnable runnable = new BukkitRunnable() {
				final BoundingBox mHitbox = BoundingBox.of(player.getLocation(), 1, 1, 1);
				final World mWorld = player.getWorld();
				final Location mLoc = player.getEyeLocation();
				final Vector mDir = mLoc.getDirection().normalize();
				int mT = 0;

				@Override
				public void run() {

					if (mT <= 1) {
						mT++;
						for (int i = 0; i < lineLength; i++) {
							mHitbox.shift(mDir);
							Location bLoc = mHitbox.getCenter().toLocation(mWorld);
							Location checkingLoc = bLoc.clone();
							checkingLoc.setY(Ruten.arenaHeightY);
							Block center = mWorld.getBlockAt(checkingLoc);

							Set<Block> blocksToConvert = new HashSet<>();
							blocksToConvert.add(center);

							for (double deg = 0; deg < 360; deg += 8) {
								double cos = FastUtils.cosDeg(deg);
								double sin = FastUtils.sinDeg(deg);
								for (double rad = 0; rad < lineRadius; rad += 1) {
									Location l = center.getLocation().clone().add(cos * rad, 0, sin * rad);
									blocksToConvert.add(l.getBlock());
								}
							}

							for (Block b : blocksToConvert) {
								Ruten.modifyAnimaAtLocation(b.getLocation(), Ruten.AnimaTendency.LIFE);
							}
						}
					}
				}
			};
			runnable.runTaskTimer(plugin, 0, 1);
		}
	}

	@Override
	public void onPlayerSwapHands(Plugin plugin, Player player, double value, PlayerSwapHandItemsEvent event) {
		Effect effect = plugin.mEffectManager.getActiveEffect(player, InfusedLife.class);
		if (effect == null) {
			return;
		}

		String source = plugin.mEffectManager.getSource(player, effect);
		double currentStacks = effect.getMagnitude();

		if (currentStacks >= InfusedLife.MAX_ENERGY - 1) {

			BoundingBox hitbox = BoundingBox.of(player.getLocation(), 1, 1, 1);
			World world = player.getWorld();
			Location loc = player.getEyeLocation();
			loc.setY(Ruten.arenaHeightY);
			Vector dir = loc.getDirection().normalize();

			ArrayList<Block> blocksToConvert = new ArrayList<>();
			blocksToConvert.add(loc.getBlock());

			for (double deg = 0; deg < 360; deg += 2) {
				double cos = FastUtils.cosDeg(deg);
				double sin = FastUtils.sinDeg(deg);
				for (double rad = 0; rad < pointBlankRadius; rad += 1) {
					Location l = loc.clone().add(cos * rad, 0, sin * rad);
					blocksToConvert.add(l.getBlock());
				}
			}

			for (Block b : blocksToConvert) {
				Ruten.modifyAnimaAtLocation(b.getLocation(), Ruten.AnimaTendency.LIFE);
			}

			for (int i = 0; i < 20; i++) {
				hitbox.shift(dir);
				Location bLoc = hitbox.getCenter().toLocation(world);
				Location checkingLoc = bLoc.clone();
				checkingLoc.setY(Ruten.arenaHeightY);
			}

			if (source != null) {
				plugin.mEffectManager.clearEffects(player, source);
			}
			plugin.mEffectManager.addEffect(player, InfusedLife.GENERIC_NAME, new InfusedLife(20 * 6000));
		}
	}



	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 7;
	}

	public static void particles(Location loc, Player player) {
		new PartialParticle(Particle.CRIT, loc, 30, 0, 0, 0, 0.65).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, loc, 30, 0, 0, 0, 0.65).spawnAsPlayerActive(player);
		player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.6f, 0.5f);
	}
}
