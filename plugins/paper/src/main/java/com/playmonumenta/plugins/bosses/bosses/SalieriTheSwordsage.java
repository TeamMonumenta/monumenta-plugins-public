package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.salieriswordsage.BackHit;
import com.playmonumenta.plugins.bosses.spells.salieriswordsage.CloseTheDistance;
import com.playmonumenta.plugins.bosses.spells.salieriswordsage.CounterHit;
import com.playmonumenta.plugins.bosses.spells.salieriswordsage.EvasiveBladeDance;
import com.playmonumenta.plugins.bosses.spells.salieriswordsage.Taunt;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.Arrays;
import java.util.Collections;
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
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SalieriTheSwordsage extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_swordsage";
	public static final int detectionRange = 75;

	private static final int BASE_HEALTH = 1750;
	private static final int DAMAGE_COUNTER = 30;
	private static final int DAMAGE_BLADEDANCE = 28;
	private static final int DAMAGE_LEAP = 30;
	private static final int DAMAGE_PARRY = 40;
	private static final int PARRY_DURATION = 20 * 3;
	private static final double BACKHIT_MULTIPLIER = 1.5;
	private static final int SHIELD_STUN = 20 * 6;

	public boolean mSpellActive = false;
	private final Taunt mTaunt;

	public SalieriTheSwordsage(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);

		mTaunt = new Taunt(plugin, boss, this, DAMAGE_PARRY, PARRY_DURATION);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new EvasiveBladeDance(plugin, boss, this, DAMAGE_BLADEDANCE),
			mTaunt
			)
		);

		List<Spell> passiveSpells = Arrays.asList(
			new BackHit(plugin, boss, BACKHIT_MULTIPLIER),
			new CounterHit(plugin, boss, DAMAGE_COUNTER, this),
			new CloseTheDistance(plugin, boss, DAMAGE_LEAP, this),
			new SpellShieldStun(SHIELD_STUN)
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();

		// Taunt after enough time has passed
		new BukkitRunnable() {
			private int mTicks = 0;

			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					return;
				}

				mTicks += 2;

				if (mTicks >= 20 * 30) {
					// Early return if the boss is not on the ground, so that weird behavior with the leap spell doesn't happen.
					if (!mBoss.isOnGround()) {
						return;
					}
					if (getActiveSpells() != null) {
						for (Spell sp : getActiveSpells()) {
							if (sp.isRunning()) {
								sp.cancel();
							}
						}
					}

					mTaunt.activate();
					mTicks = 0;
				}
			}
		}.runTaskTimer(plugin, 20 * 3, 2);

		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 2, 2f);

		mBoss.setAI(false);
		mBoss.setGravity(false);
		mBoss.setInvulnerable(true);
		mBoss.setPersistent(true);

		sendMessage("It all comes down to this, @S. This is a fight we were destined to have from the moment we met.");

		new BukkitRunnable() {
			private int mTicks = 0;

			@Override
			public void run() {
				if (mTicks == 20 * 2) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 2, 2f);
					new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 120).delta(2, 2, 2).extra(0.05).spawnAsBoss();
				} else if (mTicks == 20 * 3) {
					this.cancel();

					mBoss.setAI(true);
					mBoss.setGravity(true);
					mBoss.setInvulnerable(false);

					BossBarManager bossBar = new BossBarManager(boss, detectionRange, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_10, events);
					constructBoss(activeSpells, passiveSpells, detectionRange, bossBar, 20 * 10);

					for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
						com.playmonumenta.plugins.utils.MessagingUtils.sendBoldTitle(player, Component.text("Salieri", NamedTextColor.RED), Component.text("The Legendary Swordsage", NamedTextColor.DARK_RED));
						player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.7f);
					}

					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_VINDICATOR_AMBIENT, 5, 1f);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 5, 0.75f);
				}

				mTicks += 10;
			}
		}.runTaskTimer(plugin, 0, 10);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		cancelAllSpells();

		List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
		BossUtils.endBossFightEffects(mBoss, players, 2 * 20);

		sendMessage("So... this is what The Machine meant by you...");
		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
		teleport(mSpawnLoc);
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);

		mBoss.remove();
	}

	//Teleport with special effects
	private void teleport(Location loc) {
		World world = loc.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70).delta(0.25, 0.45, 0.25).extra(0.15).spawnAsBoss();
		new PartialParticle(Particle.CLOUD, mBoss.getLocation().add(0, 1, 0), 35).delta(0.1, 0.45, 0.1).extra(0.15).spawnAsBoss();
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25).delta(0.2, 0, 0.2).extra(0.1).spawnAsBoss();
		mBoss.teleport(loc);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70).delta(0.25, 0.45, 0.25).extra(0.15).spawnAsBoss();
		new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35).delta(0.1, 0.45, 0.1).extra(0.15).spawnAsBoss();
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25).delta(0.2, 0, 0.2).extra(0.1).spawnAsBoss();
	}

	@Override
	public void init() {
		int playerCount = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true).size();
		double bossTargetHp = BASE_HEALTH * BossUtils.healthScalingCoef(playerCount, 0.5, 0.5);

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, bossTargetHp);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
		mBoss.setHealth(bossTargetHp);

		mBoss.setPersistent(true);
	}

	public void cancelAllSpells() {
		if (getActiveSpells() != null) {
			for (Spell sp : getActiveSpells()) {
				if (sp.isRunning()) {
					sp.cancel();
				}
			}
		}
		if (getPassives() != null) {
			for (Spell sp : getPassives()) {
				if (sp.isRunning()) {
					sp.cancel();
				}
			}
		}
	}

	public void sendMessage(String message) {
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			MessagingUtils.sendNPCMessage(player, "Salieri", message);
		}
	}
}
