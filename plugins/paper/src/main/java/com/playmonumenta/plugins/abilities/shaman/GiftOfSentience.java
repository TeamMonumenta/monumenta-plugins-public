package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import org.bukkit.Material;
import org.bukkit.entity.Cat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class GiftOfSentience extends Ability {

	private static final int COOLDOWN = 60 * 20;

	public static final AbilityInfo<GiftOfSentience> INFO =
		new AbilityInfo<>(GiftOfSentience.class, "The Gift of Sentience", GiftOfSentience::new)
			.linkedSpell(ClassAbility.GIFT_OF_SENTIENCE)
			.scoreboardId("GiftOfSentience")
			.shorthandName("GoS")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Share the gift of sentience")
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", GiftOfSentience::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)))
			.displayItem(Material.EXPERIENCE_BOTTLE);


	public GiftOfSentience(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		for (LivingEntity totem : ShamanPassiveManager.getTotemList(mPlayer)) {
			if (totem.getVehicle() != null) {
				continue;
			}
			Cat kitty = totem.getWorld().spawn(totem.getLocation(), Cat.class);
			kitty.addPassenger(totem);
			kitty.setInvulnerable(true);
			kitty.setPersistent(false);
		}

		return true;
	}

	private static Description<GiftOfSentience> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addDashedLine();
	}

	private static Description<GiftOfSentience> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addDashedLine();
	}

	private static Description<GiftOfSentience> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addTrigger()
			.addDashedLine()
			.addLine("With your immense power, you finally grant yourself")
			.addLine("and your totems the gift of sentience. With your")
			.addLine("totems' new found sentience, they become ferocious")
			.addLine("warriors that can walk, talk and strategize...")
			.addDashedLine();
	}

}
