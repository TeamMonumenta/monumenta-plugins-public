package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellPurgeNegatives;
import com.playmonumenta.plugins.bosses.spells.SpellTpBehindPlayer;
import com.playmonumenta.plugins.bosses.spells.mimicqueen.SpellMultihitHeal;
import com.playmonumenta.plugins.bosses.spells.mimicqueen.SpellSummonMiniboss;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Arrays;
import java.util.List;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public final class MimicQueen extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_mimicqueen";
	public static final int detectionRange = 20;

	private static final boolean SINGLE_TARGET = false;
	private static final boolean LAUNCH_TRACKING = false;
	private static final int COOLDOWN = Constants.TICKS_PER_SECOND * 6;
	private static final int DELAY = Constants.TICKS_PER_SECOND;
	private static final double SPEED = 0.4;
	private static final double TURN_RADIUS = Math.PI / 30;
	private static final int LIFETIME_TICKS = Constants.TICKS_PER_SECOND * 8;
	private static final double HITBOX_LENGTH = 0.5;
	private static final boolean COLLIDES_WITH_BLOCKS = true;
	private static final boolean LINGERS = true;
	private static final int DAMAGE = 35;
	private static final String COLOR = "red";

	public MimicQueen(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellMultihitHeal(mPlugin, mBoss),
			new SpellSummonMiniboss(mPlugin, mBoss),
			new SpellTpBehindPlayer(mPlugin, mBoss, COOLDOWN, 80, 50, 10, true),
			new SpellBaseSeekingProjectile(mPlugin, mBoss, detectionRange, SINGLE_TARGET, LAUNCH_TRACKING, COOLDOWN, DELAY,
				SPEED, TURN_RADIUS, LIFETIME_TICKS, HITBOX_LENGTH, COLLIDES_WITH_BLOCKS, LINGERS,
				// Initiate Aesthetic
				(World world, Location loc, int ticks) -> {
					GlowingManager.startGlowing(mBoss, NamedTextColor.NAMES.valueOr(COLOR, NamedTextColor.RED), DELAY, GlowingManager.BOSS_SPELL_PRIORITY);
					world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.HOSTILE, 1f, 0.5f);
				},
				// Launch Aesthetic
				(World world, Location loc, int ticks) -> {
					new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0).minimumCount(1).spawnAsEntityActive(mBoss);
					world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 0.5f, 0.5f);
				},
				// Projectile Aesthetic
				(World world, Location loc, int ticks) -> {
					new PartialParticle(Particle.FLAME, loc, 3, 0, 0, 0, 0.1).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SMOKE_LARGE, loc, 2, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mBoss);
					if (ticks % 40 == 0) {
						world.playSound(loc, Sound.ENTITY_BLAZE_BURN, SoundCategory.HOSTILE, 0.5f, 0.2f);
					}
				},
				// Hit Action
				(World world, @Nullable LivingEntity target, Location loc, @Nullable Location prevLoc) -> {
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.5f, 0.5f);
					new PartialParticle(Particle.FLAME, loc, 50, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);
					if (target != null) {
						BossUtils.blockableDamage(boss, target, DamageType.MAGIC, DAMAGE, prevLoc);
					}
				})
		));

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBlockBreak(mBoss),
			new SpellPurgeNegatives(mBoss, COOLDOWN)
		);

		BossBarManager bossBar = new BossBarManager(mBoss, detectionRange + 30, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10, null);
		super.constructBoss(activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void init() {
		final List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
		int bossTargetHp = 1000;
		if (players.size() > 1) { /* Only 1 or 2 players in a Hoard instance */
			bossTargetHp += 500;
		}

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, bossTargetHp);
		mBoss.setHealth(bossTargetHp);

		for (Player player : players) {
			MessagingUtils.sendBoldTitle(player, Component.text("Mimic Queen", NamedTextColor.DARK_PURPLE), Component.text("Varcosa's Plunder Protector", NamedTextColor.LIGHT_PURPLE));
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Constants.TICKS_PER_SECOND * 2, 2, true, false, false));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.7f);
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}
}
