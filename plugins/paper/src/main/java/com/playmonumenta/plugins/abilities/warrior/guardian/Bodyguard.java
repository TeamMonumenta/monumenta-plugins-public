package com.playmonumenta.plugins.abilities.warrior.guardian;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
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
	public static final String CHARM_ABSORPTION = "Bodyguard Absorption";
	public static final String CHARM_ABSORPTION_DURATION = "Bodyguard Absorption Duration";
	public static final String CHARM_STUN_DURATION = "Bodyguard Stun Duration";
	public static final String CHARM_KNOCKBACK = "Bodyguard Knockback";

	private final double mAbsorptionHealth;

	private int mLeftClicks = 0;

	public Bodyguard(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Bodyguard");
		mInfo.mScoreboardId = "Bodyguard";
		mInfo.mShorthandName = "Bg";
		mInfo.mDescriptions.add("Left-click the air twice while looking directly at another player within 25 blocks to charge to them (cannot be used in safezones). Upon arriving, knock away all mobs within 4 blocks. Both you and the other player gain 4 Absorption hearts for 10 seconds. Left-click twice while looking down to cast on yourself. Cooldown: 30s.");
		mInfo.mDescriptions.add("Absorption increased to 6 hearts. Additionally, affected mobs are stunned for 3 seconds.");
		mInfo.mLinkedSpell = ClassAbility.BODYGUARD;
		mInfo.mCooldown = CharmManager.getCooldown(mPlayer, CHARM_COOLDOWN, COOLDOWN);
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.IRON_CHESTPLATE, 1);
		mAbsorptionHealth = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, isLevelOne() ? ABSORPTION_HEALTH_1 : ABSORPTION_HEALTH_2);
	}

	@Override
	public void cast(Action action) {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)
				|| ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)
				|| ItemUtils.isPickaxe(mainHand)) {
			return;
		}

		BoundingBox box = BoundingBox.of(mPlayer.getEyeLocation(), 1, 1, 1);
		Location oLoc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		boolean lookingDown = oLoc.getPitch() > 50;
		Vector dir = oLoc.getDirection();
		double range = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);
		List<Player> players = PlayerUtils.otherPlayersInRange(mPlayer, range, true);
		for (int i = 0; i < range; i++) {
			box.shift(dir);
			Location bLoc = box.getCenter().toLocation(world);
			if (!bLoc.isChunkLoaded() || bLoc.getBlock().getType().isSolid()) {
				if (lookingDown) {
					mLeftClicks++;
					new BukkitRunnable() {
						@Override
						public void run() {
							if (mLeftClicks > 0) {
								mLeftClicks--;
							}
							this.cancel();
						}
					}.runTaskLater(mPlugin, 5);
				}

				break;
			}

			boolean hasTeleported = false;
			for (Player player : players) {
				//Prevents bodyguarding to multiple people
				if (hasTeleported) {
					break;
				}

				// If looking at another player, or reached the end of range check and looking down
				if (player.getBoundingBox().overlaps(box) && !ZoneUtils.hasZoneProperty(player, ZoneProperty.NO_MOBILITY_ABILITIES)) {
					// Double LClick detection
					mLeftClicks++;
					new BukkitRunnable() {
						@Override
						public void run() {
							if (mLeftClicks > 0) {
								mLeftClicks--;
							}
							this.cancel();
						}
					}.runTaskLater(mPlugin, 5);
					if (mLeftClicks < 2) {
						return;
					}
					// Don't set mLeftClicks to 0, self cast below handles that

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
					if (userLoc.distance(player.getLocation()) > 1) {
						mPlayer.teleport(targetLoc);
						hasTeleported = true;
					}

					world.playSound(targetLoc, Sound.ENTITY_BLAZE_SHOOT, 0.75f, 0.75f);
					world.playSound(targetLoc, Sound.ENTITY_ENDER_DRAGON_HURT, 0.75f, 0.9f);

					giveAbsorption(player);
				}
			}
		}

		// Self trigger
		if (mLeftClicks < 2) {
			return;
		}
		mLeftClicks = 0;
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
