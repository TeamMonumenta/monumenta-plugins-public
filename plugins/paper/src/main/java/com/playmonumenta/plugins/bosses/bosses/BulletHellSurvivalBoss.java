package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BulletHellSurvivalBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_bullet_hell_survival";
	public static final int detectionRange = 50;

	public static class Parameters extends BossParameters {
		public double HEALING = 0.02;
		// Default 60 second survival
		public double DAMAGE = 0.004;
	}

	final Parameters mParam;
	private static double PLAYER_BOUNDING_BOX_XZ = 0.61;
	private static double PLAYER_BASE_JUMP_HEIGHT = 1.2523;
	private static double BULLET_HITBOX_RADIUS = 0.3125 / 2.0;
	private static List<Material> cheeseBlocks = Arrays.asList(Material.TWISTING_VINES, Material.CAVE_VINES, Material.LADDER, Material.WATER, Material.LAVA, Material.VINE, Material.WEEPING_VINES, Material.SCAFFOLDING);

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BulletHellSurvivalBoss(plugin, boss);
	}

	public BulletHellSurvivalBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = Parameters.getParameters(boss, identityTag, new BulletHellSurvivalBoss.Parameters());
		List<Spell> passiveSpells = Arrays.asList(
			new SpellRunAction(() -> {
				if (!mBoss.isDead()) {
					int doDamage = shouldDamage(mBoss);
					if (doDamage == 1) {
						mBoss.setHealth(Math.max(mBoss.getHealth() - mParam.DAMAGE * EntityUtils.getMaxHealth(mBoss), 0));
					} else if (doDamage == -1) {
						mBoss.setHealth(Math.min(mBoss.getHealth() + mParam.HEALING * EntityUtils.getMaxHealth(mBoss), EntityUtils.getMaxHealth(mBoss)));
					}
				}
			})
		);
		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null, 0, 5);
	}

	private int shouldDamage(LivingEntity boss) {
		List<Player> players = PlayerUtils.playersInRange(boss.getLocation(), 13, false);
		for (Player player : players) {
			if (!EffectManager.getInstance().hasEffect(player, RespawnStasis.class) && !player.isDead() && player.getGameMode() != GameMode.SPECTATOR) {
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
		boolean cheesing = false;
		for (int i = -1; i < 2; i += 2) {
			for (int j = -1; j < 2; j += 2) {
				cheesing = cheesing || cheeseBlocks.contains(loc.clone().add(i * PLAYER_BOUNDING_BOX_XZ / 2.0, 0, j * PLAYER_BOUNDING_BOX_XZ / 2.0).getBlock().getType());
			}
		}
		return cheesing;
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		if (event.getDamage() > 20 * mParam.DAMAGE * EntityUtils.getMaxHealth(mBoss)) {
			// Strongest attack can only shorten it by 5 seconds
			event.setDamage(20 * mParam.DAMAGE * EntityUtils.getMaxHealth(mBoss));
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

}



