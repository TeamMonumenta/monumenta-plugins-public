package com.playmonumenta.plugins.parrots;

import com.playmonumenta.plugins.bosses.spells.Spell;
import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.Parrot;

class RainbowParrotPassive extends Spell {
	private static final List<Parrot.Variant> mVariants = Arrays.asList(
		Parrot.Variant.BLUE,
		Parrot.Variant.CYAN,
		Parrot.Variant.GRAY,
		Parrot.Variant.GREEN,
		Parrot.Variant.RED
	);

	private final Parrot mParrot;
	private int mIndex;

	public RainbowParrotPassive(Parrot parrot, int start) {
		mParrot = parrot;
		mIndex = start;
	}

	@Override
	public void run() {
		mParrot.setVariant(mVariants.get(mIndex % 5));
		mIndex++;
	}

	@Override
	public int cooldownTicks() {
		return 20;
	}
}
