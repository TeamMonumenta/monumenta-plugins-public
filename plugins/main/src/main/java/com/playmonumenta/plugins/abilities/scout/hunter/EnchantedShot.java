package com.playmonumenta.plugins.abilities.scout.hunter;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.scout.BowMastery;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
 * Enchanted Arrow: Left click will prime an enchanted arrow.
 * If the next arrow is fired within 5 seconds, the ability goes on cooldown and
 * the arrow will instantaneously travel in a straight line for 30
 * blocks until hitting a block, piercing through all targets,
 * dealing 25 / 40 damage. (Cooldown: 25 / 20 s)
 */
public class EnchantedShot extends Ability {

	private static final int ENCHANTED_1_DAMAGE = 25;
	private static final int ENCHANTED_2_DAMAGE = 40;
	private static final int ENCHANTED_1_COOLDOWN = 20 * 25;
	private static final int ENCHANTED_2_COOLDOWN = 20 * 20;

	private boolean active = false;

	public EnchantedShot(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "EnchantedArrow";
		mInfo.linkedSpell = Spells.ENCHANTED_ARROW;
		mInfo.cooldown = getAbilityScore() == 1 ? ENCHANTED_1_COOLDOWN : ENCHANTED_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
		mInfo.ignoreCooldown = true;
	}

	@Override
	public boolean cast() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.ENCHANTED_ARROW) && InventoryUtils.isBowItem(mainHand)) {
			Player player = mPlayer;
			active = true;
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 1.45f);
			new BukkitRunnable() {
				int t = 0;
				@Override
				public void run() {
					t++;
					mWorld.spawnParticle(Particle.SPELL_INSTANT, player.getLocation(), 4, 0.25, 0, 0.25, 0);
					if (!active || t >= 20 * 5) {
						if (t >= 20 * 5) {
							mPlugin.mTimers.removeCooldown(mPlayer.getUniqueId(), Spells.ENCHANTED_ARROW);
						}
						active = false;
						// For some reason this is going on cooldown after casting even though I never
						// called putOnCooldown(), so this is here as a janky fix
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
		return true;
	}

	@Override
	public boolean PlayerShotArrowEvent(Arrow arrow) {
		if (active) {
			arrow.remove();
			active = false;
			BoundingBox box = BoundingBox.of(mPlayer.getEyeLocation(), 0.65, 0.65, 0.65);
			double damage = getAbilityScore() == 1 ? ENCHANTED_1_DAMAGE : ENCHANTED_2_DAMAGE;

			Player player = mPlayer;
			Location loc = player.getEyeLocation();
			Vector dir = loc.getDirection().normalize();
			player.getWorld().playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1, 0.85f);
			player.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1, 0.65f);
			player.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.85f);
			mWorld.spawnParticle(Particle.FIREWORKS_SPARK, loc.clone().add(dir), 10, 0.1, 0.1, 0.1, 0.2);

			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getEyeLocation(), 30, mPlayer);
			BowMastery bm = (BowMastery) AbilityManager.getManager().getPlayerAbility(mPlayer, BowMastery.class);
			if (bm != null) {
				damage += bm.getBonusDamage();
			}
			Sharpshooter ss = (Sharpshooter) AbilityManager.getManager().getPlayerAbility(mPlayer, Sharpshooter.class);
			if (ss != null) {
				damage += ss.getSharpshot();
			}
			for (int i = 0; i < 30; i++) {
				box.shift(dir);
				Location bLoc = box.getCenter().toLocation(mWorld);
				mWorld.spawnParticle(Particle.SPELL_INSTANT, bLoc, 5, 0.35, 0.35, 0.35, 0);
				mWorld.spawnParticle(Particle.FIREWORKS_SPARK, bLoc, 2, 0.1, 0.1, 0.1, 0.1);
				Iterator<LivingEntity> iterator = mobs.iterator();
				while (iterator.hasNext()) {
					LivingEntity mob = iterator.next();
					if (mob.getBoundingBox().overlaps(box)) {
						if (mob instanceof Player) {
							damage *= 0.75;
						}
						EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer);
						/* Prevent mob from being hit twice in one shot */
						iterator.remove();
					}
				}
				if (bLoc.getBlock().getType().isSolid()) {
					mWorld.spawnParticle(Particle.FIREWORKS_SPARK, bLoc, 150, 0.1, 0.1, 0.1, 0.2);
					player.getWorld().playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.85f);
					break;
				}
			}

			putOnCooldown();
			return false;
		}
		return true;
	}

}
