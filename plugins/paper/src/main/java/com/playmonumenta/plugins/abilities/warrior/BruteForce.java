package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.BruteForceCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class BruteForce extends Ability {

	private static final float BRUTE_FORCE_RADIUS = 2.0f;
	private static final int BRUTE_FORCE_DAMAGE = 2;
	private static final double BRUTE_FORCE_2_MODIFIER = 0.1;
	private static final float BRUTE_FORCE_KNOCKBACK_SPEED = 0.7f;
	private static final double ENHANCEMENT_DAMAGE_INCREASE = 0.25;

	public static final String CHARM_RADIUS = "Brute Force Radius";
	public static final String CHARM_DAMAGE = "Brute Force Damage";
	public static final String CHARM_KNOCKBACK = "Brute Force Knockback";
	public static final String CHARM_BONUS_DAMAGE = "Brute Force Bonus Damage";

	private final double mMultiplier;

	private final BruteForceCS mCosmetic;

	private int mComboNumber = 0;
	private BukkitRunnable mComboRunnable = null;

	public BruteForce(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Brute Force");
		mInfo.mLinkedSpell = ClassAbility.BRUTE_FORCE;
		mInfo.mScoreboardId = "BruteForce";
		mInfo.mShorthandName = "BF";
		mInfo.mDescriptions.add("Attacking an enemy with a critical attack passively deals 2 more damage to the mob and 2 damage to all enemies in a 2-block cube around it, and knocks all non-boss enemies away from you.");
		mInfo.mDescriptions.add("Damage is increased to 10 percent of the attack's damage plus 2.");
		mInfo.mDescriptions.add("Attack damage is increased by " + (int) (ENHANCEMENT_DAMAGE_INCREASE * 100) + "% on critical hits while running or sprinting.");
		mDisplayItem = new ItemStack(Material.STONE_AXE, 1);

		mMultiplier = isLevelOne() ? 0 : BRUTE_FORCE_2_MODIFIER;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new BruteForceCS(), BruteForceCS.SKIN_LIST);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		// If Player is enhanced, melee strike, and is a critical or sprinting attack
		// Boost damage by 25%
		if (mPlayer != null && isEnhanced() && event.getType() == DamageType.MELEE && (PlayerUtils.isCriticalAttack(mPlayer) || mPlayer.isSprinting())) {
			event.setDamage(event.getDamage() * (1 + ENHANCEMENT_DAMAGE_INCREASE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BONUS_DAMAGE)));
		}

		if (mPlayer != null && event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(mPlayer)) {
			double damageBonus = BRUTE_FORCE_DAMAGE + event.getDamage() * mMultiplier;
			damageBonus = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damageBonus);

			event.setDamage(event.getDamage() + damageBonus);

			Location loc = enemy.getLocation().add(0, 0.75, 0);
			mCosmetic.bruteOnDamage(mPlayer, loc, mComboNumber);

			if (mComboNumber == 0 || mComboRunnable != null) {
				if (mComboRunnable != null) {
					mComboRunnable.cancel();
				}
				mComboRunnable = new BukkitRunnable() {
					@Override
					public void run() {
						mComboNumber = 0;
						mComboRunnable = null;
					}
				};
				mComboRunnable.runTaskLater(mPlugin, (long) ((1D / mPlayer.getAttribute(Attribute.GENERIC_ATTACK_SPEED).getValue()) * 20) + 15);
			}
			mComboNumber++;

			if (mComboNumber >= 3) {
				if (mComboRunnable != null) {
					mComboRunnable.cancel();
					mComboRunnable = null;
				}
				mComboNumber = 0;
			}

			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, CharmManager.getRadius(mPlayer, CHARM_RADIUS, BRUTE_FORCE_RADIUS), enemy)) {
				DamageUtils.damage(mPlayer, mob, DamageType.OTHER, damageBonus, mInfo.mLinkedSpell, true);
				mCosmetic.bruteOnSpread(mPlayer, mob);

				if (!EntityUtils.isBoss(mob)) {
					float knockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, BRUTE_FORCE_KNOCKBACK_SPEED);
					MovementUtils.knockAway(mPlayer.getLocation(), mob, knockback, knockback / 2, true);
				}
			}
			return true;
		}
		return false;
	}
}
