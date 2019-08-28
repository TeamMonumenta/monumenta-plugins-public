package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

/*
 * Shift left click with an Alchemist Potion to shoot a mixture
 * that deals 8 / 16 damage to every enemy touched and adds 2 / 3
 * absorption health to players (including yourself), maximum 12.
 * After hitting a block or travelling 10 blocks, the mixture traces
 * and returns to you, able to damage enemies and shield allies a
 * second time. Cooldown: 30 seconds.
 */

public class AlchemicalAmalgam extends Ability {

	private static final int AMALGAM_1_DAMAGE = 8;
	private static final int AMALGAM_2_DAMAGE = 16;
	private static final int AMALGAM_1_SHIELD = 2;
	private static final int AMALGAM_2_SHIELD = 3;
	private static final int AMALGAM_MAX_SHIELD = 12;
	// Calculate the range with MAX_DURATION * MOVE_SPEED
	private static final int AMALGAM_MAX_DURATION = (int) (20 * 2.5);
	private static final double AMALGAM_MOVE_SPEED = 0.2;
	private static final double AMALGAM_RADIUS = 1.5;
	private static final Particle.DustOptions AMALGAM_LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 100), 1.0f);
	private static final Particle.DustOptions AMALGAM_DARK_COLOR = new Particle.DustOptions(Color.fromRGB(83, 0, 135), 1.0f);

	private int mDamage;
	private int mShield;

	public AlchemicalAmalgam(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Alchemical";
		mInfo.cooldown = 20 * 30;
		mInfo.linkedSpell = Spells.ALCHEMICAL_AMALGAM;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
		mDamage = getAbilityScore() == 1 ? AMALGAM_1_DAMAGE : AMALGAM_2_DAMAGE;
		mShield = getAbilityScore() == 1 ? AMALGAM_1_SHIELD : AMALGAM_2_SHIELD;
	}

	@Override
	public void cast() {
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1, 1.75f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1, 0.75f);
		mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 1.25f);
		mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 1);
		mWorld.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 1);

		AbsorptionUtils.addAbsorption(mPlayer, mShield, AMALGAM_MAX_SHIELD);
		putOnCooldown();

		new BukkitRunnable() {
			Location loc = mPlayer.getEyeLocation();
			BoundingBox box = BoundingBox.of(loc, AMALGAM_RADIUS, AMALGAM_RADIUS, AMALGAM_RADIUS);
			Vector increment = loc.getDirection().multiply(0.2);

			// Convoluted range parameter makes sure we grab all possible entities to be hit without recalculating manually
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, AMALGAM_MOVE_SPEED * AMALGAM_MAX_DURATION + 2, mPlayer);
			List<Player> players = PlayerUtils.getNearbyPlayers(mPlayer, AMALGAM_MOVE_SPEED * AMALGAM_MAX_DURATION + 2, false);

			int t = 0;
			double degree = 0;
			boolean reverse = false;

			@Override
			public void run() {
				box.shift(increment);
				loc.add(increment);
				Iterator<LivingEntity> mobIter = mobs.iterator();
				while (mobIter.hasNext()) {
					LivingEntity mob = mobIter.next();
					if (box.overlaps(mob.getBoundingBox())) {
						EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer);
						mobIter.remove();
					}
				}
				Iterator<Player> playerIter = players.iterator();
				while (playerIter.hasNext()) {
					Player player = playerIter.next();
					if (box.overlaps(player.getBoundingBox())) {
						AbsorptionUtils.addAbsorption(player, mShield, AMALGAM_MAX_SHIELD);
						playerIter.remove();
					}
				}

				degree += 12;
				Vector vec;
				for (int i = 0; i < 2; i++) {
					double radian1 = Math.toRadians(degree + (i * 180));
					vec = new Vector(Math.cos(radian1) * 0.325, 0, Math.sin(radian1) * 0.325);
					vec = VectorUtils.rotateXAxis(vec, -loc.getPitch() + 90);
					vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

					Location l = loc.clone().add(vec);
					mWorld.spawnParticle(Particle.REDSTONE, l, 5, 0.1, 0.1, 0.1, AMALGAM_LIGHT_COLOR);
					mWorld.spawnParticle(Particle.REDSTONE, l, 5, 0.1, 0.1, 0.1, AMALGAM_DARK_COLOR);
				}
				mWorld.spawnParticle(Particle.SPELL_INSTANT, loc, 5, 0.35, 0.35, 0.35, 1);
				mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 5, 0.35, 0.35, 0.35, 1);

				if (!reverse && (LocationUtils.collidesWithSolid(loc, loc.getBlock()) || t >= AMALGAM_MAX_DURATION)) {
					mobs = EntityUtils.getNearbyMobs(loc, (0.3 + AMALGAM_MOVE_SPEED) * AMALGAM_MAX_DURATION + 2, mPlayer);
					players = PlayerUtils.getNearbyPlayers(mPlayer, (0.3 + AMALGAM_MOVE_SPEED) * AMALGAM_MAX_DURATION + 2, false);
					reverse = true;
				}

				if (reverse) {
					if (t <= 0) {
						AbsorptionUtils.addAbsorption(mPlayer, mShield, AMALGAM_MAX_SHIELD);
						mWorld.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.2f, 2.4f);
						this.cancel();
					}

					// The increment is calculated by the distance to the player divided by the number of increments left
					increment = mPlayer.getEyeLocation().toVector().subtract(loc.toVector()).multiply(1.0 / t);

					// To make the particles function without rewriting the particle code, manually calculate and set pitch and yaw
					double x = increment.getX();
					double y = increment.getY();
					double z = increment.getZ();
					// As long as Z is nonzero, we won't get division by 0
					if (z == 0) {
						z = 0.0001;
					}
					float pitch = (float) Math.toDegrees(-Math.atan(y / Math.sqrt(Math.pow(x, 2) + Math.pow(z, 2))));
					float yaw = (float) Math.toDegrees(Math.atan(-x / z));
					if (z < 0) {
						yaw += 180;
					}
					loc.setPitch(pitch);
					loc.setYaw(yaw);

					t--;
				} else {
					t++;
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean runCheck() {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSneaking() && InventoryUtils.testForItemWithName(inMainHand, "Alchemist's Potion");
	}

}
