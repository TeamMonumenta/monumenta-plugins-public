package com.playmonumenta.plugins.depths.bosses.spells.broodmother;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.bosses.Broodmother;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

public class PassivePoisonousSkin extends Spell {
	public static String SPELL_NAME = "Poisonous Skin";
	public static int TRIGGER_COOLDOWN = 5;

	private final LivingEntity mBoss;
	private final @Nullable DepthsParty mParty;
	private final Hitbox mSkinArea;

	private int mTimer = 0;

	public PassivePoisonousSkin(LivingEntity boss, @Nullable DepthsParty party) {
		mBoss = boss;
		mParty = party;
		mSkinArea = new Hitbox.AABBHitbox(mBoss.getWorld(), BoundingBox.of(mBoss.getLocation().add(-9, 2, 13), mBoss.getLocation().add(20, 15, -13)));
	}

	@Override
	public void run() {
		if (mTimer >= TRIGGER_COOLDOWN) {
			mSkinArea.getHitPlayers(true).forEach(player -> {
				if (PlayerUtils.isOnGround(player)) {
					DamageUtils.damage(mBoss, player, DamageEvent.DamageType.TRUE, 1, null, true, false, SPELL_NAME);
					PotionUtils.applyPotion(mBoss, player, new PotionEffect(PotionEffectType.POISON, 100, 1));
					PotionUtils.applyPotion(mBoss, player, new PotionEffect(PotionEffectType.WITHER, 100, 1));
					if (mParty != null && mParty.getAscension() >= 4) {
						EntityUtils.applyVulnerability(Plugin.getInstance(), 100, Broodmother.getVulnerabilityAmount(mParty), player);
					}
				}
			});
			mTimer = 0;
		}
		mTimer++;
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}

}
