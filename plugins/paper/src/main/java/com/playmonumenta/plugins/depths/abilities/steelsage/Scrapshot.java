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
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class Scrapshot extends DepthsAbility {
	public static final String ABILITY_NAME = "Scrapshot";
	private static final int COOLDOWN = 10 * 20;
	private static final int[] DAMAGE = {30, 37, 45, 52, 60, 75};
	private static final double VELOCITY = 1;
	private static final int RANGE = 8;

	private static final Particle.DustOptions SCRAPSHOT_COLOR = new Particle.DustOptions(Color.fromRGB(130, 130, 130), 1.0f);

	public static final String CHARM_COOLDOWN = "Scrapshot Cooldown";

	public static final DepthsAbilityInfo<Scrapshot> INFO =
		new DepthsAbilityInfo<>(Scrapshot.class, ABILITY_NAME, Scrapshot::new, DepthsTree.STEELSAGE, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.SCRAPSHOT)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.actionBarColor(TextColor.color(130, 130, 130))
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Scrapshot::cast, DepthsTrigger.SHIFT_LEFT_CLICK))
			.displayItem(Material.NETHERITE_SCRAP)
			.descriptions(Scrapshot::getDescription);

	private final double mDamage;
	private final double mRange;

	public Scrapshot(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.SCRAPSHOT_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mRange = CharmManager.getRadius(mPlayer, CharmEffects.SCRAPSHOT_RANGE.mEffectName, RANGE);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		Vector dir = loc.getDirection();
		World world = mPlayer.getWorld();
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 50, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);

		world.playSound(mPlayer.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 2);

		Vector velocity = mPlayer.getLocation().getDirection().multiply(-CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.SCRAPSHOT_VELOCITY.mEffectName, VELOCITY));
		mPlayer.setVelocity(velocity.setY(Math.max(0.1, velocity.getY())));

		Set<LivingEntity> hitMobs = new HashSet<>();
		for (double y = -0.1; y <= 0.1; y += 0.1) {
			for (int a = -1; a <= 1; a++) {
				//Do not make corners
				if (Math.abs(y) == Math.abs(a * 0.1)) {
					continue;
				}

				double angle = a * Math.toRadians(5);
				Vector newDir = new Vector(FastUtils.cos(angle) * dir.getX() + FastUtils.sin(angle) * dir.getZ(), dir.getY() + y, FastUtils.cos(angle) * dir.getZ() - FastUtils.sin(angle) * dir.getX());
				newDir.normalize();

				new PartialParticle(Particle.CLOUD, loc, 0, newDir.getX(), newDir.getY(), newDir.getZ(), 1).spawnAsPlayerActive(mPlayer);

				RayTraceResult result = world.rayTrace(loc, dir, mRange, FluidCollisionMode.NEVER, true, 0.425,
					e -> e instanceof LivingEntity && EntityUtils.isHostileMob(e) && !e.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));

				Location endLoc;
				if (result == null) {
					endLoc = loc.add(dir.clone().multiply(mRange));
				} else {
					endLoc = result.getHitPosition().toLocation(world);
					if (result.getHitEntity() instanceof LivingEntity le) {
						hitMobs.add(le);
					}
				}

				new PPLine(Particle.SMOKE_NORMAL, loc, endLoc).shiftStart(0.75).countPerMeter(3).minParticlesPerMeter(0).delta(0.025).extra(0.05).spawnAsPlayerActive(mPlayer);
				new PPLine(Particle.REDSTONE, loc, endLoc).shiftStart(0.75).countPerMeter(3).delta(0.05).data(SCRAPSHOT_COLOR).spawnAsPlayerActive(mPlayer);
			}
		}

		for (LivingEntity mob : hitMobs) {
			Location l = mob.getLocation();
			double dist = mob.getLocation().distance(loc);
			double mult = Math.min(1, (mRange * 1.25 - dist) / mRange);
			DamageUtils.damage(mPlayer, mob, DamageType.PROJECTILE_SKILL, mult * mDamage, mInfo.getLinkedSpell());
			new PartialParticle(Particle.SQUID_INK, l, (int) ((mRange - dist) * 2), 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);
			world.playSound(l, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 0);
		}

		return true;
	}

	private static Description<Scrapshot> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Scrapshot>(color)
			.add("Left click while sneaking to fire a blunderbuss shot that goes up to ")
			.add(a -> a.mRange, RANGE)
			.add(" blocks in a cone, dealing ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" projectile damage and knocks you backward. Damage is decreased based on distance if the distance is greater than 25% of the max range.")
			.addCooldown(COOLDOWN);
	}
}
