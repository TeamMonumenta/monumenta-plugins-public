package com.playmonumenta.plugins.bosses.spells.sealedremorse;

import com.playmonumenta.plugins.bosses.bosses.BeastOfTheBlackFlame;
import com.playmonumenta.plugins.bosses.bosses.Svalgot;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

public class SvalgotOrbOfBones extends SpellBaseSeekingProjectile {

	private static final boolean SINGLE_TARGET = true;
	private static final boolean LAUNCH_TRACKING = true;
	private static final int COOLDOWN = 20 * 12;
	private static final int DELAY = 20 * 1;
	private static final double SPEED = 0.2;
	private static final double TURN_RADIUS = Math.PI / 45;
	private static final int LIFETIME_TICKS = 20 * 6;
	private static final double HITBOX_LENGTH = 2;
	private static final boolean COLLIDES_WITH_BLOCKS = false;
	private static final boolean LINGERS = true;
	private static final int DAMAGE = 42;

	private final Plugin mPlugin;
	private final Svalgot mBossClass;
	private final LivingEntity mBoss;

	private boolean mOnCooldown = false;

	public SvalgotOrbOfBones(LivingEntity boss, Plugin plugin, Svalgot bossClass) {
		super(plugin, boss, Svalgot.detectionRange, SINGLE_TARGET, LAUNCH_TRACKING, COOLDOWN, DELAY,
			SPEED, TURN_RADIUS, LIFETIME_TICKS, HITBOX_LENGTH, COLLIDES_WITH_BLOCKS, LINGERS, 20, true,
			// Initiate Aesthetic
			(World world, Location loc, int ticks) -> {
				PotionUtils.applyPotion(null, boss, new PotionEffect(PotionEffectType.GLOWING, DELAY, 0));

				if (ticks % 2 == 0) {
					new PartialParticle(Particle.SOUL_FIRE_FLAME, loc, 8, 0.5, 0.5, 0.5, 0.2).spawnAsEntityActive(boss);
					new PartialParticle(Particle.FLAME, loc, 8, 0.5, 0.5, 0.5, 0.2).spawnAsEntityActive(boss);
				}
			},
			// Launch Aesthetic
			(World world, Location loc, int ticks) -> {
				world.playSound(loc, Sound.ITEM_FIRECHARGE_USE, SoundCategory.HOSTILE, 3, 0.5f);
				world.playSound(loc, Sound.ENTITY_GHAST_HURT, SoundCategory.HOSTILE, 5, 1.5f);
			},
			// Projectile Aesthetic
			(World world, Location loc, int ticks) -> {

				new PartialParticle(Particle.SOUL_FIRE_FLAME, loc, 6, 0.5, 0.5, 0.5, 0.1).spawnAsEntityActive(boss);
				new PartialParticle(Particle.FLAME, loc, 8, 1, 1, 1, 0.1).spawnAsEntityActive(boss);
				new PartialParticle(Particle.CLOUD, loc, 6, 0.5, 0.5, 0.5, 0).spawnAsEntityActive(boss);
			},
			// Hit Action
			(World world, LivingEntity player, Location loc, Location prevLoc) -> {
				world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3, 1);
				new PartialParticle(Particle.FLAME, loc, 80, 2, 2, 2, 0.5).spawnAsEntityActive(boss);
				new PartialParticle(Particle.SOUL_FIRE_FLAME, loc, 80, 2, 2, 2, 0.5).spawnAsEntityActive(boss);
				new PartialParticle(Particle.CLOUD, loc, 40, 1, 1, 1, 0.5).spawnAsEntityActive(boss);

				for (Player p : PlayerUtils.playersInRange(loc, 5, true)) {
					DamageUtils.damage(boss, p, DamageType.MAGIC, DAMAGE, null, false, true, "Orb of Bones");
					EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), 4 * 20, p, boss);
				}
			});
		mBossClass = bossClass;
		mBoss = boss;
		mPlugin = plugin;
	}

	@Override
	public int cooldownTicks() {
		return (int) (7 * 20 * mBossClass.mCastSpeed);
	}

	@Override
	public <V extends LivingEntity> void launch(V target, Location targetLoc) {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mOnCooldown = false, 20 * 15);

		//List is farthest players in the beginning, and nearest players at the end
		List<Player> players = EntityUtils.getNearestPlayers(mBoss.getLocation(), BeastOfTheBlackFlame.detectionRange);
		if (players.size() > 0) {
			Player playerOne = players.get(0);
			super.launch(playerOne, playerOne.getEyeLocation());
		}
	}

	@Override
	public boolean canRun() {
		if (mOnCooldown) {
			return false;
		} else {
			return super.canRun();
		}
	}

	@Override
	protected void onEndAction(Location projLoc, BoundingBox projHitbox) {
		World world = mBoss.getWorld();

		world.playSound(projLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3, 1);
		new PartialParticle(Particle.FLAME, projLoc, 80, 2, 2, 2, 0.5).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.SOUL_FIRE_FLAME, projLoc, 80, 2, 2, 2, 0.5).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.CLOUD, projLoc, 40, 1, 1, 1, 0.5).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.REDSTONE, projLoc, 40, 0.5, 0.5, 0.5, new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f)).spawnAsEntityActive(mBoss);

		for (Player p : PlayerUtils.playersInRange(projLoc, 6, true)) {
			DamageUtils.damage(mBoss, p, DamageType.MAGIC, DAMAGE, null, false, true, "Orb of Bones");
			EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), 4 * 20, p, mBoss);
		}
	}
}
