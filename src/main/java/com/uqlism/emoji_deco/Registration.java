package com.uqlism.emoji_deco;

import com.uqlism.emoji_deco.block.GraffitiBlock;
import com.uqlism.emoji_deco.block.GraffitiBlockEntity;
import com.uqlism.emoji_deco.item.GraffitiInkItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = EmojiDeco.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Registration {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, EmojiDeco.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EmojiDeco.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EmojiDeco.MODID);

    public static final RegistryObject<Block> GRAFFITI_BLOCK = BLOCKS.register("graffiti",
            () -> new GraffitiBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(0.2f)
                    .noOcclusion()
                    .noCollission()
                    .sound(GraffitiBlock.GRAFFITI_SOUND_TYPE)));

    public static final RegistryObject<Item> GRAFFITI_INK_ITEM = ITEMS.register("graffiti_ink",
            () -> new GraffitiInkItem(new Item.Properties().durability(8)));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<GraffitiBlockEntity>> GRAFFITI_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("graffiti",
                    () -> BlockEntityType.Builder.of(GraffitiBlockEntity::new, GRAFFITI_BLOCK.get())
                            .build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(GRAFFITI_INK_ITEM.get());
        }
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(EmojiDeco.MODID, path);
    }
}
