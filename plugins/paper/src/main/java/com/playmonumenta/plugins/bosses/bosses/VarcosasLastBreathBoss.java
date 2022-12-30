package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBossBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellPlayerAction;
import com.playmonumenta.plugins.bosses.spells.SpellPurgeNegatives;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.varcosamist.ForcefulGrip;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellActions;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellDeathlyCharge;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellGhostlyCannons;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellJibberJabber;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellSummonConstantly;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellSwitcheroo;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
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

public final class VarcosasLastBreathBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_varcosa_breath";
	public static final int detectionRange = 50;
	private static final String[] mSpeak = {"Arr, I be killin' ye myself then. I shan't be stopped twice...", "The air be growin' stale. I shan't let me end be this!"};
	private final Location mSpawnLoc;
	private final Location mEndLoc;
	private Location mCenter;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> new VarcosasLastBreathBoss(plugin, boss, spawnLoc, endLoc));
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public VarcosasLastBreathBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mBoss.setRemoveWhenFarAway(false);

		//Possible summons
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

		//Active Spells
		SpellManager spells = new SpellManager(
			Arrays.asList(
				new SpellDeathlyCharge(mPlugin, mBoss, 20 * 20, "It be time. The beyond is callin' for ye, thief. The Mist will take ye to yer grave..."),
				new SpellGhostlyCannons(mPlugin, mBoss, 22, mCenter, false, "Call down the cannons mateys!"),
				new SpellSwitcheroo(mPlugin, mBoss, 20 * 16, 50, "A trap be set now. Get ye into it!"),
				new ForcefulGrip(mPlugin, mBoss, 20 * 10, "Yarr, get ye over here! I'll deal with ye myself.")
			));
		//Passive Spells
		SpellPlayerAction action = SpellActions.getTooLowAction(mBoss, mCenter);

		SpellPlayerAction tooHighAction = SpellActions.getTooHighAction(mBoss, mCenter);
		//Passives

		List<Spell> passiveSpells = Arrays.asList(
			new SpellSummonConstantly(mSummonableMobs, 20 * 16, 50, 6, 2, mCenter, this),
			new SpellConditionalTeleport(mBoss, mSpawnLoc, b -> b.getLocation().getBlock().getType() == Material.WATER),
			new SpellJibberJabber(mBoss, mSpeak, detectionRange),
			new SpellPurgeNegatives(mBoss, 20 * 3),
			new SpellBossBlockBreak(mBoss, 175, 1, 3, 1, true, true),
			new SpellShieldStun(6 * 20),
			action, tooHighAction
		);

		BukkitRunnable runnable = SpellActions.getTeleportEntityRunnable(mBoss, mCenter);
		runnable.runTaskTimer(plugin, 20, 20 * 2);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();
		events.put(10, mBoss -> forceCastSpell(SpellGhostlyCannons.class));
		BossBarManager bossBar = new BossBarManager(mPlugin, mBoss, detectionRange + 20, BarColor.RED, BarStyle.SEGMENTED_10, events);

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
			player.playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, 0.3f, 0.9f);
			new PartialParticle(Particle.VILLAGER_ANGRY, player.getLocation(), 25, 1.5, 1.5, 1.5).spawnAsEntityActive(mBoss);
		}
	}

	@Override
	public void init() {
		mBoss.teleport(mCenter.clone().add(0, 1, 0));
		int hpDelta = 2000;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		double finalHp = hpDelta * BossUtils.healthScalingCoef(playerCount, 0.5, 0.5);

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, finalHp);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
		mBoss.setHealth(finalHp);

		summonArmorStandIfNoneAreThere(mCenter.clone().add(0, 0, 11.5));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(0, 0, -11.5));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(11.5, 0, 0));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(-11.5, 0, 0));

		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			MessagingUtils.sendBoldTitle(player, ChatColor.RED + "Varcosa's", ChatColor.DARK_RED + "Last Breath");
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, false, true, true));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 10, 0.7f);
		}

		mBoss.setAI(true);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);

		if (players.size() <= 0) {
			return;
		}

		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
		String dio = "Yarr... why be this hurtin'? I shan't go!";
		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + dio + "\",\"color\":\"red\"}]");

		new BukkitRunnable() {
			@Override
			public void run() {
				if (!PlayerUtils.playersInRange(mCenter, detectionRange, true).isEmpty()) {
					mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
				}
			}
		}.runTaskLater(mPlugin, 20 * 1);

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
}
