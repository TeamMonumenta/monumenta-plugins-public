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
import com.playmonumenta.plugins.utils.ScoreboardUtils;
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

public class Sidearm extends DepthsAbility {

	public static final String ABILITY_NAME = "Sidearm";
	private static final int COOLDOWN = 4 * 20;
	private static final double KILL_COOLDOWN_REDUCTION = 0.67;
	private static final int[] DAMAGE = {14, 17, 20, 23, 26, 32};
	private static final int RANGE = 14;

	private static final Particle.DustOptions SIDEARM_COLOR = new Particle.DustOptions(Color.fromRGB(130, 130, 130), 1.0f);

	public static final String CHARM_COOLDOWN = "Sidearm Cooldown";

	public static final DepthsAbilityInfo<Sidearm> INFO =
		new DepthsAbilityInfo<>(Sidearm.class, ABILITY_NAME, Sidearm::new, DepthsTree.STEELSAGE, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.SIDEARM)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.actionBarColor(TextColor.color(130, 130, 130))
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Sidearm::cast, DepthsTrigger.RIGHT_CLICK))
			.displayItem(Material.CROSSBOW)
			.descriptions(Sidearm::getDescription);

	private final double mRange;
	private final double mDamage;
	private final double mCDR;

	public Sidearm(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRange = CharmManager.getRadius(mPlayer, CharmEffects.SIDEARM_RANGE.mEffectName, RANGE);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.SIDEARM_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mCDR = KILL_COOLDOWN_REDUCTION + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.SIDEARM_KILL_CDR.mEffectName);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		Location startLoc = mPlayer.getEyeLocation();
		Vector dir = startLoc.getDirection();
		World world = startLoc.getWorld();
		RayTraceResult result = world.rayTrace(startLoc, dir, mRange, FluidCollisionMode.NEVER, true, 0.425,
			e -> EntityUtils.isHostileMob(e) && !ScoreboardUtils.checkTag(e, AbilityUtils.IGNORE_TAG) && !e.isDead() && e.isValid());

		if (result == null) {
			Location endLoc = startLoc.clone().add(dir.multiply(mRange));
			hitEffect(endLoc);
			lineEffect(startLoc, endLoc);
			return true;
		}

		Location endLoc = result.getHitPosition().toLocation(world);
		hitEffect(endLoc);
		if (startLoc.distance(endLoc) > 5) {
			hitEffect(startLoc.clone().add(startLoc.getDirection().multiply(5)));
		}

		if (result.getHitEntity() instanceof LivingEntity mob) {
			DamageUtils.damage(mPlayer, mob, DamageType.PROJECTILE_SKILL, mDamage, mInfo.getLinkedSpell());
			if (mob.isDead() || mob.getHealth() <= 0) {
				mPlugin.mTimers.addCooldown(mPlayer, ClassAbility.SIDEARM, getModifiedCooldown((int) (getModifiedCooldown() * (1 - mCDR))));
			}
			mob.setVelocity(new Vector(0, 0, 0));
		}

		lineEffect(startLoc, endLoc);
		return true;
	}

	private void lineEffect(Location startLoc, Location endLoc) {
		new PPLine(Particle.SMOKE_NORMAL, startLoc, endLoc).shiftStart(0.75).countPerMeter(6).minParticlesPerMeter(0).delta(0.05).extra(0.05).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, startLoc, endLoc).shiftStart(0.75).countPerMeter(18).delta(0.1).data(SIDEARM_COLOR).spawnAsPlayerActive(mPlayer);
	}

	private void hitEffect(Location loc) {
		new PartialParticle(Particle.SQUID_INK, loc, 30, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);
		loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 0);
	}

	private static Description<Sidearm> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Sidearm>(color)
			.add("Right click to fire a short range flintlock shot that goes up to ")
			.add(a -> a.mRange, RANGE)
			.add(" blocks, stopping at the first enemy hit, dealing ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" projectile damage. If it kills a mob, the cooldown is reduced by ")
			.addPercent(a -> a.mCDR, KILL_COOLDOWN_REDUCTION)
			.add(".")
			.addCooldown(COOLDOWN);
	}
}
