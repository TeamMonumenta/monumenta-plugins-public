package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLaser;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellGenericCharge;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.SpellTpSwapPlaces;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public final class Varcosa extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_varcosa";
	public static final int detectionRange = 110;

	private double mCoef;

	public Varcosa(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		boss.setRemoveWhenFarAway(false);

		boss.addScoreboardTag("Boss");

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellGenericCharge(plugin, boss, detectionRange, 15.0F),
			new SpellTpSwapPlaces(plugin, boss, 5),
			new SpellBaseLaser(plugin, boss, detectionRange, 100, false, false, 160,

				// Tick action per player
				(LivingEntity player, int ticks, boolean blocked) -> {
					player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 80f) * 1.5f);
					boss.getLocation().getWorld().playSound(boss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 80f) * 1.5f);

					if (ticks == 0) {
						boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 110, 4));
					}
				},

				// Particles generated by the laser
				(Location loc) -> {
					new PartialParticle(Particle.CLOUD, loc, 1, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(boss);
					new PartialParticle(Particle.FLAME, loc, 1, 0.04, 0.04, 0.04, 1).spawnAsEntityActive(boss);
				},

				// Damage generated at the end of the attack
				(LivingEntity target, Location loc, boolean blocked) -> {
					loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.HOSTILE, 1f, 1.5f);
					new PartialParticle(Particle.FIREWORKS_SPARK, loc, 300, 0.8, 0.8, 0.8, 0).spawnAsEntityActive(boss);

					if (!blocked) {
						BossUtils.blockableDamage(boss, target, DamageType.MAGIC, 30);
						// Shields don't stop fire!
						EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), 4 * 20, target, boss);
					}
				})));

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBlockBreak(boss),
			new SpellShieldStun(6 * 20),
			// Teleport the boss to spawnLoc if he is stuck in bedrock
			new SpellConditionalTeleport(boss, spawnLoc, b -> b.getLocation().getBlock().getType() == Material.BEDROCK ||
				                                                  b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK ||
				                                                  b.getLocation().getBlock().getType() == Material.LAVA)
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(100, (mob) -> {
			sendMessage("Yarharhar! Thank ye fer comin' and seein' me, but now this will be ye grave as well!");
		});
		events.put(50, (mob) -> {
			sendMessage("I will hang ye out to dry!");
		});
		events.put(25, (mob) -> {
			sendMessage("Yarharhar! Do ye feel it as well? That holy fleece? It be waitin' fer me!");
		});
		events.put(10, (mob) -> {
			sendMessage("I be too close ter be stoppin' now! Me greed will never die!");
		});
		BossBarManager bossBar = new BossBarManager(boss, detectionRange, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10, events);

		super.constructBoss(activeSpells, passiveSpells, detectionRange, bossBar);

		new BukkitRunnable() {

			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
				int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
				mCoef = BossUtils.healthScalingCoef(playerCount, 0.5, 0.5);
			}
		}.runTaskTimer(mPlugin, 0, 100);
	}

	@Override
	public void init() {
		int bossTargetHp = 1650;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		mCoef = BossUtils.healthScalingCoef(playerCount, 0.5, 0.5);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, bossTargetHp);
		mBoss.setHealth(bossTargetHp);

		for (Player player : getPlayers()) {
			MessagingUtils.sendBoldTitle(player, Component.text("Captain Varcosa", NamedTextColor.DARK_PURPLE), Component.text("The Legendary Pirate King", NamedTextColor.RED));
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, false, true, true));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.7f);
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		for (Player player : getPlayers()) {
			player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.HOSTILE, 100.0f, 0.8f);
			sendMessage("Ye thought I be the one in control here? Yarharhar! N'argh me lad, I merely be its pawn! But now me soul can rest, and ye will be its next meal! Yarharhar!", player);
		}
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	//Reduce damage taken for each player by a percent
	@Override
	public void onHurt(DamageEvent event) {
		event.setFlatDamage(event.getFlatDamage() / mCoef);
	}

	private List<Player> getPlayers() {
		return PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
	}

	private void sendMessage(String message) {
		for (Player player : getPlayers()) {
			sendMessage(message, player);
		}
	}

	private void sendMessage(String message, Player player) {
		com.playmonumenta.scriptedquests.utils.MessagingUtils.sendNPCMessage(player, "Captain Varcosa", message);
	}
}
