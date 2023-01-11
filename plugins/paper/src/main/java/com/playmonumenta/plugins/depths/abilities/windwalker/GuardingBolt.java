package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class GuardingBolt extends DepthsAbility {

	public static final String ABILITY_NAME = "Guarding Bolt";
	public static final int COOLDOWN = 24 * 20;
	private static final int RADIUS = 4;
	private static final int RANGE = 25;
	private static final int[] DAMAGE = {12, 14, 16, 18, 20, 24};
	private static final int[] STUN_DURATION = {20, 25, 30, 35, 40, 50};
	public static final Particle.DustOptions COLOR_YELLOW = new Particle.DustOptions(Color.YELLOW, 0.75f);
	public static final Particle.DustOptions COLOR_AQUA = new Particle.DustOptions(Color.AQUA, 0.75f);

	public static final DepthsAbilityInfo<GuardingBolt> INFO =
		new DepthsAbilityInfo<>(GuardingBolt.class, ABILITY_NAME, GuardingBolt::new, DepthsTree.WINDWALKER, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.GUARDING_BOLT)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", GuardingBolt::cast,
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true).keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE), HOLDING_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.HORN_CORAL))
			.descriptions(GuardingBolt::getDescription, MAX_RARITY);

	public GuardingBolt(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		BoundingBox box = BoundingBox.of(mPlayer.getEyeLocation(), 1, 1, 1);
		Location oLoc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		Vector dir = oLoc.getDirection();
		List<Player> players = PlayerUtils.playersInRange(mPlayer.getEyeLocation(), RANGE, true);
		players.remove(mPlayer);

		//Do not teleport to players who aren't in the depths system
		//This allows players to teleport into another players loot room (stuck spot as well as abusable)
		DepthsManager manager = DepthsManager.getInstance();
		players.removeIf(p -> !manager.mPlayers.containsKey(p.getUniqueId()));

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
						new PartialParticle(Particle.CLOUD, loc, 4, 0.25, 0.25, 0.25, 0f).spawnAsPlayerActive(mPlayer);
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
						new PartialParticle(Particle.CLOUD, player.getLocation().add(0, 0.15, 0), 0, (float) pdir.getX(), 0f, (float) pdir.getZ(), FastUtils.randomDoubleInRange(0.1, 0.4)).spawnAsPlayerActive(mPlayer);
					}

					// Yellow particles
					for (int k = 0; k < 60; k++) {
						double x = FastUtils.randomDoubleInRange(-3, 3);
						double z = FastUtils.randomDoubleInRange(-3, 3);
						Location to = player.getLocation().add(x, 0.15, z);
						Vector pdir = LocationUtils.getDirectionTo(to, player.getLocation().add(0, 0.15, 0));
						new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 0.15, 0), 0, (float) pdir.getX(), 0f, (float) pdir.getZ(), FastUtils.randomDoubleInRange(0.15, 0.5), COLOR_YELLOW).spawnAsPlayerActive(mPlayer);
					}

					Location userLoc = mPlayer.getLocation();
					Location targetLoc = player.getLocation().setDirection(mPlayer.getEyeLocation().getDirection()).subtract(dir.clone().multiply(0.5)).add(0, 0.5, 0);
					if (userLoc.distance(player.getLocation()) > 1) {
						mPlayer.teleport(targetLoc);
						doDamage(targetLoc);
						hasTeleported = true;
					}

					world.playSound(targetLoc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 0.75f, 0.9f);
				}
			}
		}
	}

	private void doDamage(Location location) {
		World world = mPlayer.getWorld();
		world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.75f, 1.5f);
		new PartialParticle(Particle.REDSTONE, location, 125, RADIUS, RADIUS, RADIUS, 3, COLOR_YELLOW).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, location, 125, RADIUS, RADIUS, RADIUS, 3, COLOR_AQUA).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLASH, location.clone().add(location.getDirection()), 1, 0, 0, 0, 10).spawnAsPlayerActive(mPlayer);

		List<LivingEntity> enemies = EntityUtils.getNearbyMobs(location, RADIUS);
		// The more enemies, the less particles for each one
		int mobParticles = Math.max(
			1,
			20 / Math.max(1, enemies.size()) // Never divide by 0. Always maximum 20 particles for <= 1 enemy
		);

		for (LivingEntity enemy : enemies) {
			DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, DAMAGE[mRarity - 1], mInfo.getLinkedSpell());
			if (!EntityUtils.isBoss(enemy)) {
				EntityUtils.applyStun(mPlugin, STUN_DURATION[mRarity - 1], enemy);
			}

			Location enemyParticleLocation = enemy.getLocation().add(0, enemy.getHeight() / 2, 0);
			new PartialParticle(Particle.CLOUD, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.END_ROD, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5).spawnAsPlayerActive(mPlayer);
		}
	}

	private static String getDescription(int rarity) {
		return "Left click the air while sneaking and looking directly at a player within " + RANGE + " blocks to dash to their location. Mobs in a " + RADIUS + " block radius of the destination are dealt " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " magic damage, knocked back, and stunned for " + DepthsUtils.getRarityColor(rarity) + STUN_DURATION[rarity - 1] / 20.0 + ChatColor.WHITE + " seconds. Cooldown: " + COOLDOWN / 20 + "s.";
	}

}
