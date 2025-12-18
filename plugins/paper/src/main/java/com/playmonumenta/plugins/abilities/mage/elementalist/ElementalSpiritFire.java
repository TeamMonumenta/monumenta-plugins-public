package com.playmonumenta.plugins.abilities.mage.elementalist;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Mage;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.AbstractPartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
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

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

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
			.hotbarName("EsF")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Deal extra damage in a radius upon dealing fire or ice damage.")
			.cooldown(COOLDOWN_TICKS, CHARM_COOLDOWN, CHARM_COOLDOWN2)
			.displayItem(Material.SUNFLOWER);

	private final double mSize;

	public ElementalSpiritFire(Plugin plugin, Player player) {
		super(plugin, player, INFO, FIRE_ABILITIES, DAMAGE_1, DAMAGE_2, BOW_MULTIPLIER_1, BOW_MULTIPLIER_2);
		mLevelDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE2, mLevelDamage);
		mSize = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SIZE, HITBOX);
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

		BoundingBox movingSpiritBox = BoundingBox.of(mPlayer.getEyeLocation(), mSize, mSize, mSize);
		double maxDistanceSquared = startLocation.distanceSquared(endLocation);
		double maxDistance = Math.sqrt(maxDistanceSquared);
		Vector vector = endLocation.clone().subtract(startLocation).toVector();
		double increment = 0.2;

		List<LivingEntity> potentialTargets = EntityUtils.getNearbyMobs(playerLocation, maxDistance + mSize);
		Vector vectorIncrement = vector.normalize().multiply(increment);

		mCosmetic.fireSpiritActivate(world, mPlayer, playerLocation, endLocation, vector, HITBOX);

		// Damage action & particles
		double maxIterations = maxDistance / increment * 1.1;
		for (int i = 0; i < maxIterations; i++) {
			Iterator<LivingEntity> iterator = potentialTargets.iterator();
			while (iterator.hasNext()) {
				LivingEntity potentialTarget = iterator.next();
				if (potentialTarget.getBoundingBox().overlaps(movingSpiritBox)) {
					damage(potentialTarget, spellDamage, playerItemStats, isElementalArrows);
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

	private static Description<ElementalSpiritFire> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Gain a *Fire Spirit* and an *Ice Spirit*").styles(Mage.FIRE_COLOR, Mage.ICE_COLOR)
			.addLine("that follow you around.")
			.addLine()
			.addLine("After dealing *Fire* damage to a mob, the").styles(Mage.FIRE_COLOR)
			.addLine("*Fire Spirit* dashes there and deals").styles(Mage.FIRE_COLOR)
			.addLine("*Fire* damage to mobs in its path.").styles(Mage.FIRE_COLOR)
			.addLine()
			.addStat("Damage: %d1 (s)")
				.statValues(stat(a -> a.mLevelDamage, DAMAGE_1))
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mSize, HITBOX))
			.addStat("Cooldown: %t")
				.statValues(cooldown(COOLDOWN_TICKS))
			.addLine()
			.addLine("After dealing *Ice* damage to a mob, the").styles(Mage.ICE_COLOR)
			.addLine("*Ice Spirit* teleports there and deals").styles(Mage.ICE_COLOR)
			.addLine("*Ice* damage in the area over time.").styles(Mage.ICE_COLOR)
			.addLine()
			.addOtherAbility(() -> ElementalSpiritIce.INFO, ElementalSpiritIce.class, desc -> desc
				.addStat("Damage: %d1 (s) every %t for %t")
					.statValues(stat(a -> a.mLevelDamage, ElementalSpiritIce.DAMAGE_1), stat(ElementalSpiritIce.PULSE_INTERVAL), stat(ElementalSpiritIce.PULSE_INTERVAL * ElementalSpiritIce.PULSES))
				.addStat("Radius: %r")
					.statValues(stat(a -> a.mSize, ElementalSpiritIce.SIZE))
				.addStat("Cooldown: %t")
					.statValues(cooldown(ElementalSpiritIce.COOLDOWN_TICKS)))
			.addLine()
			.addLine("When *Elemental Arrows* activates a spirit,").styles(UNDERLINED)
			.addLine("it deals bonus damage based on the")
			.addLine("projectile weapon's base damage.")
			.addLine()
			.addStat("Fire Spirit Bonus Damage: +%p1 (s)")
				.statValues(stat(a -> a.mLevelBowMultiplier, BOW_MULTIPLIER_1))
			.addOtherAbility(() -> ElementalSpiritIce.INFO, ElementalSpiritIce.class, desc -> desc
				.addStat("Ice Spirit Bonus Damage: +%p1 (s)")
					.statValues(stat(a -> a.mLevelBowMultiplier, ElementalSpiritIce.BOW_MULTIPLIER_1)))
			.addDashedLine();
	}

	private static Description<ElementalSpiritFire> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Elemental Spirits*' damage and").styles(UNDERLINED)
			.addLine("the bonus damage from *Elemental Arrows*.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Fire Spirit Damage: %d1 -> %d2 (s)")
				.statValues(stat(DAMAGE_1), stat(a -> a.mLevelDamage, DAMAGE_2))
			.addOtherAbility(() -> ElementalSpiritIce.INFO, ElementalSpiritIce.class, desc1 -> desc1
				.addStatComparison("Ice Spirit Damage: %d1 -> %d2 (s)")
				.statValues(stat(ElementalSpiritIce.DAMAGE_1), stat(a -> a.mLevelDamage, ElementalSpiritIce.DAMAGE_2)))
			.addLine()
			.addStatComparison("Fire Spirit Bonus Damage: +%p1 -> +%p2 (s)")
				.statValues(stat(BOW_MULTIPLIER_1), stat(a -> a.mLevelBowMultiplier, BOW_MULTIPLIER_2))
			.addOtherAbility(() -> ElementalSpiritIce.INFO, ElementalSpiritIce.class, desc2 -> desc2
				.addStatComparison("Ice Spirit Bonus Damage: +%p1 -> +%p2 (s)")
					.statValues(stat(ElementalSpiritIce.BOW_MULTIPLIER_1), stat(a -> a.mLevelBowMultiplier, ElementalSpiritIce.BOW_MULTIPLIER_2)))
			.addDashedLine();
	}
}
