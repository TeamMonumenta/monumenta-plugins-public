package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.RespawnStasis;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;

public final class BulletHellSurvivalBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_bullet_hell_survival";
	public static final int detectionRange = 50;

	@BossParam(help = "Should only be used on Mutated Astrals. The boss gains or loses health depending on player actions")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Percent health the launcher heals no players are present or a player is cheesing per 5 ticks")
		public double HEALING = 0.02;

		@BossParam(help = "Percent health the launcher loses per 5 ticks if conditions are met")
		public double DAMAGE_SELF = 0.004; // Default 62.5 second survival

		@BossParam(help = "Maximum percent health a player can deal to this launcher in one damage instance")
		public double PLAYER_DAMAGE_CAP = 0.08;

		@BossParam(help = "Radius in blocks the launcher checks for players. The launcher heals if it detects a " +
			"cheesing player or player death within the radius")
		public double RADIUS = 13;

		@BossParam(help = "Percent health the launcher regains when a player dies in range")
		public double PLAYER_KILL_HEAL = 1;
	}

	private static final double PLAYER_BOUNDING_BOX_XZ = 0.61;
	private static final double BULLET_HITBOX_RADIUS = 0.5 * BulletHellBoss.DEFAULT_BULLET_RADIUS;
	private static final List<Material> cheeseBlocks = Arrays.asList(
		Material.TWISTING_VINES,
		Material.CAVE_VINES,
		Material.LADDER,
		Material.WATER,
		Material.LAVA,
		Material.VINE,
		Material.WEEPING_VINES,
		Material.SCAFFOLDING
	);

	private enum DamageTernary {
		DAMAGE(),
		HEAL(),
		NEITHER();

		DamageTernary() {
		}
	}

	private final Parameters mParam;

	public BulletHellSurvivalBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = Parameters.getParameters(mBoss, identityTag, new Parameters());
		final List<Spell> passiveSpells = List.of(
			new Spell() {
				@Override
				public void run() {
					if (!mBoss.isDead()) {
						final DamageTernary outcome = shouldDamage();
						if (outcome == DamageTernary.DAMAGE) {
							mBoss.setHealth(Math.max(mBoss.getHealth() - mParam.DAMAGE_SELF * EntityUtils.getMaxHealth(mBoss), 0));
						} else if (outcome == DamageTernary.HEAL) {
							mBoss.setHealth(Math.min(mBoss.getHealth() + mParam.HEALING * EntityUtils.getMaxHealth(mBoss),
								EntityUtils.getMaxHealth(mBoss)));
						}
						// else do not heal nor damage
					}
				}

				@Override
				public int cooldownTicks() {
					return 1;
				}
			}
		);
		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null, 0);
	}

	private DamageTernary shouldDamage() {
		final List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mParam.RADIUS, false);
		if (players.isEmpty()) {
			return DamageTernary.HEAL;
		}

		for (final Player player : players) {
			final double heightDelta = player.getLocation().getY() - mBoss.getLocation().getY();
			final double jumpHeight = PlayerUtils.getJumpHeight(player);

			if (EffectManager.getInstance().hasEffect(player, RespawnStasis.class) || jumpHeight != PlayerUtils.BASE_JUMP_HEIGHT) {
				return DamageTernary.NEITHER;
			} else if (player.isSleeping() || ((heightDelta > BULLET_HITBOX_RADIUS && playerOnBlock(player))
				|| (heightDelta < -PlayerUtils.BASE_JUMP_HEIGHT || heightDelta > jumpHeight))) {
				return DamageTernary.HEAL;
			}
		}

		// All nearby players meet the conditions to hurt the boss
		return DamageTernary.DAMAGE;
	}

	private boolean playerOnBlock(final Player p) {
		return NmsUtils.getVersionAdapter().hasCollisionWithBlocks(p.getWorld(),
			p.getBoundingBox().shift(0, -0.01, 0), false)
			|| playerIsCheesing(p);
	}

	private boolean playerIsCheesing(final Player p) {
		final Location loc = p.getLocation().clone().add(0, -0.15, 0);
		for (int i = -1; i < 2; i += 2) {
			for (int j = -1; j < 2; j += 2) {
				if (cheeseBlocks.contains(loc.clone().add(i * PLAYER_BOUNDING_BOX_XZ / 2.0, 0,
					j * PLAYER_BOUNDING_BOX_XZ / 2.0).getBlock().getType())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void onHurtByEntityWithSource(final DamageEvent event, final Entity damager, final LivingEntity source) {
		if (event.getDamage() > mParam.PLAYER_DAMAGE_CAP * EntityUtils.getMaxHealth(mBoss)) {
			// Strongest attack can only shorten it by 5 seconds
			event.setDamageCap(mParam.PLAYER_DAMAGE_CAP * EntityUtils.getMaxHealth(mBoss));
		}
		final Location loc = mBoss.getLocation();
		if (source instanceof final Player p) {
			final double heightdiff = p.getLocation().getY() - loc.getY();
			final double jumpHeight = PlayerUtils.getJumpHeight(p);
			if (heightdiff > jumpHeight || heightdiff < -PlayerUtils.BASE_JUMP_HEIGHT
				|| (heightdiff > BULLET_HITBOX_RADIUS && playerOnBlock(p))) {
				event.setCancelled(true);
				loc.add(0, 1, 0);
				new PartialParticle(Particle.FIREWORKS_SPARK, loc).count(20).extra(0.3).spawnAsEntityActive(mBoss);
				mBoss.getWorld().playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.2f, 1.5f);
			}
		}
	}

	@Override
	public void nearbyPlayerDeath(final PlayerDeathEvent event) {
		if (event.getPlayer().getLocation().distanceSquared(mBoss.getLocation()) <= mParam.RADIUS * mParam.RADIUS) {
			mBoss.setHealth(Math.min(mBoss.getHealth() + mParam.PLAYER_KILL_HEAL * EntityUtils.getMaxHealth(mBoss),
				EntityUtils.getMaxHealth(mBoss)));
		}
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}
}
