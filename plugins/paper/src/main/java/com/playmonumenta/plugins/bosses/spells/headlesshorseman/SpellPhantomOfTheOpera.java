package com.playmonumenta.plugins.bosses.spells.headlesshorseman;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Phantom;
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
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 *Phantom of the Opera (Idfk) - PASSIVE: chooses random players and spawns phantoms 5 blocks above 
 *with delayed spawning for 1 second.
 */
public class SpellPhantomOfTheOpera extends Spell {

	private int mCooldown = 0;
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mCenter;
	private double mRange;
	private int mTimer;
	private Set<UUID> mSummoned = new HashSet<UUID>();
	private static final String mPhantom = "{CustomName:\"\\\"Night Terror\\\"\",Health:45.0f,Size:6,Attributes:[{Base:45.0d,Name:\"generic.maxHealth\"},{Base:100.0d,Name:\"generic.followRange\"}],Tags:[\"boss_charger\"],HandItems:[{id:\"minecraft:arrow\",Count:1b,tag:{display:{Name:\"\\\"Phantom Fang\\\"\"},AttributeModifiers:[{UUIDMost:-6498451240576266699L,UUIDLeast:-6858106810304442920L,Amount:12.0d,Slot:\"mainhand\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"}]}},{}]}";

	public SpellPhantomOfTheOpera(Plugin plugin, LivingEntity entity, Location center, int range, int timer) {
		mPlugin = plugin;
		mBoss = entity;
		mCenter = center;
		mRange = range;
		mTimer = timer;

	}
	
	@Override
	public void run() {
		mCooldown -= 5;
		if (mCooldown <= 0) {
			mCooldown = mTimer;
			World world = mBoss.getWorld();
			int num = 0;
			
			//set amount of phantoms spawn
			List<Player> players = PlayerUtils.playersInRange(mCenter, mRange);
			if (players.size() == 0) {
				return; 
			}
			if (players.size() <= 2) {
				num = 2;
			} else {
				num = (int)Math.ceil(players.size() / 3 + 1);
			}
			
			if (num >= 4) {
				num = 4;
			}
			int amt = num;
			
			if (mBoss.isDead() || !mBoss.isValid()) {
				this.cancel();
				return;
			}
			
			world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 3, 0.75f);
			//choose random players until amt
			for (int i = 0; i < amt; i++) {
				Collections.shuffle(players);
				Player player = players.get(0); 
				
				//spawn SMOKE_LARGE particles moving up at chosen player for 5 blocks in 1 second
				Location pLoc = player.getLocation();
				
				new BukkitRunnable() {
					int mInc = 0;
					double mN = 0;
					double mPlayerScalingHP = 0;
					@Override
					public void run() {
						mInc++;
						int y = mInc / 4;
						Location particle = pLoc.clone().add(0, y, 0);
						world.spawnParticle(Particle.SMOKE_LARGE, particle, 4, 0.3, 0, 0.3, 0.1);
						
						//stop particle spawns + spawn phantoms after 1.5 second
						if (mInc >= 30) {
							this.cancel();
							Location sLoc = pLoc.clone().add(0, 7.5, 0);
							world.playSound(sLoc, Sound.ENTITY_WITHER_HURT, 3, 0.75f);
							world.spawnParticle(Particle.EXPLOSION_NORMAL, sLoc, 20, 0.3, 0.3, 0.3, 0.1);
							EntityUtils.summonEntityAt(sLoc, EntityType.PHANTOM, mPhantom);
							
							List<Player> players = PlayerUtils.playersInRange(mCenter, mRange);
							if (players.size() == 0) {
							      return; 
							}
							
							int playerCount = players.size();
							for (int j = 1; j <= playerCount; j++) {
								mN = mN + (45 / j);
							}
							mPlayerScalingHP = mN;
							
							LivingEntity mEntity = null;
							for (Entity e : mCenter.getWorld().getNearbyEntities(sLoc, 0.4, 0.4, 0.4)) {
								if (e instanceof LivingEntity && !(e instanceof Player) && e instanceof Phantom && !mSummoned.contains(e.getUniqueId())) {
									mEntity = (LivingEntity) e;
									break;
								}
							}
							if (!mSummoned.contains(mEntity.getUniqueId())) {
								mSummoned.add(mEntity.getUniqueId());
							}
							mEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(mPlayerScalingHP);
							mEntity.setHealth(mPlayerScalingHP);
						}
					}
					
				}.runTaskTimer(mPlugin, 0, 1);
			}
		}
	}
	
	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return 0;
	}

}
