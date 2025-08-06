package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DivineJusticeCS implements CosmeticSkill {

	private final float HEAL_PITCH_SELF = Constants.NotePitches.C18;
	private final float HEAL_PITCH_OTHER = Constants.NotePitches.E22;

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.DIVINE_JUSTICE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.IRON_SWORD;
	}

	public float getHealPitchSelf() {
		return HEAL_PITCH_SELF;
	}

	public float getHealPitchOther() {
		return HEAL_PITCH_OTHER;
	}

	public void justiceOnDamage(Player player, LivingEntity enemy, World world, Location enemyLoc, double widerWidthDelta, int combo, boolean enhanced) {
		PartialParticle partialParticle = new PartialParticle(
			Particle.END_ROD,
			LocationUtils.getHalfHeightLocation(enemy),
			10,
			widerWidthDelta,
			PartialParticle.getHeightDelta(enemy),
			widerWidthDelta,
			0.08
		).spawnAsPlayerActive(player);
		partialParticle.mParticle = Particle.FLAME;
		partialParticle.spawnAsPlayerActive(player);

		world.playSound(enemyLoc, Sound.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.15f, 1.5f);
		world.playSound(enemyLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.5f, 2.0f);
		world.playSound(enemyLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.playSound(enemyLoc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.5f, 1.2f);
		if (combo == 2) {
			world.playSound(enemyLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, enhanced ? 1.0f : 0.8f, 1.2f);
			if (enhanced) {
				world.playSound(enemyLoc, Sound.BLOCK_AMETHYST_CLUSTER_STEP, SoundCategory.PLAYERS, 1.3f, 1f);
			}
		}
	}

	public void justiceKill(Player player, Location loc) {
		new PPCircle(Particle.TOTEM, loc.clone().add(0, 0.2, 0), 0.5)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(0.5, 0, 0)
			.count(20)
			.extra(1)
			.spawnAsPlayerActive(player);
		new PartialParticle(Particle.HEART, loc.clone().add(0, 1, 0), 2, 0.1, 0.1, 0.1, 0.001).spawnAsPlayerActive(player);
	}

	public void justiceHealSound(List<Player> players, float pitch) {
		for (Player healedPlayer : players) {
			healedPlayer.playSound(
				healedPlayer.getLocation(),
				Sound.BLOCK_NOTE_BLOCK_CHIME,
				SoundCategory.PLAYERS,
				1f,
				pitch
			);
		}
	}
}
