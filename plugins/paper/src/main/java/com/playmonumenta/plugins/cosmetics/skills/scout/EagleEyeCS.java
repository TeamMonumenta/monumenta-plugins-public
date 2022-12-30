package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

public class EagleEyeCS implements CosmeticSkill {

	public static final ImmutableMap<String, EagleEyeCS> SKIN_LIST = ImmutableMap.<String, EagleEyeCS>builder()
		.put(EverseeingEyeCS.NAME, new EverseeingEyeCS())
		.build();

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.EAGLE_EYE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENDER_EYE;
	}

	@Override
	public @Nullable String getName() {
		return null;
	}

	public void eyeStart(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.5f, 1.25f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.5f, 1.25f);
	}

	public void eyeOnTarget(World world, Player mPlayer, LivingEntity mob) {
		world.playSound(mob.getLocation(), Sound.ENTITY_PARROT_IMITATE_SHULKER, SoundCategory.PLAYERS, 0.4f, 0.7f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mob.getLocation().add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
	}

	public void eyeFirstStrike(World world, Player mPlayer, LivingEntity mob) {
		//Nope!
	}

	public Team createTeams() {
		return ScoreboardUtils.getExistingTeamOrCreate("eagleEyeColor", NamedTextColor.YELLOW);
	}
}
