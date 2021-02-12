package com.playmonumenta.plugins.bosses.bosses;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBarrier;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSlam;
import com.playmonumenta.plugins.bosses.spells.SpellCrowdControlClear;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;

public class TealQuestBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_teal_quest";
	public static final int detectionRange = 100;
	//This is going to be a monster of a tag, sorry for all the numbers =(
	//Meteor Slam
	private static final int COOLDOWN_SLAM = 20 * 8;
	private static final int MIN_RANGE = 0;
	private static final int RUN_DISTANCE = 0;
	private static final double VELOCITY_MULTIPLIER = 0.75;
	private static final double DAMAGE_RADIUS = 3;
	private static final double DAMAGE_PERCENT = .5;
	private static final int JUMP_HEIGHT = 1;
	//Reckless Swing
	private static final int RADIUS = 5;
	private static final int DURATION = 20;
	private static final int COOLDOWN_SWING = 20 * 6;
	private static final Particle.DustOptions REDSTONE_COLOR_SWING = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);
	//Crowd Control Resistance
	private static final int CLEAR_TIME = 20 * 4;
	//Barrier
	private static final int RECHARGE_TIME = 20 * 5;
	private static final int HITS_TO_BREAK = 1;
	private static final Particle.DustOptions REDSTONE_COLOR_BARRIER = new Particle.DustOptions(Color.fromRGB(225, 15, 255), 1.0f);

	private Location mEndLoc;
	private Location mSpawnLoc;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
        return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
            return new Varcosa(plugin, boss, spawnLoc, endLoc);
        });
    }

    @Override
    public String serialize() {
        return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
    }

	public TealQuestBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mEndLoc = endLoc;
		mSpawnLoc = spawnLoc;
		SpellManager manager = new SpellManager(Arrays.asList(new SpellBaseSlam(plugin, boss, JUMP_HEIGHT, detectionRange, MIN_RANGE, RUN_DISTANCE, COOLDOWN_SLAM, VELOCITY_MULTIPLIER,
				(World world, Location loc) -> {
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1, 1);
					world.spawnParticle(Particle.LAVA, loc, 15, 1, 0f, 1, 0);
				}, (World world, Location loc) -> {
					world.playSound(loc, Sound.ENTITY_HORSE_JUMP, SoundCategory.PLAYERS, 1, 1);
					world.spawnParticle(Particle.LAVA, loc, 15, 1, 0f, 1, 0);
				}, (World world, Location loc) -> {
					world.spawnParticle(Particle.REDSTONE, loc, 4, 0.5, 0.5, 0.5, 1, new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f));
				}, (World world, Player player, Location loc, Vector dir) -> {
					ParticleUtils.explodingRingEffect(plugin, loc, 4, 1, 4,
							Arrays.asList(
									new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.5, (Location location) -> {
										world.spawnParticle(Particle.FLAME, loc, 1, 0.1, 0.1, 0.1, 0.1);
										world.spawnParticle(Particle.CLOUD, loc, 1, 0.1, 0.1, 0.1, 0.1);
									})
							));

					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.3F, 0);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2, 1.25F);
					world.spawnParticle(Particle.FLAME, loc, 60, 0F, 0F, 0F, 0.2F);
					world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 20, 0F, 0F, 0F, 0.3F);
					world.spawnParticle(Particle.LAVA, loc, 3 * (int)(DAMAGE_RADIUS * DAMAGE_RADIUS), DAMAGE_RADIUS, 0.25f, DAMAGE_RADIUS, 0);
					if (player != null) {
						BossUtils.bossDamagePercent(mBoss, player, DAMAGE_PERCENT);
						return;
					}
					for (Player players : PlayerUtils.playersInRange(loc, DAMAGE_RADIUS)) {
						BossUtils.bossDamagePercent(mBoss, players, DAMAGE_PERCENT);
					}
					}),
					new SpellBaseAoE(plugin, boss, RADIUS, DURATION, COOLDOWN_SWING, false, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
						(Location loc) -> {
							boss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1, 2));
							World world = loc.getWorld();
							world.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, ((double) RADIUS) / 3, ((double) RADIUS) / 3, ((double) RADIUS) / 3, 0.05);
						}, (Location loc) -> {
							World world = loc.getWorld();
							world.spawnParticle(Particle.CRIT, loc, 1, 0.25, 0.25, 0.25, 0.05);
						}, (Location loc) -> {
							World world = loc.getWorld();
							world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.5f, 0.65F);
						}, (Location loc) -> {
							World world = loc.getWorld();
							world.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0.1, 0.1, 0.1, 0.3);
							world.spawnParticle(Particle.REDSTONE, loc, 2, 0.25, 0.25, 0.25, 0.1, REDSTONE_COLOR_SWING);
						}, (Location loc) -> {
							for (Player player : PlayerUtils.playersInRange(boss.getLocation(), RADIUS)) {
								BossUtils.bossDamage(boss, player, 35);
							}
						})));

		List<Spell> passives = new ArrayList<>(Arrays.asList(new SpellBarrier(plugin, mBoss, detectionRange, RECHARGE_TIME, HITS_TO_BREAK,
				(Location loc) -> {
					World world = loc.getWorld();
					world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 1, 1);
				}, (Location loc) -> {
					World world = loc.getWorld();
					world.spawnParticle(Particle.REDSTONE, loc, 4, 0, 1, 0, REDSTONE_COLOR_BARRIER);
				}, (Location loc) -> {
					World world = loc.getWorld();
					world.playSound(loc, Sound.ITEM_SHIELD_BREAK, SoundCategory.HOSTILE, 1, 1);
				}), new SpellCrowdControlClear(boss, CLEAR_TIME)));

		BossBarManager barManager = new BossBarManager(plugin, mBoss, detectionRange, BarColor.BLUE, BarStyle.SOLID, null);

		super.constructBoss(manager, passives, detectionRange, barManager);
	}

	@Override
	public void init() {
		int players = BossUtils.getPlayersInRangeForHealthScaling(mBoss, 35);
		double targetHealth = 1000 * (Math.log1p(players)/Math.log(2));

		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(targetHealth);
		mBoss.setHealth(targetHealth);
	}

	@Override
	public void death(EntityDeathEvent event) {
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}
}
