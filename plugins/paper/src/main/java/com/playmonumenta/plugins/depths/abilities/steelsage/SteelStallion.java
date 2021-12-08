package com.playmonumenta.plugins.depths.abilities.steelsage;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;

import net.md_5.bungee.api.ChatColor;

public class SteelStallion extends DepthsAbility {
	public static final String ABILITY_NAME = "Steel Stallion";
	public static final int COOLDOWN = 90 * 20;
	private static final double TRIGGER_HEALTH = 0.25;
	public static final int[] HEALTH = {60, 70, 80, 90, 100, 120};
	public static final double[] SPEED = {.2, .24, .28, .32, .36, .44};
	public static final double[] JUMP_STRENGTH = {.5, .6, .7, .8, .9, 1.3};
	public static final int[] DURATION = {10 * 20, 11 * 20, 12 * 20, 13 * 20, 14 * 20, 18 * 20};
	public static final int TICK_INTERVAL = 5;

	private Mob mHorse;

	public SteelStallion(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.IRON_HORSE_ARMOR;
		mTree = DepthsTree.METALLIC;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.STEEL_STALLION;
		mInfo.mIgnoreCooldown = true;
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (event.isCancelled() || event.getDamage() <= 0) {
			return true;
		}

		if (mHorse != null) {
			mHorse.setHealth(Math.max(0, mHorse.getHealth() - event.getDamage()));
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_HORSE_HURT, 0.8f, 1.0f);
			event.setDamage(0);
			event.setCancelled(true);
			return true;
		}

		execute(event);
		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		if (event.isCancelled() || event.getDamage() <= 0) {
			return true;
		}

		if (mHorse != null) {
			mHorse.setHealth(Math.max(0, mHorse.getHealth() - event.getDamage()));
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_HORSE_HURT, 0.8f, 1.0f);
			event.setDamage(0);
			event.setCancelled(true);
			return true;
		}

		execute(event);
		return true;
	}

	/*
	 * Works against all types of damage
	 */
	@Override
	public boolean playerDamagedEvent(EntityDamageEvent event) {
		// Do not process cancelled damage events
		if (event.isCancelled() || event instanceof EntityDamageByEntityEvent || event.getDamage() <= 0) {
			return true;
		}

		if (mHorse != null) {
			mHorse.setHealth(Math.max(0, mHorse.getHealth() - event.getDamage()));
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_HORSE_HURT, 0.8f, 1.0f);
			event.setDamage(0);
			event.setCancelled(true);
			return true;
		}

		execute(event);
		return true;
	}

	private void execute(EntityDamageEvent event) {
		if (AbilityUtils.isBlocked(event) || mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			return;
		}

		// Calculate whether this effect should not be run based on player health.
		double healthRemaining = mPlayer.getHealth() + AbsorptionUtils.getAbsorption(mPlayer) - EntityUtils.getRealFinalDamage(event);

		AttributeInstance maxHealth = mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH);
		if (healthRemaining > maxHealth.getValue() * TRIGGER_HEALTH) {
			return;
		}

		Location loc = mPlayer.getLocation();
		Entity horse = LibraryOfSoulsIntegration.summon(loc, "SteelStallion");
		if (horse != null) {
			horse.addPassenger(mPlayer);
			mHorse = (Mob) horse;
			mHorse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH[mRarity - 1]);
			//Horse absorbs the damage from the hit that triggers it
			mHorse.setHealth(Math.max(HEALTH[mRarity - 1] - event.getDamage(), 0));
			mHorse.setInvulnerable(true);
			event.setDamage(0);
			event.setCancelled(true);
			putOnCooldown();
		}

		new BukkitRunnable() {
			int mTicksElapsed = 0;
			@Override
			public void run() {
				boolean isOutOfTime = mTicksElapsed >= DURATION[mRarity - 1];
				if (isOutOfTime || mHorse == null || mHorse.getHealth() <= 0 || mHorse.getPassengers().size() == 0) {
					if (isOutOfTime) {
						Location horseLoc = mHorse.getLocation();
						World world = horseLoc.getWorld();
						world.playSound(horseLoc, Sound.ENTITY_HORSE_DEATH, 0.8f, 1.0f);
						world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, horseLoc, 15);
						world.spawnParticle(Particle.SMOKE_NORMAL, horseLoc, 20);
					}

					mHorse.remove();
					mHorse = null;
					this.cancel();
				}
				mTicksElapsed += TICK_INTERVAL;
			}
		}.runTaskTimer(mPlugin, 0, TICK_INTERVAL);

		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.HEART, loc, 10, 2, 2, 2);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 1, 0.5f);

		MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Steel Stallion has been activated!");
	}

	public static boolean isSteelStallion(Entity entity) {
		return entity instanceof Horse && entity.getName() == ABILITY_NAME;
	}

	@Override
	public String getDescription(int rarity) {
		return "When your health drops below " + (int) DepthsUtils.roundPercent(TRIGGER_HEALTH) + "%, summon and ride a horse with " + DepthsUtils.getRarityColor(rarity) + HEALTH[rarity - 1] + ChatColor.WHITE + " health that disappears after " + DepthsUtils.getRarityColor(rarity) + DURATION[rarity - 1] / 20 + ChatColor.WHITE + " seconds. While you are riding the horse, all damage you receive is redirected to the horse, including the damage that triggered this ability. The horse has a speed of " + DepthsUtils.getRarityColor(rarity) + SPEED[rarity - 1] + ChatColor.WHITE + " and a jump strength of " + DepthsUtils.getRarityColor(rarity) + JUMP_STRENGTH[rarity - 1] + ChatColor.WHITE + ". Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.METALLIC;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.LIFELINE;
	}
}
