package com.playmonumenta.plugins.cosmetics.skills.rogue.assassin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PrestigiousBlitzCS extends BodkinBlitzCS implements PrestigeCS {

	public static final String NAME = "Prestigious Blitz";

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 232, 52), 1.0f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 251, 216), 1.0f);

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"BODKIN_DESC"
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.BODKIN_BLITZ;
	}

	@Override
	public Material getDisplayItem() {
		return Material.YELLOW_DYE;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public boolean isUnlocked(Player mPlayer) {
		return mPlayer != null;
	}

	@Override
	public String[] getLockDesc() {
		return List.of("LOCKED").toArray(new String[0]);
	}

	@Override
	public int getPrice() {
		return 1;
	}

	@Override
	public void blitzStartSound(World world, Location loc) {
		world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 1.5f, 0.6f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2f, 1.6f);
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.25f, 0.45f);
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.35f, 0.55f);
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.5f, 0.6f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT_GROUND, SoundCategory.PLAYERS, 2f, 0.65f);
	}

	@Override
	public void blitzTrailEffect(Player mPlayer, Location loc, Vector dir) {
		new PartialParticle(Particle.FALLING_DUST, loc, 4, 0.15, 0.45, 0.1,
			Bukkit.createBlockData(Material.YELLOW_CONCRETE)).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_INSTANT, loc, 5, 0.25, 0.5, 0.25, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.END_ROD, loc, 2, 0.25, 0.5, 0.25, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 2, 0.25, 0.5, 0.25, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 3, 0.25, 0.5, 0.25, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void blitzEndEffect(World world, Player mPlayer, Location tpLoc) {
		world.playSound(tpLoc, Sound.BLOCK_ENDER_CHEST_CLOSE, 1.25f, 2f);
		world.playSound(tpLoc, Sound.ITEM_TRIDENT_RETURN, 1.5f, 0.65f);
		world.playSound(tpLoc, Sound.ITEM_TRIDENT_THROW, 1.2f, 0.5f);
		world.playSound(tpLoc, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.75f, 1.6f);
		world.playSound(tpLoc, Sound.ITEM_TRIDENT_HIT_GROUND, 1f, 0.75f);
		world.playSound(tpLoc, Sound.ENTITY_PHANTOM_HURT, 1f, 0.75f);
		world.playSound(tpLoc, Sound.ENTITY_WITHER_SHOOT, 0.8f, 0.7f);

		new PartialParticle(Particle.CLOUD, tpLoc.clone().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 0.2).spawnAsPlayerActive(mPlayer);
		new BukkitRunnable() {
			int mT = 0;
			final double mDRadius = 0.04;
			final double mDTheta = 3.1416 / 12;
			final double mDHeight = 2.0 / 40;
			@Override
			public void run() {
				do {
					Location loc1 = tpLoc.clone().add((mT + 10) * mDRadius * FastUtils.cos(mT * mDTheta), mT * mDHeight, (mT + 10) * mDRadius * FastUtils.sin(mT * mDTheta));
					Location loc2 = tpLoc.clone().add((mT + 10) * mDRadius * FastUtils.cos(mT * mDTheta + 3.1416 * 2 / 3), mT * mDHeight, (mT + 10) * mDRadius * FastUtils.sin(mT * mDTheta + 3.1416 * 2 / 3));
					Location loc3 = tpLoc.clone().add((mT + 10) * mDRadius * FastUtils.cos(mT * mDTheta - 3.1416 * 2 / 3), mT * mDHeight, (mT + 10) * mDRadius * FastUtils.sin(mT * mDTheta - 3.1416 * 2 / 3));
					new PartialParticle(Particle.REDSTONE, loc1, 2, 0.05, 0, 0.05, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, loc1, 1, 0.05, 0, 0.05, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, loc2, 2, 0.05, 0, 0.05, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, loc2, 1, 0.05, 0, 0.05, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, loc3, 2, 0.05, 0, 0.05, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, loc3, 1, 0.05, 0, 0.05, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
				} while (mT++ % 2 == 0);

				if (mT >= 20 * 2) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void blitzBuffEffect(Player mPlayer) {
		Location loc = mPlayer.getLocation().clone().add(0, 0.5, 0);
		new PartialParticle(Particle.FALLING_DUST, loc, 1, 0.35, 0.25, 0.35, Bukkit.createBlockData(Material.WHITE_CONCRETE)).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FALLING_DUST, loc, 1, 0.35, 0.25, 0.35, Bukkit.createBlockData(Material.LIGHT_GRAY_CONCRETE_POWDER)).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 3, 0.45, 0.3, 0.45, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void blitzOnDamage(World world, Player mPlayer, Location entityLoc) {
		world.playSound(entityLoc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.8f, 0.6f);
		world.playSound(entityLoc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1.25f, 1.75f);
		world.playSound(entityLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 3f, 1.6f);
		world.playSound(entityLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 1.5f, 0.5f);
		world.playSound(entityLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 1f, 0.6f);
		new PartialParticle(Particle.BLOCK_CRACK, entityLoc, 15, 0.35, 0.25, 0.35, 1, Bukkit.createBlockData(Material.IRON_BLOCK)).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.BLOCK_CRACK, entityLoc, 15, 0.35, 0.25, 0.35, 1, Bukkit.createBlockData(Material.GOLD_BLOCK)).spawnAsPlayerActive(mPlayer);
		new BukkitRunnable() {
			int mT = 40;
			final int THRESH = 12;
			double mDHeight = 0.1;
			double mDRadius = 0.25;

			@Override
			public void run() {
				do {
					Location mCenter = entityLoc.clone().add(0, mT > THRESH ? (mT - THRESH) * mDHeight - 0.9 : -0.9, 0);
					double radius = mT > THRESH ? FastUtils.RANDOM.nextDouble() * 0.3 + 0.15 : (THRESH - mT) * mDRadius;
					ParticleUtils.drawRing(mCenter, (int) Math.ceil(radius * 16), new Vector(0, 1, 0), radius,
						(l, t) -> new PartialParticle(Particle.FALLING_DUST, l, 1, 0, 0, 0, 0,
							Bukkit.createBlockData(Material.COPPER_BLOCK)).spawnAsPlayerActive(mPlayer)
					);
				} while (--mT % 4 != 0);

				if (mT <= 0) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

}
