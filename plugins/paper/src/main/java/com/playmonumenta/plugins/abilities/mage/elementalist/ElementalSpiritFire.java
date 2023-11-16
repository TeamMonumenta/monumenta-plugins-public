package com.playmonumenta.plugins.abilities.mage.elementalist;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.AbstractPartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ElementalSpiritFire extends BaseElementalSpirit {
	public static final String NAME = "Elemental Spirits";

	public static final int DAMAGE_1 = 10;
	public static final int DAMAGE_2 = 15;
	public static final double BOW_MULTIPLIER_1 = 0.25;
	public static final double BOW_MULTIPLIER_2 = 0.4;
	public static final double HITBOX = 1.5;
	public static final int COOLDOWN_TICKS = 8 * Constants.TICKS_PER_SECOND;
	public static final EnumSet<ClassAbility> FIRE_ABILITIES = EnumSet.of(ClassAbility.ELEMENTAL_ARROWS_FIRE, ClassAbility.STARFALL, ClassAbility.MAGMA_SHIELD);

	public static final String CHARM_DAMAGE = "Elemental Spirits Damage";
	public static final String CHARM_COOLDOWN = "Elemental Spirits Cooldown";
	public static final String CHARM_SIZE = "Elemental Spirits Size";
	public static final String CHARM_DAMAGE2 = "Fire Elemental Spirit Damage";
	public static final String CHARM_COOLDOWN2 = "Fire Elemental Spirit Cooldown";

	public static final AbilityInfo<ElementalSpiritFire> INFO =
		new AbilityInfo<>(ElementalSpiritFire.class, NAME, ElementalSpiritFire::new)
			.linkedSpell(ClassAbility.ELEMENTAL_SPIRIT_FIRE)
			.scoreboardId("ElementalSpirit")
			.shorthandName("ES")
			.descriptions(
				String.format(
					"Two spirits accompany you - one of fire and one of ice. The next moment after you deal fire damage, " +
						"the fire spirit instantly dashes from you towards the farthest enemy that spell hit, " +
						"dealing %s fire magic damage to all enemies in a %s-block cube around it along its path. " +
						"The next moment after you deal ice damage, the ice spirit warps to the closest enemy that spell hit and induces an extreme local climate, " +
						"dealing %s ice magic damage to all enemies in a %s-block cube around it every second for %ss. " +
						"If the spell was %s, the fire spirit does an additional %s%% of the projectile weapon's original damage, " +
						"and for the ice spirit, an additional %s%%. The spirits' damage ignores iframes. Independent cooldown: %ss.",
					DAMAGE_1,
					HITBOX,
					ElementalSpiritIce.DAMAGE_1,
					ElementalSpiritIce.SIZE,
					ElementalSpiritIce.PULSES,
					ElementalArrows.NAME,
					StringUtils.multiplierToPercentage(BOW_MULTIPLIER_1),
					StringUtils.multiplierToPercentage(ElementalSpiritIce.BOW_MULTIPLIER_1),
					StringUtils.ticksToSeconds(COOLDOWN_TICKS)
				), // Ice pulse interval of 20 ticks hardcoded to say "every second"
				String.format(
					"Fire spirit damage is increased from %s to %s. Ice spirit damage is increased from %s to %s. " +
						"Bonus %s damage is increased from %s%% to %s%% for the fire spirit, and from %s%% to %s%% for the ice spirit.",
					DAMAGE_1,
					DAMAGE_2,
					ElementalSpiritIce.DAMAGE_1,
					ElementalSpiritIce.DAMAGE_2,
					ElementalArrows.NAME,
					StringUtils.multiplierToPercentage(BOW_MULTIPLIER_1),
					StringUtils.multiplierToPercentage(BOW_MULTIPLIER_2),
					StringUtils.multiplierToPercentage(ElementalSpiritIce.BOW_MULTIPLIER_1),
					StringUtils.multiplierToPercentage(ElementalSpiritIce.BOW_MULTIPLIER_2)
				)
			)
			.simpleDescription("Deal extra damage in a radius upon dealing fire or ice damage.")
			.cooldown(COOLDOWN_TICKS, CHARM_COOLDOWN, CHARM_COOLDOWN2)
			.displayItem(Material.SUNFLOWER);

	public ElementalSpiritFire(Plugin plugin, Player player) {
		super(plugin, player, INFO, FIRE_ABILITIES, DAMAGE_1, DAMAGE_2, BOW_MULTIPLIER_1, BOW_MULTIPLIER_2);
	}

	@Override
	protected @Nullable LivingEntity getTargetEntity() {
		Location playerLocation = mPlayer.getLocation();
		@Nullable LivingEntity farthestEnemy = null;
		double farthestDistanceSquared = 0;

		for (LivingEntity enemy : mEnemiesAffected) {
			if (enemy.isValid()) { // If neither dead nor despawned
				double distanceSquared = playerLocation.distanceSquared(enemy.getLocation());
				if (distanceSquared > farthestDistanceSquared) {
					farthestEnemy = enemy;
					farthestDistanceSquared = distanceSquared;
				}
			}
		}
		return farthestEnemy;
	}

	@Override
	protected void activate(LivingEntity target, World world, double spellDamage, ItemStatManager.PlayerItemStats playerItemStats, boolean isElementalArrows) {
		Location startLocation = LocationUtils.getHalfHeightLocation(mPlayer);
		Location endLocation = LocationUtils.getHalfHeightLocation(target);
		Location playerLocation = mPlayer.getLocation();

		double size = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SIZE, HITBOX);
		double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, ElementalSpiritFire.CHARM_DAMAGE2, spellDamage);
		BoundingBox movingSpiritBox = BoundingBox.of(mPlayer.getEyeLocation(), size, size, size);
		double maxDistanceSquared = startLocation.distanceSquared(endLocation);
		double maxDistance = Math.sqrt(maxDistanceSquared);
		Vector vector = endLocation.clone().subtract(startLocation).toVector();
		double increment = 0.2;

		List<LivingEntity> potentialTargets = EntityUtils.getNearbyMobs(playerLocation, maxDistance + size);
		Vector vectorIncrement = vector.normalize().multiply(increment);

		mCosmetic.fireSpiritActivate(world, playerLocation);

		// Damage action & particles
		double maxIterations = maxDistance / increment * 1.1;
		for (int i = 0; i < maxIterations; i++) {
			Iterator<LivingEntity> iterator = potentialTargets.iterator();
			while (iterator.hasNext()) {
				LivingEntity potentialTarget = iterator.next();
				if (potentialTarget.getBoundingBox().overlaps(movingSpiritBox)) {
					damage(potentialTarget, damage, playerItemStats, isElementalArrows);
					iterator.remove();
				}
			}

			// The first shift happens after the first damage attempt,
			// unlike something like LocationUtils.travelTillObstructed().
			// The spirit starts at the player's eyes so this could damage enemies right beside/behind them
			movingSpiritBox.shift(vectorIncrement);
			Location newPotentialLocation = movingSpiritBox.getCenter().toLocation(world);
			if (playerLocation.distanceSquared(newPotentialLocation) > maxDistanceSquared) {
				break;
			} else {
				// Else spawn particles at the new location and continue doing damage at this place the next tick
				// These particles skip the first damage attempt
				mCosmetic.fireSpiritTravel(mPlayer, newPotentialLocation, HITBOX);
			}
		}
	}

	@Override
	protected AbstractPartialParticle<?> getPeriodicParticle() {
		return mCosmetic.getFirePeriodicParticle(mPlayer);
	}
}
