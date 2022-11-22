package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.tenebrist.HauntingShadesCS;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class HauntingShades extends Ability {

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
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", HauntingShades::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(new ItemStack(Material.SKELETON_SKULL, 1));

	private final HauntingShadesCS mCosmetic;

	public HauntingShades(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new HauntingShadesCS(), HauntingShadesCS.SKIN_LIST);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		Vector direction = loc.getDirection();
		Vector shift = direction.normalize().multiply(HITBOX_LENGTH);
		BoundingBox box = BoundingBox.of(loc, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
		box.shift(direction);

		World world = mPlayer.getWorld();
		mCosmetic.shadesStartSound(world, mPlayer);

		Set<LivingEntity> nearbyMobs = new HashSet<>(EntityUtils.getNearbyMobs(loc, RANGE));

		for (double r = 0; r < RANGE; r += HITBOX_LENGTH) {
			Location bLoc = box.getCenter().toLocation(world);

			mCosmetic.shadesTrailParticle(mPlayer, bLoc, direction, r);

			for (LivingEntity mob : nearbyMobs) {
				if (mob.getBoundingBox().overlaps(box)) {
					if (EntityUtils.isHostileMob(mob)) {
						placeShade(bLoc);
						return;
					}
				}
			}

			if (!bLoc.isChunkLoaded() || bLoc.getBlock().getType().isSolid()) {
				bLoc.subtract(direction.multiply(0.5));
				placeShade(bLoc);
				return;
			}

			box.shift(shift);
		}
		placeShade(box.getCenter().toLocation(world));
	}

	private void placeShade(Location bLoc) {
		World world = mPlayer.getWorld();
		bLoc.setDirection(mPlayer.getLocation().toVector().subtract(bLoc.toVector()).normalize());
		ArmorStand stand = (ArmorStand) LibraryOfSoulsIntegration.summon(bLoc, mCosmetic.getAsName());
		if (stand == null) {
			return;
		}
		stand.setDisabledSlots(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
		stand.setGravity(false);
		stand.setCanMove(false);
		stand.setSilent(true);
		stand.setBasePlate(false);
		stand.setMarker(true);
		stand.setVisible(true);
		stand.setCustomNameVisible(false);

		new BukkitRunnable() {
			final double mAoeRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, AOE_RANGE);
			int mT = 0;
			@Override
			public void run() {
				mT++;
				if (mT % 5 == 0) {
					List<Player> affectedPlayers = PlayerUtils.playersInRange(bLoc, mAoeRadius, true);
					Set<LivingEntity> affectedMobs = new HashSet<LivingEntity>(EntityUtils.getNearbyMobs(bLoc, mAoeRadius));
					if (isLevelTwo()) {
						for (Player p : affectedPlayers) {
							double maxHealth = EntityUtils.getMaxHealth(p);
							mPlugin.mEffectManager.addEffect(p, HEAL_NAME, new CustomRegeneration(EFFECT_DURATION, maxHealth * CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, HEAL_PERCENT), mPlayer, mPlugin));
							mPlugin.mEffectManager.addEffect(p, STR_NAME, new PercentDamageDealt(EFFECT_DURATION, EFFECT_LEVEL));
						}
					}

				    for (LivingEntity m : affectedMobs) {
						EntityUtils.applyVulnerability(mPlugin, EFFECT_DURATION, VULN, m);
				    }
				}

				mCosmetic.shadesTickEffect(mPlugin, world, mPlayer, bLoc, mAoeRadius, mT);

				if (mT >= SHADES_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION) || mPlayer.isDead() || !mPlayer.isValid()) {
					stand.remove();
					mCosmetic.shadesEndEffect(world, mPlayer, bLoc, mAoeRadius);
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}
}
