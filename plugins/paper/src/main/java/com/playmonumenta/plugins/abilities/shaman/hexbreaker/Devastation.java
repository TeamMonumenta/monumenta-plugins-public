package com.playmonumenta.plugins.abilities.shaman.hexbreaker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.shaman.TotemAbility;
import com.playmonumenta.plugins.abilities.shaman.TotemicEmpowerment;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Devastation extends Ability {
	public static final int COOLDOWN = 15 * 20;
	public static final int RADIUS_1 = 6;
	public static final int RADIUS_2 = 8;
	public static final int DAMAGE_1 = 23;
	public static final int DAMAGE_2 = 28;
	public static final int CDR_ON_KILL = 3 * 20;

	public static final String CHARM_DAMAGE = "Devastation Damage";
	public static final String CHARM_RADIUS = "Devastation Radius";
	public static final String CHARM_COOLDOWN = "Devastation Cooldown";

	public static final AbilityInfo<Devastation> INFO =
		new AbilityInfo<>(Devastation.class, "Devastation", Devastation::new)
			.linkedSpell(ClassAbility.DEVASTATION)
			.scoreboardId("Devastation")
			.shorthandName("DV")
			.descriptions(
				String.format("Press Swap while sneaking with a projectile weapon to destroy the nearest totem, dealing %s magic damage within a %s block radius of the totem. (%ss cooldown)",
					DAMAGE_1,
					RADIUS_1,
					StringUtils.ticksToSeconds(COOLDOWN)
				),
				String.format("Magic damage is increased to %s and the radius is increased to %s blocks.",
					DAMAGE_2,
					RADIUS_2)
			)
			.simpleDescription("Press swap to destroy your nearest totem, dealing massive damage within a medium radius.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Devastation::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON)))
			.displayItem(Material.COAL_BLOCK);

	public double mDamage;
	private final double mRadius;

	public Devastation(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDamage *= DestructiveExpertise.damageBuff(mPlayer);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, isLevelOne() ? RADIUS_1 : RADIUS_2);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		List<LivingEntity> totemList = TotemicEmpowerment.getTotemList(mPlayer);
		if (totemList.isEmpty()) {
			return;
		}
		LivingEntity totemToNuke = totemList.get(0);
		for (LivingEntity totem : totemList) {
			if (!totemToNuke.equals(totem) && mPlayer.getLocation().distance(totemToNuke.getLocation()) > mPlayer.getLocation().distance(totem.getLocation())) {
				totemToNuke = totem;
			}
		}
		putOnCooldown();

		Location targetLoc = totemToNuke.getLocation();
		for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
			if (abil instanceof TotemAbility totemAbility
				&& totemAbility.getRemainingAbilityDuration() > 0
				&& totemAbility.mDisplayName.equalsIgnoreCase(totemToNuke.getName())) {
				ClassAbility linkedSpell = abil.getInfo().getLinkedSpell();
				if (linkedSpell != null) {
					mPlugin.mTimers.updateCooldown(mPlayer, linkedSpell, CDR_ON_KILL);
				}
			}
		}
		TotemicEmpowerment.removeTotem(mPlayer, totemToNuke);

		for (Particle particle : List.of(Particle.FLAME, Particle.LAVA)) {
			ParticleUtils.explodingRingEffect(mPlugin, targetLoc.clone().add(0, 0.1, 0), mRadius, 1.2, 10, 0.2, loc -> new PartialParticle(particle, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer));
		}
		targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_END_PORTAL_SPAWN, 0.3f, 2.0f);
		targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_ENDER_DRAGON_HURT, 1.0f, 1.0f);

		for (LivingEntity mob : EntityUtils.getNearbyMobs(targetLoc, mRadius)) {
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mDamage, mInfo.getLinkedSpell());
		}
	}
}
