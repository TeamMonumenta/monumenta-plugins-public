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
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class CrushingEarth extends DepthsAbility {

	public static final String ABILITY_NAME = "Crushing Earth";
	private static final int COOLDOWN = 20 * 8;
	private static final int[] DAMAGE = {8, 10, 12, 14, 16, 24};
	private static final int RANGE = 4;
	private static final int CONE_ANGLE = 50;
	private static final int[] STUN_DURATION = {15, 20, 25, 30, 35, 45};

	public static final String CHARM_COOLDOWN = "Crushing Earth Cooldown";

	public static final DepthsAbilityInfo<CrushingEarth> INFO =
		new DepthsAbilityInfo<>(CrushingEarth.class, ABILITY_NAME, CrushingEarth::new, DepthsTree.EARTHBOUND, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.CRUSHING_EARTH)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CrushingEarth::cast, DepthsTrigger.RIGHT_CLICK))
			.displayItem(Material.SHIELD)
			.descriptions(CrushingEarth::getDescription);

	private final double mRange;
	private final double mAngle;
	private final double mDamage;
	private final int mStunDuration;

	public CrushingEarth(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRange = CharmManager.getRadius(mPlayer, CharmEffects.CRUSHING_EARTH_RANGE.mEffectName, RANGE);
		mAngle = CONE_ANGLE;
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.CRUSHING_EARTH_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mStunDuration = CharmManager.getDuration(mPlayer, CharmEffects.CRUSHING_EARTH_STUN_DURATION.mEffectName, STUN_DURATION[mRarity - 1]);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();

		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.5f, 0.5f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.3f, 1.0f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 1f, 0.6f);
		world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.4f, 2.0f);
		world.playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 1.0f, 0.9f);

		for (LivingEntity mob : Hitbox.approximateCylinderSegment(LocationUtils.getHalfHeightLocation(mPlayer).add(0, -mRange, 0), 2 * mRange, mRange, Math.toRadians(mAngle)).getHitMobs()) {
			Location mobLoc = mob.getEyeLocation();

			EntityUtils.applyStun(mPlugin, mStunDuration, mob);
			DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDamage, mInfo.getLinkedSpell(), true, false);

			new PartialParticle(Particle.CRIT, mobLoc, 25, 0, 0.25, 0, 0.25).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CRIT_MAGIC, mobLoc, 25, 0, 0.25, 0, 0.25).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SPIT, mobLoc, 5, 0.15, 0.5, 0.15, 0).spawnAsPlayerActive(mPlayer);
		}

		new BukkitRunnable() {
			double mCurrentRadius = 0.5;
			final Location mLoc = mPlayer.getLocation();

			@Override
			public void run() {
				for (int i = 0; i < 4; i++) {
					double degree = 90 - mAngle;
					int degreeSteps = ((int) (2 * mAngle)) / 10;
					double degreeStep = 2 * mAngle / degreeSteps;
					for (int step = 0; step < degreeSteps + 1; step++, degree += degreeStep) {
						double radian1 = Math.toRadians(degree);
						Vector vec = new Vector(FastUtils.cos(radian1) * mCurrentRadius, 0, FastUtils.sin(radian1) * mCurrentRadius);
						vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());
						Location l = mLoc.clone().add(vec);

						new PartialParticle(Particle.BLOCK_CRACK, l, 2, 0.25, 0, 0.25).data(Material.PODZOL.createBlockData()).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.BLOCK_CRACK, l, 2, 0.25, 0, 0.25).data(Material.GRANITE.createBlockData()).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.BLOCK_CRACK, l, 2, 0.25, 0, 0.25).data(Material.IRON_ORE.createBlockData()).spawnAsPlayerActive(mPlayer);
					}

					mCurrentRadius += 0.5;
					if (mCurrentRadius > mRange) {
						this.cancel();
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		putOnCooldown();
		return true;
	}


	private static Description<CrushingEarth> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<CrushingEarth>(color)
			.add("Right click to deal ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" melee damage to all mobs in a ")
			.add(a -> a.mRange, RANGE)
			.add(" block cone in front of you and stun them for ")
			.addDuration(a -> a.mStunDuration, STUN_DURATION[rarity - 1], false, true)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}


}
