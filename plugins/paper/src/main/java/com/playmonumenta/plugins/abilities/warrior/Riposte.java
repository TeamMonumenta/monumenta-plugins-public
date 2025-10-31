package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.RiposteCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class Riposte extends Ability implements AbilityWithDuration {
	private static final int RIPOSTE_1_COOLDOWN = Constants.TICKS_PER_SECOND * 15;
	private static final int RIPOSTE_2_COOLDOWN = Constants.TICKS_PER_SECOND * 12;
	private static final int RIPOSTE_SWORD_DURATION = Constants.TICKS_PER_SECOND * 2;
	private static final int RIPOSTE_AXE_DURATION = Constants.TICKS_PER_SECOND * 3;
	private static final float RIPOSTE_KNOCKBACK_SPEED = 0.15f;
	private static final double RIPOSTE_SWORD_BONUS_DAMAGE = 1;
	private static final double ENHANCEMENT_DAMAGE = 15;
	private static final double ENHANCEMENT_RADIUS = 4;
	private static final int ENHANCEMENT_ROOT_DURATION = (int) (Constants.TICKS_PER_SECOND * 1.5);

	public static final String CHARM_COOLDOWN = "Riposte Cooldown";
	public static final String CHARM_DAMAGE_DURATION = "Riposte Sword Bonus Damage Duration";
	public static final String CHARM_STUN_DURATION = "Riposte Axe Stun Duration";
	public static final String CHARM_KNOCKBACK = "Riposte Knockback";
	public static final String CHARM_BONUS_DAMAGE = "Riposte Sword Bonus Damage";
	public static final String CHARM_DAMAGE = "Riposte Enhancement Damage";
	public static final String CHARM_RADIUS = "Riposte Enhancement Range";
	public static final String CHARM_ROOT_DURATION = "Riposte Enhancement Root Duration";

	public static final AbilityInfo<Riposte> INFO =
		new AbilityInfo<>(Riposte.class, "Riposte", Riposte::new)
			.linkedSpell(ClassAbility.RIPOSTE)
			.scoreboardId("Obliteration")
			.shorthandName("Rip")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("While wielding a sword or axe, block a mob's melee attack to stun the mob or gain damage.")
			.cooldown(RIPOSTE_1_COOLDOWN, RIPOSTE_2_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.SKELETON_SKULL);

	private final double mSwordDamage;
	private final int mMaxSwordDuration;
	private final int mStunDuration;
	private final float mKnockAwaySpeed;
	private final double mEnhancementDamage;
	private final double mEnhancementRadius;
	private final int mEnhancementRootDuration;
	private final RiposteCS mCosmetic;

	private @Nullable BukkitRunnable mRunnable = null;
	private int mCurrDuration = -1;
	private boolean mHasTriggeredSwordL2 = false;

	public Riposte(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mSwordDamage = RIPOSTE_SWORD_BONUS_DAMAGE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BONUS_DAMAGE);
		mMaxSwordDuration = CharmManager.getDuration(mPlayer, CHARM_DAMAGE_DURATION, RIPOSTE_SWORD_DURATION);
		mStunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, RIPOSTE_AXE_DURATION);
		mKnockAwaySpeed = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, RIPOSTE_KNOCKBACK_SPEED);
		mEnhancementDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, ENHANCEMENT_DAMAGE);
		mEnhancementRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, ENHANCEMENT_RADIUS);
		mEnhancementRootDuration = CharmManager.getDuration(mPlayer, CHARM_ROOT_DURATION, ENHANCEMENT_ROOT_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new RiposteCS());
	}

	@Override
	public void onHurt(final DamageEvent event, @Nullable final Entity damager, @Nullable final LivingEntity source) {
		if (isOnCooldown() || source == null || event.getType() != DamageType.MELEE || event.isBlocked()) {
			return;
		}

		final boolean holdingSword = ItemUtils.isSword(mPlayer.getInventory().getItemInMainHand());
		final boolean holdingAxe = ItemUtils.isAxe(mPlayer.getInventory().getItemInMainHand());
		if (!holdingAxe && !holdingSword) {
			return;
		}

		final World world = mPlayer.getWorld();
		final Location playerLoc = mPlayer.getLocation();

		if (isLevelTwo() && holdingSword) {
			mCurrDuration = 0;
			mHasTriggeredSwordL2 = false;
			mRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					mCurrDuration++;
					if (mCurrDuration >= mMaxSwordDuration) {
						this.cancel();
					}
				}

				@Override
				public synchronized void cancel() {
					super.cancel();
					mCurrDuration = -1;
					ClientModHandler.updateAbility(mPlayer, Riposte.this);
				}
			};
			cancelOnDeath(mRunnable.runTaskTimer(mPlugin, 0, 1));
		} else if (isLevelTwo() && holdingAxe) {
			EntityUtils.applyStun(mPlugin, mStunDuration, source);
			mCosmetic.onAxeStun(world, playerLoc);
		}

		MovementUtils.knockAway(mPlayer, source, mKnockAwaySpeed, true);
		mCosmetic.onParry(mPlayer, world, playerLoc, source);
		putOnCooldown();
		ClientModHandler.updateAbility(mPlayer, this);
		mPlayer.setNoDamageTicks(20);
		mPlayer.setLastDamage(event.getDamage());
		event.setFlatDamage(0);
		event.setCancelled(true);

		if (isEnhanced()) {
			for (final LivingEntity mob : new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), mEnhancementRadius).getHitMobs()) {
				DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mEnhancementDamage, mInfo.getLinkedSpell(), true, false);
				EntityUtils.applySlow(mPlugin, mEnhancementRootDuration, 1.0f, mob);
			}
			mCosmetic.onEnhancedParry(world, playerLoc);
		}
	}

	@Override
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		if ((event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_ENCH)
			&& ItemUtils.isSword(mPlayer.getInventory().getItemInMainHand())
			&& mCurrDuration != -1) {
			event.updateDamageWithMultiplier(1 + mSwordDamage);
			if (mRunnable != null && !mRunnable.isCancelled() && !mHasTriggeredSwordL2) {
				// Disable next tick, buff only for this tick
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					if (mRunnable != null) {
						mRunnable.cancel();
					}
				}, 1);
				mHasTriggeredSwordL2 = true;
				// Prevent it from making one Runnable per event - optimisation
			}
			mCosmetic.onSwordAttack(mPlayer.getWorld(), mPlayer.getLocation());
		}
		return false; // prevents multiple applications itself by clearing mSwordTimer
	}

	@Override
	public int getInitialAbilityDuration() {
		return mMaxSwordDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return mCurrDuration == -1 ? 0 : Math.min(mMaxSwordDuration, mMaxSwordDuration - mCurrDuration);
	}

	private static Description<Riposte> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("While wielding a sword or axe, block an incoming melee attack.")
			.addCooldown(RIPOSTE_1_COOLDOWN, Ability::isLevelOne);
	}

	private static Description<Riposte> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Blocking a melee attack with Riposte's effect while holding a sword grants ")
			.addPercent(a -> a.mSwordDamage, RIPOSTE_SWORD_BONUS_DAMAGE)
			.add(" extra damage on your next sword swing within ")
			.addDuration(a -> a.mMaxSwordDuration, RIPOSTE_SWORD_DURATION)
			.add(" seconds. Blocking with Riposte's effect while holding an axe stuns the attacking mob for ")
			.addDuration(a -> a.mStunDuration, RIPOSTE_AXE_DURATION)
			.add(" seconds.")
			.addCooldown(RIPOSTE_2_COOLDOWN, Ability::isLevelTwo);
	}

	private static Description<Riposte> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("When Riposte activates, deal ")
			.add(a -> a.mEnhancementDamage, ENHANCEMENT_DAMAGE)
			.add(" melee damage to all mobs within ")
			.add(a -> a.mEnhancementRadius, ENHANCEMENT_RADIUS)
			.add(" blocks and root them for ")
			.addDuration(a -> a.mEnhancementRootDuration, ENHANCEMENT_ROOT_DURATION)
			.add(" seconds.");
	}
}
