package com.playmonumenta.plugins.depths.abilities.dawnbringer;


import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

public class DivineBeam extends DepthsAbility {

	public static final String ABILITY_NAME = "Divine Beam";
	public static final double HEAL_HITBOX_SIZE = 1.4;
	public static final double STUN_HITBOX_SIZE = 1.1;
	public static final double[] HEAL = {0.2, 0.3, 0.4, 0.5, 0.6, 1.0};
	public static final int[] STUN_DURATION = {10, 12, 14, 16, 20, 30};
	public static double HEAL_INCREASE_PER_TARGET = 0.2;
	public static int STUN_INCREASE_PER_TARGET = 5;
	public static int ABSORPTION_DURATION = 6 * 20;
	public static double MAX_ABSORPTION = 4;
	public static int MAX_TARGET_BONUS = 4;
	public static int COOLDOWN_REDUCTION = 4 * 20;
	public static final int COOLDOWN = 20 * 20;
	private static final int MAX_DISTANCE = 50;

	public static final String CHARM_COOLDOWN = "Divine Beam Cooldown";

	public static final DepthsAbilityInfo<DivineBeam> INFO =
		new DepthsAbilityInfo<>(DivineBeam.class, ABILITY_NAME, DivineBeam::new, DepthsTree.DAWNBRINGER, DepthsTrigger.SHIFT_BOW)
			.linkedSpell(ClassAbility.DIVINE_BEAM)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.actionBarColor(TextColor.color(200, 200, 50))
			.displayItem(Material.YELLOW_CANDLE)
			.descriptions(DivineBeam::getDescription);

	private final double mHeal;
	private final int mStunDuration;
	private final int mMaxTargetBonus;
	private final int mCDR;
	private final double mMaxAbsorption;
	private final int mAbsorptionDuration;

	public DivineBeam(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mHeal = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.DIVINE_BEAM_HEALING.mEffectName, HEAL[mRarity - 1]);
		mStunDuration = CharmManager.getDuration(mPlayer, CharmEffects.DIVINE_BEAM_STUN_DURATION.mEffectName, STUN_DURATION[mRarity - 1]);
		mMaxTargetBonus = MAX_TARGET_BONUS + (int) CharmManager.getLevel(mPlayer, CharmEffects.DIVINE_BEAM_MAX_TARGETS_BONUS.mEffectName);
		mCDR = CharmManager.getDuration(mPlayer, CharmEffects.DIVINE_BEAM_COOLDOWN_REDUCTION.mEffectName, COOLDOWN_REDUCTION);
		mMaxAbsorption = MAX_ABSORPTION + CharmManager.getLevel(mPlayer, CharmEffects.DIVINE_BEAM_MAX_ABSORPTION.mEffectName);
		mAbsorptionDuration = CharmManager.getDuration(mPlayer, CharmEffects.DIVINE_BEAM_ABSORPTION_DURATION.mEffectName, ABSORPTION_DURATION);
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (!mPlayer.isSneaking()
			|| !EntityUtils.isAbilityTriggeringProjectile(projectile, false)
			|| isOnCooldown()) {
			return true;
		}
		projectile.remove();
		mPlugin.mProjectileEffectTimers.removeEntity(projectile);
		putOnCooldown((int) (getModifiedCooldown() * BowAspect.getCooldownReduction(mPlayer)));

		World world = mPlayer.getWorld();
		Location startLoc = mPlayer.getEyeLocation();
		Vector dir = startLoc.getDirection().normalize();

