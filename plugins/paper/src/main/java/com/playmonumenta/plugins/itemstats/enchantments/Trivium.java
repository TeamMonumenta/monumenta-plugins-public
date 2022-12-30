package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public class Trivium implements Enchantment {

	private final double DAMAGE_PER_LEVEL = 0.1;

	private HashMap<Player, HashMap<ClassAbility, List<DamageEvent>>> mDamageInTick = new HashMap<>();
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
		if (event.getType() == DamageEvent.DamageType.MAGIC && event.getAbility() != null) {
			mDamageInTick.computeIfAbsent(player, key -> new HashMap<>()).computeIfAbsent(event.getAbility(), key -> new ArrayList<>()).add(event);

			if (mRunDamageTask == null || !Bukkit.getScheduler().isQueued(mRunDamageTask.getTaskId())) {
				mRunDamageTask = Bukkit.getScheduler().runTask(plugin, () -> {
					HashMap<Player, HashMap<ClassAbility, List<DamageEvent>>> damageInTick = mDamageInTick;
					mDamageInTick = new HashMap<>();
					damageInTick.forEach((p, map) -> {
						map.forEach((ability, eventList) -> {
							if (eventList.size() >= 3) {
								// If this tick had more than 3 of the same damage event of the same ability...
								// Proc Trivium, damage entity for 10% more damage per level.
								for (DamageEvent e : eventList) {
									DamageUtils.damage(p, e.getDamagee(), DamageEvent.DamageType.OTHER, e.getDamage() * (DAMAGE_PER_LEVEL * value), null, true, false);
								}
								p.playSound(
									p.getLocation(),
									Sound.ENTITY_ILLUSIONER_CAST_SPELL,
									SoundCategory.PLAYERS,
									0.8f,
									2f
								);
								p.playSound(
									p.getLocation(),
									Sound.ENTITY_BLAZE_SHOOT,
									SoundCategory.PLAYERS,
									0.8f,
									2f
								);
							}
						});
					});
					mRunDamageTask = null;
				});
			}
		}
	}
}
