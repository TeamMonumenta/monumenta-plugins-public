package com.playmonumenta.plugins.cosmetics.skills.cleric.hierophant;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class EnchantedPrayerCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ENCHANTED_PRAYER;
	}

	@Override
	public Material getDisplayItem() {
		return Material.CHORUS_FRUIT;
	}

	public void onCast(Plugin plugin, Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 0.8f, 2.0f);
		world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 0.8f, 1.2f);
		world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.1f, 0.7f);

		new BukkitRunnable() {
			final Location mLoc = player.getLocation().add(0, 0.15, 0);
			double mRadius = 0;

			@Override
			public void run() {
				mRadius += 0.25;
				new PPCircle(Particle.SPELL_INSTANT, mLoc, mRadius).count(72).delta(0.15).spawnAsPlayerActive(player);
				if (mRadius >= 5) {
					this.cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 1);
	}

	public void applyToPlayer(Player otherPlayer, Player user) {
		Location loc = otherPlayer.getLocation();
		otherPlayer.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.8f, 2.0f);
		new PartialParticle(Particle.SPELL_INSTANT, loc, 50, 0.25, 0, 0.25, 0.01).spawnAsPlayerActive(user);
	}

	public void onEffectTrigger(Player player, World world, Location loc, LivingEntity enemy) {
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1.2f, 0.7f);
		world.playSound(loc, Sound.ENTITY_BLAZE_DEATH, SoundCategory.PLAYERS, 0.5f, 1.8f);
		world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 0.8f, 1.6f);
		world.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.2f, 0.6f);
		world.playSound(loc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.1f, 0.6f);
		new PartialParticle(Particle.SPELL_INSTANT, loc.clone().add(0, enemy.getHeight() / 2, 0), 100, 0.25f, 0.3f, 0.25f, 1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc.clone().add(0, enemy.getHeight() / 2, 0), 75, 0, 0, 0, 0.3).spawnAsPlayerActive(player);
	}

	public void effectTick(Player player) {
		Location loc = player.getEyeLocation();
		Location rightHand = PlayerUtils.getRightSide(loc, 0.45).subtract(0, .8, 0);
		Location leftHand = PlayerUtils.getRightSide(loc, -0.45).subtract(0, .8, 0);
		new PartialParticle(Particle.SPELL_INSTANT, leftHand, 2, 0.05f, 0.05f, 0.05f, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_INSTANT, rightHand, 2, 0.05f, 0.05f, 0.05f, 0).spawnAsPlayerActive(player);
	}
}
