package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.tenebrist.HauntingShadesCS;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class HauntingShades extends Ability implements AbilityWithDuration {

	private static final String HEAL_NAME = "HauntingShadesHealing";
	private static final String STR_NAME = "HauntingShadesStrength";

	private static final int COOLDOWN = 10 * 20;
	private static final int SHADES_DURATION = 7 * 20;
	private static final double VULN = 0.1;
	private static final double HEAL_PERCENT = 0.025;
	private static final double EFFECT_LEVEL = 0.1;
	private static final int EFFECT_DURATION = 20;
	private static final int RANGE = 10;
	private static final int AOE_RANGE = 6;
	private static final double HITBOX_LENGTH = 0.55;

	public static final String CHARM_HEALING = "Haunting Shades Healing";
	public static final String CHARM_COOLDOWN = "Haunting Shades Cooldown";
	public static final String CHARM_RADIUS = "Haunting Shades Radius";
	public static final String CHARM_DURATION = "Haunting Shades Duration";
	public static final String CHARM_VULN = "Haunting Shades Vulnerability Modifier";
	public static final String CHARM_DAMAGE = "Haunting Shades Damage Modifier";

	public static final AbilityInfo<HauntingShades> INFO =
		new AbilityInfo<>(HauntingShades.class, "Haunting Shades", HauntingShades::new)
			.linkedSpell(ClassAbility.HAUNTING_SHADES)
			.scoreboardId("HauntingShades")
			.shorthandName("HS")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Place a Shade that debuffs nearby enemies with Vulnerability.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", HauntingShades::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(Material.SKELETON_SKULL);


	private final int mMaxDuration;
	private final double mRadius;
	private final double mHealing;
	private final double mStrength;
	private final double mVuln;
	private int mCurrDuration = -1;
	private final HauntingShadesCS mCosmetic;

	public HauntingShades(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, SHADES_DURATION);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, AOE_RANGE);
		mHealing = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, HEAL_PERCENT);
		mStrength = EFFECT_LEVEL + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mVuln = VULN + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_VULN);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new HauntingShadesCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		Vector direction = loc.getDirection();
		Vector shift = direction.normalize().multiply(HITBOX_LENGTH);
		BoundingBox box = BoundingBox.of(loc, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
		box.shift(direction);

		World world = mPlayer.getWorld();
		mCosmetic.shadesStartSound(world, mPlayer, mPlayer.getLocation());

		Set<LivingEntity> nearbyMobs = new HashSet<>(EntityUtils.getNearbyMobs(loc, RANGE));

		for (double r = 0; r < RANGE; r += HITBOX_LENGTH) {
			Location bLoc = box.getCenter().toLocation(world);

			mCosmetic.shadesTrailParticle(mPlayer, bLoc, direction, r);

			for (LivingEntity mob : nearbyMobs) {
				if (mob.getBoundingBox().overlaps(box)) {
					if (EntityUtils.isHostileMob(mob)) {
						placeShade(bLoc);
						return true;
					}
				}
			}

			if (!bLoc.isChunkLoaded() || bLoc.getBlock().getType().isSolid()) {
				//If the player is incapable of going through the block and doesn't have line of sight with the next possible position
				if (!bLoc.getBlock().isPassable() && !mPlayer.hasLineOfSight(box.shift(shift).getCenter().toLocation(world))) {
					bLoc.subtract(direction.multiply(0.5));
					placeShade(bLoc);
					return true;
				}
			}

			box.shift(shift);
		}
		placeShade(box.getCenter().toLocation(world));
		return true;
	}

	private void placeShade(Location bLoc) {
		World world = mPlayer.getWorld();
		ArmorStand stand = (ArmorStand) LibraryOfSoulsIntegration.summon(bLoc, mCosmetic.getAsName());
		Vector v = mPlayer.getLocation().toVector().subtract(bLoc.toVector());
		float f = (float) -Math.toDegrees(Math.atan2(v.getX(), v.getZ()));
		if (stand == null) {
			return;
		}
		stand.setDisabledSlots(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
		stand.setGravity(false);
		stand.setCanMove(false);
		stand.setSilent(true);
		stand.setBasePlate(false);
		stand.setMarker(true);
		stand.setVisible(false);
		stand.setCustomNameVisible(false);
		stand.setSmall(true);
		stand.setRotation(f, 0);
		mCurrDuration = 0;

		ClientModHandler.updateAbility(mPlayer, this);
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;
				mCurrDuration++;
				if (mT % 5 == 0) {
					if (isLevelTwo()) {
						List<Player> affectedPlayers = PlayerUtils.playersInRange(bLoc, mRadius, true);

						for (Player p : affectedPlayers) {
							double maxHealth = EntityUtils.getMaxHealth(p);
							mPlugin.mEffectManager.addEffect(p, HEAL_NAME, new CustomRegeneration(EFFECT_DURATION, maxHealth * mHealing, mPlayer, mPlugin).displaysTime(false).deleteOnAbilityUpdate(true));
							mPlugin.mEffectManager.addEffect(p, STR_NAME, new PercentDamageDealt(EFFECT_DURATION, mStrength).displaysTime(false).deleteOnAbilityUpdate(true));
						}
					}

					List<LivingEntity> affectedMobs = EntityUtils.getNearbyMobs(bLoc, mRadius);
					for (LivingEntity m : affectedMobs) {
						EntityUtils.applyVulnerability(mPlugin, EFFECT_DURATION, mVuln, m);
					}
				}

				mCosmetic.shadesTickEffect(mPlugin, world, mPlayer, bLoc, mRadius, mT);

				if (mT >= mMaxDuration || mPlayer.isDead() || !mPlayer.isOnline()) {
					stand.remove();
					mCosmetic.shadesEndEffect(world, mPlayer, bLoc, mRadius);
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				mCurrDuration = -1;
				ClientModHandler.updateAbility(mPlayer, HauntingShades.this);
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int getInitialAbilityDuration() {
		return mMaxDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}

	private static Description<HauntingShades> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to conjure a Shade which lasts ")
			.addDuration(a -> a.mMaxDuration, SHADES_DURATION)
			.add(" seconds at the target block or mob location. Mobs within ")
			.add(a -> a.mRadius, AOE_RANGE)
			.add(" blocks of a Shade are afflicted with ")
			.addPercent(a -> a.mVuln, VULN)
			.add(" vulnerability.")
			.addCooldown(COOLDOWN);
	}

	private static Description<HauntingShades> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Players within ")
			.add(a -> a.mRadius, AOE_RANGE)
			.add(" blocks of the Shade are given ")
			.addPercent(a -> a.mStrength, EFFECT_LEVEL)
			.add(" strength and heal ")
			.addPercent(a -> a.mHealing, HEAL_PERCENT)
			.add(" of their max health each second.");
	}
}
