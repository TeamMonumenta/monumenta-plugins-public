package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellPurgeNegatives;
import com.playmonumenta.plugins.bosses.spells.SpellTpBehindPlayer;
import com.playmonumenta.plugins.bosses.spells.mimicqueen.SpellMultihitHeal;
import com.playmonumenta.plugins.bosses.spells.mimicqueen.SpellSummonMiniboss;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;

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

public class MimicQueen extends BossAbilityGroup {
	public static final String identityTag = "boss_mimicqueen";
	public static final int detectionRange = 20;

	private static final boolean SINGLE_TARGET = false;
	private static final boolean LAUNCH_TRACKING = false;
	private static final int COOLDOWN = 20 * 6;
	private static final int DELAY = 20 * 1;
	private static final double SPEED = 0.4;
	private static final double TURN_RADIUS = Math.PI / 30;
	private static final int LIFETIME_TICKS = 20 * 8;
	private static final double HITBOX_LENGTH = 0.5;
	private static final boolean COLLIDES_WITH_BLOCKS = true;
	private static final boolean LINGERS = true;
	private static final int DAMAGE = 35;

	private final Location mSpawnLoc;
	private final Location mEndLoc;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new MimicQueen(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}


	public MimicQueen(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;


		SpellManager activeSpells = new SpellManager(Arrays.asList(
				new SpellMultihitHeal(plugin, boss),
				new SpellSummonMiniboss(plugin, boss),
				new SpellTpBehindPlayer(plugin, boss, 120, 80, 50, 10, true),
				new SpellBaseSeekingProjectile(plugin, boss, detectionRange, SINGLE_TARGET, LAUNCH_TRACKING, COOLDOWN, DELAY,
						SPEED, TURN_RADIUS, LIFETIME_TICKS, HITBOX_LENGTH, COLLIDES_WITH_BLOCKS, LINGERS,
						// Initiate Aesthetic
						(World world, Location loc, int ticks) -> {
							PotionUtils.applyPotion(null, boss, new PotionEffect(PotionEffectType.GLOWING, DELAY, 0));
							world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.HOSTILE, 1f, 0.5f);
						},
						// Launch Aesthetic
						(World world, Location loc, int ticks) -> {
							world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
							world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 0.5f, 0.5f);
						},
						// Projectile Aesthetic
						(World world, Location loc, int ticks) -> {
							world.spawnParticle(Particle.FLAME, loc, 3, 0, 0, 0, 0.1);
							world.spawnParticle(Particle.SMOKE_LARGE, loc, 2, 0.25, 0.25, 0.25, 0);
							if (ticks % 40 == 0) {
								world.playSound(loc, Sound.ENTITY_BLAZE_BURN, SoundCategory.HOSTILE, 0.5f, 0.2f);
							}
						},
						// Hit Action
						(World world, Player player, Location loc) -> {
							world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.5f, 0.5f);
							world.spawnParticle(Particle.FLAME, loc, 50, 0, 0, 0, 0.25);
							if (player != null) {
								BossUtils.bossDamage(boss, player, DAMAGE);
							}
						})
		));

		List<Spell> passiveSpells = Arrays.asList(
				new SpellBlockBreak(boss),
				new SpellPurgeNegatives(boss, 20 * 6)
			);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();
		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange + 30, BarColor.RED, BarStyle.SEGMENTED_10, events);

		super.constructBoss(activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		int hpDelta = 1000;
		int armor = (int)(Math.sqrt(playerCount * 2) - 1);
		while (playerCount > 0) {
			bossTargetHp = bossTargetHp + hpDelta;
			hpDelta = hpDelta / 2;
			playerCount--;
		}
		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.setHealth(bossTargetHp);

		Location loc = mBoss.getLocation();

		//launch event related spawn commands
		PlayerUtils.executeCommandOnNearbyPlayers(loc, detectionRange + 20, "effect give @s minecraft:blindness 2 2");
		PlayerUtils.executeCommandOnNearbyPlayers(loc, detectionRange + 20, "title @s title [\"\",{\"text\":\"Mimic Queen\",\"color\":\"dark_purple\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(loc, detectionRange + 20, "title @s subtitle [\"\",{\"text\":\"Varcosa's Plunder Protector\",\"color\":\"purple\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(loc, detectionRange + 20, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 0.7");
	}

	@Override
	public void death(EntityDeathEvent event) {
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}
}
