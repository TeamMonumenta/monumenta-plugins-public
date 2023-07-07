package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class CursedWoundCS implements CosmeticSkill {
	private static final BlockData FALLING_DUST_DATA = Material.ANVIL.createBlockData();
	private static final Color LIGHT_COLOR = Color.fromRGB(217, 217, 217);
	private static final Color DARK_COLOR = Color.fromRGB(13, 13, 13);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.CURSED_WOUND;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GOLDEN_SWORD;
	}

	public void onAttack(Player player, Entity entity) {
		anvilParticle(player, entity);
	}

	public void onCriticalAttack(World world, Location loc) {
		world.playSound(loc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2.0f, 0.8f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 0.3f, 1.5f);
		world.playSound(loc, Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, SoundCategory.PLAYERS, 0.2f, 1.4f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.6f, 1.0f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.3f, 1.2f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.5f, 0.6f);
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 0.5f, 1.2f);
	}

	public void onEffectApplication(Player player, Entity entity) {
		anvilParticle(player, entity);
	}

	public void onReleaseStoredEffects(Player player, World world, Location loc, Entity entity, double radius) {
		world.playSound(loc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2.0f, 0.8f);
		anvilParticle(player, entity, 25, radius, radius);
	}

	public void onStoreEffects(Player player, World world, Location loc, LivingEntity entity) {
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.6f, 1.65f);
		createOrb(new Vector(FastUtils.randomDoubleInRange(-0.75, 0.75),
			FastUtils.randomDoubleInRange(1, 1.5),
			FastUtils.randomDoubleInRange(-0.75, 0.75)), loc.clone().add(0, 1, 0), player, entity, null);
	}

	private void anvilParticle(Player player, Entity entity) {
		anvilParticle(player, entity, 3, entity.getWidth(), entity.getHeight());
	}

	private void anvilParticle(Player player, Entity entity, int count, double xz, double y) {
		xz = Math.max(xz / 2, 0.1);
		y = Math.max(y / 3, 0.1);
		new PartialParticle(Particle.FALLING_DUST, entity.getLocation().add(0, entity.getHeight() / 2, 0), count,
			xz, y, xz, FALLING_DUST_DATA)
			.spawnAsPlayerActive(player);
	}

	private void createOrb(Vector dir, Location loc, Player player, LivingEntity target, @Nullable Location optLoc) {
		World world = loc.getWorld();
		new BukkitRunnable() {
			final Location mL = target.getLocation().clone();
			int mT = 0;
			double mArcCurve = 0;
			Vector mD = dir.clone();

			@Override
			public void run() {
				mT++;

				Location to = optLoc != null ? optLoc : LocationUtils.getHalfHeightLocation(player);

				for (int i = 0; i < 4; i++) {
					if (mT <= 2) {
						mD = dir.clone();
					} else {
						mArcCurve += 0.085;
						mD = dir.clone().add(LocationUtils.getDirectionTo(to, mL).multiply(mArcCurve));
					}

					if (mD.length() > 0.2) {
						mD.normalize().multiply(0.2);
					}

					mL.add(mD);

					for (int j = 0; j < 2; j++) {
						Color c = FastUtils.RANDOM.nextBoolean() ? DARK_COLOR : LIGHT_COLOR;
						double red = c.getRed() / 255D;
						double green = c.getGreen() / 255D;
						double blue = c.getBlue() / 255D;
						new PartialParticle(Particle.SPELL_MOB,
							mL.clone().add(FastUtils.randomDoubleInRange(-0.05, 0.05),
								FastUtils.randomDoubleInRange(-0.05, 0.05),
								FastUtils.randomDoubleInRange(-0.05, 0.05)),
							1, red, green, blue, 1)
							.directionalMode(true).spawnAsPlayerActive(player);
					}
					Color c = FastUtils.RANDOM.nextBoolean() ? DARK_COLOR : LIGHT_COLOR;
					new PartialParticle(Particle.REDSTONE, mL, 1, 0, 0, 0, 0,
						new Particle.DustOptions(c, 1.4f))
						.spawnAsPlayerActive(player);

					if (mT > 5 && mL.distance(to) < 0.35) {
						world.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, SoundCategory.PLAYERS, 1, 0.8f);
						new PartialParticle(Particle.SPELL, loc.add(0, 1, 0), 20, 0.4f, 0.4f, 0.4f, 0.6F)
							.spawnAsPlayerActive(player);
						new PartialParticle(Particle.FALLING_DUST, mL, 45, 0, 0, 0, 0.75F, Material.ANVIL.createBlockData())
							.spawnAsPlayerActive(player);
						this.cancel();
						return;
					}
				}

				if (mT >= 100) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
