package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.Arrays;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;

public class FestiveTessUpgradeSnowmenBoss extends BossAbilityGroup {
	public static final String deathMetakey = "PLAYER_SNOWMAN_DEATH_METAKEY";
	public static final String identityTag = "boss_festivetess_snowman";
	public static final int detectionRange = 50;
	private static final int LIFETIME = 60 * 20;
	private Parameters mParams;
	private int mTicksLived = 0;

	public static class Parameters extends BossParameters {
		@BossParam(help = "int")
		public int DAMAGE = 1;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FestiveTessUpgradeSnowmenBoss(plugin, boss);
	}

	public FestiveTessUpgradeSnowmenBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);

		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());

		List<Spell> passiveSpells = Arrays.asList(
			new SpellRunAction(() -> {
				mTicksLived += 5;
				if (mTicksLived > LIFETIME) {
					boss.damage(999);
				}
			})
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}

	@Override
	public void onHurt(DamageEvent event) {
		Entity damager = event.getDamager();

		if (damager != null && damager instanceof Player player && player.getGameMode().equals(GameMode.CREATIVE)) {
			// This event happens like normal
			return;
		} else if (damager instanceof Snowball) {
			return;
		}

		Location loc = mBoss.getLocation();
		loc.getWorld().playSound(loc, Sound.BLOCK_CORAL_BLOCK_BREAK, SoundCategory.HOSTILE, 2, 0);
		new PartialParticle(Particle.CLOUD, loc, 100, 1, 1, 1, 0.1).spawnAsEntityActive(mBoss);
		event.setDamage(1);
	}

	@Override
	public void bossProjectileHit(ProjectileHitEvent event) {
		if (event.isCancelled()) {
			return;
		}

		if (event.getHitEntity() != null && (event.getHitEntity().isInvulnerable() || ZoneUtils.hasZoneProperty(event.getHitEntity(), ZoneProperty.ADVENTURE_MODE))) {
			return;
		}

		if (event.getHitEntity() != null && !(event.getHitEntity() instanceof Player) &&
			    event.getHitEntity() instanceof LivingEntity le && !(event.getHitEntity() instanceof Snowman)) {
			DamageUtils.damage(mBoss, le, DamageType.PROJECTILE, mParams.DAMAGE);
		}
	}

	@Override
	public void areaEffectAppliedToBoss(AreaEffectCloudApplyEvent event) {
		event.getAffectedEntities().remove(mBoss);
	}

	@Override
	public void splashPotionAppliedToBoss(PotionSplashEvent event) {
		event.getAffectedEntities().remove(mBoss);
	}
}
