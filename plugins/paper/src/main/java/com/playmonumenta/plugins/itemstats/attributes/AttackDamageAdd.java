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
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
	public void onDamage(@NotNull Plugin plugin, @NotNull Player player, double value, @NotNull DamageEvent event, @NotNull LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			event.setDamage(event.getDamage() + value);
		} else if (event.getType() == DamageType.MELEE_SKILL && player.getItemInHand().getType() != Material.AIR) {
			NBTItem nbt = new NBTItem(player.getItemInHand());
			NBTCompoundList compound = ItemStatUtils.getAttributes(nbt);
			event.setDamage(event.getDamage() + (value - ItemStatUtils.getAttributeAmount(compound, AttributeType.ATTACK_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND)));
		}
	}
}
