package com.playmonumenta.plugins.cosmetics.skills.cleric.paladin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.format.NamedTextColor;
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

public class LuminousInfusionCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.LUMINOUS_INFUSION;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLAZE_POWDER;
	}

	public void infusionStartEffect(World world, Player player, Location loc, int stacks) {
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.8f, 2.0f);
		world.playSound(loc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 0.6f, 1.5f);
		world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 0.6f, 2.0f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.4f, 0.2f);
		world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.3f, 2.0f);
		new PartialParticle(Particle.SPELL_INSTANT, loc, 50, 0.75f, 0.25f, 0.75f, 1).spawnAsPlayerActive(player);
	}

	public void infusionAddStack(World world, Player player, Location loc, int stacks) {
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.5f, 2.0f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.5f, 0.8f);
		new PartialParticle(Particle.SPELL_INSTANT, loc, 10, 0.75f, 0.25f, 0.75f, 1).spawnAsPlayerActive(player);
	}

	public void gainMaxCharge(Player player, Location loc) {
		player.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 0.7f, 1.8f);
		new PartialParticle(Particle.SPELL_INSTANT, loc, 15, 0.75f, 0.25f, 0.75f, 1).spawnAsPlayerActive(player);
	}

	public void infusionStartMsg(Player player, int stacks) {
		MessagingUtils.sendActionBarMessage(player, "Holy energy radiates from your hands... (" + stacks + ")", NamedTextColor.YELLOW);
	}

	public void infusionExpireMsg(Player player) {
		MessagingUtils.sendActionBarMessage(player, "The light from your hands fades...", NamedTextColor.YELLOW);
	}

	public void infusionTickEffect(Player player, int tick) {
		Location rightHand = PlayerUtils.getRightSide(player.getEyeLocation(), 0.45).subtract(0, .8, 0);
		Location leftHand = PlayerUtils.getRightSide(player.getEyeLocation(), -0.45).subtract(0, .8, 0);
		new PartialParticle(Particle.SPELL_INSTANT, leftHand, 1, 0.05f, 0.05f, 0.05f, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_INSTANT, rightHand, 1, 0.05f, 0.05f, 0.05f, 0).spawnAsPlayerActive(player);
	}

	public void infusionHitEffect(World world, Player player, LivingEntity damagee, double radius, double ratio, float volumeScaling) {
		Location loc = damagee.getLocation();
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, (int) (ratio * 100), 0.05f, 0.03f, 0.05f, 0.05 * radius).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLAME, loc, (int) (ratio * 75), 0.05f, 0.05f, 0.03f, 0.15).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.4f * volumeScaling, 1.1f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, volumeScaling, 1.1f);
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 2.0f * volumeScaling, 0.1f);
		world.playSound(loc, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 0.8f * volumeScaling, 0.1f);
		world.playSound(loc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 0.8f * volumeScaling, 0.6f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.8f * volumeScaling, 0.1f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, volumeScaling, 0.6f);
		world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.1f * volumeScaling, 1.1f);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				new PPCircle(Particle.REDSTONE, loc.clone().add(0, 0.2, 0), radius * mTicks / 4)
					.countPerMeter(4)
					.data(new Particle.DustOptions(ParticleUtils.getTransition(Color.WHITE, Color.YELLOW, mTicks / 4.0), 0.8f))
					.spawnAsPlayerActive(player);

				new PPCircle(Particle.FIREWORKS_SPARK, loc.clone().add(0, 0.2, 0), radius * mTicks / 4)
					.countPerMeter(2)
					.delta(0.4)
					.spawnAsPlayerActive(player);
				if (mTicks > 4) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public void infusionSpreadEffect(World world, Player player, LivingEntity damagee, LivingEntity e, float volume) {
		Location loc = damagee.getLocation();
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 10, 0.05f, 0.05f, 0.05f, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLAME, loc, 7, 0.05f, 0.05f, 0.05f, 0.1).spawnAsPlayerActive(player);
	}
}
