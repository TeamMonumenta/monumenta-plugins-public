package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;



public class DarkPunishmentCS extends DivineJusticeCS {
	//Darker divine justice. Depth set: shadow
	//Dark CLERIC!

	public static final String NAME = "Dark Punishment";

	private final float HEAL_PITCH_SELF = 1.5f;
	private final float HEAL_PITCH_OTHER = 1.75f;

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName());
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.DIVINE_JUSTICE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHERITE_SWORD;
	}

	@Override
	public float getHealPitchSelf() {
		return HEAL_PITCH_SELF;
	}

	@Override
	public float getHealPitchOther() {
		return HEAL_PITCH_OTHER;
	}

	@Override
	public void justiceOnDamage(Player mPlayer, LivingEntity enemy, double widerWidthDelta) {
		mPlayer.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.6f, 1.65f);
		mPlayer.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.5f);
		PartialParticle partialParticle = new PartialParticle(
			Particle.DRAGON_BREATH,
			LocationUtils.getHalfHeightLocation(enemy),
			10,
			widerWidthDelta,
			PartialParticle.getHeightDelta(enemy),
			widerWidthDelta,
			0.05
		).spawnAsPlayerActive(mPlayer);
		partialParticle.mParticle = Particle.SPELL_WITCH;
		partialParticle.spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void justiceHealSound(List<Player> players, float pitch) {
			for (Player healedPlayer : players) {
				healedPlayer.playSound(
					healedPlayer.getLocation(),
					Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED,
					0.5f,
					pitch
				);
			}
	}
}
