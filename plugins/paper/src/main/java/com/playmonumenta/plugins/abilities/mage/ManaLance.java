package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.ManaLanceCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;



public class ManaLance extends Ability {

	private static final float DAMAGE_1 = 6.0f;
	private static final float DAMAGE_2 = 7.0f;
	private static final int COOLDOWN_1 = 5 * 20;
	private static final int COOLDOWN_2 = 3 * 20;

	private final ManaLanceCS mCosmetic;

	public ManaLance(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Mana Lance");
		mInfo.mLinkedSpell = ClassAbility.MANA_LANCE;
		mInfo.mScoreboardId = "ManaLance";
		mInfo.mShorthandName = "ML";
		mInfo.mDescriptions.add("Right clicking with a wand fires forth a piercing beam of Mana going 8 blocks, dealing 6 magic damage to enemies in the path of the beam. This beam will not go through solid blocks. Cooldown: 5s.");
		mInfo.mDescriptions.add("The beam instead deals 7 damage. Cooldown: 3s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? COOLDOWN_1 : COOLDOWN_2;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.TRIDENT, 1);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ManaLanceCS(), ManaLanceCS.SKIN_LIST);
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		float damage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
		damage = SpellPower.getSpellDamage(mPlugin, mPlayer, damage);
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		BoundingBox box = BoundingBox.of(loc, 0.55, 0.55, 0.55);
		Vector dir = loc.getDirection();
		box.shift(dir);
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getLocation(), 10, mPlayer);
		World world = mPlayer.getWorld();

		Location endLoc = loc;

		for (int i = 0; i < 8; i++) {
			box.shift(dir);
			Location bLoc = box.getCenter().toLocation(world);
			endLoc = bLoc;

			if (!bLoc.isChunkLoaded() || bLoc.getBlock().getType().isSolid()) {
				bLoc.subtract(dir.multiply(0.5));
				mCosmetic.lanceHit(mPlayer, bLoc, world);
				break;
			}
			Iterator<LivingEntity> iter = mobs.iterator();
			while (iter.hasNext()) {
				LivingEntity mob = iter.next();
				if (box.overlaps(mob.getBoundingBox())) {
					DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, damage, mInfo.mLinkedSpell, true);
					MovementUtils.knockAway(mPlayer.getLocation(), mob, 0.25f, 0.25f, true);
					iter.remove();
				}
			}
		}

		mCosmetic.lanceParticle(mPlayer, loc, endLoc);
		mCosmetic.lanceSound(world, mPlayer);
	}

	@Override
	public boolean runCheck() {
		if (mPlayer == null) {
			return false;
		}
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return !mPlayer.isSneaking() && ItemUtils.isWand(mainHand);
	}

}
