package pe.bossfights.utils;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;

public class SerializationUtils
{
	private static final String SERIALCONST = "SERIALDATA=";
	private static final int SERIALLEN = 50;

	public static List<String> serializeStringToLore(String data)
	{
		List<String> ret = new LinkedList<String>();
		for (int start = 0; start < data.length(); start += SERIALLEN)
			ret.add(SERIALCONST + data.substring(start, Math.min(data.length(), start + SERIALLEN)));
		return ret;
	}

	public static String deserializeStringFromLore(List<String> lore)
	{
		String retval = "";
		for (String str : lore)
			if (str.startsWith(SERIALCONST))
				retval += str.substring(SERIALCONST.length());
		return retval;
	}


	public static void storeDataOnEntity(LivingEntity entity, String data)
	{
		boolean placeholderItem = false;

		EntityEquipment equip = entity.getEquipment();
		if (equip == null)
		{
			//TODO: ERROR!
			return;
		}

		ItemStack item = equip.getItemInOffHand();
		if (item == null)
		{
			item = new ItemStack(Material.GREEN_RECORD);
			placeholderItem = true;
		}

		ItemMeta meta = item.getItemMeta();
		if (meta == null)
		{
			// TODO: ERROR!
			return;
		}

		if (placeholderItem)
			meta.setDisplayName(SERIALCONST);

		List<String> addLore = serializeStringToLore(data);
		if (meta.hasLore())
		{
			List<String> currentLore = meta.getLore();

			/* Remove existing serialization data, if any */
			currentLore.removeIf(lore -> lore.startsWith(SERIALCONST));

			currentLore.addAll(addLore);
			meta.setLore(currentLore);
		}
		else
			meta.setLore(addLore);
	}

	public static String retrieveDataFromEntity(LivingEntity entity)
	{
		EntityEquipment equip = entity.getEquipment();
		if (equip == null)
		{
			//TODO: ERROR!
			return "";
		}

		ItemStack item = equip.getItemInOffHand();
		if (item == null || !item.hasItemMeta())
			return "";

		ItemMeta meta = item.getItemMeta();
		if (meta == null || !meta.hasLore())
			return "";

		List<String> currentLore = meta.getLore();
		String data = deserializeStringFromLore(currentLore);

		/* Don't leave any serialization data on the entity */
		currentLore.removeIf(lore -> lore.startsWith(SERIALCONST));
		meta.setLore(currentLore);

		/* If this item's only purpose for existing was to hold data, remove it */
		if (meta.hasDisplayName() && meta.getDisplayName().equals(SERIALCONST))
			equip.setItemInOffHand(null);

		return data;
	}
}
