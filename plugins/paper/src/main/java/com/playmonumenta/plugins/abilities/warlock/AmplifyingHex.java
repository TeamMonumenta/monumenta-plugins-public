package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.AmplifyingHexCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Inferno;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class AmplifyingHex extends Ability {

	private static final float FLAT_DAMAGE = 2f;
	private static final float DAMAGE_PER_SKILL_POINT = 0.5f;
	private static final int AMPLIFIER_DAMAGE_1 = 1;
	private static final int AMPLIFIER_DAMAGE_2 = 2;
	private static final int AMPLIFIER_CAP_1 = 2;
	private static final int AMPLIFIER_CAP_2 = 3;
	private static final float R1_CAP = 3.5f;
	private static final float R2_CAP = 5f;
	private static final int RADIUS_1 = 8;
	private static final int RADIUS_2 = 10;
	private static final double DOT_ANGLE = 0.33;
	private static final int COOLDOWN = 20 * 10;
	private static final float KNOCKBACK_SPEED = 0.12f;

	public static final String CHARM_DAMAGE = "Amplifying Hex Damage";
	public static final String CHARM_RANGE = "Amplifying Hex Range";
	public static final String CHARM_COOLDOWN = "Amplifying Hex Cooldown";
	public static final String CHARM_CONE = "Amplifying Hex Cone";
	public static final String CHARM_EFFECT = "Amplifying Hex Damage per Effect Potency";

	private final float mAmplifierDamage;
	private final float mAmplifierCap;
	private final float mRadius;
	private float mRegionCap;
	private float mDamage = 0f;

	private final AmplifyingHexCS mCosmetic;

	public AmplifyingHex(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Amplifying Hex");
		mInfo.mScoreboardId = "AmplifyingHex";
		mInfo.mShorthandName = "AH";
		mInfo.mDescriptions.add("Left-click while sneaking with a scythe to fire a magic cone up to 8 blocks in front of you, dealing 2 + (0.5 * number of Skill Points, capped at the maximum available Skill Points for each Region) magic damage to each enemy per debuff (potion effects like Weakness or Wither, as well as Fire and custom effects like Bleed) they have, and an extra +1 damage per extra level of debuff, capped at 2 extra levels. 10% Slowness, Weaken, etc. count as one level. Cooldown: 10s.");
		mInfo.mDescriptions.add("The range is increased to 10 blocks, extra damage increased to +2 per extra level, and the extra level cap is increased to 3 extra levels.");
		mInfo.mDescriptions.add("For every 1% health you have above 80% of your max health, Amplifying Hex will deal 1% more damage to enemies and deal 1% max health damage to yourself.");
		mInfo.mLinkedSpell = ClassAbility.AMPLIFYING;
		mInfo.mCooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, COOLDOWN);
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDisplayItem = new ItemStack(Material.DRAGON_BREATH, 1);
		mAmplifierDamage = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_EFFECT, isLevelOne() ? AMPLIFIER_DAMAGE_1 : AMPLIFIER_DAMAGE_2);
		mAmplifierCap = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_EFFECT, isLevelOne() ? AMPLIFIER_CAP_1 : AMPLIFIER_CAP_2);
		mRadius = (float) CharmManager.getRadius(player, CHARM_RANGE, isLevelOne() ? RADIUS_1 : RADIUS_2);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new AmplifyingHexCS(), AmplifyingHexCS.SKIN_LIST);

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				int skillPoints = Stream.of(AmplifyingHex.class, CholericFlames.class, GraspingClaws.class, SoulRend.class,
						SanguineHarvest.class, MelancholicLament.class, CursedWound.class, PhlegmaticResolve.class)
					.map(c -> AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, c))
					.mapToInt(a -> a == null ? 0 : Math.min(a.getAbilityScore(), 2))
					.sum();
				mDamage = DAMAGE_PER_SKILL_POINT * skillPoints;
			});
		}
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		World world = mPlayer.getWorld();
		mRegionCap = ServerProperties.getClassSpecializationsEnabled() ? R2_CAP : R1_CAP;

		new BukkitRunnable() {
			final Location mLoc = mPlayer.getLocation();
			double mRadiusIncrement = 0.5;

			@Override
			public void run() {
				if (mRadiusIncrement == 0.5) {
					mLoc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec;
				mRadiusIncrement += 1.25;
				for (double degree = 30; degree <= 150; degree += 10) {
					double radian1 = Math.toRadians(degree + mCosmetic.amplifyingAngle(degree, mRadiusIncrement));
					vec = new Vector(FastUtils.cos(radian1) * mRadiusIncrement,
						0.15 + mCosmetic.amplifyingHeight(mRadiusIncrement, mRadius + 1),
						FastUtils.sin(radian1) * mRadiusIncrement);
					vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().clone().add(0, 0.15, 0).add(vec);
					mCosmetic.amplifyingParticle(mPlayer, l);
				}

				if (mRadiusIncrement >= mRadius + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);


		final Location soundLoc = mPlayer.getLocation();
		mCosmetic.amplifyingEffects(mPlayer, world, soundLoc);

		Vector playerDir = mPlayer.getEyeLocation().getDirection().setY(0).normalize();

		double maxHealth = EntityUtils.getMaxHealth(mPlayer);
		double percentBoost = 0;
		if (isEnhanced() && mPlayer.getHealth() > maxHealth * 0.8) {
			percentBoost = mPlayer.getHealth() / maxHealth - 0.8;
			double selfHarm = maxHealth * percentBoost;
			double absorp = mPlayer.getAbsorptionAmount();
			double newAbsorp = absorp - selfHarm;
			if (absorp > 0) {
				AbsorptionUtils.setAbsorption(mPlayer, (float) Math.max(newAbsorp, 0), -1);
			}
			if (newAbsorp < 0) {
				mPlayer.setHealth(maxHealth + newAbsorp);
			}
			//dummy damage
			DamageUtils.damage(null, mPlayer, new DamageEvent.Metadata(DamageType.OTHER, null, null, null), 0.001, true, false, false);
		}

		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), mRadius, mPlayer)) {
			Vector toMobVector = mob.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();
			if (playerDir.dot(toMobVector) > CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CONE, DOT_ANGLE)) {
				int debuffCount = 0;
				int amplifierCount = 0;
				for (PotionEffectType effectType : AbilityUtils.DEBUFFS) {
					PotionEffect effect = mob.getPotionEffect(effectType);
					if (effect != null) {
						debuffCount++;
						amplifierCount += Math.min(mAmplifierCap, effect.getAmplifier());
					}
				}

				if (mob.getFireTicks() > 0) {
					debuffCount++;
					amplifierCount += Math.min(mAmplifierCap, Inferno.getInfernoLevel(mPlugin, mob));
					// mPlayer.sendMessage("On Fire");
				}

				if (EntityUtils.isStunned(mob)) {
					debuffCount++;
					// mPlayer.sendMessage("On Stun");
				}

				if (EntityUtils.isParalyzed(mPlugin, mob)) {
					debuffCount++;
					// mPlayer.sendMessage("On Paralyzed");
				}

				if (EntityUtils.isSilenced(mob)) {
					debuffCount++;
					// mPlayer.sendMessage("On Silenced");
				}

				if (EntityUtils.isBleeding(mPlugin, mob)) {
					debuffCount++;
					amplifierCount += Math.min(mAmplifierCap, EntityUtils.getBleedLevel(mPlugin, mob) - 1);
				}

				//Custom slow effect interaction
				if (EntityUtils.isSlowed(mPlugin, mob) && mob.getPotionEffect(PotionEffectType.SLOW) == null) {
					debuffCount++;
					double slowAmp = EntityUtils.getSlowAmount(mPlugin, mob);
					int slowLevel = (int) Math.floor(slowAmp * 10);
					amplifierCount += Math.min((int) mAmplifierCap, Math.max(slowLevel - 1, 0));
				}

				//Custom weaken interaction
				if (EntityUtils.isWeakened(mPlugin, mob)) {
					debuffCount++;
					double weakAmp = EntityUtils.getWeakenAmount(mPlugin, mob);
					int weakLevel = (int) Math.floor(weakAmp * 10);
					amplifierCount += Math.min((int) mAmplifierCap, Math.max(weakLevel - 1, 0));
				}

				//Custom vuln interaction
				if (EntityUtils.isVulnerable(mPlugin, mob)) {
					debuffCount++;
					double vulnAmp = EntityUtils.getVulnAmount(mPlugin, mob);
					amplifierCount += Math.min(mAmplifierCap, Math.max((int) Math.floor(vulnAmp * 10) - 1, 0));
				}

				//Custom DoT interaction
				if (EntityUtils.hasDamageOverTime(mPlugin, mob)) {
					debuffCount++;
					int dotLevel = (int) EntityUtils.getHighestDamageOverTime(mPlugin, mob);
					amplifierCount += (int) Math.min(mAmplifierCap, dotLevel - 1);
				}

				if (debuffCount > 0) {
					double finalDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, debuffCount * (FLAT_DAMAGE + Math.min(mDamage, mRegionCap)) + amplifierCount * mAmplifierDamage);
					finalDamage *= (1 + percentBoost);
					DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, finalDamage, mInfo.mLinkedSpell, true);
					MovementUtils.knockAway(mPlayer, mob, KNOCKBACK_SPEED, true);
				}
			}
		}
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		if (mPlayer == null) {
			return false;
		}
		double pitch = mPlayer.getLocation().getPitch();
		return (mPlayer.isSneaking() && pitch < 50 && pitch > -50
			&& ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand()));
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			cast(Action.LEFT_CLICK_AIR);
		}
		return false;
	}
}
