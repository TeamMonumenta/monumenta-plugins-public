package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class VitalityExtractionCS extends HeavenlyBoonCS {

	public static final String NAME = "Vitality Extraction";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A deceased mind is not necessarily devoid of use.",
			"A simple transmission can grant its essence new life."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private static final Particle.DustOptions colorRegeneration = new Particle.DustOptions(Color.fromRGB(255, 102, 153), 1.2f);
	private static final Particle.DustOptions colorSpeed = new Particle.DustOptions(Color.fromRGB(0, 220, 220), 1.2f);
	private static final Particle.DustOptions colorStrength = new Particle.DustOptions(Color.fromRGB(180, 0, 60), 1.2f);
	private static final Particle.DustOptions colorResistance = new Particle.DustOptions(Color.fromRGB(51, 153, 102), 1.2f);
	private static final Particle.DustOptions colorAbsorption = new Particle.DustOptions(Color.fromRGB(255, 153, 0), 1.2f);

	@Override
	public void splashEffectRegeneration(Player player, LivingEntity mob) {
		Color c = colorRegeneration.getColor();
		double red = c.getRed() / 255D;
		double green = c.getGreen() / 255D;
		double blue = c.getBlue() / 255D;
		Location loc = player.getLocation().subtract(0, LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)), 0);
		player.playSound(player.getLocation(), Sound.ENTITY_GLOW_SQUID_AMBIENT, SoundCategory.PLAYERS, 1.8f, 1.2f);
		player.playSound(player.getLocation(), Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 1.1f, 1.0f);
		player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.1f, 1.7f);
		new PPCircle(Particle.REDSTONE, loc, 0.8).delta(0.05, 0.05, 0.05).data(colorRegeneration).ringMode(true).countPerMeter(5).spawnAsPlayerActive(player);
		new PPCircle(Particle.SPELL_MOB, loc, 1.6).delta(red, green, blue).extra(1).ringMode(true).directionalMode(true).countPerMeter(5).spawnAsPlayerActive(player);
		new PPCircle(Particle.SPELL_MOB_AMBIENT, loc.clone().add(0, 0.5, 0), 2.4).delta(red, green, blue).extra(1).ringMode(true).directionalMode(true).countPerMeter(3).spawnAsPlayerActive(player);
		createOrb(new Vector(FastUtils.randomDoubleInRange(-0.75, 0.75),
			FastUtils.randomDoubleInRange(1, 1.5),
			FastUtils.randomDoubleInRange(-0.75, 0.75)), loc.clone().add(0, 1, 0), player, mob, colorRegeneration);
	}

	@Override
	public void splashEffectSpeed(Player player, LivingEntity mob) {
		Color c = colorSpeed.getColor();
		double red = c.getRed() / 255D;
		double green = c.getGreen() / 255D;
		double blue = c.getBlue() / 255D;
		Location loc = player.getLocation().subtract(0, LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)), 0);
		player.playSound(player.getLocation(), Sound.ENTITY_GLOW_SQUID_AMBIENT, SoundCategory.PLAYERS, 1.8f, 1.2f);
		player.playSound(player.getLocation(), Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 1.1f, 1.0f);
		player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 2.0f);
		new PPCircle(Particle.REDSTONE, loc, 0.8).delta(0.05, 0.05, 0.05).data(colorSpeed).ringMode(true).countPerMeter(5).spawnAsPlayerActive(player);
		new PPCircle(Particle.SPELL_MOB, loc, 1.6).delta(red, green, blue).extra(1).ringMode(true).directionalMode(true).countPerMeter(5).spawnAsPlayerActive(player);
		new PPCircle(Particle.SPELL_MOB_AMBIENT, loc.clone().add(0, 0.5, 0), 2.4).delta(red, green, blue).extra(1).ringMode(true).directionalMode(true).countPerMeter(3).spawnAsPlayerActive(player);
		createOrb(new Vector(FastUtils.randomDoubleInRange(-0.75, 0.75),
			FastUtils.randomDoubleInRange(1, 1.5),
			FastUtils.randomDoubleInRange(-0.75, 0.75)), loc.clone().add(0, 1, 0), player, mob, colorSpeed);
	}

	@Override
	public void splashEffectStrength(Player player, LivingEntity mob) {
		Color c = colorStrength.getColor();
		double red = c.getRed() / 255D;
		double green = c.getGreen() / 255D;
		double blue = c.getBlue() / 255D;
		Location loc = player.getLocation().subtract(0, LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)), 0);
		player.playSound(player.getLocation(), Sound.ENTITY_GLOW_SQUID_AMBIENT, SoundCategory.PLAYERS, 1.8f, 1.2f);
		player.playSound(player.getLocation(), Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 1.1f, 1.0f);
		player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SNIFF, SoundCategory.PLAYERS, 1.3f, 1.8f);
		new PPCircle(Particle.REDSTONE, loc, 0.8).delta(0.05, 0.05, 0.05).data(colorStrength).ringMode(true).countPerMeter(5).spawnAsPlayerActive(player);
		new PPCircle(Particle.SPELL_MOB, loc, 1.6).delta(red, green, blue).extra(1).ringMode(true).directionalMode(true).countPerMeter(5).spawnAsPlayerActive(player);
		new PPCircle(Particle.SPELL_MOB_AMBIENT, loc.clone().add(0, 0.5, 0), 2.4).delta(red, green, blue).extra(1).ringMode(true).directionalMode(true).countPerMeter(3).spawnAsPlayerActive(player);
		createOrb(new Vector(FastUtils.randomDoubleInRange(-0.75, 0.75),
			FastUtils.randomDoubleInRange(1, 1.5),
			FastUtils.randomDoubleInRange(-0.75, 0.75)), loc.clone().add(0, 1, 0), player, mob, colorStrength);
	}

	@Override
	public void splashEffectResistance(Player player, LivingEntity mob) {
		Color c = colorResistance.getColor();
		double red = c.getRed() / 255D;
		double green = c.getGreen() / 255D;
		double blue = c.getBlue() / 255D;
		Location loc = player.getLocation().subtract(0, LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)), 0);
		player.playSound(player.getLocation(), Sound.ENTITY_GLOW_SQUID_AMBIENT, SoundCategory.PLAYERS, 1.8f, 1.2f);
		player.playSound(player.getLocation(), Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 1.1f, 1.0f);
		player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, SoundCategory.PLAYERS, 1.8f, 0.9f);
		new PPCircle(Particle.REDSTONE, loc, 0.8).delta(0.05, 0.05, 0.05).data(colorResistance).ringMode(true).countPerMeter(5).spawnAsPlayerActive(player);
		new PPCircle(Particle.SPELL_MOB, loc, 1.6).delta(red, green, blue).extra(1).ringMode(true).directionalMode(true).countPerMeter(5).spawnAsPlayerActive(player);
		new PPCircle(Particle.SPELL_MOB_AMBIENT, loc.clone().add(0, 0.5, 0), 2.4).delta(red, green, blue).extra(1).ringMode(true).directionalMode(true).countPerMeter(3).spawnAsPlayerActive(player);
		createOrb(new Vector(FastUtils.randomDoubleInRange(-0.75, 0.75),
			FastUtils.randomDoubleInRange(1, 1.5),
			FastUtils.randomDoubleInRange(-0.75, 0.75)), loc.clone().add(0, 1, 0), player, mob, colorResistance);
	}

	@Override
	public void splashEffectAbsorption(Player player, LivingEntity mob) {
		Color c = colorAbsorption.getColor();
		double red = c.getRed() / 255D;
		double green = c.getGreen() / 255D;
		double blue = c.getBlue() / 255D;
		Location loc = player.getLocation().subtract(0, LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)), 0);
		player.playSound(player.getLocation(), Sound.ENTITY_GLOW_SQUID_AMBIENT, SoundCategory.PLAYERS, 1.8f, 1.2f);
		player.playSound(player.getLocation(), Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 1.1f, 1.0f);
		player.playSound(player.getLocation(), Sound.ENTITY_STRIDER_HAPPY, SoundCategory.PLAYERS, 1.5f, 1.0f);
		new PPCircle(Particle.REDSTONE, loc, 0.8).delta(0.05, 0.05, 0.05).data(colorAbsorption).ringMode(true).countPerMeter(5).spawnAsPlayerActive(player);
		new PPCircle(Particle.SPELL_MOB, loc, 1.6).delta(red, green, blue).extra(1).ringMode(true).directionalMode(true).countPerMeter(5).spawnAsPlayerActive(player);
		new PPCircle(Particle.SPELL_MOB_AMBIENT, loc.clone().add(0, 0.5, 0), 2.4).delta(red, green, blue).extra(1).ringMode(true).directionalMode(true).countPerMeter(3).spawnAsPlayerActive(player);
		createOrb(new Vector(FastUtils.randomDoubleInRange(-0.75, 0.75),
			FastUtils.randomDoubleInRange(1, 1.5),
			FastUtils.randomDoubleInRange(-0.75, 0.75)), loc.clone().add(0, 1, 0), player, mob, colorAbsorption);
	}

	@Override
	public void enhanceCDR(Player player) {
		Location loc = player.getLocation();
		player.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.PLAYERS, 0.9f, 1.75f);
	}

	private void createOrb(Vector dir, Location loc, Player player, LivingEntity target, Particle.DustOptions color) {
		World world = loc.getWorld();
		new BukkitRunnable() {
			final Location mL = target.getLocation().clone();
			int mT = 0;
			double mArcCurve = 0;
			Vector mD = dir.clone();

			@Override
			public void run() {
				mT++;

				Location to = LocationUtils.getHalfHeightLocation(player);

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
						Color c = color.getColor();
						double red = c.getRed() / 255D;
						double green = c.getGreen() / 255D;
						double blue = c.getBlue() / 255D;
						new PartialParticle(Particle.SPELL_MOB_AMBIENT,
							mL.clone().add(FastUtils.randomDoubleInRange(-0.05, 0.05),
								FastUtils.randomDoubleInRange(-0.05, 0.05),
								FastUtils.randomDoubleInRange(-0.05, 0.05)),
							1, red, green, blue, 1)
							.directionalMode(true).spawnAsPlayerActive(player);
					}
					Color c = color.getColor();
					new PartialParticle(Particle.REDSTONE, mL, 1, 0.04, 0.04, 0.04, 0,
						new Particle.DustOptions(c, 1.2f))
						.spawnAsPlayerActive(player);

					if (mT > 5 && mL.distance(to) < 0.35) {
						world.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, SoundCategory.PLAYERS, 1.3f, 1.0f);
						world.playSound(player.getLocation(), Sound.ITEM_GLOW_INK_SAC_USE, SoundCategory.PLAYERS, 2.0f, 0.8f);
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
