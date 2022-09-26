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
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
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
	private static final double ENHANCEMENT_DAMAGE_RATIO = 0.5;
	private static final int ENHANCEMENT_DELAY = 10;

	public static final String CHARM_RADIUS = "Brute Force Radius";
	public static final String CHARM_DAMAGE = "Brute Force Damage";
	public static final String CHARM_KNOCKBACK = "Brute Force Knockback";
	public static final String CHARM_WAVE_DAMAGE_RATIO = "Brute Force Wave Damage Ratio";
	public static final String CHARM_WAVES = "Brute Force Waves";

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
		mInfo.mDescriptions.add("Half a second after triggering this ability, it triggers another wave centered on the same mob, with 50% of the damage and all of the knockback.");
		mDisplayItem = new ItemStack(Material.STONE_AXE, 1);

		mMultiplier = isLevelOne() ? 0 : BRUTE_FORCE_2_MODIFIER;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new BruteForceCS(), BruteForceCS.SKIN_LIST);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mPlayer != null && event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(mPlayer)) {
			double damageBonus = BRUTE_FORCE_DAMAGE + event.getDamage() * mMultiplier;
			damageBonus = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damageBonus);

			event.setDamage(event.getDamage() + damageBonus);

			Location playerLoc = mPlayer.getLocation();
			wave(enemy, playerLoc, damageBonus, false);
			if (isEnhanced()) {
				double damageRatio = ENHANCEMENT_DAMAGE_RATIO + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WAVE_DAMAGE_RATIO);
				int waves = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_WAVES);
				for (int i = 1; i <= waves; i++) {
					double damage = damageBonus * Math.pow(damageRatio, i);
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						wave(enemy, playerLoc, damage, true);
					}, (long) ENHANCEMENT_DELAY * i);
				}
			}

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
				mComboRunnable.runTaskLater(mPlugin, (long) ((1D / EntityUtils.getAttributeOrDefault(mPlayer, Attribute.GENERIC_ATTACK_SPEED, 4)) * 20) + 15);
			}
			mComboNumber++;

			if (mComboNumber >= 3) {
				if (mComboRunnable != null) {
					mComboRunnable.cancel();
					mComboRunnable = null;
				}
				mComboNumber = 0;
			}
			return true;
		}
		return false;
	}

	private void wave(LivingEntity target, Location playerLoc, double damageBonus, boolean damageTarget) {
		Location loc = target.getLocation().add(0, 0.75, 0);
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, CharmManager.getRadius(mPlayer, CHARM_RADIUS, BRUTE_FORCE_RADIUS));
		if (!mobs.isEmpty()) {
			mCosmetic.bruteOnDamage(mPlayer, loc, mComboNumber);
		}

		float knockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, BRUTE_FORCE_KNOCKBACK_SPEED);

		for (LivingEntity mob : mobs) {
			if (damageTarget || mob != target) {
				DamageUtils.damage(mPlayer, mob, DamageType.OTHER, damageBonus, mInfo.mLinkedSpell, true);
				mCosmetic.bruteOnSpread(mPlayer, mob);
			}

			if (!EntityUtils.isBoss(mob)) {
				MovementUtils.knockAway(playerLoc, mob, knockback, knockback / 2, true);
			}
		}
	}
}
