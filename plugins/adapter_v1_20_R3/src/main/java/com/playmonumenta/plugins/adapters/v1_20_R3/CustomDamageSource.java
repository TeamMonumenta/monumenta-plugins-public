package com.playmonumenta.plugins.adapters.v1_20_R3;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CustomDamageSource extends DamageSource {
	private final boolean mBlockable;
	@Nullable
	private final String mKilledUsingMsg;
	@Nullable
	private final net.minecraft.world.entity.Entity mDamager;

	public CustomDamageSource(Holder<DamageType> type, @Nullable net.minecraft.world.entity.Entity damager,
							  boolean blockable, @Nullable String killedUsingMsg) {
		super(type, damager, damager);
		mDamager = damager;
		mBlockable = blockable;
		mKilledUsingMsg = killedUsingMsg;
	}

	@Override
	public @Nullable Vec3 getSourcePosition() {
		return mBlockable ? super.getSourcePosition() : null;
	}

	@Override
	public Component getLocalizedDeathMessage(net.minecraft.world.entity.LivingEntity killed) {
		if (this.mDamager == null) {
			// death.attack.magic=%1$s was killed by magic
			String s = "death.attack.magic";
			return Component.translatable(s, killed.getDisplayName());
		} else if (mKilledUsingMsg == null || mKilledUsingMsg.isEmpty()) {
			// death.attack.mob=%1$s was killed by %2$s
			String s = "death.attack.mob";
			return Component.translatable(s, killed.getDisplayName(), this.mDamager.getDisplayName());
		} else {
			// death.attack.indirectMagic.item=%1$s was killed by %2$s using %3$s
			String s = "death.attack.indirectMagic.item";
			return Component.translatable(s, killed.getDisplayName(), this.mDamager.getDisplayName(), mKilledUsingMsg);
		}
	}
}

