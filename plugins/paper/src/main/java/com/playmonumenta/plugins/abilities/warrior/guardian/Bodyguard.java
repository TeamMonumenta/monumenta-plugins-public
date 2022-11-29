package com.playmonumenta.plugins.abilities.warrior.guardian;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class Bodyguard extends Ability {
	private static final int COOLDOWN = 30 * 20;
	private static final int RANGE = 25;
	private static final int RADIUS = 4;
	private static final int ABSORPTION_HEALTH_1 = 8;
	private static final int ABSORPTION_HEALTH_2 = 12;
	private static final int BUFF_DURATION = 20 * 10;
	private static final int STUN_DURATION = 20 * 3;
	private static final float KNOCKBACK = 0.45f;

	public static final String CHARM_COOLDOWN = "Bodyguard Cooldown";
	public static final String CHARM_RANGE = "Bodyguard Range";
	public static final String CHARM_RADIUS = "Bodyguard Stun Radius";
	public static final String CHARM_ABSORPTION = "Bodyguard Absorption Health";
	public static final String CHARM_ABSORPTION_DURATION = "Bodyguard Absorption Duration";
	public static final String CHARM_STUN_DURATION = "Bodyguard Stun Duration";
	public static final String CHARM_KNOCKBACK = "Bodyguard Knockback";

	public static final AbilityInfo<Bodyguard> INFO =
		new AbilityInfo<>(Bodyguard.class, "Bodyguard", Bodyguard::new)
			.linkedSpell(ClassAbility.BODYGUARD)
			.scoreboardId("Bodyguard")
			.shorthandName("Bg")
			.descriptions(
				"Left-click the air twice while looking directly at another player within 25 blocks to charge to them (cannot be used in safezones). " +
					"Upon arriving, knock away all mobs within 4 blocks. Both you and the other player gain 4 Absorption hearts for 10 seconds. " +
					"Left-click twice while looking down to cast on yourself. Cooldown: 30s.",
				"Absorption increased to 6 hearts. Additionally, affected mobs are stunned for 3 seconds.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("castSelf", "cast on self or others", bg -> bg.cast(true),
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).doubleClick().lookDirections(AbilityTrigger.LookDirection.DOWN)
					.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.addTrigger(new AbilityTriggerInfo<>("castOthers", "cast on others only", bg -> bg.cast(false),
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).doubleClick()
					.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(new ItemStack(Material.IRON_CHESTPLATE, 1));

	private final double mAbsorptionHealth;

	public Bodyguard(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mAbsorptionHealth = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, isLevelOne() ? ABSORPTION_HEALTH_1 : ABSORPTION_HEALTH_2);
	}

	public void cast(boolean allowSelfCast) {
		if (isOnCooldown()) {
			return;
		}

		World world = mPlayer.getWorld();
		Location oLoc = mPlayer.getLocation();

		BoundingBox box = BoundingBox.of(mPlayer.getEyeLocation(), 1, 1, 1);
		Vector dir = oLoc.getDirection();
		double range = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);
		List<Player> players = PlayerUtils.otherPlayersInRange(mPlayer, range, true);
		boolean foundPlayer = false;
		for (int i = 0; i < range; i++) {
			box.shift(dir);
			Location bLoc = box.getCenter().toLocation(world);
			if (!bLoc.isChunkLoaded() || bLoc.getBlock().getType().isSolid()) {
				break;
			}

			for (Player player : players) {
				// If looking at another player
				if (player.getBoundingBox().overlaps(box)) {
					new PPLine(Particle.FLAME, mPlayer.getEyeLocation(), bLoc)
						.countPerMeter(12)
						.delta(0.25, 0.25, 0.25)
						.spawnAsPlayerActive(mPlayer);

					// Flame
					new PPExplosion(Particle.FLAME, player.getLocation().add(0, 0.15, 0))
						.flat(true)
						.speed(1)
						.count(120)
						.extraRange(0.1, 0.4)
						.spawnAsPlayerActive(mPlayer);

					// Explosion_Normal
					new PPExplosion(Particle.EXPLOSION_NORMAL, player.getLocation().add(0, 0.15, 0))
						.flat(true)
						.speed(1)
						.count(60)
						.extraRange(0.15, 0.5)
						.spawnAsPlayerActive(mPlayer);

					Location userLoc = mPlayer.getLocation();
					Location targetLoc = player.getLocation().setDirection(mPlayer.getEyeLocation().getDirection()).subtract(dir.clone().multiply(0.5)).add(0, 0.5, 0);

					world.playSound(targetLoc, Sound.ENTITY_BLAZE_SHOOT, 0.75f, 0.75f);
					world.playSound(targetLoc, Sound.ENTITY_ENDER_DRAGON_HURT, 0.75f, 0.9f);

					giveAbsorption(player);

					if (userLoc.distance(player.getLocation()) > 1
						    && !ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)
						    && !ZoneUtils.hasZoneProperty(targetLoc, ZoneProperty.NO_MOBILITY_ABILITIES)) {
						mPlayer.teleport(targetLoc);
					}

					foundPlayer = true;
					break;
				}
			}
		}
		if (!foundPlayer && !allowSelfCast) {
			return;
		}

		putOnCooldown();

		world.playSound(oLoc, Sound.ENTITY_BLAZE_SHOOT, 1, 0.75f);
		new PartialParticle(Particle.FLAME, oLoc.add(0, 0.15, 0), 25, 0.2, 0, 0.2, 0.1).spawnAsPlayerActive(mPlayer);

		giveAbsorption(mPlayer);

		float knockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK);
		int duration = STUN_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_STUN_DURATION);
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS))) {
			MovementUtils.knockAway(mPlayer, mob, knockback, true);
			if (isLevelTwo()) {
				EntityUtils.applyStun(mPlugin, duration, mob);
			}
		}

	}

	private void giveAbsorption(Player player) {
		AbsorptionUtils.addAbsorption(player, mAbsorptionHealth, mAbsorptionHealth, BUFF_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_ABSORPTION_DURATION));
	}
}
