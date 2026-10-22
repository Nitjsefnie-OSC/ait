package dev.amble.ait.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import dev.amble.ait.core.AITItems;
import dev.amble.ait.core.AITVillagers;
import dev.amble.ait.core.item.blueprint.BlueprintItem;
import dev.amble.ait.core.item.blueprint.BlueprintRegistry;
import dev.amble.ait.core.item.blueprint.BlueprintSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;

class CustomTradesTest {
    private static final Set<Identifier> SCHEMA_IDS = Set.of(
            new Identifier("ait", "trade_test_stone"),
            new Identifier("ait", "trade_test_dirt"));
    private static List<BlueprintSchema> previousSchemas;

    @BeforeAll
    static void setUp() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();

        Registry.register(Registries.ITEM, new Identifier("ait", "blueprint"), AITItems.BLUEPRINT);
        BlueprintRegistry registry = BlueprintRegistry.getInstance();
        previousSchemas = List.copyOf(registry.toList());
        registry.clearCache();
        for (Identifier id : SCHEMA_IDS) {
            registry.register(new BlueprintSchema(id, Text.literal(id.getPath()),
                    new BlueprintSchema.InputList(), new ItemStack(Items.STONE)));
        }
        CustomTrades.register();
    }

    @AfterAll
    static void restoreSchemas() {
        BlueprintRegistry registry = BlueprintRegistry.getInstance();
        registry.clearCache();
        previousSchemas.forEach(registry::register);
    }

    @Test
    void blueprintOffersKeepTheirSchemaWhenCopiedAndSerialized() {
        assertEquals(2, BlueprintRegistry.getInstance().toList().size());
        TradeOffers.Factory[] factories = TradeOffers.PROFESSION_TO_LEVELED_TRADE
                .get(AITVillagers.FABRICATOR_ENGINEER).get(4);

        for (int seed = 0; seed < 32; seed++) {
            int blueprintOffers = 0;
            for (TradeOffers.Factory factory : factories) {
                TradeOffer offer = factory.create(null, Random.create(seed));
                assertNotNull(offer);
                ItemStack stack = offer.getSellItem();
                if (!stack.isOf(AITItems.BLUEPRINT)) continue;
                blueprintOffers++;

                ItemStack copy = stack.copy();
                ItemStack restored = ItemStack.fromNbt(stack.writeNbt(new NbtCompound()));
                Identifier id = schemaId(stack);
                assertEquals(id, schemaId(copy));
                assertEquals(id, schemaId(restored));
                assertEquals(id, schemaId(offer.getSellItem().copy()));
            }
            assertEquals(1, blueprintOffers);
        }
    }

    private static Identifier schemaId(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        assertNotNull(nbt, "Blueprint offers must carry schema NBT before tooltip access");
        assertTrue(nbt.contains("Blueprint", NbtElement.STRING_TYPE),
                "Blueprint offers must already contain the Blueprint key");
        Identifier storedId = Identifier.tryParse(nbt.getString("Blueprint"));
        assertNotNull(storedId);
        assertTrue(SCHEMA_IDS.contains(storedId), "Blueprint id must belong to a registered fixture schema");

        BlueprintSchema schema = BlueprintItem.getSchema(stack);
        assertNotNull(schema);
        assertEquals(storedId, schema.id());
        return schema.id();
    }
}
