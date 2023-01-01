package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.BruteForceCS;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class BruteForce extends Ability {

	private static final float BRUTE_FORCE_RADIUS = 2.0f;
	private static final int BRUTE_FORCE_DAMAGE = 2;
	private static final double BRUTE_FORCE_2_MODIFIER = 0.1;
	private static final float BRUTE_FORCE_KNOCKBACK_SPEED = 0.7f;
	private static final double ENHANCEMENT_DAMAGE_RATIO = 0.75;
	private static final int ENHANCEMENT_DELAY = 10;

	public static final String CHARM_RADIUS = "Brute Force Radius";
	public static final String CHARM_DAMAGE = "Brute Force Damage";
	public static final String CHARM_KNOCKBACK = "Brute Force Knockback";
	public static final String CHARM_WAVE_DAMAGE_RATIO = "Brute Force Wave Damage Ratio";
	public static final String CHARM_WAVES = "Brute Force Waves";

	public static final AbilityInfo<BruteForce> INFO =
		new AbilityInfo<>(BruteForce.class, "Brute Force", BruteForce::new)
			.linkedSpell(ClassAbility.BRUTE_FORCE)
			.scoreboardId("BruteForce")
			.shorthandName("BF")
			.descriptions(
				"Attacking an enemy with a critical attack passively deals 2 more damage to the mob and 2 damage to all enemies in a 2 block radus around it, " +
					"and knocks all non-boss enemies away from you.",
				"Damage is increased to 10 percent of the attack's damage plus 2.",
				"Half a second after triggering this ability, it triggers another wave centered on the same mob, with 75% of the damage and all of the knockback.")
			.displayItem(new ItemStack(Material.STONE_AXE, 1));

	private final double mMultiplier;

	private final BruteForceCS mCosmetic;

	private int mComboNumber = 0;
	private @Nullable BukkitRunnable mComboRunnable = null;

	public BruteForce(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMultiplier = isLevelOne() ? 0 : BRUTE_FORCE_2_MODIFIER;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new BruteForceCS(), BruteForceCS.SKIN_LIST);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(mPlayer)) {
			double damageBonus = BRUTE_FORCE_DAMAGE + event.getDamage() * mMultiplier;
			damageBonus = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damageBonus);

			event.setDamage(event.getDamage() + damageBonus);

			if (mPlugin.mEffectManager.hasEffect(mPlayer, PercentDamageDealt.class)) {
				for (Effect priorityEffects : mPlugin.mEffectManager.getPriorityEffects(mPlayer).values()) {
					if (priorityEffects instanceof PercentDamageDealt damageEffect) {
						EnumSet<DamageType> types = damageEffect.getAffectedDamageTypes();
						if (types == null || types.contains(DamageType.MELEE)) {
							damageBonus = damageBonus * (1 + damageEffect.getMagnitude() * (damageEffect.isBuff() ? 1 : -1));
						}
					}
				}
			}

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
				cancelOnDeath(mComboRunnable.runTaskLater(mPlugin, (long) ((1D / EntityUtils.getAttributeOrDefault(mPlayer, Attribute.GENERIC_ATTACK_SPEED, 4)) * 20) + 15));
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
		double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, BRUTE_FORCE_RADIUS);
		List<LivingEntity> mobs = new Hitbox.SphereHitbox(loc, radius).getHitMobs();

		mCosmetic.bruteOnDamage(mPlayer, loc, radius, mComboNumber);

		float knockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, BRUTE_FORCE_KNOCKBACK_SPEED);

		for (LivingEntity mob : mobs) {
			if (damageTarget || mob != target) {
				DamageUtils.damage(mPlayer, mob, DamageType.OTHER, damageBonus, mob == target ? ClassAbility.BRUTE_FORCE : ClassAbility.BRUTE_FORCE_AOE, true);
				mCosmetic.bruteOnSpread(mPlayer, mob);
			}

			if (!EntityUtils.isBoss(mob)) {
				MovementUtils.knockAway(playerLoc, mob, knockback, knockback / 2, true);
			}
		}
	}
}
