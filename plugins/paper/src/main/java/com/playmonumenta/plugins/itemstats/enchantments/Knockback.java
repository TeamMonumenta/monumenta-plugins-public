package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.EnumSet;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.util.Vector;

public class Knockback implements Enchantment {
	public static final float KB_VEL_PER_LEVEL = 0.5f;
	public static final float VERTICAL_LAUNCH = 0.2f;

	@Override
	public String getName() {
		return "Knockback";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.KNOCKBACK;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 31;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.MELEE) {
			applyKnockback(plugin, level, enemy, player.getLocation().getDirection());
		}
	}

	public static void applyKnockback(Plugin plugin, double level, LivingEntity enemy, Vector direction) {
		if (level <= 0) {
			return;
		}
		com.playmonumenta.scriptedquests.Plugin scriptedQuestsPlugin = (com.playmonumenta.scriptedquests.Plugin) Bukkit.getPluginManager().getPlugin("ScriptedQuests");
		if (enemy instanceof Villager
			|| enemy instanceof ArmorStand
			|| enemy instanceof Player
			|| (scriptedQuestsPlugin != null && scriptedQuestsPlugin.mNpcManager.isQuestNPC(enemy))) {
			return;
		}
			float speed = KB_VEL_PER_LEVEL * (float) level;
			direction.setY(0);
			if (direction.length() < 0.001) {
				direction = new Vector(0, VERTICAL_LAUNCH, 0);
			} else {
				direction.normalize()
					.multiply(speed)
					.setY(VERTICAL_LAUNCH);
			}
		// TODO: Override the Minecraft default attack knockback (which is 0.4f), then we can add 0.2f to speed and remove the need to fire the KB event 1 tick late
			final Vector dir = direction.clone();
			Bukkit.getScheduler().runTask(plugin, () -> MovementUtils.knockAwayDirection(dir, enemy, 0.5f, true, false));
	}
}
