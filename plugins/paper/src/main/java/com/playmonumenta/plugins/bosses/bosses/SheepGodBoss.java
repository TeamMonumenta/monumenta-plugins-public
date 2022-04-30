package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.Collections;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

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
		constructBoss(SpellManager.EMPTY, Collections.emptyList(), 20, null);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (mPhase2) {
			event.setDamage(event.getDamage() * 15);
		}
	}

	@Override
	public void death(EntityDeathEvent event) {
		World world = mBoss.getWorld();
		mBoss.setHealth(800);
		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
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
							if (effect != null && effect.getAmplifier() > 1) {
								player.removePotionEffect(PotionEffectType.SPEED);
							}
						}
					}
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
						for (Player p : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
							MessagingUtils.sendBoldTitle(p, ChatColor.GOLD + "VICTORY", ChatColor.DARK_GREEN + "April Clucking Fools");
							p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 100, 1.15f);
						}
					}, 20 * 3);
				}
			}

		}.runTaskTimer(mPlugin, 0, 5);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		int hpDelta = 9001;
		while (playerCount > 0) {
			bossTargetHp = bossTargetHp + hpDelta;
			hpDelta = (hpDelta / 2) + 25;
			playerCount--;
		}
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, bossTargetHp);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
		mBoss.setHealth(bossTargetHp);
		mBoss.setCustomName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "The Sheep God");
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			MessagingUtils.sendBoldTitle(player, ChatColor.DARK_RED + "The Sheep God", ChatColor.RED + "Master of the Hundred Wools");
			player.sendMessage(Component.text("BAA BAA!!! BAA BAAA, BAA BAAAAA!?!?", NamedTextColor.DARK_RED));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 10, 1.25f);
		}
	}
}
