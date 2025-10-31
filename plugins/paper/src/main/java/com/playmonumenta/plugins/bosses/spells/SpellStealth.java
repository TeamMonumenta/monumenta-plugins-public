package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.StealthBoss;
import com.playmonumenta.plugins.delves.abilities.Cloaked;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SpellStealth extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final StealthBoss.Parameters mParameters;
	Map<Entity, ItemUtils.EquipmentItems> mCloakedMobs = new HashMap<>();

	@Nullable
	private ItemUtils.EquipmentItems mEquipmentItems;

	private boolean mActive = false;
	private boolean mWasGlowing = false;

	public static final String STEALTH_IMMUNE = "boss_stealthimmune";

	public SpellStealth(Plugin plugin, LivingEntity boss, StealthBoss.Parameters parameters) {
		mPlugin = plugin;
		mBoss = boss;
		mParameters = parameters;
	}

	@Override
	public void run() {
		// If the boss is still in stealth, exit stealth to reset armour properly
		if (mActive) {
			cancel();
			endStealth();
		}
		if (mParameters.HIDE_GLOWING && mBoss.isGlowing()) {
			mBoss.setGlowing(false);
			mWasGlowing = true;
		}
		mParameters.EFFECTS_STEALTH.apply(mBoss, mBoss);
		mParameters.SOUND_STEALTH.play(mBoss.getLocation());
		mParameters.PARTICLE_STEALTH.spawn(mBoss, mBoss.getLocation());

		@Nullable
		EntityEquipment entityEquipment = mBoss.getEquipment();
		if (entityEquipment != null) {
			mEquipmentItems = ItemUtils.getEquipmentItems(entityEquipment);
			entityEquipment.clear();
		}
		mBoss.setInvisible(true);

		if (mParameters.STEALTH_PASSENGERS) {
			List<Entity> cloakedMobsList = mBoss.getPassengers();
			for (Entity cloakedMob : cloakedMobsList) {
				if (!(cloakedMob.getScoreboardTags().contains(STEALTH_IMMUNE)
					|| cloakedMob.getScoreboardTags().contains(Cloaked.AVOID_CLOAKED))) {
					if (cloakedMob instanceof LivingEntity) {
						@Nullable
						EntityEquipment cloakedMobEquipment = ((LivingEntity) cloakedMob).getEquipment();
						if (cloakedMobEquipment != null) {
							ItemUtils.EquipmentItems cloakedMobEquipmentItems = ItemUtils.getEquipmentItems(cloakedMobEquipment);
							cloakedMobEquipment.clear();
							mCloakedMobs.put(cloakedMob, cloakedMobEquipmentItems);
						} else {
							mCloakedMobs.put(cloakedMob, null);
						}
					} else {
						mCloakedMobs.put(cloakedMob, null);
					}
					cloakedMob.setInvisible(true);
				}
			}
		}


		mActive = true;
		if (mParameters.DURATION >= 0) {
			mActiveTasks.add(new BukkitRunnable() {
				@Override
				public void run() {
					endStealth();
				}
			}.runTaskLater(mPlugin, mParameters.DURATION));
		}

		if (mParameters.PROXIMITY_CHECK) {
			// For performance reasons, we don't run this unless the parameter is enabled
			mActiveTasks.add(new BukkitRunnable() {
				@Override
				public void run() {
					List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mParameters.PROXIMITY, true, false);
					if (!players.isEmpty()) {
						for (Player p : players) {
							// Method might not be performant. Keep stealth detection ranges low.
							if (mBoss.hasLineOfSight(p)) {
								endStealth();
								this.cancel();
							}
						}
					}
				}
			}.runTaskTimer(mPlugin, 0, 1));
			// This method might not be performant. MUST CHECK the lag on a server when running full delves with it.
			// If needed, we might have to make them unstealth after some number of ticks.
		}
	}

	private void endStealth() {
		mActive = false;
		// If the arrays are empty, do not set entity equipment as the entity either has none, or has bugged
		if (mEquipmentItems == null) {
			return;
		}

		if (mParameters.HIDE_GLOWING && mWasGlowing) {
			mBoss.setGlowing(true);
			mWasGlowing = false;
		}
		mParameters.SOUND_EXIT.play(mBoss.getLocation());
		mParameters.PARTICLE_EXIT.spawn(mBoss, mBoss.getLocation());

		@Nullable
		EntityEquipment entityEquipment = mBoss.getEquipment();
		if (entityEquipment != null) {
			// This is probably causing the bug where vanities vanish - the entityEquipment is supposed to be null after we cleared it????
			// Fixable but not tonight, scream at me next week
			ItemUtils.setEquipmentItems(entityEquipment, mEquipmentItems);
		}
		mBoss.setInvisible(false);

		if (mParameters.STEALTH_PASSENGERS) {
			for (Entity cloakedMob : mCloakedMobs.keySet()) {
				if (mCloakedMobs.get(cloakedMob) != null) {
					@Nullable
					EntityEquipment cloakedMobEquipment = ((LivingEntity) cloakedMob).getEquipment();
					if (cloakedMobEquipment != null) {
						ItemUtils.setEquipmentItems(cloakedMobEquipment, mCloakedMobs.get(cloakedMob));
					}
				}
				cloakedMob.setInvisible(false);
			}
		}
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		super.onDamage(event, damagee);
		// stop the stealth tick down
		if (mActive) {
			cancel();
			endStealth();
			mParameters.EFFECTS_HIT.apply(damagee, mBoss);
			mParameters.PARTICLE_HIT.spawn(mBoss, damagee.getLocation());
			mParameters.SOUND_HIT.play(mBoss.getLocation());
			DamageUtils.damage(mBoss, damagee, DamageEvent.DamageType.MELEE_SKILL, mParameters.DAMAGE, null, true, false, mParameters.SPELL_NAME);
		}
	}

	@Override
	public int cooldownTicks() {
		return mParameters.COOLDOWN;
	}
}
