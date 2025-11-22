package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentAbilityDamageReceived;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class IdolatryBoss extends BossAbilityGroup implements Listener {

	public static final String identityTag = "boss_idolatry";

	private static final String IDOLATRY_IMMUNE_TAG = "boss_idolatryimmune";
	// metadata keys to handle multiple idols damaging the same target in one tick
	private static final String PROCESSED_METADATA_PREFIX = "idolatry_redirect_tick_";
	private static final String ORIGINAL_DAMAGE_METADATA = "idolatry_original_damage";
	private final Parameters mParams;
	private boolean mHasNotInitiatedNextTickRunnable = true;
	private final Map<LivingEntity, Double> mEntityDamageMap = new HashMap<>();

	public static class Parameters extends BossParameters {
		@BossParam(help = "Spherical radius centered at the idol in which it can take redirected damage from.")
		public double RADIUS = 8.0;
		@BossParam(help = "The percentage of the original damage that is redirected to the idol.")
		public double DAMAGE_REDIRECT_PERCENTAGE = 0.75;
		@BossParam(help = "The percentage of the original damage that the target still receives. The rest is absorbed.")
		public double DAMAGE_PASSTHROUGH_PERCENTAGE = 0.10;
		@BossParam(help = "Sounds played at the idol's location when damage is redirected.")
		public SoundsList SOUNDS_REDIRECT = SoundsList.fromString("[(ENTITY_VEX_HURT, 1.0, 0.6), (ITEM_SHIELD_BLOCK, 1.0, 0.5), (ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0, 1.65)]");
		@BossParam(help = "Number of damage redirects this Idol can take per tick.")
		public int MAX_REDIRECTS = 3;
	}

	public IdolatryBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		this.mParams = BossParameters.getParameters(boss, identityTag, new Parameters());
		List<Spell> passiveSpells = List.of();
		super.constructBoss(SpellManager.EMPTY, passiveSpells, (int) Math.ceil(mParams.RADIUS), null, 0, 1);
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityDamaged(DamageEvent event) {
		LivingEntity damagedEntity = event.getDamagee();
		if (mBoss == null || mBoss.isDead()
			|| damagedEntity.getWorld() != mBoss.getWorld()
			|| mBoss.getLocation().distanceSquared(damagedEntity.getLocation()) > mParams.RADIUS * mParams.RADIUS
			|| damagedEntity.equals(mBoss)
			|| event.getType() == DamageEvent.DamageType.PROJECTILE // Prevents doubled Projectile damage
			|| ElementalArrows.isElementalArrowDamage(event) // Prevents doubled EArrows damage
			|| event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION
			|| damagedEntity instanceof Player
			|| damagedEntity instanceof Creeper
			|| DelvesUtils.isDelveMob(damagedEntity)
			|| EntityUtils.isSilenced(mBoss)
			|| damagedEntity.getScoreboardTags().contains(identityTag)
			|| damagedEntity.getScoreboardTags().contains(IDOLATRY_IMMUNE_TAG)
			|| event.getAbility() == ClassAbility.COUP_DE_GRACE
			|| event.getDamager() == damagedEntity) {
			return;
		} // This event is called a LOT and has a LOT of if statements. someone very very experienced with the damage pipeline should optimise the order
		// Note on the doubled proj and earrows damage: Projectile hits deal both a normal and a True damage pop, because of... iframes probably.
		// Despite allegedly cancelling if the event is cancelled, Idolatry processes both.

		// reduces damage on mob once, but sends to other damage amount to other idols
		String metadataKey = PROCESSED_METADATA_PREFIX + damagedEntity.getWorld().getFullTime() + event.getAbility();
		double originalDamage;
		if (damagedEntity.hasMetadata(metadataKey)) {
			// another idol reduced damage, make this idol take damage but not mob
			List<MetadataValue> values = damagedEntity.getMetadata(ORIGINAL_DAMAGE_METADATA);
			if (values.isEmpty()) {
				return;
			}
			originalDamage = values.get(0).asDouble();
		} else {
			// first idol damage, store that so other idols know
			originalDamage = event.getFinalDamage(true);
			damagedEntity.setMetadata(ORIGINAL_DAMAGE_METADATA, new FixedMetadataValue(mPlugin, originalDamage));
			damagedEntity.setMetadata(metadataKey, new FixedMetadataValue(mPlugin, true));
			// amount that the mob still takes
			event.setFlatDamage(event.getFlatDamage() * mParams.DAMAGE_PASSTHROUGH_PERCENTAGE);
			Bukkit.getScheduler().runTask(mPlugin, () -> {
				damagedEntity.removeMetadata(metadataKey, mPlugin);
				damagedEntity.removeMetadata(ORIGINAL_DAMAGE_METADATA, mPlugin);
			});
		}

		originalDamage = Math.min(originalDamage, EntityUtils.getMaxHealth(damagedEntity));
		if (originalDamage <= 0) {
			return;
		}
		double redirectedDamage = originalDamage * mParams.DAMAGE_REDIRECT_PERCENTAGE;
		// Put this into a Map for every tick
		if (mEntityDamageMap.containsKey(damagedEntity)) {
			// Add the damage taken from this enemy this tick
			mEntityDamageMap.replace(damagedEntity, mEntityDamageMap.get(damagedEntity) + redirectedDamage);
		} else {
			// Create a key for the damage from this enemy
			mEntityDamageMap.put(damagedEntity, redirectedDamage);
		}
		// Next tick, sort the list, but only one time...
		if (mHasNotInitiatedNextTickRunnable) {
			new BukkitRunnable() {
				@Override
				public void run() {
					ArrayList<Double> sortedEntityDamageMap = new ArrayList<>(mEntityDamageMap.values());
					Collections.sort(sortedEntityDamageMap);
					// After the list is sorted, take the top 3 elements and whack the boss with it
					int i = 0;
					while (i < mParams.MAX_REDIRECTS && !sortedEntityDamageMap.isEmpty()) {
						// Update to getLast() method when upgrading to Java 21
						DamageUtils.damage(mBoss,
							mBoss,
							DamageEvent.DamageType.TRUE,
							sortedEntityDamageMap.remove(sortedEntityDamageMap.size() - 1),
							null,
							true,
							false);
						i++;
					}
					mHasNotInitiatedNextTickRunnable = true;
					mEntityDamageMap.clear();
				}
			}.runTaskLater(mPlugin, 1);
			mHasNotInitiatedNextTickRunnable = false;
		}

		long currentTick = mBoss.getWorld().getFullTime();
		String soundMetadataKey = "idolatry_redirect_sound_" + currentTick;
		if (!damagedEntity.hasMetadata(soundMetadataKey)) {
			mParams.SOUNDS_REDIRECT.play(mBoss.getLocation());
			damagedEntity.setMetadata(soundMetadataKey, new FixedMetadataValue(mPlugin, true));
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				damagedEntity.removeMetadata(soundMetadataKey, mPlugin);
			}, 5L);
		}
		// VFX: particle line
		new PPLine(Particle.SOUL_FIRE_FLAME, damagedEntity.getEyeLocation(), mBoss.getEyeLocation(), 0.08).deltaVariance(true).countPerMeter(2).spawnAsEnemy();
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onHeal(EntityRegainHealthEvent event) {
		if (event.getEntity().equals(mBoss)) {
			event.setCancelled(true);
		}
	}

	@Override
	public void customEffectAppliedToBoss(CustomEffectApplyEvent event) {
		Effect effect = event.getEffect();
		if ((effect instanceof PercentDamageReceived || effect instanceof PercentAbilityDamageReceived) && effect.isDebuff()) {
			event.setCancelled(true);
		}
	}
}
