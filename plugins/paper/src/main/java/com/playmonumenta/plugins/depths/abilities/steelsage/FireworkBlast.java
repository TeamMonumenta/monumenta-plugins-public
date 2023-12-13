package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class FireworkBlast extends DepthsAbility {
	public static final String ABILITY_NAME = "Firework Blast";
	private static final String ABILITY_METAKEY = "FireworkBlastMetakey";
	private static final int COOLDOWN = 12 * 20;
	private static final int[] DAMAGE = {16, 20, 24, 28, 32, 40};
	private static final int[] DAMAGE_CAP = {32, 40, 48, 56, 64, 80};
	private static final double DAMAGE_INCREASE_PER_BLOCK = 0.05;
	private static final double RADIUS = 1.5;
	private static final double DIRECT_HIT_RADIUS = 4;

	private static final Particle.DustOptions GRAY_COLOR = new Particle.DustOptions(Color.fromRGB(130, 130, 130), 1.0f);
	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(244, 56, 0), 1.0f);
	private static final Particle.DustOptions ORANGE_COLOR = new Particle.DustOptions(Color.fromRGB(255, 127, 20), 1.0f);

	public static final String CHARM_COOLDOWN = "Firework Blast Cooldown";

	public static final DepthsAbilityInfo<FireworkBlast> INFO =
		new DepthsAbilityInfo<>(FireworkBlast.class, ABILITY_NAME, FireworkBlast::new, DepthsTree.STEELSAGE, DepthsTrigger.SHIFT_RIGHT_CLICK)
			.linkedSpell(ClassAbility.FIREWORKBLAST)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", FireworkBlast::cast, DepthsTrigger.SHIFT_RIGHT_CLICK))
			.displayItem(Material.FIREWORK_ROCKET)
			.descriptions(FireworkBlast::getDescription);

	private final double mBaseDamage;
	private final double mDamagePerBlock;
	private final double mDamageCap;
	private final double mRadius;
	private final double mDirectHitRadius;

	public FireworkBlast(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mBaseDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.FIREWORK_BLAST_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mDamagePerBlock = DAMAGE_INCREASE_PER_BLOCK + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.FIREWORK_BLAST_DAMAGE_PER_BLOCK.mEffectName);
		mDamageCap = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.FIREWORK_BLAST_DAMAGE_CAP.mEffectName, DAMAGE_CAP[mRarity - 1]);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.FIREWORK_BLAST_RADIUS.mEffectName, RADIUS);
		mDirectHitRadius = CharmManager.getRadius(mPlayer, CharmEffects.FIREWORK_BLAST_RADIUS.mEffectName, DIRECT_HIT_RADIUS);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		Firework rocket = (Firework) mPlayer.getWorld().spawnEntity(mPlayer.getLocation().add(0, 1.3, 0), EntityType.FIREWORK);
		rocket.setShooter(mPlayer);
		rocket.setShotAtAngle(true);
		rocket.setMetadata(ABILITY_METAKEY, new FixedMetadataValue(mPlugin, null));

		Vector vel = mPlayer.getLocation().getDirection();
		rocket.setVelocity(vel);

		FireworkEffect effect = FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(Color.BLACK, Color.RED).withFade(Color.GRAY, Color.ORANGE).build();
		FireworkMeta meta = rocket.getFireworkMeta();
		meta.addEffect(effect);
		meta.setPower(3);

		rocket.setFireworkMeta(meta);

		mPlugin.mProjectileEffectTimers.addEntity(rocket, Particle.SMOKE_NORMAL);

		ProjectileLaunchEvent event = new ProjectileLaunchEvent(rocket);
		Bukkit.getPluginManager().callEvent(event);
		rocket.setVelocity(rocket.getVelocity().multiply(2));

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (rocket.isDead() || !rocket.isValid() || rocket.getVelocity().equals(new Vector(0, 0, 0))) {
					Location loc = rocket.getLocation();
					World world = rocket.getWorld();

					double dist = mPlayer.getLocation().distance(loc);
					double mult = 1 + dist * mDamagePerBlock;
					double damage = Math.min(mBaseDamage * mult, mDamageCap);

					new PartialParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer);

					// use a raytrace because for some reason vanilla fireworks can detonate well away from the actual mob...
					// so we continue the direction of travel and see if it was going to hit something
					boolean directHit = false;
					RayTraceResult result = world.rayTrace(loc, vel, 2, FluidCollisionMode.NEVER, true, 0.425,
						e -> EntityUtils.isHostileMob(e) && !ScoreboardUtils.checkTag(e, AbilityUtils.IGNORE_TAG) && !e.isDead() && e.isValid());
					if (result != null && result.getHitEntity() != null) {
						directHit = true;
					}

					if (!directHit) {
						for (LivingEntity e : EntityUtils.getNearbyMobs(loc, mRadius)) {
							DamageUtils.damage(mPlayer, e, new DamageEvent.Metadata(DamageType.PROJECTILE_SKILL, mInfo.getLinkedSpell(), playerItemStats), damage, false, true, false);

							new PartialParticle(Particle.SMOKE_LARGE, e.getLocation(), 20, 0, 0, 0, 0.2).spawnAsPlayerActive(mPlayer);
						}
					} else {
						// direct hit
						new PartialParticle(Particle.FIREWORKS_SPARK, loc, 50, 0, 0, 0, 0.3).spawnAsPlayerActive(mPlayer);
						world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 2f, 1.2f);
						world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 2f, 1.2f);

						for (LivingEntity e : EntityUtils.getNearbyMobs(loc, mDirectHitRadius)) {
							DamageUtils.damage(mPlayer, e, new DamageEvent.Metadata(DamageType.PROJECTILE_SKILL, mInfo.getLinkedSpell(), playerItemStats), damage, false, true, false);

							new PartialParticle(Particle.SMOKE_LARGE, e.getLocation(), 20, 0, 0, 0, 0.2).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.REDSTONE, e.getLocation(), 10, 0.25, 0.25, 0.25, GRAY_COLOR).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.REDSTONE, e.getLocation(), 5, 0.25, 0.25, 0.25, RED_COLOR).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.REDSTONE, e.getLocation(), 5, 0.25, 0.25, 0.25, ORANGE_COLOR).spawnAsPlayerActive(mPlayer);
						}
					}

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		return true;
	}

	public static boolean isDamaging(Firework fw) {
		return fw.hasMetadata(ABILITY_METAKEY);
	}

	private static Description<FireworkBlast> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<FireworkBlast>(color)
			.add("Right click while sneaking and holding a weapon to shoot a firework that deals ")
			.addDepthsDamage(a -> a.mBaseDamage, DAMAGE[rarity - 1], true)
			.add(" projectile damage to enemies within ")
			.add(RADIUS)
			.add(" blocks of its explosion. If an enemy is directly hit by the firework, increase the radius to ")
			.add(a -> a.mDirectHitRadius, DIRECT_HIT_RADIUS)
			.add(" blocks instead. The damage is increased by ")
			.addPercent(a -> a.mDamagePerBlock, DAMAGE_INCREASE_PER_BLOCK)
			.add(" for every block the firework travels, up to ")
			.addDepthsDamage(a -> a.mDamageCap, DAMAGE_CAP[rarity - 1], true)
			.add(" damage.")
			.addCooldown(COOLDOWN);
	}
}
