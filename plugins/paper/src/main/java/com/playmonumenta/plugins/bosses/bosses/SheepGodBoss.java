package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;

public class SheepGodBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_sheepgod";
	public static final int detectionRange = 30;

	private final Location mSpawnLoc;
	private final Location mEndLoc;

	private boolean mPhase2;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new SheepGodBoss(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public SheepGodBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mPhase2 = false;
		boss.setRemoveWhenFarAway(false);
		constructBoss(null, null, 20, null);
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		if (mPhase2) {
			event.setDamage(event.getDamage() * 15);
		}
	}

	@Override
	public void death(EntityDeathEvent event) {
		World world = mBoss.getWorld();
		mBoss.setHealth(800);
		changePhase(null, null, null);
		mBoss.setAI(false);
		mBoss.setInvulnerable(true);
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"BAAA! BAA BAAAA! BAA BAAA BAAA BAAAAAA!?!?!?\",\"color\":\"dark_red\"}]");
		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				mT++;
				Location loc = mBoss.getLocation().add(FastUtils.randomDoubleInRange(-10, 10), FastUtils.randomDoubleInRange(0, 3), FastUtils.randomDoubleInRange(-10, 10));
				world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0, 0, 0, 0.175);
				world.spawnParticle(Particle.FLAME, loc, 100, 0, 0, 0, 0.175);
				world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1f);

				if (mT >= 12) {
					PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"BAAAAAAAAAAAAAAAAAAAAAAAAAAAA!!!!!!!\",\"color\":\"dark_red\"}]");
					world.playSound(mBoss.getLocation(), Sound.ENTITY_SHEEP_DEATH, 1.5f, 0.75f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_SHEEP_DEATH, 1.5f, 1f);
					mBoss.remove();
					this.cancel();
					world.spawnParticle(Particle.FLAME, mBoss.getLocation(), 300, 0, 0, 0, 0.2);
					world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 150, 0, 0, 0, 0.25);
					world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 150, 0, 0, 0, 0.25);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 1.5f, 0f);
					for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
						player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
						player.removePotionEffect(PotionEffectType.ABSORPTION);
						if (player.hasPotionEffect(PotionEffectType.SPEED)) {
							PotionEffect effect = player.getPotionEffect(PotionEffectType.SPEED);
							if (effect.getAmplifier() > 1) {
								player.removePotionEffect(PotionEffectType.SPEED);
							}
						}
					}
					new BukkitRunnable() {

						@Override
						public void run() {
							mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
							PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:ui.toast.challenge_complete master @s ~ ~ ~ 100 1.15");
							PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"VICTORY\",\"color\":\"green\",\"bold\":true}]");
							PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"April Clucking Fools\",\"color\":\"dark_green\",\"bold\":true}]");
						}

					}.runTaskLater(mPlugin, 20 * 3);

				}
			}

		}.runTaskTimer(mPlugin, 0, 5);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		int hpDelta = 9001;
		int armor = (int)(Math.sqrt(playerCount * 2) - 1);
		while (playerCount > 0) {
			bossTargetHp = bossTargetHp + hpDelta;
			hpDelta = (hpDelta / 2) + 25;
			playerCount--;
		}
		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);
		mBoss.setHealth(bossTargetHp);
		mBoss.setCustomName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "The Sheep God");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"BAA BAA!!! BAA BAAAA, BAA BAAAAA!?!?\",\"color\":\"dark_red\"}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"The Sheep God\",\"color\":\"dark_red\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"Master of the Hundred Wools\",\"color\":\"red\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 1.25");
	}
}
