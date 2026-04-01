package com.playmonumenta.plugins.abilities.cleric.seraph;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithCustomDisplay;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.cleric.TouchofRadiance;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.redissync.utils.ScoreboardUtils;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;


public class KeeperVirtueHarmingFlare extends MultipleChargeAbility implements AbilityWithCustomDisplay {

	public static final AbilityInfo<KeeperVirtueHarmingFlare> INFO =
		new AbilityInfo<>(KeeperVirtueHarmingFlare.class, "Keeper Virtue Harming Flare", KeeperVirtueHarmingFlare::new)
			.linkedSpell(ClassAbility.KEEPER_VIRTUE_HARMING)
			.scoreboardId("KeeperVirtue")
			.shorthandName("KV")
			.simpleDescription("An angelic spirit follows you, supporting nearby players and attacking mobs.")
			.canUse(p -> ScoreboardUtils.getScoreboardValue(p.getName(), "KeeperVirtue") > 0 && ServerProperties.getClassSpecializationsEnabled(p))
			.cooldown(KeeperVirtue.HARMING_COOLDOWN_1, KeeperVirtue.HARMING_COOLDOWN_2, KeeperVirtue.CHARM_HARMING_COOLDOWN);

	private @Nullable KeeperVirtue mKeeperVirtue;

	public KeeperVirtueHarmingFlare(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = KeeperVirtue.HARMING_FLARE_CAPACITY + (int) CharmManager.getLevel(player, KeeperVirtue.CHARM_HARMING);
		mCharges = getChargesOffCooldown();
		Bukkit.getScheduler().runTask(plugin, () -> mKeeperVirtue = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, KeeperVirtue.class));

	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if ((event.getType() != DamageEvent.DamageType.MELEE && event.getType() != DamageEvent.DamageType.PROJECTILE && (event.getAbility() == null || (event.getAbility() != ClassAbility.ETHEREAL_ASCENSION && event.getAbility() != ClassAbility.HALLOWED_BEAM)))) {
			return false;
		}
		if (mKeeperVirtue == null || getCharges() <= 0) {
			return true;
		}
		// We're doing two delays, so need to avoid rounding errors
		// (for example, we need a 9 tick delay to be 4 and 5, not 2x 4.5 rounding up to 2x 5)
		int actionTime = mKeeperVirtue.mActionTime / 2;
		int remainder = mKeeperVirtue.mActionTime - actionTime;

		mKeeperVirtue.mTarget = enemy;
		mKeeperVirtue.mTicksSinceTargetChance = 0;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			if (mKeeperVirtue == null) {
				return;
			}
			// Wait a few ticks first as we don't want to fire flares at dead mobs
			LivingEntity target = enemy;
			if (!enemy.isValid()) {
				target = EntityUtils.getNearestMob(enemy.getLocation(), mKeeperVirtue.mRedirectRange);
			}
			LivingEntity finalTarget = target;
			if (finalTarget != null && finalTarget.isValid() && mKeeperVirtue.mBoss != null && mPlayer.getLocation().distance(finalTarget.getLocation()) <= mKeeperVirtue.mActionRange) {
				if (consumeCharge()) {
					mKeeperVirtue.mTarget = finalTarget;
					mKeeperVirtue.mTicksSinceTargetChance = 0;

					mKeeperVirtue.mCosmetic.attackHeretic(mPlayer, finalTarget, mKeeperVirtue.mBoss, remainder);
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						if (mKeeperVirtue == null || mKeeperVirtue.mBoss == null) {
							return;
						}
						Effect tor = mPlugin.mEffectManager.getPriorityEffects(mKeeperVirtue.mBoss).get(TouchofRadiance.VIRTUE_EFFECT_NAME);
						if (tor != null) {
							EntityUtils.applyStun(mPlugin, (int) tor.getMagnitude(), finalTarget);
						}
						double damage = tor != null ? mKeeperVirtue.mDamageToR : mKeeperVirtue.mDamage;

						EntityUtils.getNearbyMobs(finalTarget.getLocation(), mKeeperVirtue.mHarmingRadius).forEach(e -> {
							DamageUtils.damage(mPlayer, e, DamageEvent.DamageType.MAGIC, damage, ClassAbility.KEEPER_VIRTUE, true, true);
							EntityUtils.applyFire(mPlugin, Objects.requireNonNull(mKeeperVirtue).mFireDuration, e, mPlayer);
						});
					}, remainder);
				}
			}
		}, actionTime);
		// Only fire one flare in a tick
		return true;
	}

	@Override
	public void showOffCooldownMessage() {
		// The off cooldown messages would display so frequently it'd block other abilities from being read.
		// The Ability Hotbar additionally does a much better job at showing the flare storages in real time.
	}

	@Override
	public Component customDisplayComponent() {
		// Display in UMM, but not in hotbar
		return Component.text("");
	}
}
