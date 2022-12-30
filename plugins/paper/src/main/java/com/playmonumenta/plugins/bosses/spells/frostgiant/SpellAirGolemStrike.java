package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.destroystokyo.paper.entity.Pathfinder;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

public class SpellAirGolemStrike extends Spell {

	private static final String GOLEM_NAME = "PermafrostConstruct";

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mStartLoc;

	private boolean mSpellCooldown = false;
	private boolean mCooldown = false;

	private final double mAttackDamage = 35;

	public SpellAirGolemStrike(Plugin plugin, LivingEntity boss, Location loc) {
		mPlugin = plugin;
		mBoss = boss;
		mStartLoc = loc;
	}

	@Override
	public void run() {
		mSpellCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mSpellCooldown = false, 20 * 24);

		int count = 0;
		//List is farthest players in the beginning, and nearest players at the end
		List<Player> players = EntityUtils.getNearestPlayers(mBoss.getLocation(), FrostGiant.detectionRange);
		players.removeIf(p -> mStartLoc.distance(p.getLocation()) > FrostGiant.fighterRange);
		if (players.size() < 3) {
			count = 1;
		} else if (players.size() == 3) {
			count = 2;
		} else if (players.size() <= 10) {
			count = players.size() / 2;
		} else {
			count = (int) ((players.size() / 3) + 1.5);
		}

		count = Math.min(12, count);

		if (count <= 0 || players.size() < 1) {
			return;
		}

		List<Player> targets = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			targets.add(players.get(i));
		}
		for (Player p : targets) {
			spawnGolems(p.getLocation(), targets);
		}

	}

	private void spawnGolems(Location loc, List<Player> players) {
		World world = mBoss.getWorld();
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0);
		//If in air, subtract 1 y value until the block is not air
		if (loc.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
			while (loc.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
				loc.add(0, -1, 0);
			}
		}

		LivingEntity golem = Objects.requireNonNull((LivingEntity) LibraryOfSoulsIntegration.summon(loc.clone().add(0, 10, 0), GOLEM_NAME));

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				Creature c = (Creature) golem;
				Pathfinder pathfinder = c.getPathfinder();

				c.setTarget(null);
				pathfinder.stopPathfinding();

				if (mT < 40) {
					Location loc = golem.getLocation();
					loc.setYaw(loc.getYaw() + 45);
					golem.teleport(loc);
				}

				mT++;

				if (mT >= 70) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		new BukkitRunnable() {
			int mT = 0;
			float mPitch = 1;

			@Override
			public void run() {
				if (mT >= 40) {
					world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.HOSTILE, 1, 0);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0);

					//The particles that damage after 2 seconds, in the larger hitbox
					BoundingBox box = BoundingBox.of(loc, 1, 20, 1);
					for (Player player : players) {
						if (box.overlaps(player.getBoundingBox())) {
							DamageUtils.damage(mBoss, player, DamageType.MELEE, 45, null, false, true, "Air Golem Strike");
						}
					}
					Location particleLoc = loc.clone();
					//Creates line of particles
					for (int y = loc.getBlockY(); y < loc.getBlockY() + 10; y += 1) {
						particleLoc.setY(y);
						new PPCircle(Particle.FLAME, loc, 2).ringMode(true).count(36).delta(0.15).extra(0.25).spawnAsEntityActive(mBoss);
						new PPCircle(Particle.DRAGON_BREATH, loc, 3).ringMode(true).count(36).delta(0.15).extra(0.1).spawnAsEntityActive(mBoss);
					}

					golem.teleport(loc);

					Bukkit.getScheduler().runTaskLater(mPlugin, () -> setGolemAttack(golem), 30);

					this.cancel();
				}

				if (mT % 10 == 0) {
					world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 0.2f, mPitch);
				}
				mPitch += 0.05f;

				new PPCircle(Particle.FLAME, loc, 2).ringMode(true).count(22).delta(0.25).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.VILLAGER_ANGRY, loc, 1, 0.5, 0.5, 0.5).spawnAsEntityActive(mBoss);

				mT += 5;
			}
		}.runTaskTimer(mPlugin, 0, 5);
	}

	//Custom attack for golems that is smaller than default
	private void setGolemAttack(LivingEntity golem) {
		World world = golem.getWorld();
		new BukkitRunnable() {
			@Override
			public void run() {
				if (golem.isDead() || !golem.isValid() || mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					if (mBoss.isDead() || !mBoss.isValid()) {
						golem.remove();
					}
				}
				Creature c = (Creature) golem;
				if (c.getTarget() != null) {
					LivingEntity target = c.getTarget();

					PotionEffect effect = golem.getPotionEffect(PotionEffectType.GLOWING);

					if (target.getBoundingBox().overlaps(golem.getBoundingBox().expand(0.4, 0, 0.4)) && !mCooldown && effect == null) {
						mCooldown = true;
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> mCooldown = false, 30);

						if (target instanceof Player player) {
							DamageUtils.damage(golem, player, DamageType.MELEE, mAttackDamage, null, false, true, "Air Golem Strike");
						} else {
							target.damage(mAttackDamage, golem);
						}
						MovementUtils.knockAway(golem.getLocation(), target, 1f, 0.5f, false);
						world.playSound(golem.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.HOSTILE, 3, 0.5f);
					}
				}
			}

		}.runTaskTimer(mPlugin, 0, 2);
	}

	@Override
	public int cooldownTicks() {
		return 5 * 20;
	}

	@Override
	public boolean canRun() {
		return !mSpellCooldown;
	}
}
