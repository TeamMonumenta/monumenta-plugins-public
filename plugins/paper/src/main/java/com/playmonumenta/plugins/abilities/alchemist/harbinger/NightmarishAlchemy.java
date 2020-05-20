package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import java.util.Collection;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public class NightmarishAlchemy extends Ability {
	private static final int NIGHTMARISH_ALCHEMY_1_DAMAGE = 2;
	private static final int NIGHTMARISH_ALCHEMY_2_DAMAGE = 4;
	private static final int NIGHTMARISH_ALCHEMY_CONFUSION_DURATION = 20 * 4;
	private static final float NIGHTMARISH_ALCHEMY_1_CONFUSION_CHANCE = 0.1f;
	private static final float NIGHTMARISH_ALCHEMY_2_CONFUSION_CHANCE = 0.2f;

	private final float mConfusionChance;
	private final int mDamage;

	public NightmarishAlchemy(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Nightmarish Alchemy");
		mInfo.linkedSpell = Spells.NIGHTMARISH_ALCHEMY;
		mInfo.scoreboardId = "Nightmarish";
		mInfo.mShorthandName = "Nm";
		mInfo.mDescriptions.add("Your Alchemist Potions deal +2 damage. Non-boss enemies hit have a 10% chance to attack other enemies for 4s.");
		mInfo.mDescriptions.add("Your Alchemist Potions deal +4 damage instead and the chance of confusing enemies is increased to 20%.");
		mDamage = getAbilityScore() == 1 ? NIGHTMARISH_ALCHEMY_1_DAMAGE : NIGHTMARISH_ALCHEMY_2_DAMAGE;
		mConfusionChance = getAbilityScore() == 1 ? NIGHTMARISH_ALCHEMY_1_CONFUSION_CHANCE : NIGHTMARISH_ALCHEMY_2_CONFUSION_CHANCE;
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		boolean guaranteedApplicationApplied = false;
		if (potion.hasMetadata("AlchemistPotion")) {
			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				int size = affectedEntities.size();
				for (LivingEntity entity : affectedEntities) {
					if (EntityUtils.isHostileMob(entity)) {
						guaranteedApplicationApplied = apply(entity, size, guaranteedApplicationApplied);
					}
				}
			}
			mWorld.spawnParticle(Particle.SPELL_WITCH, potion.getLocation(), 50, 1, 0, 1, 1);
		}

		return true;
	}

	public boolean apply(LivingEntity mob, int size, boolean guaranteedApplicationApplied) {
		EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.ALCHEMY, true, mInfo.linkedSpell);
		if (mob instanceof Mob) {
			if (FastUtils.RANDOM.nextDouble() < mConfusionChance || !guaranteedApplicationApplied && size >= 1 / mConfusionChance) {
				EntityUtils.applyConfusion(mPlugin, NIGHTMARISH_ALCHEMY_CONFUSION_DURATION, mob);
				guaranteedApplicationApplied = true;
			}
		}

		return guaranteedApplicationApplied;
	}

}
