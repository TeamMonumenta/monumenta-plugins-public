package com.playmonumenta.plugins.abilities.scout.ranger;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Reflexes extends Ability {

	private static final double REFLEXES_1_DAMAGE_RESISTANCE_PER_MOB = 0.02;
	private static final double REFLEXES_2_DAMAGE_RESISTANCE_PER_MOB = 0.03;
	private static final double REFLEXES_1_DAMAGE_BONUS_PER_MOB = 0.03;
	private static final double REFLEXES_2_DAMAGE_BONUS_PER_MOB = 0.05;
	private static final int REFLEXES_MAX_MOBS = 10;
	private static final int REFLEXES_MOB_RADIUS = 8;

	private final double mDamageResistancePerMob;
	private final double mDamageBonusPerMob;

	public Reflexes(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Reflexes");
		mInfo.mScoreboardId = "Reflexes";
		mInfo.mShorthandName = "Re";
		mInfo.mDescriptions.add("For each mob in an 8 block radius (up to 10 mobs), gain 2% damage resistance and 3% extra damage.");
		mInfo.mDescriptions.add("Instead, gain 3% damage resistance and 5% extra damage for each mob.");
		mInfo.mLinkedSpell = Spells.REFLEXES;
		mInfo.mIgnoreTriggerCap = true;

		mDamageResistancePerMob = getAbilityScore() == 1 ? REFLEXES_1_DAMAGE_RESISTANCE_PER_MOB : REFLEXES_2_DAMAGE_RESISTANCE_PER_MOB;
		mDamageBonusPerMob = getAbilityScore() == 1 ? REFLEXES_1_DAMAGE_BONUS_PER_MOB : REFLEXES_2_DAMAGE_BONUS_PER_MOB;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		event.setDamage(event.getDamage() *
				(1 + mDamageBonusPerMob * Math.min(REFLEXES_MAX_MOBS, EntityUtils.getNearbyMobs(mPlayer.getLocation(), REFLEXES_MOB_RADIUS).size())));
		return true;
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(event,
				1 - mDamageResistancePerMob * Math.min(REFLEXES_MAX_MOBS, EntityUtils.getNearbyMobs(mPlayer.getLocation(), REFLEXES_MOB_RADIUS).size())));
		return true;
	}

}
