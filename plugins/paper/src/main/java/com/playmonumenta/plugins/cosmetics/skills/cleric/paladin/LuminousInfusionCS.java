package com.playmonumenta.plugins.cosmetics.skills.cleric.paladin;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

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
		MessagingUtils.sendActionBarMessage(player, "Holy energy radiates from your hands... (" + stacks + ")");
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
		MessagingUtils.sendActionBarMessage(player, "Holy energy radiates from your hands... (" + stacks + ")");
	}

	public void infusionExpireMsg(Player player) {
		MessagingUtils.sendActionBarMessage(player, "The light from your hands fades...");
	}

	public void infusionTickEffect(Player player, int tick) {
		Location rightHand = PlayerUtils.getRightSide(player.getEyeLocation(), 0.45).subtract(0, .8, 0);
		Location leftHand = PlayerUtils.getRightSide(player.getEyeLocation(), -0.45).subtract(0, .8, 0);
		new PartialParticle(Particle.SPELL_INSTANT, leftHand, 1, 0.05f, 0.05f, 0.05f, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_INSTANT, rightHand, 1, 0.05f, 0.05f, 0.05f, 0).spawnAsPlayerActive(player);
	}

	public void infusionHitEffect(World world, Player player, LivingEntity damagee, double radius, double ratio, float volumeScaling) {
		Location loc = damagee.getLocation();
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, (int) (ratio * 100), 0.05f, 0.05f, 0.05f, 0.3).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLAME, loc, (int) (ratio * 75), 0.05f, 0.05f, 0.05f, 0.3).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.55f * volumeScaling, 1.1f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 1.0f * volumeScaling, 1.1f);
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 2.0f * volumeScaling, 0.1f);
		world.playSound(loc, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 0.8f * volumeScaling, 0.1f);
		world.playSound(loc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 0.8f * volumeScaling, 0.6f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.8f * volumeScaling, 0.1f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 1.0f * volumeScaling, 0.6f);
	}

	public void infusionSpreadEffect(World world, Player player, LivingEntity damagee, LivingEntity e, float volume) {
		Location loc = damagee.getLocation();
		world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, volume, 1.1f);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 10, 0.05f, 0.05f, 0.05f, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLAME, loc, 7, 0.05f, 0.05f, 0.05f, 0.1).spawnAsPlayerActive(player);
	}
}
