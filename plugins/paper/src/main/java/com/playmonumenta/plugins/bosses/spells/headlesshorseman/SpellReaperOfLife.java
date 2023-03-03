package com.playmonumenta.plugins.bosses.spells.headlesshorseman;

import com.playmonumenta.plugins.bosses.bosses.HeadlessHorsemanBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class SpellReaperOfLife extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final double mRange;
	private final Location mCenter;
	private final Set<UUID> mSummoned = new HashSet<>();
	private final Set<Player> mWarnedPlayers = new HashSet<>();
	private final int mCooldownTicks;

	public SpellReaperOfLife(Plugin plugin, LivingEntity entity, Location center, double range, int cooldown) {
		mPlugin = plugin;
		mBoss = entity;
		mRange = range;
		mCenter = center;
		mCooldownTicks = cooldown;
	}

	@Override
	public boolean canRun() {
		for (LivingEntity entity : mCenter.getNearbyLivingEntities(30)) {
			//If there exists a magma cube currently alive in the fight, return and do not run this spell.
			if (entity.getType() == EntityType.MAGMA_CUBE) {
				return false;
			}
		}
		return true;
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
			Vector vector = new Vector(pLoc.getX() - tLoc.getX(), 0, pLoc.getZ() - tLoc.getZ());
			vector.normalize().multiply(pLoc.distance(tLoc) / 25).setY(0.7f);
			fallingBlock.setVelocity(vector);

			world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_DEATH, SoundCategory.HOSTILE, 3, 1.5f);
			new PartialParticle(Particle.FLAME, fallingBlock.getLocation().add(0, fallingBlock.getHeight() / 2, 0), 3, 0.25, .25, .25, 0.025).spawnAsEntityActive(mBoss);
			new PartialParticle(Particle.SMOKE_NORMAL, fallingBlock.getLocation().add(0, fallingBlock.getHeight() / 2, 0), 2, 0.25, .25, .25, 0.025).spawnAsEntityActive(mBoss);
			PlayerUtils.executeCommandOnNearbyPlayers(mCenter, range, "tellraw @s [{\"text\":\"[The Horseman] \",\"color\":\"dark_red\",\"bold\":\"false\",\"italic\":\"false\"},{\"text\":\"May your life force fuel \",\"color\":\"gold\"},{\"text\":\"our \",\"color\":\"dark_red\"},{\"text\":\"existence.\",\"color\":\"gold\"}]");
			List<Player> players = PlayerUtils.playersInRange(mCenter, HeadlessHorsemanBoss.arenaSize, true);
			if (players.size() != 0) {
				for (Player player : players) {
					if (!mWarnedPlayers.contains(player)) {
						mWarnedPlayers.add(player);
						player.sendMessage(ChatColor.AQUA + "Seems like the Horseman threw a bomb to the center of the arena. Maybe you can disarm it?");
					}
				}
			}


			new BukkitRunnable() {
				double mTempPlayerScalingHP = 0;
				double mPlayerScalingHP = 0;

				@Override
				public void run() {
					if (fallingBlock.isOnGround() || !fallingBlock.isValid()) {
						fallingBlock.remove();
						fallingBlock.getLocation().getBlock().setType(Material.AIR);

						List<Player> players = PlayerUtils.playersInRange(mCenter, mRange, true);
						if (players.size() == 0) {
							return;
						}

						int playerCount = players.size();
						for (int i = 1; i <= playerCount; i++) {
							mTempPlayerScalingHP = mTempPlayerScalingHP + (150 / (Math.log(i + 1) / Math.log(2)));
						}
						mPlayerScalingHP = mTempPlayerScalingHP;
						if (mPlayerScalingHP > 1000) {
							mPlayerScalingHP = 1000;
						}
						LivingEntity nuke = (LivingEntity) LibraryOfSoulsIntegration.summon(mCenter, "WorldEnder");
						if (nuke != null) {
							EntityUtils.setAttributeBase(nuke, Attribute.GENERIC_MAX_HEALTH, mPlayerScalingHP);
							nuke.setHealth(mPlayerScalingHP);
							mSummoned.add(nuke.getUniqueId());
							bomb(nuke, mPlayerScalingHP);
						}
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		} catch (Exception e) {
			mPlugin.getLogger().warning("Failed to summon nuke for Reaper Of Life: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void bomb(LivingEntity nuke, double playerScalingHP) {
		nuke.setAI(false);
		EntityUtils.setAttributeBase(nuke, Attribute.GENERIC_MAX_HEALTH, playerScalingHP);
		nuke.setHealth(playerScalingHP);
		new BukkitRunnable() {
			int mInc = 0;

			@Override
			public void run() {
				World world = mBoss.getWorld();
				mInc++;
				nuke.setNoDamageTicks(0);
				if (nuke.isDead() || !nuke.isValid()) {
					mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
					this.cancel();
					return;

				}
				if (mBoss.isDead() || !mBoss.isValid()) {
					if (!nuke.isDead()) {
						nuke.setHealth(0);
					}
					this.cancel();
					return;
				}
				if (mInc % 10 == 0) {
					world.playSound(mCenter, Sound.ENTITY_CREEPER_HURT, SoundCategory.HOSTILE, 3, 1f);
					new PartialParticle(Particle.LAVA, nuke.getLocation(), 30, 0.3, 0.3, 0.3, 1).spawnAsEntityActive(mBoss);
				}
				if (mInc % 20 == 0) {
					new PartialParticle(Particle.LAVA, nuke.getLocation(), 30, 0.3, 0.3, 0.3, 1).spawnAsEntityActive(mBoss);
				}
				if (mInc >= 20 * 7) {
					nuke.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 3, 1));
				}
				if (mInc >= 20 * 10) {
					mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
					nuke.setHealth(0);
					world.playSound(mCenter, Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 3, 1f);
					world.playSound(mCenter, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3, 1f);
					new PartialParticle(Particle.EXPLOSION_NORMAL, mCenter, 250, 21, 0.3, 21, 0.1).spawnAsEntityActive(mBoss);
					for (Player player : PlayerUtils.playersInRange(mCenter, mRange, true)) {
						if (mCenter.distance(player.getLocation()) < mRange) {
							BossUtils.bossDamagePercent(mBoss, player, 0.85, "Reaper of Life");
							EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), 20 * 3, player, mBoss);
							player.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 20 * 10, 4));
							player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 10, 2));
						}
					}
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks + (5 * 20);
	}

}
