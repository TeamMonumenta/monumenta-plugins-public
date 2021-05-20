package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public class NightmarishAlchemy extends PotionAbility {
	private static final int NIGHTMARISH_ALCHEMY_1_DAMAGE = 2;
	private static final int NIGHTMARISH_ALCHEMY_2_DAMAGE = 4;
	private static final int NIGHTMARISH_ALCHEMY_CONFUSION_DURATION = 20 * 4;
	private static final float NIGHTMARISH_ALCHEMY_1_CONFUSION_CHANCE = 0.1f;
	private static final float NIGHTMARISH_ALCHEMY_2_CONFUSION_CHANCE = 0.2f;

	private final float mConfusionChance;

	public NightmarishAlchemy(Plugin plugin, Player player) {
		super(plugin, player, "Nightmarish Alchemy", NIGHTMARISH_ALCHEMY_1_DAMAGE, NIGHTMARISH_ALCHEMY_2_DAMAGE);
		mInfo.mLinkedSpell = ClassAbility.NIGHTMARISH_ALCHEMY;
		mInfo.mScoreboardId = "Nightmarish";
		mInfo.mShorthandName = "Nm";
		mInfo.mDescriptions.add("Your Alchemist Potions deal +2 damage. Non-boss enemies hit have a 10% chance to attack other enemies for 4s.");
		mInfo.mDescriptions.add("Your Alchemist Potions deal +4 damage instead and the chance of confusing enemies is increased to 20%.");

		mConfusionChance = getAbilityScore() == 1 ? NIGHTMARISH_ALCHEMY_1_CONFUSION_CHANCE : NIGHTMARISH_ALCHEMY_2_CONFUSION_CHANCE;
	}

	@Override
	public void createAura(Location loc, double radius) {
		loc.getWorld().spawnParticle(Particle.SPELL_WITCH, loc, 50, 1, 0, 1, 1);
	}

	@Override
	public void apply(LivingEntity mob) {
		if (mob instanceof Mob && FastUtils.RANDOM.nextDouble() < mConfusionChance) {
			EntityUtils.applyConfusion(mPlugin, NIGHTMARISH_ALCHEMY_CONFUSION_DURATION, mob);
		}
	}

}
