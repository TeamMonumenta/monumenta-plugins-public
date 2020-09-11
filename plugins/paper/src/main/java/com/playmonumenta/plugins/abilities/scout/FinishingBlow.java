package com.playmonumenta.plugins.abilities.scout;

import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.effects.FinishingBlowBonusDamage;

public class FinishingBlow extends Ability {

	private static final String FINISHING_BLOW_DAMAGE_BONUS_EFFECT_NAME_PREFIX = "FinishingBlowDamageBonus";
	private static final int DURATION = 20 * 5;
	private static final double PERCENT_OF_BOW_DAMAGE_1 = 0.15;
	private static final double PERCENT_OF_BOW_DAMAGE_2 = 0.25;

	public static final double LOW_HEALTH_THRESHOLD = 0.5;
	public static final int LOW_HEALTH_DAMAGE_DEALT_MULTIPLIER = 2;


	private final double mPercentOfBowDamage;

	public FinishingBlow(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Finishing Blow");
		mInfo.mScoreboardId = "FinishingBlow";
		mInfo.mDescriptions.add("Shooting an enemy with a bow marks it for 5 seconds. If you melee attack a marked enemy, remove the mark and deal bonus damage equal to 10% of the damage of the bow shot that left the mark. The bonus damage is doubled on enemies below 50% health.");
		mInfo.mDescriptions.add("Bonus damage increased to 20% of the damage of the bow shot.");
		mPercentOfBowDamage = getAbilityScore() == 1 ? PERCENT_OF_BOW_DAMAGE_1 : PERCENT_OF_BOW_DAMAGE_2;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (proj instanceof Arrow || proj instanceof SpectralArrow) {
			mPlugin.mEffectManager.addEffect(damagee, FINISHING_BLOW_DAMAGE_BONUS_EFFECT_NAME_PREFIX + mPlayer.getName(),
					new FinishingBlowBonusDamage(DURATION, event.getDamage() * mPercentOfBowDamage, mPlayer));
		}

		return true;
	}

}