		world.playSound(startLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1f, 2f);
		world.playSound(startLoc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1f, 2f);
		world.playSound(startLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1f, 1.2f);
		world.playSound(startLoc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 0.6f);
		world.playSound(startLoc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(startLoc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1f, 1.5f);
		new PartialParticle(Particle.CRIT, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);

		Location endLoc = LocationUtils.rayTraceToBlock(mPlayer, MAX_DISTANCE, (hitBlockLoc) -> {
			new PartialParticle(Particle.SPELL_INSTANT, hitBlockLoc, 30, 0.1, 0.1, 0.1, 0.2).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CLOUD, hitBlockLoc, 80, 0.1, 0.1, 0.1, 0.2).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.FIREWORKS_SPARK, hitBlockLoc, 50, 0.1, 0.1, 0.1, 0.3).spawnAsPlayerActive(mPlayer);
			world.playSound(hitBlockLoc, Sound.ITEM_TRIDENT_HIT_GROUND, SoundCategory.PLAYERS, 2f, 0.75f);
		});

		Location pLoc = mPlayer.getEyeLocation();
		pLoc.setPitch(pLoc.getPitch() + 90);
		Vector pVec = new Vector(pLoc.getDirection().getX(), pLoc.getDirection().getY(), pLoc.getDirection().getZ());
		pVec = pVec.normalize();

		Location currLoc = startLoc.clone();
		for (int i = 0; i < MAX_DISTANCE; i++) {
			currLoc.add(dir);

			new PartialParticle(Particle.REDSTONE, currLoc, 15, 0.23, 0.23, 0.23, 0,
				new Particle.DustOptions(Color.fromRGB(255, 200, 0), 1.2f)).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SPELL_INSTANT, currLoc, 3, 0.25, 0.25, 0.25, 0.12).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CRIT, currLoc, 3, 0.2, 0.2, 0.2, 0.4).spawnAsPlayerActive(mPlayer);
			if (i % 6 == 0) {
				new PPCircle(Particle.END_ROD, currLoc, 0.5).count(15).extra(0.15)
					.delta(pVec.getX(), pVec.getY(), pVec.getZ()).directionalMode(true).rotateDelta(true)
					.axes(pVec, pVec.clone().crossProduct(startLoc.getDirection())).ringMode(true).spawnAsPlayerActive(mPlayer);
			}

			Block block = currLoc.getBlock();
			if (block.getType().isSolid()) {
				break;
			}
		}

		List<LivingEntity> hitMobs = Hitbox.approximateCylinder(startLoc, endLoc, STUN_HITBOX_SIZE, true).accuracy(0.5).getHitMobs();
		hitMobs.removeIf(e -> e.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
		List<Player> hitPlayers = Hitbox.approximateCylinder(startLoc, endLoc, HEAL_HITBOX_SIZE, true).accuracy(0.5).getHitPlayers(mPlayer, true);

		// count things hit and apply effects
		int targetsHit = Math.min(hitMobs.size() + hitPlayers.size(), mMaxTargetBonus);
		int stun = mStunDuration + targetsHit * STUN_INCREASE_PER_TARGET;
		double heal = mHeal * (1 + targetsHit * HEAL_INCREASE_PER_TARGET);

		for (LivingEntity mob : hitMobs) {
			if (EntityUtils.isElite(mob) || EntityUtils.isBoss(mob)) {
				EntityUtils.applySlow(mPlugin, stun, 1, mob);
			} else {
				EntityUtils.applyStun(mPlugin, stun, mob);
			}
		}
		for (Player player : hitPlayers) {
			double healthToHeal = EntityUtils.getMaxHealth(player) * heal;
			double healed = PlayerUtils.healPlayer(mPlugin, player, healthToHeal, mPlayer);
			double remainingHealing = healthToHeal - healed;
			if (remainingHealing > 0) {
				AbsorptionUtils.addAbsorption(player, remainingHealing, mMaxAbsorption, mAbsorptionDuration);
			}

			new PartialParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0).spawnAsPlayerActive(mPlayer);
		}

		// Do not change with charms
		if (targetsHit >= MAX_TARGET_BONUS) {
			mPlugin.mTimers.updateCooldown(mPlayer, ClassAbility.DIVINE_BEAM, mCDR);
		}

		return true;
	}

	private static Description<DivineBeam> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<DivineBeam>(color)
			.add("Shooting a projectile while sneaking instead shoots a beam of light, healing players hit for ")
			.addPercent(a -> a.mHeal, HEAL[rarity - 1], false, true)
			.add(" of their max health and stunning non-Elite mobs hit for ")
			.addDuration(a -> a.mStunDuration, STUN_DURATION[rarity - 1], false, true)
			.add(" seconds. Elites are rooted instead. Healing is increased by " + StringUtils.multiplierToPercentage(HEAL_INCREASE_PER_TARGET) + "% and stun duration is increased by " + StringUtils.ticksToSeconds(STUN_INCREASE_PER_TARGET) + "s per player or mob hit, up to ")
			.add(a -> a.mMaxTargetBonus, MAX_TARGET_BONUS)
			.add(" targets. Excess healing is converted to absorption, up to ")
			.add(a -> a.mMaxAbsorption, MAX_ABSORPTION)
			.add(", that lasts for ")
			.addDuration(a -> a.mAbsorptionDuration, ABSORPTION_DURATION)
			.add(" seconds. If at least " + MAX_TARGET_BONUS + " targets were hit, also reduce the cooldown of this ability by ")
			.addDuration(a -> a.mCDR, COOLDOWN_REDUCTION)
			.add("s.")
			.addCooldown(COOLDOWN);
	}


}

