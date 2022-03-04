package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.destroystokyo.paper.entity.Pathfinder;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.List;
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

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mStartLoc;

	private boolean mSpellCooldown = false;
	private boolean mCooldown = false;

	private double mAttackDamage = 35;

	public SpellAirGolemStrike(Plugin plugin, LivingEntity boss, Location loc) {
		mPlugin = plugin;
		mBoss = boss;
		mStartLoc = loc;
	}

	@Override
	public void run() {
		mSpellCooldown = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				mSpellCooldown = false;
			}

		}.runTaskLater(mPlugin, 20 * 24);

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
			count = (int)((players.size() / 3) + 1.5);
		}

		count = Math.min(12, count);

		if (count <= 0 || players.size() < 1) {
			return;
		}

		List<Player> targets = new ArrayList<Player>();
		for (int i = 0; i < count; i++) {
			targets.add(players.get(i));
		}
		for (Player p : targets) {
			spawnGolems(p.getLocation(), targets);
		}

	}

	private void spawnGolems(Location target, List<Player> players) {
		Location loc = target;
		World world = mBoss.getWorld();
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 5, 0);
		//If in air, subtract 1 y value until the block is not air
		if (target.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
			while (loc.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
				loc.add(0, -1, 0);
			}
		}

		LivingEntity golem = (LivingEntity) LibraryOfSoulsIntegration.summon(loc.clone().add(0, 10, 0), GOLEM_NAME);

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
					world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.HOSTILE, 5, 0);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5, 0);

					//The particles that damage after 2 seconds, in the larger hitbox
					BoundingBox box = BoundingBox.of(loc, 1, 20, 1);
					for (Player player : players) {
						if (box.overlaps(player.getBoundingBox())) {
							BossUtils.blockableDamage(mBoss, player, DamageType.MELEE, 45, "Air Golem Strike", mBoss.getLocation());
						}
					}
					Location particleLoc = loc.clone();
					//Creates line of particles
					for (double deg = 0; deg < 360; deg += (1 * 10)) {
						double cos = FastUtils.cos(deg);
						double sin = FastUtils.sin(deg);
						for (int y = loc.getBlockY(); y < loc.getBlockY() + 10; y += 1) {
							particleLoc.setY(y);
							world.spawnParticle(Particle.FLAME, loc.clone().add(2 * cos, 0, 2 * sin), 1, 0.15, 0.15, 0.15, 0.25);
							world.spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(3 * cos, 0, 3 * sin), 1, 0.15, 0.15, 0.15, 0.1);
						}
					}

					golem.teleport(loc);

					new BukkitRunnable() {
						@Override
						public void run() {
							setGolemAttack(golem);
						}
					}.runTaskLater(mPlugin, 30);

					this.cancel();
				}

				if (mT % 10 == 0) {
					world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 1, mPitch);
				}
				mPitch += 0.05f;

				for (double deg = 0; deg < 360; deg += (1 * 10)) {
					if (FastUtils.RANDOM.nextDouble() > 0.4) {
						double cos = FastUtils.cos(deg);
						double sin = FastUtils.sin(deg);
						world.spawnParticle(Particle.FLAME, loc.clone().add(2 * cos, 0, 2 * sin), 1, 0.25, 0.25, 0.25, 0);
					}
				}
				world.spawnParticle(Particle.VILLAGER_ANGRY, loc, 1, 0.5, 0.5, 0.5);

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
						new BukkitRunnable() {

							@Override
							public void run() {
								mCooldown = false;
							}

						}.runTaskLater(mPlugin, 30);
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
