package com.playmonumenta.plugins.depths.abilities.windwalker;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import net.md_5.bungee.api.ChatColor;

public class GuardingBolt extends DepthsAbility {

	public static final String ABILITY_NAME = "Guarding Bolt";
	public static final int COOLDOWN = 16 * 20;
	private static final int RADIUS = 3;
	private static final int RANGE = 25;
	private static final int[] DAMAGE = {16, 20, 24, 28, 32};
	private static final int[] STUN_DURATION = {20, 25, 30, 35, 40};
	public static final Particle.DustOptions COLOR_YELLOW = new Particle.DustOptions(Color.YELLOW, 0.75f);
	public static final Particle.DustOptions COLOR_AQUA = new Particle.DustOptions(Color.AQUA, 0.75f);

	public GuardingBolt(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.HORN_CORAL;
		mTree = DepthsTree.WINDWALKER;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.GUARDING_BOLT;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mIgnoreCooldown = true;
	}

	@Override
	public void cast(Action trigger) {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			return;
		}

		BoundingBox box = BoundingBox.of(mPlayer.getEyeLocation(), 1, 1, 1);
		Location oLoc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		Vector dir = oLoc.getDirection();
		List<Player> players = PlayerUtils.playersInRange(mPlayer.getEyeLocation(), RANGE, true);
		players.remove(mPlayer);
		for (int i = 0; i < RANGE; i++) {
			box.shift(dir);
			Location bLoc = box.getCenter().toLocation(world);
			boolean hasTeleported = false;
			for (Player player : players) {
				//Prevents bodyguarding to multiple people
				if (hasTeleported) {
					break;
				}

				// If looking at another player
				if (player.getBoundingBox().overlaps(box)) {
					putOnCooldown();

					Location loc = mPlayer.getEyeLocation();
					for (int j = 0; j < 45; j++) {
						loc.add(dir.clone().multiply(0.33));
						world.spawnParticle(Particle.CLOUD, loc, 4, 0.25, 0.25, 0.25, 0f);
						if (loc.distance(bLoc) < 1) {
							break;
						}
					}

					// Wind particles
					for (int k = 0; k < 120; k++) {
						double x = FastUtils.randomDoubleInRange(-3, 3);
						double z = FastUtils.randomDoubleInRange(-3, 3);
						Location to = player.getLocation().add(x, 0.15, z);
						Vector pdir = LocationUtils.getDirectionTo(to, player.getLocation().add(0, 0.15, 0));
						world.spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.15, 0), 0, (float) pdir.getX(), 0f, (float) pdir.getZ(), FastUtils.randomDoubleInRange(0.1, 0.4));
					}

					// Yellow particles
					for (int k = 0; k < 60; k++) {
						double x = FastUtils.randomDoubleInRange(-3, 3);
						double z = FastUtils.randomDoubleInRange(-3, 3);
						Location to = player.getLocation().add(x, 0.15, z);
						Vector pdir = LocationUtils.getDirectionTo(to, player.getLocation().add(0, 0.15, 0));
						world.spawnParticle(Particle.REDSTONE, player.getLocation().add(0, 0.15, 0), 0, (float) pdir.getX(), 0f, (float) pdir.getZ(), FastUtils.randomDoubleInRange(0.15, 0.5), COLOR_YELLOW);
					}

					Location userLoc = mPlayer.getLocation();
					Location targetLoc = player.getLocation().setDirection(mPlayer.getEyeLocation().getDirection()).subtract(dir.clone().multiply(0.5)).add(0, 0.5, 0);
					if (userLoc.distance(player.getLocation()) > 1) {
						mPlayer.teleport(targetLoc);
						doDamage(targetLoc);
						hasTeleported = true;
					}

					world.playSound(targetLoc, Sound.ENTITY_ENDER_DRAGON_HURT, 0.75f, 0.9f);
				}
			}
		}
	}

	private void doDamage(Location location) {
		World world = mPlayer.getWorld();
		world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.75f, 1.5f);
		world.spawnParticle(Particle.REDSTONE, location, 125, RADIUS, RADIUS, RADIUS, 3, COLOR_YELLOW);
		world.spawnParticle(Particle.REDSTONE, location, 125, RADIUS, RADIUS, RADIUS, 3, COLOR_AQUA);
		world.spawnParticle(Particle.FLASH, location.clone().add(location.getDirection()), 1, 0, 0, 0, 10);

		List<LivingEntity> enemies = EntityUtils.getNearbyMobs(location, RADIUS);
		// The more enemies, the less particles for each one
		int mobParticles = Math.max(
			1,
			20 / Math.max(1, enemies.size()) // Never divide by 0. Always maximum 20 particles for <= 1 enemy
		);

		for (LivingEntity enemy : enemies) {
			EntityUtils.damageEntity(mPlugin, enemy, DAMAGE[mRarity - 1], mPlayer, MagicType.ARCANE, true, mInfo.mLinkedSpell, true, true, false);
			if (!EntityUtils.isBoss(enemy)) {
				EntityUtils.applyStun(mPlugin, STUN_DURATION[mRarity - 1], enemy);
			}

			Location enemyParticleLocation = enemy.getLocation().add(0, enemy.getHeight() / 2, 0);
			world.spawnParticle(Particle.CLOUD, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5);
			world.spawnParticle(Particle.END_ROD, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5);
		}
	}

	@Override
	public String getDescription(int rarity) {
		return "Left click the air while sneaking and looking directly at a player within " + RANGE + " blocks to dash to their location. Mobs in a " + RADIUS + " block radius of the destination are dealt " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " damage, knocked back, and stunned for " + DepthsUtils.getRarityColor(rarity) + STUN_DURATION[rarity - 1] / 20 + ChatColor.WHITE + " seconds. Cooldown: " + COOLDOWN + "s.";
	}

	@Override
	public boolean runCheck() {
		return (mPlayer.isSneaking() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand()));
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.WINDWALKER;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_LEFT_CLICK;
	}
}
