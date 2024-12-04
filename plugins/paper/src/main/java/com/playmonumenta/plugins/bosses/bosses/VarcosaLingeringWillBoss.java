package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellPlayerAction;
import com.playmonumenta.plugins.bosses.spells.SpellPurgeNegatives;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.varcosamist.ForcefulGrip;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellActions;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellDeathlyCharge;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellGhostlyCannons;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellJibberJabber;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellPurgeGlowing;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellSummonConstantly;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellSwitcheroo;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public final class VarcosaLingeringWillBoss extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_varcosa_will";
	public static final int detectionRange = 50;
	private Location mCenter;
	private static final String[] mSpeak = {"The cold beyond be takin' me. It'll be takin' ye too...", "The veil be partin'... I won't go... not without me treasure..."};

	public VarcosaLingeringWillBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		mBoss.setRemoveWhenFarAway(false);

		List<String> mSummonableMobs = new ArrayList<>();
		mSummonableMobs.add("SeaWolf");
		mSummonableMobs.add("PirateGunner");
		mSummonableMobs.add("DrownedCrewman");

		for (LivingEntity e : mBoss.getLocation().getNearbyEntitiesByType(ArmorStand.class, detectionRange, detectionRange, detectionRange)) {
			if (e.getScoreboardTags().contains("varcosa_center")) {
				mCenter = e.getLocation();
				break;
			}
		}

		if (mCenter == null) {
			//This should be the same spot as the armor stand - but it is really bad to do things like this, and should only be the fallback
			mCenter = mSpawnLoc.clone().subtract(0, 0.5, 0);
		}

		SpellManager spells = new SpellManager(
			Arrays.asList(
				new SpellDeathlyCharge(mPlugin, mBoss, 20 * 15, "The Mist is callin' ye still. Let it in...!"),
				new SpellGhostlyCannons(mPlugin, mBoss, 22, mCenter, true, "Call down the cannons mateys, right quick!"),
				new SpellSwitcheroo(mPlugin, mBoss, 20 * 10, 30, "A trap be set now. Get ye into it!"),
				new ForcefulGrip(mPlugin, mBoss, 20 * 10, "Yarr, get ye over here! I'll deal with ye myself.") //change later lmao
			));

		//Passive Spells
		SpellPlayerAction action = SpellActions.getTooLowAction(mBoss, mCenter);
		SpellPlayerAction tooHighAction = SpellActions.getTooHighAction(mBoss, mCenter);
		BukkitRunnable runnable = SpellActions.getTeleportEntityRunnable(mBoss, mCenter);

		runnable.runTaskTimer(plugin, 20, 20 * 2);
		List<Spell> passiveSpells = Arrays.asList(
			new SpellSummonConstantly(mSummonableMobs, 20 * 16, 50, 6, 2, mCenter, this),
			new SpellJibberJabber(mBoss, mSpeak, detectionRange),
			new SpellPurgeNegatives(mBoss, 2),
			new SpellPurgeGlowing(mBoss, 20 * 15),
			new SpellBlockBreak(mBoss, 1, 3, 1, 175, false, true, false),
			new SpellShieldStun(6 * 20),
			action, tooHighAction
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(10, mBoss -> forceCastSpell(SpellGhostlyCannons.class));
		BossBarManager bossBar = new BossBarManager(mBoss, detectionRange + 20, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10, events);

		super.constructBoss(spells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		//If we hit a player
		if (damagee instanceof Player player) {
			//Set all nearby mobs to target them
			for (LivingEntity le : EntityUtils.getNearbyMobs(mBoss.getLocation(), detectionRange)) {
				if (le instanceof Mob mob) {
					mob.setTarget(player);
				}
			}
			//Let the players know something happened
			player.playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, SoundCategory.HOSTILE, 0.3f, 0.9f);
			new PartialParticle(Particle.VILLAGER_ANGRY, player.getLocation(), 25, 1.5, 1.5, 1.5).spawnAsEntityActive(mBoss);
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		List<Player> players = getPlayers();
		BossUtils.endBossFightEffects(mBoss, players);
		sendMessage("I feel it... partin'... the beyond calls... and I answer...");
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!players.isEmpty()) {
					mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
				}
			}
		}.runTaskLater(mPlugin, 20 * 3);
	}

	@Override
	public void init() {
		mBoss.teleport(mSpawnLoc);
		int hpDelta = 2000;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		double finalHp = hpDelta * BossUtils.healthScalingCoef(playerCount, 0.5, 0.5);

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, finalHp);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
		mBoss.setHealth(finalHp);

		for (Player player : getPlayers()) {
			MessagingUtils.sendBoldTitle(player, Component.text("Lingering Will", NamedTextColor.DARK_RED), null);
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.7f);
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, true, false, false));
		}

		summonArmorStandIfNoneAreThere(mCenter.clone().add(0, 0, 11.5));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(0, 0, -11.5));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(11.5, 0, 0));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(-11.5, 0, 0));

		mBoss.setAI(true);

		new BukkitRunnable() {
			int mCount = 0;

			@Override
			public void run() {
				String[] dio = {"The light... it burns...", "That lantern be shinin' through the beyond..."};
				sendMessage(dio[mCount]);
				mCount++;
				if (mCount == dio.length) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 20, 20 * 2);
	}

	private void summonArmorStandIfNoneAreThere(Location loc) {
		if (loc.getNearbyEntitiesByType(ArmorStand.class, 2, 2, 2).isEmpty()) {
			ArmorStand as = (ArmorStand) mBoss.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
			as.setInvisible(true);
			as.setInvulnerable(true);
			as.setMarker(true);
			as.addScoreboardTag("summon_constantly_stand");
		}
	}

	private List<Player> getPlayers() {
		return PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
	}

	private void sendMessage(String message) {
		Component component = Component.text(message, NamedTextColor.RED);
		for (Player player : getPlayers()) {
			player.sendMessage(component);
		}
	}
}
