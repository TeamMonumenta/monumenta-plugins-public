package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.BruteForceCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.CritScaling;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.HALF_TICKS_PER_SECOND;
import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public final class BruteForce extends Ability {
	private static final float BRUTE_FORCE_RADIUS = 2.0f;
	private static final int BRUTE_FORCE_DAMAGE = 2;
	private static final double BRUTE_FORCE_2_MODIFIER = 0.1;
	private static final float BRUTE_FORCE_KNOCKBACK_SPEED = 0.45f;
	private static final double ENHANCEMENT_DAMAGE_RATIO = 0.75;
	private static final int ENHANCEMENT_DELAY = HALF_TICKS_PER_SECOND;

	public static final String CHARM_RADIUS = "Brute Force Radius";
	public static final String CHARM_DAMAGE = "Brute Force Damage";
	public static final String CHARM_KNOCKBACK = "Brute Force Knockback";
	public static final String CHARM_WAVE_DAMAGE_RATIO = "Brute Force Wave Damage Ratio";
	public static final String CHARM_WAVES = "Brute Force Waves";
	public static final String CHARM_WAVE_DELAY = "Brute Force Wave Delay";

	public static final AbilityInfo<BruteForce> INFO =
		new AbilityInfo<>(BruteForce.class, "Brute Force", BruteForce::new)
			.linkedSpell(ClassAbility.BRUTE_FORCE)
			.scoreboardId("BruteForce")
			.shorthandName("BF")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Critical melee attacks deal extra damage and knock back nearby mobs.")
			.displayItem(Material.STONE_AXE);

	private final double mFlatDamage;
	private final double mMultiplier;
	private final double mWaveRadius;
	private final float mForceScalar;
	private final double mEnhanceDamageMult;
	private final int mEnhanceWaves;
	private final int mEnhanceWaveDelay;

	private final BruteForceCS mCosmetic;

	private int mComboNumber = 0;
	private @Nullable BukkitRunnable mComboRunnable = null;

	public BruteForce(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mFlatDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, BRUTE_FORCE_DAMAGE);
		mMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelTwo() ? BRUTE_FORCE_2_MODIFIER : 0);
		mWaveRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, BRUTE_FORCE_RADIUS);
		mForceScalar = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, BRUTE_FORCE_KNOCKBACK_SPEED);
		mEnhanceDamageMult = ENHANCEMENT_DAMAGE_RATIO + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WAVE_DAMAGE_RATIO);
		mEnhanceWaves = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_WAVES);
		mEnhanceWaveDelay = CharmManager.getDuration(mPlayer, CHARM_WAVE_DELAY, ENHANCEMENT_DELAY);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new BruteForceCS());
	}

	@Override
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		if (!(event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(mPlayer))) {
			return false;
		}

		// Event's flat damage does not include any multipliers, readd crit scaling if Cumbersome is not present
		final boolean weaponHasCumbersome = ItemStatUtils.hasEnchantment(mPlayer.getInventory().getItemInMainHand(), EnchantmentType.CUMBERSOME);
		final double baseDamage = mMultiplier * (event.getFlatDamage() * (weaponHasCumbersome ? 1 : CritScaling.CRIT_BONUS)) + mFlatDamage;
		final int waveCount = 1 + (isEnhanced() ? mEnhanceWaves : 0);
		final float kbMultiplier = 1 + (0.25f * ItemStatUtils.getEnchantmentLevel(mPlayer.getEquipment().getItemInMainHand(), EnchantmentType.KNOCKBACK));

		for (int i = 0; i < waveCount; i++) {
			final double damage = baseDamage * Math.pow(mEnhanceDamageMult, i); // Reduces damage if waveCount > 1
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> wave(enemy, mPlayer.getLocation(), damage, kbMultiplier),
				(long) mEnhanceWaveDelay * i);
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
			cancelOnDeath(mComboRunnable.runTaskLater(mPlugin,
				(long) ((1D / EntityUtils.getAttributeOrDefault(mPlayer, Attribute.GENERIC_ATTACK_SPEED, 4)) * TICKS_PER_SECOND) + 15));
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

	private void wave(final LivingEntity target, final Location playerLoc, final double damage, float kbMultiplier) {
		final Location loc = target.getLocation().add(0, 0.75, 0);
		for (final LivingEntity mob : new Hitbox.SphereHitbox(loc, mWaveRadius).getHitMobs()) {
			DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, damage,
				mob == target ? ClassAbility.BRUTE_FORCE : ClassAbility.BRUTE_FORCE_AOE, true);

			if (!EntityUtils.isBoss(mob) && mob == target) {
				MovementUtils.knockAway(playerLoc, mob, mForceScalar * kbMultiplier, mForceScalar / 2.0f, true);
			} else if (!EntityUtils.isBoss(mob)) {
				MovementUtils.knockAway(playerLoc, mob, mForceScalar, mForceScalar / 2.0f, true);
			}
		}

		mCosmetic.bruteOnDamage(mPlayer, loc.getWorld(), loc, mWaveRadius, mComboNumber);
	}

	private static Description<BruteForce> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Performing a critical melee attack deals ")
			.add(a -> a.mFlatDamage, BRUTE_FORCE_DAMAGE)
			.add(" damage and applies knockback to the hit enemy and all enemies within ")
			.add(a -> a.mWaveRadius, BRUTE_FORCE_RADIUS)
			.add(" blocks. Bosses do not take knockback.");
	}

	private static Description<BruteForce> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The damage is increased to ")
			.add(a -> a.mFlatDamage, BRUTE_FORCE_DAMAGE)
			.add(" plus ")
			.addPercent(a -> a.mMultiplier, BRUTE_FORCE_2_MODIFIER, false, Ability::isLevelTwo)
			.add(" of the critical attack's damage.");
	}

	private static Description<BruteForce> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Triggering this ability causes a subsequent wave after ")
			.addDuration(a -> a.mEnhanceWaveDelay, ENHANCEMENT_DELAY)
			.add(" seconds centered on the hit enemy that deals ")
			.addPercent(a -> a.mEnhanceDamageMult, ENHANCEMENT_DAMAGE_RATIO)
			.add(" of the damage and applies knockback.");
	}
}
