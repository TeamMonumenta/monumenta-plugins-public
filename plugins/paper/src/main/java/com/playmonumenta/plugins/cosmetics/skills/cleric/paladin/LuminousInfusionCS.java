package com.playmonumenta.plugins.cosmetics.skills.cleric.paladin;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
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
import org.jetbrains.annotations.Nullable;

public class LuminousInfusionCS implements CosmeticSkill {

	public static final ImmutableMap<String, LuminousInfusionCS> SKIN_LIST = ImmutableMap.<String, LuminousInfusionCS>builder()
		.put(PrestigiousInfusionCS.NAME, new PrestigiousInfusionCS())
		.build();

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.LUMINOUS_INFUSION;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLAZE_POWDER;
	}

	@Override
	public @Nullable String getName() {
		return null;
	}

	public void infusionStartEffect(World world, Player mPlayer) {
		MessagingUtils.sendActionBarMessage(mPlayer, "Holy energy radiates from your hands...");
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1, 1);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 1, 1.65f);
		new PartialParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 50, 0.75f, 0.25f, 0.75f, 1).spawnAsPlayerActive(mPlayer);
	}

	public void infusionExpireMsg(Player mPlayer) {
		MessagingUtils.sendActionBarMessage(mPlayer, "The light from your hands fades...");
	}

	public void infusionTickEffect(Player mPlayer, int tick) {
		Location rightHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), 0.45).subtract(0, .8, 0);
		Location leftHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), -0.45).subtract(0, .8, 0);
		new PartialParticle(Particle.SPELL_INSTANT, leftHand, 1, 0.05f, 0.05f, 0.05f, 0).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_INSTANT, rightHand, 1, 0.05f, 0.05f, 0.05f, 0).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
	}

	public void infusionHitEffect(World world, Player mPlayer, LivingEntity damagee) {
		Location loc = damagee.getLocation();
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 100, 0.05f, 0.05f, 0.05f, 0.3).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLAME, loc, 75, 0.05f, 0.05f, 0.05f, 0.3).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.8f, 1.1f);
	}

	public void infusionSpreadEffect(World world, Player mPlayer, LivingEntity damagee, LivingEntity e, float volume) {
		Location loc = damagee.getLocation();
		world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, volume, 1.1f);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 10, 0.05f, 0.05f, 0.05f, 0.1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLAME, loc, 7, 0.05f, 0.05f, 0.05f, 0.1).spawnAsPlayerActive(mPlayer);
	}
}
