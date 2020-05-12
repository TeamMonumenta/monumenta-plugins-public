package com.playmonumenta.plugins.cooking;

public class CookingModifierCombos {
	static class ComboEntry {
		String[] mInputs;
		String mOutput;

		ComboEntry(String output, String... inputs) {
			this.mInputs = inputs;
			this.mOutput = output;
		}
	}

	public static ComboEntry[] list = {
		new ComboEntry("Claude's Dream", "Claude1", "Claude2", "Claude3", "Claude4"),
		new ComboEntry("Deadly", "Putrid", "Putrid"),
		new ComboEntry("Da Bomb", "Extra Spicy", "Extra Spicy"),
		new ComboEntry("Extra Spicy", "Spicy", "Spicy"),
		new ComboEntry("Putrid", "Decayed", "Decayed"),
		new ComboEntry("Too Salty", "Salty", "Salty"),
		new ComboEntry("Sushi", "Vinegar", "Rice and Fish"),
		new ComboEntry("Rice and Fish", "Rice", "Fish"),
		new ComboEntry("Chocolate Milkshake", "Milkshake", "Chocolate"),
		new ComboEntry("Fruit Milkshake", "Fruit", "Milkshake"),
		new ComboEntry("Milkshake", "Milk", "Ice Cream"),
		new ComboEntry("Ice Cream", "Ice", "Cream"),
		new ComboEntry("Cream", "Milk", "Fat"),
		new ComboEntry("Fries", "Potato", "Oil"),
		new ComboEntry("Crisps", "Potato", "Salty"),
		new ComboEntry("Cheese", "Milk", "Rotten"),
		new ComboEntry("Soup", "Water", "Veggie Salad"),
		new ComboEntry("Fruit Mix", "Fruit Salad", "Fruit Salad"),
		new ComboEntry("Veggie Salad", "Vegetable", "Vegetable", "Vegetable"),
		new ComboEntry("Fruit Salad", "Fruit", "Fruit", "Fruit"),
		new ComboEntry("Decayed", "Rotten", "Rotten"),
		new ComboEntry("Seasoned", "Salty", "Spicy"),
		new ComboEntry("Salty", "Salt", "Salt"),
		new ComboEntry("Spicy", "Spice", "Spice"),
		new ComboEntry("Borsch", "Water", "Beetroot"),
		new ComboEntry("Cake", "Cake Base"),
		new ComboEntry("Cake Base", "Dough", "Egg", "sugar"),
		new ComboEntry("Crêpe", "Dough", "Egg", "Pan"),
		new ComboEntry("Dough", "Wheat", "Cream"),
		new ComboEntry("Dough", "Wheat", "Water", "Milk"),

	};
}
