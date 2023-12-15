package com.playmonumenta.plugins.abilities.mage.elementalist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.elementalist.ElementalSpiritCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.particle.AbstractPartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public abstract class BaseElementalSpirit extends Ability {

	protected final float mLevelDamage;
	protected final double mLevelBowMultiplier;
	protected final EnumSet<ClassAbility> mAffectedAbilities;
	protected final Set<LivingEntity> mEnemiesAffected = new HashSet<>();

	protected @Nullable ElementalArrows mElementalArrows;
	protected @Nullable BukkitTask mPlayerParticlesGenerator;
	protected @Nullable BukkitTask mEnemiesAffectedProcessor;

	protected final ElementalSpiritCS mCosmetic;

	public BaseElementalSpirit(Plugin plugin, Player player, AbilityInfo<?> info, EnumSet<ClassAbility> affectedAbilities, double damage1, double damage2, double bowMultiplier1, double bowMultiplier2) {
		super(plugin, player, info);
		mLevelDamage = (float) CharmManager.calculateFlatAndPercentValue(player, ElementalSpiritFire.CHARM_DAMAGE, isLevelOne() ? damage1 : damage2);
		mLevelBowMultiplier = isLevelOne() ? bowMultiplier1 : bowMultiplier2;
		mAffectedAbilities = affectedAbilities;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ElementalSpiritCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mElementalArrows = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, ElementalArrows.class);
		});
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		ClassAbility ability = event.getAbility();
		if (ability != null && !isOnCooldown() && mAffectedAbilities.contains(ability)) {
			mEnemiesAffected.add(event.getDamagee());
			if (mEnemiesAffectedProcessor == null) {

				boolean isElementalArrows = ability == ClassAbility.ELEMENTAL_ARROWS_FIRE || ability == ClassAbility.ELEMENTAL_ARROWS_ICE;
				float spellDamage = isElementalArrows ? mLevelDamage : SpellPower.getSpellDamage(mPlugin, mPlayer, mLevelDamage);
				ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

				mEnemiesAffectedProcessor = new BukkitRunnable() {
					@Override
					public void run() {
						mEnemiesAffectedProcessor = null;

						@Nullable LivingEntity target = getTargetEntity();
						mEnemiesAffected.clear();

						if (target != null) {
							putOnCooldown();
							activate(target, mPlayer.getWorld(), spellDamage, playerItemStats, isElementalArrows);
						}
					}
				}.runTaskLater(mPlugin, 2);
			}
		}
		return false;
	}

	protected void damage(LivingEntity entity, double spellDamage, ItemStatManager.PlayerItemStats playerItemStats, boolean isElementalArrows) {
		if (isElementalArrows && mElementalArrows != null) {
			spellDamage += Math.max(0, mElementalArrows.getLastDamage() * mLevelBowMultiplier);
		}

		DamageUtils.damage(mPlayer, entity, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), spellDamage, true, false, false);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mPlayerParticlesGenerator == null) {
			cancelOnDeath(mPlayerParticlesGenerator = new BukkitRunnable() {
				double mVerticalAngle = 0;
				double mRotationAngle = 0;
				final AbstractPartialParticle<?> mParticle = getPeriodicParticle();

				@Override
				public void run() {
					if (isOnCooldown()
						    || !mPlayer.isOnline()
							|| mPlayer.isDead()
						    || PremiumVanishIntegration.isInvisibleOrSpectator(mPlayer)) {
						this.cancel();
						mPlayerParticlesGenerator = null;
					}

					mVerticalAngle += 5.5 * getAngleMultiplier();
					mRotationAngle += 10 * getAngleMultiplier();
					mVerticalAngle %= 360 * getAngleMultiplier();
					mRotationAngle %= 360 * getAngleMultiplier();

					mParticle.location(
							LocationUtils
								.getHalfHeightLocation(mPlayer)
								.add(
									FastUtils.cos(Math.toRadians(mRotationAngle)),
									FastUtils.sin(Math.toRadians(mVerticalAngle)) * 0.5,
									FastUtils.sin(Math.toRadians(mRotationAngle))
								))
						.spawnAsPlayerPassive(mPlayer);
				}
			}.runTaskTimer(mPlugin, 0, 1));
		}
	}

	@Override
	public void invalidate() {
		if (mPlayerParticlesGenerator != null) {
			mPlayerParticlesGenerator.cancel();
		}

		if (mEnemiesAffectedProcessor != null) {
			mEnemiesAffectedProcessor.cancel();
		}
	}

	protected abstract @Nullable LivingEntity getTargetEntity();

	protected abstract void activate(LivingEntity target, World world, double spellDamage, ItemStatManager.PlayerItemStats playerItemStats, boolean isElementalArrows);

	protected abstract AbstractPartialParticle<?> getPeriodicParticle();

	protected int getAngleMultiplier() {
		return 1;
	}
}
