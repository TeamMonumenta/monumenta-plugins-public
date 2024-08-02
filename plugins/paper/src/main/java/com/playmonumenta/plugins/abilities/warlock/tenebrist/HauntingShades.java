package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
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
	private static final int EFFECT_DURATION = 20 * 1;
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
					.descriptions(
							"Press the swap key while not sneaking with a scythe to conjure a Shade at the target block or mob location. " +
									"Mobs within 6 blocks of a Shade are afflicted with 10% Vulnerability. A Shade fades back into darkness after 7 seconds. Cooldown: 10s.",
							"Players within 6 blocks of the shade are given 10% damage dealt and gain a custom healing effect that regenerates 2.5% of max health every second for 1 second. " +
									"Effects do not stack with other Tenebrists.")
					.simpleDescription("Place a Shade that debuffs nearby enemies with Vulnerability.")
					.cooldown(COOLDOWN, CHARM_COOLDOWN)
					.addTrigger(new AbilityTriggerInfo<>("cast", "cast", HauntingShades::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false),
							AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
					.displayItem(Material.SKELETON_SKULL);

	private final HauntingShadesCS mCosmetic;

	private final int mMaxDuration;
	private int mCurrDuration = -1;

	public HauntingShades(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new HauntingShadesCS());
		mMaxDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, SHADES_DURATION);
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
		Vector v = mPlayer.getLocation().toVector().subtract(bLoc.toVector()).normalize();
		float f = (float) (-180 * (Math.atan2(v.getX(), v.getZ()) / Math.PI));
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
			final double mAoeRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, AOE_RANGE);
			int mT = 0;

			@Override
			public void run() {
				mT++;
				mCurrDuration++;
				if (mT % 5 == 0) {
					if (isLevelTwo()) {
						List<Player> affectedPlayers = PlayerUtils.playersInRange(bLoc, mAoeRadius, true);

						double healPercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, HEAL_PERCENT);
						double strength = EFFECT_LEVEL + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
						for (Player p : affectedPlayers) {
							double maxHealth = EntityUtils.getMaxHealth(p);
							mPlugin.mEffectManager.addEffect(p, HEAL_NAME, new CustomRegeneration(EFFECT_DURATION, maxHealth * healPercent, mPlayer, mPlugin));
							mPlugin.mEffectManager.addEffect(p, STR_NAME, new PercentDamageDealt(EFFECT_DURATION, strength));
						}
					}

					List<LivingEntity> affectedMobs = EntityUtils.getNearbyMobs(bLoc, mAoeRadius);
					double vuln = VULN + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_VULN);
					for (LivingEntity m : affectedMobs) {
						EntityUtils.applyVulnerability(mPlugin, EFFECT_DURATION, vuln, m);
					}
				}

				mCosmetic.shadesTickEffect(mPlugin, world, mPlayer, bLoc, mAoeRadius, mT);

				if (mT >= mMaxDuration || mPlayer.isDead() || !mPlayer.isOnline()) {
					stand.remove();
					mCosmetic.shadesEndEffect(world, mPlayer, bLoc, mAoeRadius);
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
}
