package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class HexfallFloramancerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_floramancer";

	private final List<Material> FLOWER_BLOCKS = List.of(Material.POPPY, Material.DANDELION, Material.PINK_TULIP, Material.CORNFLOWER);
	private static final Particle.DustOptions LIME = new Particle.DustOptions(Color.fromRGB(155, 210, 0), 1f);
	private static final Particle.DustOptions NEON = new Particle.DustOptions(Color.fromRGB(215, 255, 0), 1f);

	public HexfallFloramancerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Spell spell = new Spell() {
			@Override
			public void run() {
				if (EntityUtils.shouldCancelSpells(mBoss)) {
					return;
				}

				List<Block> flowers = BlockUtils.getBlocksInSphere(mBoss.getLocation(), 20);
				flowers.removeIf(block -> !FLOWER_BLOCKS.contains(block.getType()));
				for (int i = 0; i < 3; i++) {
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						if (!flowers.isEmpty()) {
							Block flower = flowers.get(FastUtils.randomIntInRange(0, flowers.size() - 1));
							launchMagicTo(flower);
						}
					}, i * 6L);
				}
			}

			@Override
			public int cooldownTicks() {
				return 6 * 20;
			}

			@Override
			public boolean canRun() {
				List<Block> flowers = BlockUtils.getBlocksInSphere(mBoss.getLocation(), 20);
				flowers.removeIf(block -> !FLOWER_BLOCKS.contains(block.getType()));
				return !flowers.isEmpty();
			}
		};

		super.constructBoss(spell, 40, null, 40);
	}

	private void launchMagicTo(Block targetBlock) {
		Color c = FastUtils.RANDOM.nextBoolean() ? NEON.getColor() : LIME.getColor();
		double red = c.getRed() / 255.0;
		double green = c.getGreen() / 255.0;
		double blue = c.getBlue() / 255.0;
		new PPCircle(Particle.SPELL_MOB, mBoss.getEyeLocation(), 0.5).directionalMode(true).delta(red, green, blue).extra(1).count(10).spawnAsBoss();
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1f, FastUtils.randomFloatInRange(1.5f, 2f));

		Vector dir = VectorUtils.rotateYAxis(new Vector(0.6, 0.85, 0), FastUtils.randomDoubleInRange(0, 360));
		new BukkitRunnable() {
			final Location mL = mBoss.getEyeLocation();
			int mT = 0;
			double mArcCurve = 0;
			Vector mD = dir.clone();

			@Override
			public void run() {
				mT++;

				Location to = BlockUtils.getCenterBlockLocation(targetBlock);

				for (int i = 0; i < 6; i++) {
					mArcCurve += 0.055;
					mD = dir.clone().add(LocationUtils.getDirectionTo(to, mL).multiply(mArcCurve));

					if (mD.length() > 0.25) {
						mD.normalize().multiply(0.25);
					}

					mL.add(mD);

					Color c = FastUtils.RANDOM.nextBoolean() ? NEON.getColor() : LIME.getColor();
					double red = c.getRed() / 255.0;
					double green = c.getGreen() / 255.0;
					double blue = c.getBlue() / 255.0;
					new PartialParticle(Particle.SPELL_MOB, mL.clone(), 1, red, green, blue, 1).directionalMode(true).spawnAsBoss();

					if (i % 2 == 0) {
						new PartialParticle(Particle.TOTEM, mL, 1).extra(1).delta(mD.getX(), mD.getY(), mD.getZ()).directionalMode(true).spawnAsBoss();
					}

					if (mL.distance(to) < 0.3) {
						animateFlower(targetBlock);
						this.cancel();
						return;
					}
				}

				if (mT >= 100) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void animateFlower(Block flower) {
		Location respawnLoc = BlockUtils.getCenterBlockLocation(flower);

		new PartialParticle(Particle.VILLAGER_HAPPY, respawnLoc, 5, 0.2, 0.2, 0.2, 0).spawnAsBoss();
		mBoss.getWorld().playSound(respawnLoc, Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, SoundCategory.HOSTILE, 1f, FastUtils.randomFloatInRange(0.5f, 1f));

		new BukkitRunnable() {
			int mTicks = 0;
			final Location mLoc = respawnLoc.clone();
			final Material mFlower = flower.getType();
			@Override
			public void run() {
				new PartialParticle(Particle.TOTEM, mLoc, 1, 0, 0, 0, 0.5).spawnAsBoss();

				mTicks++;
				if (mTicks >= 15) {
					String mob;
					switch (mFlower) {
						case POPPY -> mob = "AnimatedPoppy";
						case DANDELION -> mob = "AnimatedDandelion";
						case PINK_TULIP -> mob = "AnimatedPinkTulip";
						case CORNFLOWER -> mob = "AnimatedCornflower";
						default -> mob = "";
					}
					Entity entity = LibraryOfSoulsIntegration.summon(mLoc, mob);
					if (entity != null) {
						entity.setVelocity(new Vector(0, 0.3, 0));
					}

					new PartialParticle(Particle.TOTEM, mLoc, 40, 0, 0, 0, 1).spawnAsBoss();
					new PartialParticle(Particle.BLOCK_CRACK, mLoc, 20, 0.2, 0.2, 0.2).data(flower.getBlockData()).spawnAsBoss();

					mBoss.getWorld().playSound(respawnLoc, Sound.BLOCK_CAVE_VINES_FALL, SoundCategory.HOSTILE, 1f, 0.5f);
					mBoss.getWorld().playSound(respawnLoc, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 1f, 0.75f);

					if (mLoc.getBlock().getType() == mFlower) {
						mLoc.getBlock().setType(Material.AIR);
					}

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}
}
