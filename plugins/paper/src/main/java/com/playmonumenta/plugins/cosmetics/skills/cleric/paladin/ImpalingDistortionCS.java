package com.playmonumenta.plugins.cosmetics.skills.cleric.paladin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLightning;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.AbstractMap;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
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

public class ImpalingDistortionCS extends LuminousInfusionCS {

	public static final String NAME = "Impaling Distortion";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Time and space seem to meld and tear at once.",
			"The world shifts, expanding yet shrinking, speeding up yet slowing down,",
			"as the mind dissolves within the infinite.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.AMETHYST_SHARD;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	Particle.DustOptions CYAN = new Particle.DustOptions(Color.fromRGB(50, 150, 200), 1.0f);
	Particle.DustOptions PURPLE = new Particle.DustOptions(Color.fromRGB(120, 120, 200), 1.0f);
	double mRed1 = CYAN.getColor().getRed() / 255.0;
	double mGreen1 = CYAN.getColor().getGreen() / 255.0;
	double mBlue1 = CYAN.getColor().getBlue() / 255.0;
	double mRed2 = PURPLE.getColor().getRed() / 255.0;
	double mGreen2 = PURPLE.getColor().getGreen() / 255.0;
	double mBlue2 = PURPLE.getColor().getBlue() / 255.0;

