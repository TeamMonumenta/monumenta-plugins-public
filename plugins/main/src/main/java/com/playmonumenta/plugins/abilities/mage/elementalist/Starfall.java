package com.playmonumenta.plugins.abilities.mage.elementalist;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
 * Triple left-click without hitting a mob makes a meteor fall where the
 * player is looking, dealing 18 / 24 damage in a 5-block radius and
 * setting mobs on fire for 3s. Cooldown 16 / 12 s
 */

public class Starfall extends Ability {
	private static final int STARFALL_1_COOLDOWN = 16 * 20;
	private static final int STARFALL_2_COOLDOWN = 12 * 20;
	private static final int STARFALL_1_DAMAGE = 18;
	private static final int STARFALL_2_DAMAGE = 26;
	private static final int STARFALL_FIRE_DURATION = 3 * 20;
	private static final double STARFALL_RADIUS = 5;

	private int mLeftClicks = 0;

	public Starfall(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.STARFALL;
		mInfo.scoreboardId = "Starfall";
		mInfo.cooldown = getAbilityScore() == 1 ? STARFALL_1_COOLDOWN : STARFALL_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		return InventoryUtils.isWandItem(mHand) || InventoryUtils.isWandItem(oHand);
	}

	@Override
	public void cast() {
		mLeftClicks++;
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mLeftClicks > 0) {
					mLeftClicks--;
				}
				this.cancel();
			}
		}.runTaskLater(mPlugin, 20);

		if (mLeftClicks < 3) {
			return;
		}
		Location loc = mPlayer.getEyeLocation();
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 0.85f);
		mWorld.spawnParticle(Particle.LAVA, mPlayer.getLocation(), 15, 0.25f, 0.1f, 0.25f);
		mWorld.spawnParticle(Particle.FLAME, mPlayer.getLocation(), 30, 0.25f, 0.1f, 0.25f, 0.15f);
		Vector dir = loc.getDirection().normalize();
		for (int i = 0; i < 25; i++) {
			loc.add(dir);

			mPlayer.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
			int size = EntityUtils.getNearbyMobs(loc, 2, mPlayer).size();
			if (loc.getBlock().getType().isSolid() || i >= 24 || size > 0) {
				launchMeteor(mPlayer, loc);
				break;
			}
		}

		mLeftClicks = 0;
		putOnCooldown();
	}

	private void launchMeteor(final Player player, final Location loc) {
		double damage = getAbilityScore() == 1 ? STARFALL_1_DAMAGE : STARFALL_2_DAMAGE;
		Location ogLoc = loc.clone();
		loc.add(0, 40, 0);
		new BukkitRunnable() {
			double t = 0;
			@Override
			public void run() {
				t += 1;
				for (int i = 0; i < 8; i++) {
					loc.subtract(0, 0.25, 0);
					if (loc.getBlock().getType().isSolid()) {
						if (loc.getY() - ogLoc.getY() <= 2) {
							loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0);
							mWorld.spawnParticle(Particle.FLAME, loc, 175, 0, 0, 0, 0.235F);
							mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.2F);
							mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0, 0, 0, 0.2F);
							this.cancel();

							for (LivingEntity e : EntityUtils.getNearbyMobs(loc, STARFALL_RADIUS, mPlayer)) {
								if (e instanceof Player) {
									EntityUtils.damageEntity(mPlugin, e, (float)(damage * 0.75), player, MagicType.FIRE);
								} else {
									EntityUtils.damageEntity(mPlugin, e, (float) damage, player, MagicType.FIRE);
								}
								e.setFireTicks(STARFALL_FIRE_DURATION);

								Vector v = e.getLocation().toVector().subtract(loc.toVector()).normalize();
								v.add(new Vector(0, 0.2, 0));
								e.setVelocity(v);
							}
							break;
						}
					}
				}
				loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 1);
				mWorld.spawnParticle(Particle.FLAME, loc, 25, 0.25F, 0.25F, 0.25F, 0.1F);
				mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 5, 0.25F, 0.25F, 0.25F, 0.1F);

				if (t >= 50) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}
}
