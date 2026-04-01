package com.playmonumenta.plugins.abilities.cleric.seraph;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithCustomDisplay;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.NegateDamage;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.redissync.utils.ScoreboardUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;


public class KeeperVirtueShieldingFlare extends MultipleChargeAbility implements AbilityWithCustomDisplay {

	public static final AbilityInfo<KeeperVirtueShieldingFlare> INFO =
		new AbilityInfo<>(KeeperVirtueShieldingFlare.class, "Keeper Virtue Shielding Flare", KeeperVirtueShieldingFlare::new)
			.linkedSpell(ClassAbility.KEEPER_VIRTUE_SHIELDING)
			.scoreboardId("KeeperVirtue")
			.shorthandName("KV")
			.simpleDescription("An angelic spirit follows you, supporting nearby players and attacking mobs.")
			.canUse(p -> ScoreboardUtils.getScoreboardValue(p.getName(), "KeeperVirtue") > 0 && ServerProperties.getClassSpecializationsEnabled(p))
			.cooldown(KeeperVirtue.SHIELDING_COOLDOWN_1, KeeperVirtue.SHIELDING_COOLDOWN_2, KeeperVirtue.CHARM_SHIELDING_COOLDOWN);

	private @Nullable KeeperVirtue mKeeperVirtue;

	public KeeperVirtueShieldingFlare(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = KeeperVirtue.SHIELDING_FLARE_CAPACITY + (int) CharmManager.getLevel(player, KeeperVirtue.CHARM_SHIELDING);
		mCharges = getChargesOffCooldown();
		Bukkit.getScheduler().runTask(plugin, () -> mKeeperVirtue = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, KeeperVirtue.class));
	}

	public void shieldPlayer(Player target) {
		if (mKeeperVirtue == null || getCharges() <= 0) {
			return;
		}
		// We're doing two delays, so need to avoid rounding errors
		// (for example, we need a -10% action time to be 4 and 5 ticks, not 2x 4.5 rounding back up to 2x 5)
		int actionTime = mKeeperVirtue.mActionTime / 2;
		int remainder = mKeeperVirtue.mActionTime - actionTime;

		mKeeperVirtue.mTarget = target;
		mKeeperVirtue.mTicksSinceTargetChance = 0;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			if (mKeeperVirtue == null) {
				return;
			}
			// Wait a few ticks first as we don't want to fire flares at dead players
			if (target.isValid() && mKeeperVirtue.mBoss != null && mPlayer.getLocation().distance(target.getLocation()) <= mKeeperVirtue.mActionRange) {
				if (consumeCharge()) {
					mKeeperVirtue.mTarget = target;
					mKeeperVirtue.mTicksSinceTargetChance = 0;

					mKeeperVirtue.mCosmetic.healPlayer(mPlayer, target, mKeeperVirtue.mBoss, remainder);
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						if (mKeeperVirtue == null || mKeeperVirtue.mBoss == null) {
							return;
						}
						double absorption = EntityUtils.getMaxHealth(target) * mKeeperVirtue.mAbsorption;
						AbsorptionUtils.addAbsorption(target, absorption, absorption, mKeeperVirtue.mAbsorptionDuration);
						mPlugin.mEffectManager.addEffect(target, "KeeperVirtueHitNegation", new NegateDamage(mKeeperVirtue.mHitNegationDuration, mKeeperVirtue.mHitNegations, EnumSet.of(DamageEvent.DamageType.MELEE, DamageEvent.DamageType.PROJECTILE)) {
							int mRotation = 0;

							@Override
							public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
								if (mKeeperVirtue != null) {
									mKeeperVirtue.mCosmetic.shieldTickEffect(target, mRotation += 36);
								}
							}
						});
					}, remainder);
				}
			}
		}, actionTime);
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
