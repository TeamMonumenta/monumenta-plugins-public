package com.playmonumenta.plugins.abilities.shaman.hexbreaker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.shaman.TotemicEmpowerment;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Devastation extends Ability {
	public static final int COOLDOWN = 20 * 20;
	public static final int RADIUS_1 = 5;
	public static final int RADIUS_2 = 8;
	public static final int DAMAGE_1 = 24;
	public static final int DAMAGE_2 = 30;
	private final Collection<Map.Entry<Double, ParticleUtils.SpawnParticleAction>> PARTICLES_FLAME = Arrays.asList(new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.2, (Location loc) -> new PartialParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer)));
	private final Collection<Map.Entry<Double, ParticleUtils.SpawnParticleAction>> PARTICLES_BURN = Arrays.asList(new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.2, (Location loc) -> new PartialParticle(Particle.LAVA, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer)));

	public double mDamage;
	private final int mRadius;

	public static final AbilityInfo<Devastation> INFO =
		new AbilityInfo<>(Devastation.class, "Devastation", Devastation::new)
			.linkedSpell(ClassAbility.DEVASTATION)
			.scoreboardId("Devastation")
			.shorthandName("DV")
			.descriptions(
				String.format("Press Swap while sneaking with a projectile weapon to destroy the nearest totem, dealing %s magic damage within a %s block radius of the totem. (%ss cooldown)",
					DAMAGE_1,
					RADIUS_1,
					COOLDOWN / 20
				),
				String.format("Magic damage is increased to %s and the radius is increased to %s blocks.",
					DAMAGE_2,
					RADIUS_2)
			)
			.simpleDescription("Press swap to destroy your nearest totem, dealing massive damage within a medium radius.")
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Devastation::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true),
				new AbilityTriggerInfo.TriggerRestriction("holding a projectile weapon", player -> ItemUtils.isProjectileWeapon(player.getInventory().getItemInMainHand()))))
			.displayItem(Material.COAL_BLOCK);

	public Devastation(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AuditListener.logSevere(player.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
			AbilityUtils.resetClass(player);
		}
		mDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
		mRadius = isLevelOne() ? RADIUS_1 : RADIUS_2;
		mDamage *= HexbreakerPassive.damageBuff(mPlayer);
	}

	public void cast() {
		if (isOnCooldown() || TotemicEmpowerment.getActiveTotems(mPlayer) <= 0) {
			return;
		}

		List<LivingEntity> totemList = TotemicEmpowerment.getTotemList(mPlayer);
		LivingEntity totemToNuke = totemList.get(0);
		for (LivingEntity totem : totemList) {
			if (!totemToNuke.equals(totem) && mPlayer.getLocation().distance(totemToNuke.getLocation()) > mPlayer.getLocation().distance(totem.getLocation())) {
				totemToNuke = totem;
			}
		}
		putOnCooldown();

		Location targetLoc = totemToNuke.getLocation();
		TotemicEmpowerment.removeTotem(mPlayer, totemToNuke);

		ParticleUtils.explodingRingEffect(mPlugin, targetLoc.clone().add(0, 0.1, 0), mRadius, 1.2, 10, PARTICLES_FLAME);
		ParticleUtils.explodingRingEffect(mPlugin, targetLoc.clone().add(0, 0.1, 0), mRadius, 1.2, 10, PARTICLES_BURN);
		targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_END_PORTAL_SPAWN, 0.3f, 2.0f);
		targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_ENDER_DRAGON_HURT, 1.0f, 1.0f);

		for (LivingEntity mob : EntityUtils.getNearbyMobs(targetLoc, mRadius)) {
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mDamage, mInfo.getLinkedSpell());
		}
	}
}
