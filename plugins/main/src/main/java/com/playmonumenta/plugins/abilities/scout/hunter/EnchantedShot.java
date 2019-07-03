package com.playmonumenta.plugins.abilities.scout.hunter;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Color;
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
 * Enchanted Arrow: Left click while not sneaking will prime an enchanted arrow.
 * If the next arrow is fired within 5 seconds, the ability goes on cooldown and
 * the arrow will instantaneously travel in a straight line for 30
 * blocks until hitting a block, piercing through all targets,
 * dealing 25 / 40 damage. (Cooldown: 25 / 20 s)
 */
public class EnchantedShot extends Ability {

	private static final int ENCHANTED_1_DAMAGE = 25;
	private static final int ENCHANTED_2_DAMAGE = 40;
	private static final int ENCHANTED_1_COOLDOWN = 20 * 25;
	private static final int ENCHANTED_2_COOLDOWN = 20 * 5;
	private static final Particle.DustOptions ENCHANTED_ARROW_COLOR = new Particle.DustOptions(Color.fromRGB(225, 255, 219), 2.0f);
	private static final Particle.DustOptions ENCHANTED_ARROW_FRINGE_COLOR = new Particle.DustOptions(Color.fromRGB(168, 255, 252), 2.0f);

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
	public void cast() {
		if (!active && !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.ENCHANTED_ARROW)) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (InventoryUtils.isBowItem(mainHand)) {
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
							active = false;
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}
		}
	}

	@Override
	public boolean PlayerShotArrowEvent(Arrow arrow) {
		if (active) {
			arrow.remove();
			mPlugin.mProjectileEffectTimers.removeEntity(arrow);
			active = false;
			BoundingBox box = BoundingBox.of(mPlayer.getEyeLocation(), 1.5, 1.5, 1.5);
			double damage = getAbilityScore() == 1 ? ENCHANTED_1_DAMAGE : ENCHANTED_2_DAMAGE;

			Player player = mPlayer;
			Location loc = player.getEyeLocation();
			Vector dir = loc.getDirection().normalize();
			player.getWorld().playSound(loc, Sound.ENTITY_PUFFER_FISH_DEATH, 0.7f, 0.8f);
			player.getWorld().playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, 1f, 1.5f);
			player.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.75f);
			player.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 1f);
			player.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 1f, 0.75f);
			mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(dir), 20, 0.2, 0.2, 0.2, 0.15);
			mWorld.spawnParticle(Particle.CRIT_MAGIC, loc.clone().add(dir), 10, 0.2, 0.2, 0.2, 0.15);

			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getEyeLocation(), 30, mPlayer);
			BowMastery bm = (BowMastery) AbilityManager.getManager().getPlayerAbility(mPlayer, BowMastery.class);
			if (bm != null) {
				damage += bm.getBonusDamage();
			}
			Sharpshooter ss = (Sharpshooter) AbilityManager.getManager().getPlayerAbility(mPlayer, Sharpshooter.class);
			if (ss != null) {
				damage += ss.getSharpshot();
			}

			Location pLoc = mPlayer.getEyeLocation();
			pLoc.setPitch(pLoc.getPitch() + 90);
			Vector pVec = new Vector(pLoc.getDirection().getX(), pLoc.getDirection().getY(), pLoc.getDirection().getZ());
			pVec = pVec.normalize();

			for (int i = 0; i < 30; i++) {
				box.shift(dir);
				Location bLoc = box.getCenter().toLocation(mWorld);
				mWorld.spawnParticle(Particle.SMOKE_NORMAL, bLoc, 5, 0.1, 0.1, 0.1, 0.2);
				mWorld.spawnParticle(Particle.FALLING_DUST, bLoc, 5, 0.1, 0.1, 0.1, 0, Bukkit.createBlockData("light_gray_glazed_terracotta"));
				mWorld.spawnParticle(Particle.SPELL_INSTANT, bLoc, 6, 0.2, 0.2, 0.2, 0.15);
				mWorld.spawnParticle(Particle.SMOKE_NORMAL, bLoc, 3, 0.1, 0.1, 0.1, 0);
				mWorld.spawnParticle(Particle.REDSTONE, bLoc, 3, 0.1, 0.1, 0.1, 0, ENCHANTED_ARROW_COLOR);
				mWorld.spawnParticle(Particle.CRIT, bLoc, 5, 0.15, 0.15, 0.15, 0.4);
				mWorld.spawnParticle(Particle.REDSTONE, bLoc.clone().add(pVec), 1, 0, 0, 0, 0, ENCHANTED_ARROW_FRINGE_COLOR);
				mWorld.spawnParticle(Particle.END_ROD, bLoc.clone().add(pVec), 1, 0, 0, 0, 0.02);
				mWorld.spawnParticle(Particle.REDSTONE, bLoc.clone().add(dir.clone().multiply(0.5)).add(pVec.clone().rotateAroundAxis(dir, Math.PI / 12)), 1, 0, 0, 0, 0, ENCHANTED_ARROW_FRINGE_COLOR);
				Iterator<LivingEntity> iterator = mobs.iterator();
				while (iterator.hasNext()) {
					LivingEntity mob = iterator.next();
					if (mob.getBoundingBox().overlaps(box)) {
						if (mob instanceof Player) {
							damage *= 0.75;
						}
						if (mob.hasMetadata("PinningShotEnemyIsPinned")) {
							damage *= mob.getMetadata("PinningShotEnemyIsPinned").get(0).asDouble();
						}
						mWorld.spawnParticle(Particle.CRIT_MAGIC, mob.getLocation().add(0, 1, 0), 15, 0.1, 0.2, 0.1, 0.15);
						mWorld.spawnParticle(Particle.SPELL_INSTANT, mob.getLocation().add(0, 1, 0), 20, 0.1, 0.2, 0.1, 0.15);
						mWorld.spawnParticle(Particle.FIREWORKS_SPARK, mob.getLocation().add(0, 1, 0), 10, 0.1, 0.2, 0.1, 0.1);
						EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer);
						/* Prevent mob from being hit twice in one shot */
						iterator.remove();
					}
				}
				if (bLoc.getBlock().getType().isSolid()) {
					mWorld.spawnParticle(Particle.SMOKE_LARGE, bLoc, 80, 0.1, 0.1, 0.1, 0.2);
					mWorld.spawnParticle(Particle.CLOUD, bLoc, 80, 0.1, 0.1, 0.1, 0.2);
					mWorld.spawnParticle(Particle.FIREWORKS_SPARK, bLoc, 50, 0.1, 0.1, 0.1, 0.3);
					player.getWorld().playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.85f);
					break;
				}

				pVec.rotateAroundAxis(dir, Math.PI / 6);
			}

			putOnCooldown();
			return false;
		}
		return true;
	}

	@Override
	public boolean runCheck() {
		return !mPlayer.isSneaking();
	}

}
