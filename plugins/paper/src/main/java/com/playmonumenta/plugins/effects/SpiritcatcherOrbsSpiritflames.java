package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.shaman.hexbreaker.SpiritcatcherOrbsCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SpiritcatcherOrbsSpiritflames extends Effect {
	private static final int DAMAGE_INTERVAL = 20;
	public static final String effectID = "SpiritcatcherOrbsSpiritflames";

	private final Player mPlayer;
	private final ClassAbility mSpell;
	private final PlayerItemStats mPlayerItemStats;
	private final SpiritcatcherOrbsCS mCosmetic;
	private final double mDamage;
	private final double mRange;

	private int mNumDamages;
	private @Nullable Location mDeadLocation = null;

	public SpiritcatcherOrbsSpiritflames(int duration, double damage, double range, Player player, ClassAbility linkedSpell, PlayerItemStats stats, SpiritcatcherOrbsCS cosmetic) {
		super(duration, effectID);
		mPlayer = player;
		mSpell = linkedSpell;
		mPlayerItemStats = stats;
		mCosmetic = cosmetic;
		mDamage = damage;
		mRange = range;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		mNumDamages = mDuration / DAMAGE_INTERVAL;
		String name = entity.getName();

		new BukkitRunnable() {
			double mTicks = -1;

			@Override
			public void run() {
				mTicks++;

				if (mTicks % DAMAGE_INTERVAL != 0) {
					return;
				}

				List<LivingEntity> affectedMobs = new Hitbox.SphereHitbox(mDeadLocation != null ? mDeadLocation : entity.getLocation(), mRange).getHitMobs().stream()
					.filter(enemy -> enemy.getName().equals(name))
					.toList();

				for (LivingEntity le : affectedMobs) {
					mCosmetic.spiritflamesFlameVisuals(le, mPlayer);
					DamageUtils.damage(mPlayer, le, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mSpell, mPlayerItemStats), mDamage, true, false, false);
				}
				mNumDamages--;

				if (mNumDamages <= 0) {
					clearEffect();
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		mDeadLocation = event.getEntity().getLocation();
	}

	public void extendDuration(int duration) {
		setDuration(duration);
		mNumDamages = duration / DAMAGE_INTERVAL;
	}

	@Override
	public String toString() {
		return effectID + " duration: " + getDuration();
	}
}
