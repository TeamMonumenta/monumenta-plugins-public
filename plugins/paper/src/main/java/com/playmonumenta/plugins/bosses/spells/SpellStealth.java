package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.StealthBoss;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SpellStealth extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final StealthBoss.Parameters mParameters;

	@Nullable
	private ItemUtils.EquipmentItems mEquipmentItems;

	private boolean mActive = false;
	private boolean mWasGlowing = false;

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

		mActive = true;
		mActiveTasks.add(new BukkitRunnable() {
			@Override
			public void run() {
				endStealth();
			}
		}.runTaskLater(mPlugin, mParameters.DURATION));
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
			ItemUtils.setEquipmentItems(entityEquipment, mEquipmentItems);
		}
		mBoss.setInvisible(false);
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
