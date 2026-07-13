/*
 * Copyright (c) 2023 Team Galacticraft
 *
 * Licensed under the MIT license.
 * See LICENSE file in the project root for details.
 */

package micdoodle8.mods.galacticraft.planets.mars.blocks;

import micdoodle8.mods.galacticraft.core.Constants;
import micdoodle8.mods.galacticraft.core.GCBlocks;
import micdoodle8.mods.galacticraft.core.blocks.BlockStairsGC;
import micdoodle8.mods.galacticraft.core.items.ItemBlockDesc;
import micdoodle8.mods.galacticraft.core.items.ItemBlockGC;
import micdoodle8.mods.galacticraft.planets.mars.items.ItemBlockMachine;
import micdoodle8.mods.galacticraft.planets.mars.items.ItemBlockMars;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class MarsBlocks
{

    public static Block marsBlock;
    public static Block blockSludge;
    // public static Block vine;  // 已删除 - 未使用
    public static Block treasureChestTier2;
    public static Block machine;
    public static Block machineT2;
    public static Block marsCobblestoneStairs;
    public static Block marsBricksStairs;
    public static Block bossSpawner;

    public static void initBlocks()
    {
        MarsBlocks.marsBlock = new BlockBasicMars("mars").setHardness(2.2F);
        MarsBlocks.treasureChestTier2 = new BlockTier2TreasureChest("treasure_t2");
        MarsBlocks.machine = new BlockMachineMars("mars_machine").setHardness(1.8F);
        MarsBlocks.machineT2 = new BlockMachineMarsT2("mars_machine_t2").setHardness(1.8F);
        MarsBlocks.bossSpawner = new BlockBossSpawnerMars("boss_spawner_mars");
        MarsBlocks.marsCobblestoneStairs =
            new BlockStairsGC("mars_stairs_cobblestone", marsBlock.getDefaultState().withProperty(BlockBasicMars.BASIC_TYPE, BlockBasicMars.EnumBlockBasic.COBBLESTONE)).setHardness(1.5F);
        MarsBlocks.marsBricksStairs =
            new BlockStairsGC("mars_stairs_brick", marsBlock.getDefaultState().withProperty(BlockBasicMars.BASIC_TYPE, BlockBasicMars.EnumBlockBasic.DUNGEON_BRICK)).setHardness(4.0F);

        GCBlocks.hiddenBlocks.add(MarsBlocks.bossSpawner);

        MarsBlocks.registerBlocks();
        MarsBlocks.setHarvestLevels();
    }

    private static void setHarvestLevel(Block block, String toolClass, int level, int meta)
    {
        block.setHarvestLevel(toolClass, level, block.getStateFromMeta(meta));
    }

    private static void setHarvestLevel(Block block, String toolClass, int level)
    {
        block.setHarvestLevel(toolClass, level);
    }

    public static void setHarvestLevels()
    {
        setHarvestLevel(MarsBlocks.marsBlock, "pickaxe", 1, 0);
        setHarvestLevel(MarsBlocks.marsBlock, "pickaxe", 1, 1);
        setHarvestLevel(MarsBlocks.marsBlock, "pickaxe", 3, 2);
        setHarvestLevel(MarsBlocks.marsBlock, "pickaxe", 1, 3);
        setHarvestLevel(MarsBlocks.marsBlock, "pickaxe", 0, 4);
        setHarvestLevel(MarsBlocks.marsBlock, "pickaxe", 3, 7);
        setHarvestLevel(MarsBlocks.marsBlock, "pickaxe", 0, 8);
        setHarvestLevel(MarsBlocks.marsBlock, "pickaxe", 1, 9);
        setHarvestLevel(MarsBlocks.marsBlock, "shovel", 0, 5);
        setHarvestLevel(MarsBlocks.marsBlock, "shovel", 0, 6);
    }

    public static void registerBlock(Block block, Class<? extends ItemBlock> itemClass)
    {
        GCBlocks.register(Constants.MOD_ID_PLANETS, block, itemClass);
    }

    public static void registerBlocks()
    {
        registerBlock(MarsBlocks.treasureChestTier2, ItemBlockDesc.class);
        registerBlock(MarsBlocks.marsBlock, ItemBlockMars.class);
        // registerBlock(MarsBlocks.vine, ItemBlockDesc.class);  // 已删除
        registerBlock(MarsBlocks.machine, ItemBlockMachine.class);
        registerBlock(MarsBlocks.machineT2, ItemBlockMachine.class);
        registerBlock(MarsBlocks.bossSpawner, ItemBlockGC.class);
        registerBlock(MarsBlocks.marsCobblestoneStairs, ItemBlockGC.class);
        registerBlock(MarsBlocks.marsBricksStairs, ItemBlockGC.class);
    }

    public static void oreDictRegistration()
    {
        OreDictionary.registerOre("oreCopper", new ItemStack(MarsBlocks.marsBlock, 1, 0));
        OreDictionary.registerOre("oreTin", new ItemStack(MarsBlocks.marsBlock, 1, 1));
        OreDictionary.registerOre("oreIron", new ItemStack(MarsBlocks.marsBlock, 1, 3));
        OreDictionary.registerOre("oreDesh", new ItemStack(MarsBlocks.marsBlock, 1, 2));
        OreDictionary.registerOre("blockDesh", new ItemStack(MarsBlocks.marsBlock, 1, 8));
    }
}
