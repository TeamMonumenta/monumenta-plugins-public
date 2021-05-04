package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.headlesshorseman.SpellBatBombs;
import com.playmonumenta.plugins.bosses.spells.headlesshorseman.SpellBurningVengence;
import com.playmonumenta.plugins.bosses.spells.headlesshorseman.SpellHallowsEnd;
import com.playmonumenta.plugins.bosses.spells.headlesshorseman.SpellHellzoneGrenade;
import com.playmonumenta.plugins.bosses.spells.headlesshorseman.SpellPhantomOfTheOpera;
import com.playmonumenta.plugins.bosses.spells.headlesshorseman.SpellReaperOfLife;
import com.playmonumenta.plugins.bosses.spells.headlesshorseman.SpellSinisterReach;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;

/*
 * Barrier of Flames - (Hard mode only) When the boss enters phase 2 he gains a shield of
 * flames around him that prevents him from taking damage. This barrier can be broken if it
 * is either blown up with TnT or blown up with the Horseman’s Explosive attack. (A Warning
 * is given to players the first time they try to deal damage while the boss has the barrier up)

Phase 1
Hellzone Grenade - The horseman fires a pumpkin (Fireball with pumpkin block maybe) that
explodes on contact with the ground or a player, dealing NM20/HM35 damage in a 3 block radius.
Enemies hit are ignited for 3 seconds. The area affected leaves behind a lingering AoE if it hits
the ground, this lingering fire is also 3 blocks in radius and deals 5% max health damage every 0.5
seconds to players and ignites them for 3 seconds. (Does not leave the lingering thing behind if
it hits a player or the horseman)

Bat Bomb - The Horseman summons 20* bats around himself to distract his enemies. After 3 seconds
the bats explode dealing 18/25 damage to anyone with 4 blocks of them, killing the bat in the process.
The bats have 20hp.

Sinister Reach - The horseman pauses momentarily for 0.8 seconds, afterwards the swing forward
targeting the player who has his aggro using the shadows to extend his reach. Each player in a
60 degree cone in front of them 8 blocks in length takes 20/32 damage, given slowness 3 and rooted
 for 5 seconds.

Burning Vengeance - The horseman after 0.5 seconds summons a ring of flames at the edge of a 16 block
radius circle that travels inwards towards the boss, this ring travels over 1 block tall blocks.
Players hit by the ring take 10/20 damage, ignited for 5 seconds, and knocked towards the boss. Players
can be hit multiple times. After the ring reaches the horseman the fire erupts, dealing 20/30 damage
in a 5 block radius and knocking them away from the boss.


Phase 2 (Skills ADDED in phase 2. All the others are kept)
Phantom of the Opera (Idfk) - Fires out black projectiles at anyone within 12 blocks of himself after
a 0.8 seconds delay. Players hit by these projectiles are blinded for 5 seconds, dealt 16/28 damage
and 2 phantoms are summoned above the player with their aggro set to them.

On the Hunt - The Horseman begins marking the player that currently has aggro to be the target of
his hunt (They are warned he is doing so). After 1.5s, For the next 5 seconds the horseman has his
aggro permanently set to that enemy, gaining a burst of speed to run down his mark. If the player is
 dealt damage by The Horseman they are skewered through the chest, taking 28/42 damage and being given
 antiheal 5 for the next 7 seconds and wither 2 for 5 seconds. (Normal mode) In hard mode the speed is
 higher and the charge-up is reduced to 1 seconds.

(Ultimate) Hallow’s End - A pillar of smoke and flames appears on the horseman, after 1 second the area
nearby explodes, dealing 20/35 damage to players in a 4 block radius, igniting them for 8 seconds and
launching them upwards greatly. Afterwards pillars appear underneath ⅓ of the players within 32 blocks of
the horseman, after 1 second they also explode dealing the same thing. This continues to repeat as long as a
player is dealt damage by the pillars explosion to a max of 8 casts of the skill. (In hard mode players
it are also blinded for 5 seconds.)

 */

