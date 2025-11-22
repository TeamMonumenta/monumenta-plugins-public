package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.DodgingCS;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.redissync.utils.ScoreboardUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class MagicDodging extends Ability {
	public static AbilityInfo<MagicDodging> INFO =
		new AbilityInfo<>(MagicDodging.class, "Magic Dodging", MagicDodging::new)
			.linkedSpell(ClassAbility.MAGIC_DODGING)
			.scoreboardId("Dodging")
			.shorthandName("Mdg")
			.simpleDescription("Dodge a magic attack that would otherwise hit you.")
			.canUse(p -> ScoreboardUtils.getScoreboardValue(p.getName(), "Dodging") > 2 && ServerProperties.getAbilityEnhancementsEnabled(p))
			.cooldown((int) (Dodging.DODGING_COOLDOWN_1 * Dodging.MAGIC_DODGING_COOLDOWN_MULTIPLIER), (int) (Dodging.DODGING_COOLDOWN_2 * Dodging.MAGIC_DODGING_COOLDOWN_MULTIPLIER), Dodging.CHARM_COOLDOWN, Dodging.CHARM_MAGIC_DODGING_COOLDOWN);

	private final double mSpeed;
	private final int mDuration;

	private final DodgingCS mCosmetic;
	private int mTriggerTick = 0;

	public MagicDodging(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mSpeed = Dodging.PERCENT_SPEED + CharmManager.getLevelPercentDecimal(mPlayer, Dodging.CHARM_SPEED);
		mDuration = CharmManager.getDuration(mPlayer, Dodging.CHARM_DURATION, Dodging.DODGING_SPEED_EFFECT_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new DodgingCS());
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		// See if we should dodge. If false, allow the event to proceed normally
		if (event.getType() == DamageEvent.DamageType.MAGIC && !event.isBlocked() && dodge()) {
			mPlayer.setNoDamageTicks(20);
			mPlayer.setLastDamage(event.getDamage());
			event.setFlatDamage(0);
			event.setCancelled(true);
		}
	}

	private boolean dodge() {
		if (mTriggerTick == Bukkit.getServer().getCurrentTick()) {
			// Dodging was activated this tick - allow it
			return true;
		}

		if (isOnCooldown()) {
			/*
			 * This ability is actually on cooldown (and was not triggered this tick)
			 * Don't process dodging
			 */
			return false;
		}

		/*
		 * Make note of which tick this triggered on so that any other event that triggers this
		 * tick will also be dodged
		 */
		mTriggerTick = Bukkit.getServer().getCurrentTick();
		putOnCooldown();

		Location loc = mPlayer.getLocation().add(0, 1, 0);
		World world = mPlayer.getWorld();
		if (isLevelTwo()) {
			mPlugin.mEffectManager.addEffect(mPlayer, Dodging.ATTR_NAME, new PercentSpeed(mDuration, mSpeed, Dodging.ATTR_NAME).deleteOnAbilityUpdate(true));

			mCosmetic.dodgeEffectLv2(mPlayer, world, loc);
		}
		mCosmetic.dodgeEffect(mPlayer, world, loc);
		return true;
	}
}
