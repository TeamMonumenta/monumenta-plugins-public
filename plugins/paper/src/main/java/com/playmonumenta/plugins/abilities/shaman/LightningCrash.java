package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DestructiveExpertise;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SupportExpertise;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.particle.PPLightning;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class LightningCrash extends Ability {

	private static final int COOLDOWN = 15 * 20;
	private static final int STUN_DURATION = 10;
	private static final int RANGE = 8;
	private static final int DAMAGE_1 = 10;
	private static final int DAMAGE_2 = 13;
	private static final double KNOCKBACK = 0.8;

	public static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(255, 255, 0), 1.25f);

	public static final String CHARM_COOLDOWN = "Lightning Crash Cooldown";
	public static final String CHARM_DAMAGE = "Lightning Crash Damage";
	public static final String CHARM_RADIUS = "Lightning Crash Radius";
	public static final String CHARM_STUN_DURATION = "Lightning Crash Stun Duration";
	public static final String CHARM_KNOCKBACK = "Lightning Crash Knockback";

	private double mDamage;
	private final double mRadius;
	private final int mStunDuration;
	private final float mKnockback;

	public static final AbilityInfo<LightningCrash> INFO =
		new AbilityInfo<>(LightningCrash.class, "Lightning Crash", LightningCrash::new)
			.linkedSpell(ClassAbility.LIGHTNING_CRASH)
			.scoreboardId("LightningCrash")
			.shorthandName("LC")
			.descriptions(
				String.format("Press swap with a weapon while sneaking to summon a lightning crash on your position. Deals %s magic damage to mobs within %s blocks and pushes them away. Cooldown: %ss.",
					DAMAGE_1,
					RANGE,
					StringUtils.ticksToSeconds(COOLDOWN)
				),
				String.format("Damage increased to %s and stuns targets for %ss.",
					DAMAGE_2,
					StringUtils.ticksToSeconds(STUN_DURATION))
			)
			.simpleDescription("Summons a lightning crash on your location, dealing damage and knocking mobs away.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", LightningCrash::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.YELLOW_BANNER);

	public LightningCrash(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDamage *= DestructiveExpertise.damageBuff(mPlayer);
		mDamage *= SupportExpertise.damageBuff(mPlayer);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RANGE);
		mStunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, STUN_DURATION);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		for (LivingEntity mob : EntityUtils.getNearbyMobsInSphere(mPlayer.getLocation(), mRadius, null)) {
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true, false);
			if (!EntityUtils.isCCImmuneMob(mob)) {
				MovementUtils.knockAway(mPlayer, mob, mKnockback);
				if (isLevelTwo()) {
					EntityUtils.applyStun(mPlugin, mStunDuration, mob);
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

		ParticleUtils.explodingRingEffect(mPlugin, loc, mRadius, 0.3, 5, 0.6, l -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0).data(YELLOW).spawnAsPlayerActive(mPlayer));
		ParticleUtils.explodingRingEffect(mPlugin, loc, mRadius, 0.3, 5, 0.3, l -> new PartialParticle(Particle.END_ROD, l, 1, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer));
		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1, 0.65f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.25f);
	}
}
