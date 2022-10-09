package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DivineJusticeCS implements CosmeticSkill {

	public static final ImmutableMap<String, DivineJusticeCS> SKIN_LIST = ImmutableMap.<String, DivineJusticeCS>builder()
		.put(DarkPunishmentCS.NAME, new DarkPunishmentCS())
		.build();

	private final float HEAL_PITCH_SELF = Constants.NotePitches.C18;
	private final float HEAL_PITCH_OTHER = Constants.NotePitches.E22;

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.DIVINE_JUSTICE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.IRON_SWORD;
	}

	@Override
	public String getName() {
		return null;
	}

	public float getHealPitchSelf() {
		return HEAL_PITCH_SELF;
	}

	public float getHealPitchOther() {
		return HEAL_PITCH_OTHER;
	}

	public void justiceOnDamage(Player mPlayer, LivingEntity enemy, double widerWidthDelta, int combo) {
		PartialParticle partialParticle = new PartialParticle(
			Particle.END_ROD,
			LocationUtils.getHalfHeightLocation(enemy),
			10,
			widerWidthDelta,
			PartialParticle.getHeightDelta(enemy),
			widerWidthDelta,
			0.05
		).spawnAsPlayerActive(mPlayer);
		partialParticle.mParticle = Particle.FLAME;
		partialParticle.spawnAsPlayerActive(mPlayer);

		// /playsound block.anvil.land master @p ~ ~ ~ 0.15 1.5
		mPlayer.getWorld().playSound(
			enemy.getLocation(),
			Sound.BLOCK_ANVIL_LAND,
			0.15f,
			1.5f
		);
	}

	public void justiceKill(Player mPlayer, Location loc) {

	}

	public void justiceHealSound(List<Player> players, float pitch) {
		for (Player healedPlayer : players) {
			healedPlayer.playSound(
				healedPlayer.getLocation(),
				Sound.BLOCK_NOTE_BLOCK_CHIME,
				0.5f,
				pitch
			);
		}
	}
}
