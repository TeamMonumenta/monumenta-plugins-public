package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Operation;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AttackDamageAdd implements Attribute {

	@Override
	public String getName() {
		return "Attack Damage Add";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.ATTACK_DAMAGE_ADD;
	}

	@Override
	public double getPriorityAmount() {
		return 2;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			event.setDamage(event.getDamage() + value);
		} else if (event.getType() == DamageType.MELEE_SKILL) {
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType().isAir()) {
				return;
			}
			NBTItem nbt = new NBTItem(item);
			NBTCompoundList compound = ItemStatUtils.getAttributes(nbt);
			event.setDamage(event.getDamage() + (value - ItemStatUtils.getAttributeAmount(compound, AttributeType.ATTACK_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND)));
		}
	}
}
