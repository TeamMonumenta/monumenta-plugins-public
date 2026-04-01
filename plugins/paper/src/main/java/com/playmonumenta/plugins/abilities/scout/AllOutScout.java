package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentHealthBoost;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.EnumSet;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AllOutScout extends Ability {

	private static final int COOLDOWN = 80 * 20;
	private static final int DURATION = 8 * 20;

	public static final AbilityInfo<AllOutScout> INFO =
		new AbilityInfo<>(AllOutScout.class, "All Out Scout", AllOutScout::new)
			.linkedSpell(ClassAbility.ALL_OUT_SCOUT)
			.scoreboardId("AllOutScout")
			.shorthandName("AOS")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Go all out")
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", AllOutScout::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true).onGround(false),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(Material.SPECTRAL_ARROW);


	public AllOutScout(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		if (ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return false;
		}
		putOnCooldown();
		mPlayer.setCooldown(mPlayer.getInventory().getItemInMainHand().getType(), 12);
		world.playSound(mPlayer.getLocation(), Sound.ITEM_SHIELD_BREAK, 1f, 1f);
		Hitbox hitbox = Hitbox.approximateCone(loc, 4, 1.3);
		hitbox.getHitMobs().forEach(
			m -> {
				DamageUtils.damage(mPlayer, m, DamageEvent.DamageType.MELEE_SKILL, 5, ClassAbility.ALL_OUT_SCOUT, true);
				MovementUtils.knockAway(loc, m, 1.5f);
			}
		);
		Vector arcDir = loc.getDirection().setY(0).normalize();
		double degree = VectorUtils.vectorToRotation(arcDir)[0] + 90;
		new PPCircle(Particle.CRIT, loc.clone().add(0, 0.15, 0), 4).arcDegree(degree - Math.toDegrees(1.3), degree + Math.toDegrees(1.3)).countPerMeter(3).delta(0, 0.1, 0).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.CRIT, loc.clone().add(0, 0.15, 0), 1.5).arcDegree(degree - Math.toDegrees(1.3), degree + Math.toDegrees(1.3)).countPerMeter(3).delta(0, 0.1, 0).spawnAsPlayerActive(mPlayer);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1f);
			world.playSound(loc, Sound.ENTITY_BREEZE_SLIDE, 1f, 1.3f);
			mPlayer.setVelocity(NmsUtils.getVersionAdapter().getActualDirection(mPlayer).multiply(0.35));
			mPlugin.mEffectManager.addEffect(mPlayer, "AllOutScoutMaxHealth", new PercentHealthBoost(DURATION, -0.54, "AllOutScoutMaxHealth"));
			mPlugin.mEffectManager.addEffect(mPlayer, "AllOutScoutMagicVuln", new PercentDamageReceived(DURATION, 0.36, EnumSet.of(DamageEvent.DamageType.MAGIC)));
			mPlugin.mEffectManager.addEffect(mPlayer, "AllOutScoutAttackDamage", new PercentDamageDealt(DURATION, 0.21).damageTypes(DamageEvent.DamageType.getAllMeleeTypes()));
			// All Out Scout disables Tactical Maneuver while active
			new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					mPlayer.setGliding(true);
					mT++;
					if (mT >= DURATION || ZoneUtils.hasZoneProperty(mPlayer.getLocation(), ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)
						|| mPlayer.getGameMode() == GameMode.ADVENTURE || !mPlayer.isValid() || !mPlayer.isOnline()) {
						cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}, 8);
		return true;
	}

	private static Description<AllOutScout> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addDashedLine();
	}

	private static Description<AllOutScout> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addDashedLine();
	}

	private static Description<AllOutScout> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addTrigger()
			.addDashedLine()
			.addLine("Scout shatters their bow,")
			.addLine("dealing physical damage and")
			.addLine("Knocking Back an enemy.")
			.addLine("Enemies that hit a wall will")
			.addLine("take a greater amount of")
			.addLine("physical damage, are Knocked")
			.addLine("Back over the wall, and are")
			.addLine("briefly Stunned. Scout then")
			.addLine("dashes after the enemy and")
			.addLine("goes All Out for an extended")
			.addLine("duration. All Out Scout loses")
			.addLine("a percentage of max health,")
			.addLine("bonus armor, and bonus magic")
			.addLine("resistance. Scout gains attack")
			.addLine("damage, omnivamp, and")
			.addLine("transforms their abilities.")
			.addDashedLine();
	}

}
