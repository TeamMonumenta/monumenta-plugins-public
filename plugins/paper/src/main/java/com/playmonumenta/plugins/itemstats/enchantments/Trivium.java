package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.DamageUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public class Trivium implements Enchantment {

	private final double DAMAGE_PER_LEVEL = 0.1;

	private final Map<Player, TriviumInstance> mDamageInTick = new HashMap<>();
	private @Nullable BukkitTask mRunDamageTask = null;

	@Override
	public String getName() {
		return "Trivium";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.TRIVIUM;
	}

	@Override
	public double getPriorityAmount() {
		return 5500;
		// This needs to be decently high as we need the final damage (After calculation of other enchants)
		// The damage type will be set to "Other" to prevent iteration.
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		ClassAbility ca = event.getAbility();
		if (ca != null && event.getType() == DamageEvent.DamageType.MAGIC) {
			// Exception for Arcane Strike which can deal 2 different class abilities at once
			if (ca == ClassAbility.ARCANE_STRIKE_ENHANCED) {
				ca = ClassAbility.ARCANE_STRIKE;
			}
			mDamageInTick.computeIfAbsent(player, key -> new TriviumInstance(value)).addEvent(ca, event);

			if (mRunDamageTask == null || !Bukkit.getScheduler().isQueued(mRunDamageTask.getTaskId())) {
				mRunDamageTask = Bukkit.getScheduler().runTask(plugin, this::task);
			}
		}
	}

	private void task() {
		mDamageInTick.forEach((p, instance) -> dealDamage(p, instance.mMap, instance.mValue));
		mDamageInTick.clear();
		mRunDamageTask = null;
	}

	private void dealDamage(Player p, Map<ClassAbility, List<DamageEvent>> map, double value) {
		map.forEach((ability, eventList) -> {
			eventList = eventList.stream().filter(e -> e.getDamagee().getWorld() == p.getWorld()).toList();
			if (eventList.size() >= 3) {
				// If this tick had more than 3 of the same damage event of the same ability...
				// Proc Trivium, damage entity for 10% more gear magic damage per level.
				Location loc = p.getLocation().zero();
				for (DamageEvent e : eventList) {
					loc.add(e.getDamagee().getLocation());
					double gearDamageMultiplier = e.getGearDamageMultiplier();
					double multiplierWithTrivium = gearDamageMultiplier + (DAMAGE_PER_LEVEL * value);
					double multiplier = multiplierWithTrivium / gearDamageMultiplier - 1;
					DamageUtils.damage(p, e.getDamagee(), DamageEvent.DamageType.TRUE, e.getDamage() * multiplier, null, true, false);
				}
				// Find the average location of all entities hit
				loc.multiply((double) 1 / eventList.size());
				p.playSound(
					loc,
					Sound.ENTITY_ILLUSIONER_CAST_SPELL,
					SoundCategory.PLAYERS,
					0.8f,
					2f
				);
				p.playSound(
					loc,
					Sound.ENTITY_BLAZE_SHOOT,
					SoundCategory.PLAYERS,
					0.8f,
					2f
				);
			}
		});
	}

	private static class TriviumInstance {
		private final double mValue;
		private final Map<ClassAbility, List<DamageEvent>> mMap = new HashMap<>();

		private TriviumInstance(double value) {
			mValue = value;
		}

		private void addEvent(ClassAbility ca, DamageEvent event) {
			mMap.computeIfAbsent(ca, key -> new ArrayList<>()).add(event);
		}
	}
}
