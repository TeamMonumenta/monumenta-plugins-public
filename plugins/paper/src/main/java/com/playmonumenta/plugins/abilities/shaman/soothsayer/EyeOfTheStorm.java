package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class EyeOfTheStorm extends Ability {
	public static final int COOLDOWN_1 = 18 * 20;
	public static final int COOLDOWN_2 = 15 * 20;
	public static final int RING_DURATION = 6 * 20;
	public static final int RADIUS = 6;
	public static final int DAMAGE_1 = 4;
	public static final int DAMAGE_2 = 6;
	private static final double VELOCITY = 2;

	public double mDamage;
	private final Map<Snowball, ItemStatManager.PlayerItemStats> mProjectiles = new WeakHashMap<>();

	public static final AbilityInfo<EyeOfTheStorm> INFO =
		new AbilityInfo<>(EyeOfTheStorm.class, "Eye of the Storm", EyeOfTheStorm::new)
			.linkedSpell(ClassAbility.GRAVITY_RING)
			.scoreboardId("EyeoftheStorm")
			.shorthandName("EOTS")
			.descriptions(
				String.format("Punch while sneaking with a projectile weapon to summon a %s block radius circle that lasts %ss and deals %s magic damage to all mobs " +
						"in it every second (goes through iframes), pulling them towards the center (%ss cooldown)",
					RADIUS,
					RING_DURATION / 20,
					DAMAGE_1,
					COOLDOWN_1 / 20
				),
				String.format("Magic damage increased to %s and cooldown reduced to %ss.",
					DAMAGE_2,
					COOLDOWN_2 / 20)
			)
			.simpleDescription("Summon a medium sized ring which deals damage to mobs within and pulls them towards its center.")
			.cooldown(COOLDOWN_1, COOLDOWN_2)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", EyeOfTheStorm::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(Material.WHITE_CANDLE);

	public EyeOfTheStorm(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AuditListener.logSevere(player.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
			AbilityUtils.resetClass(player);
		}
		mDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
		mDamage *= SoothsayerPassive.damageBuff(mPlayer);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, 1.0f, 0.25f);
		Snowball proj = AbilityUtils.spawnAbilitySnowball(mPlugin, mPlayer, world, VELOCITY, "Eye of the Storm Projectile", Particle.CLOUD);

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		mProjectiles.put(proj, playerItemStats);
		putOnCooldown();

		// Clear out list just in case
		mProjectiles.keySet().removeIf(p -> p.isDead() || !p.isValid() || p.getTicksLived() >= 100);
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (!(proj instanceof Snowball)) {
			return;
		}
		ItemStatManager.PlayerItemStats stats = mProjectiles.remove(proj);
		if (stats != null) {
			ring(proj.getLocation(), stats);
		}
	}

	private void ring(Location loc, ItemStatManager.PlayerItemStats stats) {
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {

				PPCircle lowerRing = new PPCircle(Particle.DRAGON_BREATH, loc.clone().add(0, 0.5, 0), RADIUS).ringMode(true).count(10).delta(0).extra(0);
				lowerRing.spawnAsPlayerActive(mPlayer);
				PPCircle higherRing = new PPCircle(Particle.GLOW, loc.clone().add(0, 1, 0), RADIUS).ringMode(true).count(10).delta(0).extra(0);
				higherRing.spawnAsPlayerActive(mPlayer);

				if (mTicks % 20 == 0) {
					Set<LivingEntity> affectedMobs = new HashSet<>(EntityUtils.getNearbyMobs(loc, RADIUS));
					if (!affectedMobs.isEmpty()) {
						loc.getWorld().playSound(loc, Sound.ENTITY_CAT_HISS, 2.0f, 1.0f);
						for (LivingEntity mob : affectedMobs) {
							DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), stats), mDamage, true, false, false);
							mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_CAT_HISS, 2.0f, 1.0f);
							MovementUtils.pullTowards(loc, mob, 0.2f);
						}
					}
				}
				if (mTicks >= RING_DURATION) {
					this.cancel();
				}

				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2);

	}
}
