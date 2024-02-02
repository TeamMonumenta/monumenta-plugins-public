package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DestructiveExpertise;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SupportExpertise;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.*;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;


public class EarthenTremor extends Ability {

	private static final int COOLDOWN = 10 * 20;
	private static final int SILENCE_DURATION = 30;
	private static final int RANGE = 7;
	private static final int DAMAGE_1 = 10;
	private static final int DAMAGE_2 = 13;
	private static final double KNOCKBACK = 0.8;
	private static final int SHOCKWAVES = 6;
	private static final int SHOCKWAVE_RADIUS = 3;
	private static final int SHOCKWAVE_DISTANCE = 5;
	private static final double DAMAGE_BONUS_ENHANCE = 2.5;

	public static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(255, 255, 0), 1.25f);

	public static final String CHARM_COOLDOWN = "Earthen Tremor Cooldown";
	public static final String CHARM_DAMAGE = "Earthen Tremor Damage";
	public static final String CHARM_RADIUS = "Earthen Tremor Radius";
	public static final String CHARM_SILENCE_DURATION = "Earthen Tremor Silence Duration";
	public static final String CHARM_KNOCKBACK = "Earthen Tremor Knockback";
	public static final String CHARM_SHOCKWAVES = "Earthen Tremor Shockwaves";
	public static final String CHARM_SHOCKWAVE_RADIUS = "Earthen Tremor Shockwave Radius";
	public static final String CHARM_SHOCKWAVE_DISTANCE = "Earthen Tremor Shockwave Distance";

	private double mDamage;
	private final double mRadius;
	private final int mSilenceDuration;
	private final float mKnockback;
	private final int mShockwaves;
	private final double mShockwaveDistance;
	private final double mShockwaveRadius;
	private final List<LivingEntity> mHitEntities = new java.util.ArrayList<>();

	public static final AbilityInfo<EarthenTremor> INFO =
		new AbilityInfo<>(EarthenTremor.class, "Earthen Tremor", EarthenTremor::new)
			.linkedSpell(ClassAbility.EARTHEN_TREMOR)
			.scoreboardId("EarthenTremor")
			.shorthandName("ET")
			.descriptions(
				String.format("Press swap with a weapon while sneaking to summon an earthen tremor on your position. Deals %s magic damage to mobs within %s blocks and pushes them away. Cooldown: %ss.",
					DAMAGE_1,
					RANGE,
					StringUtils.ticksToSeconds(COOLDOWN)
				),
				String.format("Damage increased to %s and silences targets for %ss.",
					DAMAGE_2,
					StringUtils.ticksToSeconds(SILENCE_DURATION)),
				String.format("Tremor now sends out an additional %s shockwaves from the edge of the radius outwards " +
					"for an additional %s blocks with a radius of %s blocks. Damage is increased by %s",
					SHOCKWAVES,
					SHOCKWAVE_DISTANCE,
					SHOCKWAVE_RADIUS,
					DAMAGE_BONUS_ENHANCE)
			)
			.simpleDescription("Summons a earthen tremor on your location, dealing damage and knocking mobs away.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", EarthenTremor::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.DIRT);

	public EarthenTremor(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE,
			isEnhanced() ? (isLevelOne() ? DAMAGE_1 : DAMAGE_2) + DAMAGE_BONUS_ENHANCE
				: (isLevelOne() ? DAMAGE_1 : DAMAGE_2));
		mDamage *= DestructiveExpertise.damageBuff(mPlayer);
		mDamage *= SupportExpertise.damageBuff(mPlayer);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RANGE);
		mSilenceDuration = CharmManager.getDuration(mPlayer, CHARM_SILENCE_DURATION, SILENCE_DURATION);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK);
		mShockwaves = (int) (SHOCKWAVES + CharmManager.getLevel(mPlayer, CHARM_SHOCKWAVES));
		mShockwaveDistance = CharmManager.getRadius(mPlayer, CHARM_SHOCKWAVE_DISTANCE, SHOCKWAVE_DISTANCE);
		mShockwaveRadius = CharmManager.getRadius(mPlayer, CHARM_SHOCKWAVE_RADIUS, SHOCKWAVE_RADIUS);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		for (LivingEntity mob : EntityUtils.getNearbyMobsInSphere(mPlayer.getLocation(), mRadius, null)) {
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true, false);
			if (!EntityUtils.isCCImmuneMob(mob)) {
				MovementUtils.knockAway(mPlayer, mob, mKnockback);
				if (isLevelTwo()) {
					EntityUtils.applySilence(mPlugin, mSilenceDuration, mob);
				}
				mHitEntities.add(mob);
			}
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation().add(0, 0.1, 0);
		DisplayEntityUtils.groundBlockQuake(loc, mRadius, List.of(Material.PODZOL, Material.DIRT, Material.MUD), new Display.Brightness(12, 12));

		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST,
			SoundCategory.PLAYERS, 2, 0.6f);
		world.playSound(loc, Sound.ITEM_AXE_WAX_OFF,
			SoundCategory.PLAYERS, 0.4f, 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3,
			SoundCategory.PLAYERS, 0.25f, 0.5f);
		world.playSound(loc, Sound.ITEM_TOTEM_USE,
			SoundCategory.PLAYERS, 0.4f, 2.0f);

		if (isEnhanced()) {
			int angleBetween = 360 / mShockwaves;
			Vector forward = mPlayer.getLocation().getDirection().setY(0).normalize();
			for (int i = 0; i < 360; i += angleBetween) {
				Vector normDir = VectorUtils.rotateYAxis(forward, i).normalize();
				Location shockwaveLoc = mPlayer.getLocation().add(normDir.clone().multiply(mRadius));
				for (int j = 0; j < mShockwaveDistance; j++) {
					DisplayEntityUtils.groundBlockQuake(shockwaveLoc.clone().add(0, 0.2, 0), mShockwaveRadius,
						List.of(Material.DIORITE, Material.GRANITE, Material.IRON_ORE),
						new Display.Brightness(8, 8));
					shockwaveLoc.add(normDir);
					List<LivingEntity> mShockwaveHits = EntityUtils.getNearbyMobsInSphere(shockwaveLoc, mShockwaveRadius, null);
					mShockwaveHits.removeAll(mHitEntities);
					for (LivingEntity mob : mShockwaveHits) {
						DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true, false);
						if (!EntityUtils.isCCImmuneMob(mob)) {
							MovementUtils.knockAway(mPlayer, mob, mKnockback);
							if (isLevelTwo()) {
								EntityUtils.applySilence(mPlugin, mSilenceDuration, mob);
							}
							mHitEntities.add(mob);
						}
					}
				}
			}
		}
		mHitEntities.clear();
		return true;
	}
}
