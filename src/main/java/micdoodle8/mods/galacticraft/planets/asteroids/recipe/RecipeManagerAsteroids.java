/*
 * Copyright (c) 2023 Team Galacticraft
 *
 * Licensed under the MIT license.
 * See LICENSE file in the project root for details.
 */

package micdoodle8.mods.galacticraft.planets.asteroids.recipe;

import java.util.HashMap;
import micdoodle8.mods.galacticraft.api.GalacticraftRegistry;
import micdoodle8.mods.galacticraft.api.recipe.CompressorRecipes;
import micdoodle8.mods.galacticraft.core.GCItems;
import micdoodle8.mods.galacticraft.core.recipe.NasaWorkbenchRecipe;
import micdoodle8.mods.galacticraft.core.util.CompatibilityManager;
import micdoodle8.mods.galacticraft.core.util.RecipeUtil;
import micdoodle8.mods.galacticraft.planets.asteroids.blocks.AsteroidBlocks;
import micdoodle8.mods.galacticraft.planets.asteroids.items.AsteroidsItems;
import micdoodle8.mods.galacticraft.planets.mars.items.MarsItems;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

public class RecipeManagerAsteroids
{

    public static void addUniversalRecipes()
    {
        ItemStack titaniumIngot = new ItemStack(AsteroidsItems.basicItem, 1, 0);
        ItemStack platingTier3 = new ItemStack(AsteroidsItems.basicItem, 1, 5);

        // 熔炉配方
        FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(AsteroidBlocks.blockBasic, 1, 3), new ItemStack(GCItems.basicItem, 1, 5), 0.5F);
        FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(AsteroidBlocks.blockBasic, 1, 4), new ItemStack(AsteroidsItems.basicItem, 1, 0), 0.5F);
        FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(AsteroidBlocks.blockBasic, 1, 5), new ItemStack(Items.IRON_INGOT), 0.5F);
        FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(AsteroidsItems.basicItem, 1, 3), new ItemStack(Items.IRON_INGOT), 0.5F);
        FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(AsteroidsItems.basicItem, 1, 4), new ItemStack(AsteroidsItems.basicItem, 1, 0), 0.5F);
        FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(AsteroidsItems.basicItem, 1, 9), new ItemStack(AsteroidsItems.basicItem, 1, 0), 0.5F);

        // 基础合成
        CompressorRecipes.addShapelessRecipe(new ItemStack(AsteroidsItems.basicItem, 1, 6), titaniumIngot, titaniumIngot);
        CompressorRecipes.addShapelessRecipe(platingTier3, new ItemStack(MarsItems.marsItemBasic, 1, 3), new ItemStack(MarsItems.marsItemBasic, 1, 5));

        // NASA 工作台 - T3 火箭配方
        HashMap<Integer, ItemStack> input = new HashMap<>();
        ItemStack plateTier3 = new ItemStack(AsteroidsItems.basicItem, 1, 5);
        ItemStack rocketFinsTier2 = new ItemStack(AsteroidsItems.basicItem, 1, 2);
        input.put(1, new ItemStack(AsteroidsItems.heavyNoseCone));
        input.put(2, plateTier3);
        input.put(3, plateTier3);
        input.put(4, plateTier3);
        input.put(5, plateTier3);
        input.put(6, plateTier3);
        input.put(7, plateTier3);
        input.put(8, plateTier3);
        input.put(9, plateTier3);
        input.put(10, plateTier3);
        input.put(11, plateTier3);
        input.put(12, new ItemStack(GCItems.rocketEngine, 1, 1));
        input.put(13, rocketFinsTier2);
        input.put(14, rocketFinsTier2);
        input.put(15, new ItemStack(AsteroidsItems.basicItem, 1, 1));
        input.put(16, new ItemStack(GCItems.rocketEngine, 1, 1));
        input.put(17, rocketFinsTier2);
        input.put(18, rocketFinsTier2);
        input.put(19, ItemStack.EMPTY);
        input.put(20, ItemStack.EMPTY);
        input.put(21, ItemStack.EMPTY);
        GalacticraftRegistry.addT3RocketRecipe(new NasaWorkbenchRecipe(new ItemStack(AsteroidsItems.tier3Rocket, 1, 0), input));

        NonNullList<ItemStack> woodChests = OreDictionary.getOres("chestWood");
        HashMap<Integer, ItemStack> input2;

        for (ItemStack woodChest : woodChests)
        {
            input2 = new HashMap<Integer, ItemStack>(input);
            input2.put(19, woodChest);
            input2.put(20, ItemStack.EMPTY);
            input2.put(21, ItemStack.EMPTY);
            GalacticraftRegistry.addT3RocketRecipe(new NasaWorkbenchRecipe(new ItemStack(AsteroidsItems.tier3Rocket, 1, 1), input2));

            input2 = new HashMap<Integer, ItemStack>(input);
            input2.put(19, ItemStack.EMPTY);
            input2.put(20, woodChest);
            input2.put(21, ItemStack.EMPTY);
            GalacticraftRegistry.addT3RocketRecipe(new NasaWorkbenchRecipe(new ItemStack(AsteroidsItems.tier3Rocket, 1, 1), input2));

            input2 = new HashMap<Integer, ItemStack>(input);
            input2.put(19, ItemStack.EMPTY);
            input2.put(20, ItemStack.EMPTY);
            input2.put(21, woodChest);
            GalacticraftRegistry.addT3RocketRecipe(new NasaWorkbenchRecipe(new ItemStack(AsteroidsItems.tier3Rocket, 1, 1), input2));

            input2 = new HashMap<Integer, ItemStack>(input);
            input2.put(19, woodChest);
            input2.put(20, woodChest);
            input2.put(21, ItemStack.EMPTY);
            GalacticraftRegistry.addT3RocketRecipe(new NasaWorkbenchRecipe(new ItemStack(AsteroidsItems.tier3Rocket, 1, 2), input2));

            input2 = new HashMap<Integer, ItemStack>(input);
            input2.put(19, woodChest);
            input2.put(20, ItemStack.EMPTY);
            input2.put(21, woodChest);
            GalacticraftRegistry.addT3RocketRecipe(new NasaWorkbenchRecipe(new ItemStack(AsteroidsItems.tier3Rocket, 1, 2), input2));

            input2 = new HashMap<Integer, ItemStack>(input);
            input2.put(19, ItemStack.EMPTY);
            input2.put(20, woodChest);
            input2.put(21, woodChest);
            GalacticraftRegistry.addT3RocketRecipe(new NasaWorkbenchRecipe(new ItemStack(AsteroidsItems.tier3Rocket, 1, 2), input2));

            input2 = new HashMap<Integer, ItemStack>(input);
            input2.put(19, woodChest);
            input2.put(20, woodChest);
            input2.put(21, woodChest);
            GalacticraftRegistry.addT3RocketRecipe(new NasaWorkbenchRecipe(new ItemStack(AsteroidsItems.tier3Rocket, 1, 3), input2));
        }
    }

    public static void loadCompatibilityRecipes()
    {
        // IC2 兼容已移除
    }
}
