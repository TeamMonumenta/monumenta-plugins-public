package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.sealedremorse.SvalgotBoneSlam;
import com.playmonumenta.plugins.bosses.spells.sealedremorse.SvalgotOrbOfBones;
import com.playmonumenta.plugins.bosses.spells.sealedremorse.SvalgotRisingBlackflame;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class Svalgot extends BossAbilityGroup {

	public static final String identityTag = "boss_svalgot";
	public static final int detectionRange = 75;

	private static final int BASE_HEALTH = 2000;
	private static final String duoTag = "ghalkortheforgemaster";

	private final Location mSpawnLoc;
	private final Location mEndLoc;

	private @Nullable LivingEntity mGhalkor;
	private @Nullable Ghalkor mGhalkorBoss;

	//True when the final boss has been called from death
	boolean mSummonedFinalBoss = false;

	//Lower number = faster cast speed
	//0.5 is double casting speed
	public double mCastSpeed = 1;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new Svalgot(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public Svalgot(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);

		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;

		SpellManager normalSpells = new SpellManager(Arrays.asList(
				new SvalgotBoneSlam(plugin, boss, this),
				new SvalgotRisingBlackflame(plugin, boss, this),
				new SvalgotOrbOfBones(boss, plugin, this)
				));

		List<Spell> passiveNormalSpells = Arrays.asList(
				new SpellBlockBreak(boss, 2, 3, 2),
				new SpellShieldStun(6 * 20)
			);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();

		events.put(50, mBoss -> {
			//Cast faster
			mCastSpeed = .5;

			World world = mBoss.getWorld();
			Location loc = mBoss.getLocation();
			world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1f, 0.6f);
			world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.5f);
			world.spawnParticle(Particle.SPELL_WITCH, loc.add(0, mBoss.getHeight() / 2, 0), 15, 0.25, 0.45, 0.25, 1);
			world.spawnParticle(Particle.VILLAGER_ANGRY, loc.add(0, mBoss.getHeight() / 2, 0), 5, 0.35, 0.5, 0.35, 0);

			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Svalgot]\",\"color\":\"gold\"},{\"text\":\" My pain only grows my power!\",\"color\":\"dark_gray\"}]");
		});

		for (LivingEntity e : EntityUtils.getNearbyMobs(spawnLoc, 75)) {
			if (e.getScoreboardTags().contains(duoTag)) {
				mGhalkor = e;
				break;
			}
		}

		//TODO: Find better way to get boss duo
		if (mGhalkor != null) {
			new BukkitRunnable() {
				@Override
				public void run() {
					mGhalkorBoss = mGhalkor != null ? BossManager.getInstance().getBoss(mGhalkor, Ghalkor.class) : null;
				}
			}.runTaskLater(mPlugin, 1);
		} else {
			mPlugin.getLogger().warning("Ghalkor was not found by Svalgot!");
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				} else if (mGhalkor == null || mGhalkor.isDead() || !mGhalkor.isValid()) {
					//changePhase to increased pace
					EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, EntityUtils.getAttributeBaseOrDefault(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, 0) * 1.05);
					EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_ATTACK_DAMAGE, EntityUtils.getAttributeBaseOrDefault(mBoss, Attribute.GENERIC_ATTACK_DAMAGE, 0) * 1.25);

					PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Svalgot]\",\"color\":\"gold\"},{\"text\":\" Broer, for you and for the Blackflame, I will bathe in their blood!\",\"color\":\"dark_gray\"}]");
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_VINDICATOR_DEATH, 3, 0);

					World world = mBoss.getWorld();
					Location loc = mBoss.getLocation();
					world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1f, 0.6f);
					world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.5f);
					world.spawnParticle(Particle.SPELL_WITCH, loc.add(0, mBoss.getHeight() / 2, 0), 15, 0.25, 0.45, 0.25, 1);
					world.spawnParticle(Particle.VILLAGER_ANGRY, loc.add(0, mBoss.getHeight() / 2, 0), 5, 0.35, 0.5, 0.35, 0);

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 5);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}

				//If below 3 blocks, teleport
				if (mSpawnLoc.getY() - mBoss.getLocation().getY() >= 3) {
					teleport(mSpawnLoc);
				}

				//Second Duo Boss does not have player damage code, would be redundant
			}
		}.runTaskTimer(mPlugin, 0, 10);

		new BukkitRunnable() {
			@Override
			public void run() {
				mBoss.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, mBoss.getLocation(), 200, 0.1, 0.1, 0.1, 0.3);
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 5, 2);
			}
		}.runTaskLater(mPlugin, 20 * 6);

		mBoss.setAI(false);
		mBoss.setGravity(false);
		mBoss.setInvulnerable(true);

		new BukkitRunnable() {
			@Override
			public void run() {
				mBoss.setAI(true);
				mBoss.setGravity(true);
				mBoss.setInvulnerable(false);

				BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
				constructBoss(normalSpells, passiveNormalSpells, detectionRange, bossBar, 20 * 10);
			}
		}.runTaskLater(mPlugin, 20 * 6 + 10);
	}

	@Override
	public void death(EntityDeathEvent event) {
		if ((mGhalkor == null || mGhalkor.isDead() || !mGhalkor.isValid()) && (mGhalkorBoss == null || !mGhalkorBoss.mSummonedFinalBoss)) {
			mSummonedFinalBoss = true;

			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Svalgot]\",\"color\":\"gold\"},{\"text\":\" With mine Laastasem...  My lifeblood fuels the ritual... Come forth o Beast!\",\"color\":\"dark_gray\"}]");

			Entity beast = LibraryOfSoulsIntegration.summon(mSpawnLoc.add(2, -3, 0), BeastOfTheBlackFlame.losName);
			if (beast != null && beast instanceof LivingEntity leBeast) {
				try {
					BossManager.createBoss(null, leBeast, BeastOfTheBlackFlame.identityTag, mEndLoc);
				} catch (Exception e) {
					mPlugin.getLogger().warning("Failed to create boss BeastOfTheBlackFlame: " + e.getMessage());
					e.printStackTrace();
				}
			} else {
				mPlugin.getLogger().warning("Failed to summon BeastOfTheBlackFlame");
			}
		}
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

	//Teleport with special effects
	private void teleport(Location loc) {
		World world = loc.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		world.spawnParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
		world.spawnParticle(Particle.CLOUD, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1);
		mBoss.teleport(loc);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		world.spawnParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
		world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1);
	}
}
