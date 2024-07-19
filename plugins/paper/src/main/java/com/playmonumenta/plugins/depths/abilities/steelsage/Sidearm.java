package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
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
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
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

public class Sidearm extends DepthsAbility implements AbilityWithChargesOrStacks {

	public static final String ABILITY_NAME = "Sidearm";
	public static final int MAX_CHARGES = 3;
	private static final int COOLDOWN = 14 * 20;
	private static final int KILL_COOLDOWN_REDUCTION = 3 * 20;
	private static final int[] DAMAGE = {12, 15, 18, 21, 24, 30};
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

	private final int mMaxCharges;
	private final double mRange;
	private final double mDamage;
	private final int mCDR;

	private int mLastCastTicks = 0;
	private boolean mWasOnCooldown;
	private int mCharges;

	public Sidearm(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = MAX_CHARGES + (int) CharmManager.getLevel(mPlayer, CharmEffects.SIDEARM_CHARGES.mEffectName);
		mRange = CharmManager.getRadius(mPlayer, CharmEffects.SIDEARM_RANGE.mEffectName, RANGE);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.SIDEARM_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mCDR = CharmManager.getDuration(mPlayer, CharmEffects.SIDEARM_KILL_CDR.mEffectName, KILL_COOLDOWN_REDUCTION);

		mCharges = Math.min(AbilityManager.getManager().getTrackedCharges(mPlayer, ClassAbility.SIDEARM), mMaxCharges);
	}

	public boolean cast() {
		if (mCharges <= 0) {
			return false;
		}

		int tick = Bukkit.getServer().getCurrentTick();
		if (mCharges > 0 && tick - mLastCastTicks > 5) {
			fireShot();
			mLastCastTicks = tick;
		}

		AbilityManager.getManager().trackCharges(mPlayer, ClassAbility.SIDEARM, mCharges);

		ClientModHandler.updateAbility(mPlayer, this);
		sendActionBarMessage("Sidearm Ammo: " + mCharges);

		return true;
	}

	private void fireShot() {
		if (!isOnCooldown()) {
			putOnCooldown();
		}

		mCharges--;

		Location startLoc = mPlayer.getEyeLocation();
		Vector dir = startLoc.getDirection();
		World world = startLoc.getWorld();
		RayTraceResult result = world.rayTrace(startLoc, dir, mRange, FluidCollisionMode.NEVER, true, 0.425,
			e -> EntityUtils.isHostileMob(e) && !ScoreboardUtils.checkTag(e, AbilityUtils.IGNORE_TAG) && !e.isDead() && e.isValid());

		world.playSound(startLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1f, mCharges == 1 ? 0.8f : 0.6f);
		world.playSound(startLoc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 1f, 2f);
		world.playSound(startLoc, Sound.BLOCK_NOTE_BLOCK_SNARE, SoundCategory.PLAYERS, 1f, mCharges == 1 ? 1.5f : 0.5f);
		world.playSound(startLoc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.PLAYERS, 1f, mCharges == 1 ? 1.5f : 0.5f);
		world.playSound(startLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 1f, 2f);

		if (result == null) {
			Location endLoc = startLoc.clone().add(dir.multiply(mRange));
			hitEffect(endLoc);
			lineEffect(startLoc, endLoc);
			return;
		}



		Location endLoc = result.getHitPosition().toLocation(world);
		hitEffect(endLoc);

		if (result.getHitEntity() instanceof LivingEntity mob) {
			world.playSound(startLoc, Sound.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.3f, 1.1f);

			DamageUtils.damage(mPlayer, mob, DamageType.PROJECTILE_SKILL, mDamage, ClassAbility.SIDEARM, true, false);
			if (mob.isDead() || mob.getHealth() <= 0) {
				mPlugin.mTimers.updateCooldown(mPlayer, ClassAbility.SIDEARM, mCDR);
				if (!isOnCooldown()) {
					mCharges = mMaxCharges; // we aren't using normal ability cooldowns, so check it right away
					showOffCooldownMessage();
					ClientModHandler.updateAbility(mPlayer, this);
				}

				new PPExplosion(Particle.LAVA, endLoc).spawnAsPlayerActive(mPlayer).speed(1).count(6);
				mPlayer.getWorld().playSound(startLoc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1, 2);
			}
		}

		lineEffect(startLoc, endLoc);
	}

	private void lineEffect(Location startLoc, Location endLoc) {
		new PPLine(Particle.SMOKE_NORMAL, startLoc, endLoc).shiftStart(0.75).countPerMeter(6).minParticlesPerMeter(0).delta(0.05).extra(0.05).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, startLoc, endLoc).shiftStart(0.75).countPerMeter(18).delta(0.075).data(SIDEARM_COLOR).spawnAsPlayerActive(mPlayer);
	}

	private void hitEffect(Location loc) {
		new PartialParticle(Particle.SQUID_INK, loc, 30, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);
		loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 0);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mWasOnCooldown && !isOnCooldown()) {
			mCharges = mMaxCharges;
			AbilityManager.getManager().trackCharges(mPlayer, ClassAbility.SIDEARM, mCharges);

			Location loc = mPlayer.getLocation();
			mPlayer.playSound(loc, Sound.BLOCK_IRON_DOOR_OPEN, SoundCategory.PLAYERS, 0.8f, 1.5f);
			mPlayer.playSound(loc, Sound.BLOCK_CHEST_LOCKED, SoundCategory.PLAYERS, 0.8f, 2.0f);
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPlayer.playSound(loc, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.PLAYERS, 0.8f, 1.2f), 1);

			showOffCooldownMessage();
			ClientModHandler.updateAbility(mPlayer, this);
		}

		mWasOnCooldown = isOnCooldown();

		if (!isOnCooldown() && mCharges != mMaxCharges) {
			putOnCooldown();
		}
	}

	@Override
	public int getCharges() {
		return mCharges;
	}

	@Override
	public int getMaxCharges() {
		return mMaxCharges;
	}

	@Override
	public ChargeType getChargeType() {
		return ChargeType.CHARGES;
	}

	private static Description<Sidearm> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Sidearm>(color)
			.add("Right click to fire a flintlock shot, dealing ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" projectile damage to the first mob hit and having a range of ")
			.add(a -> a.mRange, RANGE)
			.add(" blocks. If that kills a mob, this ability's cooldown is reduced by ")
			.addDuration(a -> a.mCDR, KILL_COOLDOWN_REDUCTION)
			.add(" seconds. Charges: ")
			.add(a -> a.mMaxCharges, MAX_CHARGES)
			.add(". Sidearm's cooldown replenishes all charges at the same time.")
			.addCooldown(COOLDOWN);
	}
}