	@Override
	public void infusionStartEffect(World world, Player player, Location loc, int stacks) {
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.6f, 0.6f);
		ParticleUtils.explodingRingEffect(Plugin.getInstance(), loc.subtract(0, LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)), 0), 2, 0, 4,
			List.of(
				new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(1.0, (Location location) ->
					new PartialParticle(Particle.REDSTONE, location, 1, 0, 0, 0, 0.1).data(CYAN).spawnAsPlayerActive(player))
			)
		);
	}

	@Override
	public void infusionAddStack(World world, Player player, Location loc, int stacks) {
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.6f, 1.2f);
	}

	@Override
	public void gainMaxCharge(Player player, Location loc) {
		player.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.8f, 0.8f);
		new PartialParticle(Particle.SPELL_MOB, loc, 15, mRed1, mGreen1, mBlue1, 1).directionalMode(true).spawnAsPlayerActive(player);
	}

	@Override
	public void infusionStartMsg(Player player, int stacks) {
		MessagingUtils.sendActionBarMessage(player, "An intense pressure emerges in the air... (" + stacks + ")", TextColor.color(50, 180, 200));
	}

	@Override
	public void infusionExpireMsg(Player player) {
		MessagingUtils.sendActionBarMessage(player, "The pressure subsides as the air stabilizes...", TextColor.color(50, 180, 200));
	}

	@Override
	public void infusionTickEffect(Player player, int tick) {
		Location loc = player.getEyeLocation().add(FastUtils.randomDoubleInRange(-2, 2), FastUtils.randomDoubleInRange(-2, 1), FastUtils.randomDoubleInRange(-2, 2));
		new PartialParticle(Particle.SPELL_MOB, loc, 1, mRed1, mGreen1, mBlue1, 1).directionalMode(true).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_MOB, loc, 1, mRed2, mGreen2, mBlue2, 1).directionalMode(true).spawnAsPlayerActive(player);
	}

	@Override
	public void infusionHitEffect(World world, Player player, LivingEntity damagee, double radius, double ratio, float volumeScaling) {
		Location loc = damagee.getLocation();
		world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.9f * volumeScaling, 1.7f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.7f * volumeScaling, 0.9f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1.3f * volumeScaling, 0.8f);
		world.playSound(loc, Sound.ENTITY_ALLAY_DEATH, SoundCategory.PLAYERS, 0.9f * volumeScaling, 0.8f);
		new PartialParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(player);
		new PPLightning(Particle.REDSTONE, loc).maxWidth(2).hopXZ(2).hopY(0).height(6 + 6 * ratio).duration(3).count(12).data(CYAN).spawnAsPlayerActive(player);
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				for (int i = 0; i < Math.max(1, ratio * 4); i++) {
					Particle.DustOptions colorTransition = new Particle.DustOptions(Color.fromRGB(30 * mTicks, 200 - 20 * mTicks, 200), 1.1f);
					Location loc = damagee.getLocation().add(FastUtils.randomDoubleInRange(-mTicks - 2, mTicks + 2), 0.5 * damagee.getHeight() + FastUtils.randomDoubleInRange(-mTicks + 2, mTicks + 2), FastUtils.randomDoubleInRange(-mTicks - 2, mTicks + 2));
					Vector dir = LocationUtils.getDirectionTo(damagee.getLocation(), loc);
					ParticleUtils.drawParticleLineSlash(loc, dir, 0, 3, 0.1, 3,
						(Location lineLoc, double middleProgress, double endProgress, boolean middle) ->
							new PartialParticle(Particle.REDSTONE, lineLoc, 1, 0.0, 0.0, 0.0, 0.0).data(colorTransition).spawnAsPlayerActive(player));
				}
				new PartialParticle(Particle.SWEEP_ATTACK, loc, 1, 1.25f, 1.8f, 1.25f).spawnAsPlayerActive(player);
				world.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 1.75f * volumeScaling, 0.8f);
				mTicks++;
				if (mTicks > 5) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
		// Hieroglyph for "Tremble"
		Vector front = player.getEyeLocation().getDirection().setY(0).normalize().multiply(radius / 2);
		Vector left120 = VectorUtils.rotateTargetDirection(front, -120, 0);
		Vector left60 = VectorUtils.rotateTargetDirection(front, -60, 0);
		Vector left30 = VectorUtils.rotateTargetDirection(front, -30, 0);
		Vector right120 = VectorUtils.rotateTargetDirection(front, 120, 0);
		Vector right60 = VectorUtils.rotateTargetDirection(front, 60, 0);
		Vector side90 = VectorUtils.rotateTargetDirection(front, 90, 0);
		Vector bottomLeft150 = VectorUtils.rotateTargetDirection(front, -150, 0).multiply(2);
		Vector bottomRight150 = VectorUtils.rotateTargetDirection(front, 150, 0).multiply(2);
		Location loc1 = loc.clone().subtract(front);
		Location loc2 = loc.clone().add(left60);
		Location loc3 = loc.clone().add(right60);
		Location loc4 = loc.clone().add(bottomLeft150);
		Location loc5 = loc.clone().add(bottomRight150);
		for (int i = 0; i < 2; i++) {
			double delta = 0.2 * i;
			final Particle.DustOptions PURPLE = new Particle.DustOptions(Color.fromRGB(140 - 40 * i, 100 - 30 * i, 220 - 50 * i), 1.2f - i * 0.2f);
			new PPLine(Particle.REDSTONE, loc1, loc2).data(PURPLE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc1, loc3).data(PURPLE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc2, loc3).data(PURPLE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc4, loc5).data(PURPLE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc2, loc2.clone().add(left30)).data(PURPLE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc3, loc3.clone().subtract(left30)).data(PURPLE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc.clone().add(left120.clone().multiply(0.5)), loc4.clone().add(side90.clone().multiply(0.5))).data(PURPLE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc.clone().add(right120.clone().multiply(0.5)), loc5.clone().subtract(side90.clone().multiply(0.5))).data(PURPLE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
		}
		new PPCircle(Particle.ENCHANTMENT_TABLE, loc, radius).countPerMeter(12).extraRange(0.1, 0.15).innerRadiusFactor(1)
			.directionalMode(true).delta(2, 1, -8).rotateDelta(true).spawnAsPlayerActive(player);
	}

	@Override
	public void infusionSpreadEffect(World world, Player player, LivingEntity damagee, LivingEntity e, float volume) {
		Location loc = damagee.getLocation();
		world.playSound(loc, Sound.ENTITY_GLOW_SQUID_AMBIENT, SoundCategory.PLAYERS, volume, 1.8f);
		new PartialParticle(Particle.GLOW, loc, 10, 0.05f, 0.05f, 0.05f, 0.1).spawnAsPlayerActive(player);
	}
}
