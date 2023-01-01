package com.playmonumenta.plugins.abilities.rogue.swordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class BladeDance extends Ability {

	private static final int DANCE_1_DAMAGE = 6;
	private static final int DANCE_2_DAMAGE = 9;
	private static final double SLOWNESS_AMPLIFIER = 1;
	private static final int SLOW_DURATION_1 = 2 * 20;
	private static final int SLOW_DURATION_2 = (int) (2.5 * 20);
	private static final int DANCE_RADIUS = 5;
	private static final float DANCE_KNOCKBACK_SPEED = 0.2f;
	private static final int INVULN_DURATION = 15;
	private static final int COOLDOWN_1 = 18 * 20;
	private static final int COOLDOWN_2 = 16 * 20;
	private static final Particle.DustOptions SWORDSAGE_COLOR = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f);

	public static final String CHARM_DAMAGE = "Blade Dance Damage";
	public static final String CHARM_ROOT = "Blade Dance Root Duration";
	public static final String CHARM_RESIST = "Blade Dance Resistance Duration";
	public static final String CHARM_COOLDOWN = "Blade Dance Cooldown";
	public static final String CHARM_RADIUS = "Blade Dance Radius";

	public static final AbilityInfo<BladeDance> INFO =
		new AbilityInfo<>(BladeDance.class, "Blade Dance", BladeDance::new)
			.linkedSpell(ClassAbility.BLADE_DANCE)
			.scoreboardId("BladeDance")
			.shorthandName("BD")
			.descriptions(
				String.format("When holding two swords, right-click while looking down to enter a defensive stance, " +
					              "parrying all attacks and becoming invulnerable for 0.75 seconds. " +
					              "Afterwards, unleash a powerful attack that deals %s melee damage to enemies in a %s block radius. " +
					              "Damaged enemies are rooted for %s seconds. Cooldown: %ss.",
					DANCE_1_DAMAGE,
					DANCE_RADIUS,
					SLOW_DURATION_1 / 20,
					COOLDOWN_1 / 20
				),
				String.format("The area attack now deals %s damage and roots for %ss. Cooldown: %ss.",
					DANCE_2_DAMAGE,
					SLOW_DURATION_2 / 20.0,
					COOLDOWN_2 / 20))
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", BladeDance::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).lookDirections(AbilityTrigger.LookDirection.DOWN).sneaking(false),
				AbilityTriggerInfo.HOLDING_TWO_SWORDS_RESTRICTION))
			.displayItem(new ItemStack(Material.STRING, 1));


	private final double mDamage;
	private final int mSlowDuration;

	public BladeDance(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DANCE_1_DAMAGE : DANCE_2_DAMAGE);
		mSlowDuration = CharmManager.getDuration(player, CHARM_ROOT, (isLevelOne() ? SLOW_DURATION_1 : SLOW_DURATION_2));
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1f, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.5f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
		new PartialParticle(Particle.VILLAGER_ANGRY, mPlayer.getLocation().clone().add(0, 1, 0), 6, 0.45, 0.5, 0.45, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, mPlayer.getLocation().clone().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 0.15).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, mPlayer.getLocation().clone().add(0, 1, 0), 6, 0.45, 0.5, 0.45, 0, SWORDSAGE_COLOR).spawnAsPlayerActive(mPlayer);
		mPlayer.setInvulnerable(true);
		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;
			float mPitch = 0.5f;

			@Override
			public void run() {
				mTicks += 1;
				Location loc = mPlayer.getLocation();
				double r = DANCE_RADIUS - (3 * mPitch);
				new PartialParticle(Particle.SWEEP_ATTACK, loc, 3, r, 2, r, 0).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.REDSTONE, loc, 4, r, 2, r, 0, SWORDSAGE_COLOR).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CLOUD, loc, 4, r, 2, r, 0).spawnAsPlayerActive(mPlayer);
				if (mTicks % 2 == 0) {
					world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, mPitch);
					mPitch += 0.1f;
				}

				if (mTicks >= CharmManager.getDuration(mPlayer, CHARM_RESIST, INVULN_DURATION)) {
					mPlayer.setInvulnerable(false);
					world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 1);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 2f);
					world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 0.75f);

					new PartialParticle(Particle.VILLAGER_ANGRY, mPlayer.getLocation().clone().add(0, 1, 0), 6, 0.45, 0.5, 0.45, 0).spawnAsPlayerActive(mPlayer);

					Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), CharmManager.getRadius(mPlayer, CHARM_RADIUS, DANCE_RADIUS));
					for (LivingEntity mob : hitbox.getHitMobs()) {
						DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDamage, mInfo.getLinkedSpell(), true);
						MovementUtils.knockAway(mPlayer, mob, DANCE_KNOCKBACK_SPEED, true);

						if (!EntityUtils.isBoss(mob)) {
							EntityUtils.applySlow(mPlugin, mSlowDuration, SLOWNESS_AMPLIFIER, mob);
						}

						Location mobLoc = mob.getLocation().add(0, 1, 0);
						new PartialParticle(Particle.SWEEP_ATTACK, mobLoc, 5, 0.35, 0.5, 0.35, 0).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.CRIT, mobLoc, 10, 0.25, 0.5, 0.25, 0.3).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.REDSTONE, mobLoc, 15, 0.35, 0.5, 0.35, 0, SWORDSAGE_COLOR).spawnAsPlayerActive(mPlayer);
					}

					cancelOnDeath(new BukkitRunnable() {
						int mTicks = 0;
						double mRadians = 0;

						@Override
						public void run() {
							Vector vec = new Vector(FastUtils.cos(mRadians) * DANCE_RADIUS / 1.5, 0, FastUtils.sin(mRadians) * DANCE_RADIUS / 1.5);

							Location loc2 = mPlayer.getEyeLocation().add(vec);
							new PartialParticle(Particle.SWEEP_ATTACK, loc2, 5, 1, 0.25, 1, 0).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.CRIT, loc2, 10, 1, 0.25, 1, 0.3).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.REDSTONE, loc2, 10, 1, 0.25, 1, 0, SWORDSAGE_COLOR).spawnAsPlayerActive(mPlayer);
							world.playSound(loc2, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1.5f);

							if (mTicks >= 5) {
								this.cancel();
							}

							mTicks++;
							mRadians += Math.toRadians(72);
						}
					}.runTaskTimer(mPlugin, 0, 1));

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));

		putOnCooldown();
	}

}
