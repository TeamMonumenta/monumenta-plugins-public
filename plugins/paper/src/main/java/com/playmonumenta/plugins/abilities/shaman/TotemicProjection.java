package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DecayedTotem;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.HexbreakerPassive;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SoothsayerPassive;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.WhirlwindTotem;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.HashSet;
import java.util.List;
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
import org.bukkit.util.Vector;

public class TotemicProjection extends Ability {

	private static final int COOLDOWN_1 = 6 * 20;
	private static final int COOLDOWN_2 = 4 * 20;
	private static final int DAMAGE_2 = 5;
	private static final int RADIUS = 4;
	private static final double VELOCITY = 2;
	private static final int DISTRIBUTION_RADIUS = 2;

	private final Map<Snowball, ItemStatManager.PlayerItemStats> mProjectiles = new WeakHashMap<>();
	private double mDamage;

	public static final AbilityInfo<TotemicProjection> INFO =
		new AbilityInfo<>(TotemicProjection.class, "Totemic Projection", TotemicProjection::new)
			.linkedSpell(ClassAbility.TOTEMIC_PROJECTION)
			.scoreboardId("TotemicProjection")
			.shorthandName("TP")
			.descriptions(
				String.format("Press drop with a weapon to fire a projectile that, on landing, moves all active totems to within %s blocks of it. %ss cooldown.",
					DISTRIBUTION_RADIUS,
					COOLDOWN_1 / 20
				),
				String.format("Deals %s magic damage within a %s block radius on hit. %ss cooldown.",
					DAMAGE_2,
					RADIUS,
					COOLDOWN_2 / 20)
			)
			.simpleDescription("Fires a projectile that summons all of your active totems around the landing location.")
			.cooldown(COOLDOWN_1, COOLDOWN_2)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", TotemicProjection::cast, new AbilityTrigger(AbilityTrigger.Key.DROP)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.ENDER_PEARL);

	public TotemicProjection(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AuditListener.logSevere(player.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
			AbilityUtils.resetClass(player);
		}
		mDamage = isLevelOne() ? 0 : DAMAGE_2;
		mDamage *= HexbreakerPassive.damageBuff(mPlayer);
		mDamage *= SoothsayerPassive.damageBuff(mPlayer);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, 1.0f, 0.25f);
		Snowball proj = AbilityUtils.spawnAbilitySnowball(mPlugin, mPlayer, world, VELOCITY, "Totemic Projection Projectile", Particle.CLOUD);

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		mProjectiles.put(proj, playerItemStats);
		putOnCooldown();

		// Clear out list just in case
		mProjectiles.keySet().removeIf(p -> p.isDead() || !p.isValid());
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (!(proj instanceof Snowball)) {
			return;
		}
		ItemStatManager.PlayerItemStats stats = mProjectiles.remove(proj);

		if (stats != null) {
			Location dropCenter = proj.getLocation();
			List<LivingEntity> theTotems = TotemicEmpowerment.getTotemList(mPlayer);
			mPlayer.playSound(dropCenter, Sound.BLOCK_VINE_BREAK, 2.0f, 1.0f);
			new PartialParticle(Particle.REVERSE_PORTAL, dropCenter, 20).spawnAsPlayerActive(mPlayer);

			for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
				if (abil instanceof TotemAbility totemAbility) {
					totemAbility.mAttachedMob = null;
					totemAbility.mMobStuckWithEffect = null;
				}
			}


			for (LivingEntity totem : theTotems) {
				for (TotemType t : TotemType.values()) {
					if (t.mName.equals(totem.getName())) {
						Location loc = dropCenter.clone().add(t.mRelative);
						if (loc.getBlock().isPassable()) {
							totem.teleport(loc);
						} else {
							totem.teleport(dropCenter);
						}
						break;
					}
				}
			}
			if (isLevelTwo()) {
				Set<LivingEntity> affectedMobs = new HashSet<>(EntityUtils.getNearbyMobsInSphere(dropCenter, RADIUS, null));
				new PPCircle(Particle.REVERSE_PORTAL, dropCenter, RADIUS).ringMode(false).count(100).spawnAsPlayerActive(mPlayer);

				for (LivingEntity mob : affectedMobs) {
					DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), stats), mDamage, true, false, false);
				}
			}
		}
	}

	private enum TotemType {
		FLAME(FlameTotem.TOTEM_NAME, DISTRIBUTION_RADIUS, DISTRIBUTION_RADIUS),
		CLEANSING(CleansingTotem.TOTEM_NAME, DISTRIBUTION_RADIUS, -DISTRIBUTION_RADIUS),
		LIGHTNING(LightningTotem.TOTEM_NAME, -DISTRIBUTION_RADIUS, -DISTRIBUTION_RADIUS),
		WHIRLWIND(WhirlwindTotem.TOTEM_NAME, -DISTRIBUTION_RADIUS, DISTRIBUTION_RADIUS),
		DECAYED(DecayedTotem.TOTEM_NAME, -DISTRIBUTION_RADIUS, DISTRIBUTION_RADIUS);

		private final String mName;
		private final Vector mRelative;
		TotemType(String name, int x, int z) {
			mName = name;
			mRelative = new Vector(x, 0.5, z);
		}
	}
}
