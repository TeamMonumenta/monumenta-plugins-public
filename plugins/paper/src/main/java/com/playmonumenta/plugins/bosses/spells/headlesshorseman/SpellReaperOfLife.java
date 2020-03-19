package com.playmonumenta.plugins.bosses.spells.headlesshorseman;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.bosses.HeadlessHorsemanBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellReaperOfLife extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mRange;
	private Location mCenter;
	private Set<UUID> mSummoned = new HashSet<UUID>();
	private Set<Player> mWarnedPlayers = new HashSet<Player>();

	public SpellReaperOfLife(Plugin plugin, LivingEntity entity, Location center, double range) {
		mPlugin = plugin;
		mBoss = entity;
		mRange = range;
		mCenter = center;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		int range = (int) Math.ceil(mRange);
		Location sLoc = mBoss.getLocation();
		sLoc.setY(sLoc.getY() + 1.7f);
		try {
			FallingBlock fallingBlock = sLoc.getWorld().spawnFallingBlock(sLoc, Material.JACK_O_LANTERN.createBlockData());
			fallingBlock.setDropItem(false);

			Location pLoc = mCenter;
			Location tLoc = fallingBlock.getLocation();
			Vector vect = new Vector(pLoc.getX() - tLoc.getX(), 0, pLoc.getZ() - tLoc.getZ());
			vect.normalize().multiply((pLoc.distance(tLoc)) / 25).setY(0.7f);
			fallingBlock.setVelocity(vect);

			world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_DEATH, 3, 1.5f);
			world.spawnParticle(Particle.FLAME, fallingBlock.getLocation().add(0, fallingBlock.getHeight() / 2, 0), 3, 0.25, .25, .25, 0.025);
			world.spawnParticle(Particle.SMOKE_NORMAL, fallingBlock.getLocation().add(0, fallingBlock.getHeight() / 2, 0), 2, 0.25, .25, .25, 0.025);
			PlayerUtils.executeCommandOnNearbyPlayers(mCenter, range, "tellraw @s [{\"text\":\"[The Horseman] \",\"color\":\"dark_red\",\"bold\":\"false\",\"italic\":\"false\"},{\"text\":\"May your life force fuel \",\"color\":\"gold\"},{\"text\":\"our \",\"color\":\"dark_red\"},{\"text\":\"existence.\",\"color\":\"gold\"}]");
			List<Player> players = PlayerUtils.playersInRange(mCenter, mRange);
			if (players.size() == 0) {
				return; 
			}
			for (Player player : PlayerUtils.playersInRange(mCenter, mRange)) {
				if (!mWarnedPlayers.contains(player)) {
					mWarnedPlayers.add(player);
					player.sendMessage(ChatColor.AQUA + "Seems like the Horseman threw a bomb to the center of the arena. Maybe you can disarm it?");
				}
			}
			
			new BukkitRunnable() {
				double mN = 0;
				double mPlayerScalingHP = 0;
				@Override
				public void run() {
					if (fallingBlock.isOnGround() || !fallingBlock.isValid()) {
						fallingBlock.remove();
						fallingBlock.getLocation().getBlock().setType(Material.AIR);
						
						List<Player> players = PlayerUtils.playersInRange(mCenter, mRange);
						if (players.size() == 0) {
						      return; 
						}
						
						int playerCount = players.size();
						for (int i = 1; i <= playerCount; i++) {
							mN = mN + (150 / (Math.log(i) / Math.log(2)));
						}
						mPlayerScalingHP = mN;
						if (mPlayerScalingHP > 600) {
							mPlayerScalingHP = 600;
						}
						String summonNbt = "{CustomName:\"{\\\"text\\\":\\\"World Ender\\\"}\",Health:" + Double.toString(mPlayerScalingHP) + "f,Attributes:[{Base:" + Double.toString(mPlayerScalingHP) + "d,Name:\"generic.maxHealth\"}],Size:1}";
						EntityUtils.summonEntityAt(mCenter, EntityType.MAGMA_CUBE, summonNbt);
						LivingEntity nuke = null;
						for (Entity e : mCenter.getWorld().getNearbyEntities(mCenter, 0.4, 0.4, 0.4)) {
							if (e instanceof LivingEntity && !(e instanceof Player) && e instanceof MagmaCube && !mSummoned.contains(e.getUniqueId())) {
								nuke = (LivingEntity) e;
								break;
							}
						}
						if (!mSummoned.contains(nuke.getUniqueId())) {
							mSummoned.add(nuke.getUniqueId());
						}
						bomb(nuke, mPlayerScalingHP);
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		} catch (Exception e) {
			mPlugin.getLogger().warning("Failed to summon nuke for Reaper Of Life: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void bomb(LivingEntity z, double mPSHP) {
		z.setAI(false);
		z.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(mPSHP);
		z.setHealth(mPSHP);
		new BukkitRunnable() {
			int mInc = 0;
			
			@Override
			public void run() {
				World world = mBoss.getWorld();
				mInc++;
				z.setNoDamageTicks(0);
				if (z.isDead() || !z.isValid()) {
					mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
					this.cancel();
					return;
					
				}
				if (mBoss.isDead() || !mBoss.isValid()) {
					if (!z.isDead()) {
						z.setHealth(0);
					}
					this.cancel();
					return;
				}
				if (mInc % 10 == 0) {
					world.playSound(mCenter, Sound.ENTITY_CREEPER_HURT, 3, 1f);
					world.spawnParticle(Particle.LAVA, z.getLocation(), 30, 0.3, 0.3, 0.3, 1);
				}
				if (mInc % 20 == 0) {
					world.spawnParticle(Particle.LAVA, z.getLocation(), 30, 0.3, 0.3, 0.3, 1);
				}
				if (mInc >= 20 * 7) {
					z.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 3, 1));
				}
				if (mInc >= 20 * 10) {
					mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
					z.setHealth(0);
					world.playSound(mCenter, Sound.ENTITY_WITHER_SPAWN, 3, 1f);
					world.playSound(mCenter, Sound.ENTITY_GENERIC_EXPLODE, 3, 1f);
					world.spawnParticle(Particle.EXPLOSION_NORMAL, mCenter, 250, 21, 0.3, 21, 0.1);
					for (Player player : PlayerUtils.playersInRange(mCenter, mRange)) {
						if (mCenter.distance(player.getLocation()) < HeadlessHorsemanBoss.detectionRange) {
							int mNDT = player.getNoDamageTicks();
							player.setNoDamageTicks(0);
							BossUtils.bossDamagePercent(mBoss, player, 0.85, (Location)null);
							player.setFireTicks(20 * 3);
							player.setNoDamageTicks(mNDT);
							player.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 20 * 10, 4));
							player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 10, 2));
						}
					}
				}
			}
			
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int duration() {
		return 20 * 15;
	}

}
