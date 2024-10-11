package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.RespawnStasis;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Arrays;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;

public class BulletHellSurvivalBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_bullet_hell_survival";
	public static final int detectionRange = 50;

	public static class Parameters extends BossParameters {
		@BossParam(help = "The amount of health the boss heals if a player is cheesing")
		public double HEALING = 0.02;
		@BossParam(help = "The amount of health the boss loses over time")
		public double DAMAGE_SELF = 0.004; // Default 62.5 second survival
		@BossParam(help = "The max % of health a player can deal to this mob at once")
		public double PLAYER_DAMAGE_CAP = 0.08;
		@BossParam(help = "The range within the boss will check for cheesing players and heal on player death")
		public double RADIUS = 13;
		@BossParam(help = "When a player dies in range, heal back this much HP")
		public double PLAYER_KILL_HEAL = 1;
	}

	final Parameters mParam;
	private static final double PLAYER_BOUNDING_BOX_XZ = 0.61;
	private static final double PLAYER_BASE_JUMP_HEIGHT = 1.2523;
	private static final double BULLET_HITBOX_RADIUS = 0.3125 / 2.0;
	private static final List<Material> cheeseBlocks = Arrays.asList(Material.TWISTING_VINES, Material.CAVE_VINES, Material.LADDER, Material.WATER, Material.LAVA, Material.VINE, Material.WEEPING_VINES, Material.SCAFFOLDING);

	public BulletHellSurvivalBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = Parameters.getParameters(boss, identityTag, new BulletHellSurvivalBoss.Parameters());
		List<Spell> passiveSpells = List.of(
			new SpellRunAction(() -> {
				if (!mBoss.isDead()) {
					int doDamage = shouldDamage(mBoss);
					if (doDamage == 1) {
						mBoss.setHealth(Math.max(mBoss.getHealth() - mParam.DAMAGE_SELF * EntityUtils.getMaxHealth(mBoss), 0));
					} else if (doDamage == -1) {
						mBoss.setHealth(Math.min(mBoss.getHealth() + mParam.HEALING * EntityUtils.getMaxHealth(mBoss), EntityUtils.getMaxHealth(mBoss)));
					}
				}
			})
		);
		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null, 0, 5);
	}

	private int shouldDamage(LivingEntity boss) {
		List<Player> players = PlayerUtils.playersInRange(boss.getLocation(), mParam.RADIUS, false);
		for (Player player : players) {
			if (!EffectManager.getInstance().hasEffect(player, RespawnStasis.class) && !player.isDead() && player.getGameMode() != GameMode.SPECTATOR) {
				if (player.isSleeping()) {
					return -1;
				}
				double heightdiff = player.getLocation().getY() - boss.getLocation().getY();
				double jumpHeight = PlayerUtils.getJumpHeight(player);
				if (heightdiff > BULLET_HITBOX_RADIUS && playerOnBlock(player)) {
					return -1;
				} else if (heightdiff < -PLAYER_BASE_JUMP_HEIGHT || heightdiff > jumpHeight) {
					return -1;
				} else if (jumpHeight != PLAYER_BASE_JUMP_HEIGHT) {
					return 0;
				}
			}
		}
		if (players.isEmpty()) {
			return -1;
		}
		return 1;
	}

	private boolean playerOnBlock(Player p) {
		boolean onBlock = NmsUtils.getVersionAdapter().hasCollisionWithBlocks(p.getWorld(), p.getBoundingBox().shift(0, -0.01, 0), false);
		return onBlock || playerIsCheesing(p);
	}

	private boolean playerIsCheesing(Player p) {
		Location loc = p.getLocation().clone().add(0, -0.15, 0);
		for (int i = -1; i < 2; i += 2) {
			for (int j = -1; j < 2; j += 2) {
				if (cheeseBlocks.contains(loc.clone().add(i * PLAYER_BOUNDING_BOX_XZ / 2.0, 0, j * PLAYER_BOUNDING_BOX_XZ / 2.0).getBlock().getType())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		if (event.getDamage() > mParam.PLAYER_DAMAGE_CAP * EntityUtils.getMaxHealth(mBoss)) {
			// Strongest attack can only shorten it by 5 seconds
			event.setDamageCap(mParam.PLAYER_DAMAGE_CAP * EntityUtils.getMaxHealth(mBoss));
		}
		Location loc = mBoss.getLocation();
		if (source instanceof Player p) {
			double heightdiff = p.getLocation().getY() - loc.getY();
			double jumpHeight = PlayerUtils.getJumpHeight(p);
			if (heightdiff > jumpHeight || heightdiff < -PLAYER_BASE_JUMP_HEIGHT || (heightdiff > BULLET_HITBOX_RADIUS && playerOnBlock(p))) {
				event.setCancelled(true);

				World world = mBoss.getWorld();
				loc.add(0, 1, 0);
				new PartialParticle(Particle.FIREWORKS_SPARK, loc, 20, 0, 0, 0, 0.3).spawnAsEntityActive(mBoss);
				world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.2f, 1.5f);
			}
		}
	}

	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		if (event.getPlayer().getLocation().distanceSquared(mBoss.getLocation()) <= mParam.RADIUS * mParam.RADIUS) {
			mBoss.setHealth(Math.min(mBoss.getHealth() + mParam.PLAYER_KILL_HEAL * EntityUtils.getMaxHealth(mBoss), EntityUtils.getMaxHealth(mBoss)));
		}
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}

}