public class HeadlessHorsemanBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_horseman";
	public static final int detectionRange = 22;

	private final Location mSpawnLoc;
	private final Location mEndLoc;

	public boolean mShieldsUp;
	private boolean mCooldown = false;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new HeadlessHorsemanBoss(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public HeadlessHorsemanBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mShieldsUp = false;
		mBoss.setRemoveWhenFarAway(false);

		SpellManager phase1Spells = new SpellManager(Arrays.asList(
				new SpellHellzoneGrenade(plugin, boss, mSpawnLoc, detectionRange, this),
				new SpellBatBombs(plugin, boss, this),
				new SpellSinisterReach(plugin, boss, this),
				new SpellBurningVengence(plugin, boss, this)
				));

		SpellManager phase2Spells = new SpellManager(Arrays.asList(
				new SpellHellzoneGrenade(plugin, boss, mSpawnLoc, detectionRange, this),
				new SpellBatBombs(plugin, boss, this),
				new SpellSinisterReach(plugin, boss, this),
				new SpellBurningVengence(plugin, boss, this),
				new SpellHallowsEnd(plugin, boss, this),
				new SpellReaperOfLife(plugin, boss, mSpawnLoc, detectionRange)
				));

		List<Spell> phase1Passives = Arrays.asList(
				// Teleport the boss to spawnLoc if he gets too far away from where he spawned
				new SpellConditionalTeleport(mBoss, spawnLoc, b -> spawnLoc.distance(b.getLocation()) > 80),
				// Teleport the boss to spawnLoc if he is stuck in bedrock
				new SpellConditionalTeleport(mBoss, spawnLoc, b -> b.getLocation().getBlock().getType() == Material.BEDROCK ||
				                                                   b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK ||
				                                                   b.getLocation().getBlock().getType() == Material.LAVA),
				new SpellPhantomOfTheOpera(plugin, boss, mSpawnLoc, detectionRange, 20 * 60)
			);

		List<Spell> phase2Passives = Arrays.asList(
				// Teleport the boss to spawnLoc if he gets too far away from where he spawned
				new SpellConditionalTeleport(mBoss, spawnLoc, b -> spawnLoc.distance(b.getLocation()) > 80),
				// Teleport the boss to spawnLoc if he is stuck in bedrock
				new SpellConditionalTeleport(mBoss, spawnLoc, b -> b.getLocation().getBlock().getType() == Material.BEDROCK ||
				                                                   b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK ||
				                                                   b.getLocation().getBlock().getType() == Material.LAVA),
				new SpellPhantomOfTheOpera(plugin, boss, mSpawnLoc, detectionRange, 20 * 60)
			);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();
		events.put(100, mBoss -> {
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[The Horseman] \",\"color\": \"dark_red\"},{\"text\":\"You? You're who they sent? Oh ho ho, \",\"color\":\"gold\"},{\"text\":\"we're \",\"color\":\"dark_red\"},{\"text\":\"going to have some fun.\",\"color\":\"gold\"}]");
		});

		events.put(50, mBoss -> {
			mShieldsUp = true;
			new BukkitRunnable() {
				int mTicks = 0;
				World mWorld = mBoss.getWorld();
				@Override
				public void run() {

					if (mBoss.isDead() || !mBoss.isValid()) {
						this.cancel();
						return;
					}

					if (!mShieldsUp) {
						mTicks = 0;
						return;
					}

					mTicks++;

					mWorld.spawnParticle(Particle.FLAME, mBoss.getLocation().add(0, 1.5, 0), 8, 0.4, 0.4, 0.4, 0.025);
					mWorld.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1.5, 0), 5, 0.4, 0.4, 0.4, 0.05);
					mWorld.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1.5, 0), 1, 0.4, 0.4, 0.4, 0.025);

					if (mTicks % 20 == 0) {
						for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 4)) {
							BossUtils.bossDamagePercent(boss, player, 0.075);
							player.setFireTicks(20 * 5);
						}
					}

				}

			}.runTaskTimer(mPlugin, 0, 1);
			changePhase(phase2Spells, phase2Passives, null);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[The Horseman] \",\"color\":\"dark_red\"},{\"text\":\"Ha ha ha! I haven't felt this alive for what feels like eternity! \",\"color\":\"gold\"},{\"text\":\"We'll \",\"color\":\"dark_red\"},{\"text\":\"have to go all out.\",\"color\":\"gold\"}]");
		});

		events.put(10, mBoss -> {
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[The Horseman] \",\"color\":\"dark_red\"},{\"text\":\"Meet your hallow end mortal!\",\"color\":\"gold\"}]");
			forceCastSpell(SpellReaperOfLife.class);
		});

		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange*2, BarColor.RED, BarStyle.SEGMENTED_10, events);

		super.constructBoss(phase1Spells, phase1Passives, detectionRange, bossBar);
	}

	public Location getSpawnLocation() {
		return mSpawnLoc;
	}

	private boolean mAggro = true;

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		if (mAggro) {
			mAggro = false;
			new BukkitRunnable() {

				@Override
				public void run() {
					mAggro = true;
				}

			}.runTaskLater(mPlugin, 20 * 6);

			if (event.getDamager() instanceof Player) {
				Player player = (Player) event.getDamager();

				((Creature) mBoss).setTarget(player);
			} else if (event.getDamager() instanceof Projectile) {
				Projectile proj = (Projectile) event.getDamager();
				if (proj.getShooter() instanceof Player) {
					Player player = (Player) proj.getShooter();

					((Creature) mBoss).setTarget(player);
				}
			}
		}
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK && event.getEntity().getLocation().distance(mBoss.getLocation()) <= 2) {
			if (!mCooldown) {
				mCooldown = true;
				new BukkitRunnable() {
					@Override
					public void run() {
						mCooldown = false;
					}
				}.runTaskLater(mPlugin, 20);
				UUID uuid = event.getEntity().getUniqueId();
				for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 4)) {
					if (!player.getUniqueId().equals(uuid)) {
						BossUtils.bossDamage(mBoss, player, event.getDamage());
					}
				}
				World world = mBoss.getWorld();
				world.spawnParticle(Particle.DAMAGE_INDICATOR, mBoss.getLocation(), 30, 2, 2, 2, 0.1);
				world.spawnParticle(Particle.SWEEP_ATTACK, mBoss.getLocation(), 10, 2, 2, 2, 0.1);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0);
			}
		}

		if (event.getEntity().getLocation().distance(mBoss.getLocation()) < 2.5) {
			List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, detectionRange);
			if (players.contains(event.getEntity())) {
				players.remove(event.getEntity());
			}

			double dist = 100;
			Player target = null;
			for (Player p : players) {
				if (p.getLocation().distance(mBoss.getLocation()) < dist) {
					target = p;
					dist = p.getLocation().distance(mBoss.getLocation());
				}
			}

			if (target != null) {
				((Creature) mBoss).setTarget(target);
			}
		}

		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			if (player.isBlocking()) {
				player.setCooldown(Material.SHIELD, 20 * 30);
			}
		}
	}

	public void disableShield() {
		if (mShieldsUp) {
			World world = mBoss.getWorld();
			world.spawnParticle(Particle.FLAME, mBoss.getLocation().add(0, 1.5, 0), 35, 0.4, 0.4, 0.4, 0.075);
			world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1.5, 0), 18, 0.4, 0.4, 0.4, 0.065);
			world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1.5, 0), 10, 0.4, 0.4, 0.4, 0.025);
			world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_DEATH, 2, 0.25f);
			world.playSound(mBoss.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 2, 0f);
			mShieldsUp = false;

			new BukkitRunnable() {

				@Override
				public void run() {
					if (mBoss.isDead() || !mBoss.isValid()) {
						this.cancel();
						return;
					}
					world.spawnParticle(Particle.FLAME, mBoss.getLocation().add(0, 1.5, 0), 35, 0.4, 0.4, 0.4, 0.075);
					world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1.5, 0), 18, 0.4, 0.4, 0.4, 0.065);
					world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1.5, 0), 10, 0.4, 0.4, 0.4, 0.025);
					world.playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 2, 0.85f);
					world.playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2, 1f);
					mShieldsUp = true;
				}

			}.runTaskLater(mPlugin, 20 * 8);
		}
	}

	public LivingEntity getEntity() {
		return mBoss;
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mSpawnLoc, detectionRange);
		int hpDelta = 2500;
		int armor = (int)(Math.sqrt(playerCount * 2) - 1);
		while (playerCount > 0) {
			bossTargetHp = bossTargetHp + hpDelta;
			hpDelta = (int)Math.floor(hpDelta / 1.5 + 32);
			playerCount--;
		}
		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.setHealth(bossTargetHp);

		//launch event related spawn commands
		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "effect give @s minecraft:blindness 2 2");
		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "title @s title [\"\",{\"text\":\"Headless Horseman\",\"color\":\"dark_red\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "title @s subtitle [\"\",{\"text\":\"Scourge of the Isles\",\"color\":\"red\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 0.7");
	}

	@Override
	public void death(EntityDeathEvent event) {
		for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
			player.sendMessage(ChatColor.DARK_RED + "[The Horseman] No matter. I'll be seeing you all again soon.");
			player.playSound(player.getLocation(), Sound.ENTITY_HORSE_DEATH, SoundCategory.MASTER, 1.0f, 0.1f);
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 2));
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 4));
		}
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}
}
