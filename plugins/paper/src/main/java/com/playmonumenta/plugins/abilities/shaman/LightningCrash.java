package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.HexbreakerPassive;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SoothsayerPassive;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.particle.PPLightning;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.*;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class LightningCrash extends Ability {

	private static final int COOLDOWN = 15 * 20;
	private static final int STUN_DURATION = 10;
	private static final int RANGE = 8;
	private static final int DAMAGE_1 = 10;
	private static final int DAMAGE_2 = 13;

	public static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(255, 255, 0), 1.25f);
	private final Collection<Map.Entry<Double, ParticleUtils.SpawnParticleAction>> PARTICLES_YELLOW = Arrays.asList(new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.6, (Location loc) -> new PartialParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0).data(YELLOW).spawnAsPlayerActive(mPlayer)));
	private final Collection<Map.Entry<Double, ParticleUtils.SpawnParticleAction>> PARTICLES_WHITE = Arrays.asList(new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.3, (Location loc) -> new PartialParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer)));

	private static double mDamage;

	public static final AbilityInfo<LightningCrash> INFO =
		new AbilityInfo<>(LightningCrash.class, "Lightning Crash", LightningCrash::new)
			.linkedSpell(ClassAbility.LIGHTNING_CRASH)
			.scoreboardId("LightningCrash")
			.shorthandName("LC")
			.descriptions(
				String.format("Press swap with a weapon while sneaking to summon a lightning crash on your position. Deals %s magic damage to mobs within %s blocks and pushes them away. Cooldown: %ss.",
					DAMAGE_1,
					RANGE,
					COOLDOWN / 20
				),
				String.format("Damage increased to %s and stuns targets for %ss.",
					DAMAGE_2,
					StringUtils.ticksToSeconds(STUN_DURATION))
			)
			.simpleDescription("Summons a lightning crash on your location, dealing damage and knocking mobs away.")
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", LightningCrash::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.YELLOW_BANNER);

	public LightningCrash(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AuditListener.logSevere(player.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
			AbilityUtils.resetClass(player);
		}
		mDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
		mDamage *= HexbreakerPassive.damageBuff(mPlayer);
		mDamage *= SoothsayerPassive.damageBuff(mPlayer);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		for (LivingEntity mob : EntityUtils.getNearbyMobsInSphere(mPlayer.getLocation(), RANGE, null)) {
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true, false);
			if (!EntityUtils.isCCImmuneMob(mob)) {
				MovementUtils.knockAway(mPlayer, mob, 0.8f);
				if (isLevelTwo()) {
					EntityUtils.applyStun(mPlugin, (int) STUN_DURATION, mob);
				}
			}
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation().add(0, 0.1, 0);
		PPLightning lightning = new PPLightning(Particle.END_ROD, loc)
			.count(8).duration(5);
		mPlayer.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.25f);
		lightning.init(6, 2.5, 0.3, 0.3);
		lightning.spawnAsPlayerActive(mPlayer);

		ParticleUtils.explodingRingEffect(mPlugin, loc, RANGE, 0.3, 5, PARTICLES_YELLOW);
		ParticleUtils.explodingRingEffect(mPlugin, loc, RANGE, 0.3, 5, PARTICLES_WHITE);
		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1, 0.65f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.25f);
	}
}
