package com.playmonumenta.plugins.depths.abilities.earthbound;

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
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class IronGrip extends DepthsAbility {

	private static final int COOLDOWN = 12 * 20;
	private static final int RADIUS = 4;
	private static final int RANGE = 20;
	private static final double[] RESISTANCE = {0.15, 0.18, 0.22, 0.26, 0.30, 0.40};
	private static final int RESISTANCE_DURATION = 5 * 20;
	private static final double[] DAMAGE = {12, 14, 16, 18, 20, 24};
	private static final int ROOT_DURATION = 3 * 20;

	public static final String CHARM_COOLDOWN = "Iron Grip Cooldown";

	public static final DepthsAbilityInfo<IronGrip> INFO =
		new DepthsAbilityInfo<>(IronGrip.class, "Iron Grip", IronGrip::new, DepthsTree.EARTHBOUND, DepthsTrigger.SHIFT_RIGHT_CLICK)
			.linkedSpell(ClassAbility.IRON_GRIP)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", IronGrip::cast, DepthsTrigger.SHIFT_RIGHT_CLICK))
			.displayItem(Material.IRON_ORE)
			.descriptions(IronGrip::getDescription);

	private final double mRadius;
	private final double mRange;
	private final double mResist;
	private final int mResistDuration;
	private final double mDamage;
	private final int mRootDuration;

	public IronGrip(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mRadius = CharmManager.getRadius(player, CharmEffects.IRON_GRIP_RADIUS.mEffectName, RADIUS);
		mRange = CharmManager.calculateFlatAndPercentValue(player, CharmEffects.IRON_GRIP_CAST_RANGE.mEffectName, RANGE);
		mResist = RESISTANCE[mRarity - 1] + CharmManager.getLevelPercentDecimal(player, CharmEffects.IRON_GRIP_RESIST_AMPLIFIER.mEffectName);
		mResistDuration = CharmManager.getDuration(player, CharmEffects.IRON_GRIP_RESIST_DURATION.mEffectName, RESISTANCE_DURATION);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CharmEffects.IRON_GRIP_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mRootDuration = CharmManager.getDuration(player, CharmEffects.IRON_GRIP_ROOT_DURATION.mEffectName, ROOT_DURATION);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		putOnCooldown();

		World world = mPlayer.getWorld();
		Location eyeLoc = mPlayer.getEyeLocation();
		RayTraceResult result = world.rayTrace(eyeLoc, eyeLoc.getDirection(), mRange, FluidCollisionMode.NEVER, true, 0.425,
			e -> EntityUtils.isHostileMob(e) && !ScoreboardUtils.checkTag(e, AbilityUtils.IGNORE_TAG) && !e.isDead() && e.isValid());
		Location targetLoc;
		if (result == null) {
			targetLoc = eyeLoc.clone().add(eyeLoc.getDirection().multiply(mRange));
		} else {
			targetLoc = result.getHitPosition().toLocation(world);
		}

		Location playerLoc = mPlayer.getLocation();
		Location destinationCenter = playerLoc.add(VectorUtils.rotateTargetDirection(new Vector(0, 1, 2.75), playerLoc.getYaw(), playerLoc.getPitch()));

		new PPCircle(Particle.FALLING_DUST, targetLoc, mRadius).data(Material.IRON_ORE.createBlockData()).ringMode(false).countPerMeter(3).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.BLOCK_CRACK, targetLoc, mRadius).data(Material.IRON_BLOCK.createBlockData()).ringMode(false).countPerMeter(4).spawnAsPlayerActive(mPlayer);

		Hitbox hitbox = new Hitbox.SphereHitbox(targetLoc, mRadius);

		List<Player> players = hitbox.getHitPlayers(true);
		players.add(mPlayer);
		for (Player player : players) {
			Location loc = player.getLocation();
			world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.6f, 1.35f);
			world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1f, 1.1f);
			new PartialParticle(Particle.FIREWORKS_SPARK, loc, 20, 0.2, 0, 0.2, 0.25).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.BLOCK_CRACK, loc, 20, 0.2, 0, 0.2, 0.25, Material.IRON_BLOCK.createBlockData()).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SPELL_INSTANT, loc, 20, 0.4, 0.4, 0.4, 0.25).spawnAsPlayerActive(mPlayer);

			mPlugin.mEffectManager.addEffect(player, "IronGripResistance", new PercentDamageReceived(mResistDuration, -mResist));
		}

		List<LivingEntity> mobs = hitbox.getHitMobs();
		mobs.removeIf(mob -> ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG));

		for (LivingEntity mob : mobs) {
			Location loc = mPlayer.getLocation();

			if (!EntityUtils.isBoss(mob) && !EntityUtils.isCCImmuneMob(mob)) {
				Location destination = destinationCenter.clone().add(mob.getLocation().subtract(targetLoc).multiply(0.5));

				// don't teleport into the ground
				// if below us and inside a block, move up to the top, if it's there
				if (destination.getY() < loc.getY() && destination.getBlock().isSolid()) {
					destination = LocationUtils.fallToGround(destination.add(0, 1.5, 0), loc.getY());
				}
				// if below us and still not visible somehow, just place on the same y-level
				if (destination.getY() < loc.getY() && !mPlayer.hasLineOfSight(destination)) {
					destination.setY(loc.getY());
				}

				final Location finalDestination = destination;
				new BukkitRunnable() { // teleport multiple times so that it actually updates

					int mTicks = 0;
					@Override
					public void run() {
						EntityUtils.teleportStack(mob, finalDestination);

						mTicks++;
						if (mTicks >= 5) {
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}

			DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDamage, mInfo.getLinkedSpell(), true, false);
			EntityUtils.applyTaunt(mob, mPlayer);
			EntityUtils.applySlow(mPlugin, mRootDuration, 1, mob);

			new PartialParticle(Particle.BLOCK_CRACK, LocationUtils.getEntityCenter(mob), 50, 0.5, 0.2, 0.5, 0, Material.IRON_ORE.createBlockData()).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.BLOCK_CRACK, LocationUtils.getEntityCenter(mob), 50, 0.5, 0.2, 0.5, 0, Material.DIRT.createBlockData()).spawnAsPlayerActive(mPlayer);
		}

		return true;
	}

	private static Description<IronGrip> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<IronGrip>(color)
			.add("Right click while sneaking to rupture a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block area up to ")
			.add(a -> a.mRange, RANGE)
			.add(" blocks away. You and players hit by the rupture gain ")
			.addPercent(a -> a.mResist, RESISTANCE[rarity - 1], false, true)
			.add(" Resistance for ")
			.addDuration(a -> a.mResistDuration, RESISTANCE_DURATION)
			.add(" seconds. Mobs hit by the rupture take ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" melee damage, are teleported to you (Bosses and CC immune mobs are immune), taunted, and rooted for ")
			.addDuration(a -> a.mRootDuration, ROOT_DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}
}
