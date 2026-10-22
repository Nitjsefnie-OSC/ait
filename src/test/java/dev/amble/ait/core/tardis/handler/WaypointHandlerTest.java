package dev.amble.ait.core.tardis.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import dev.amble.ait.core.tardis.control.ControlTypes;
import dev.amble.ait.core.tardis.control.impl.ConsolePortControl;
import dev.amble.ait.data.schema.console.ConsoleTypeSchema;
import dev.amble.ait.data.schema.console.type.*;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

class WaypointHandlerTest {
    private static final BlockPos CONSOLE = new BlockPos(17, 64, -31);

    @BeforeAll
    static void setUp() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @TestFactory
    Stream<DynamicTest> cartridgesSpawnAtTheirConsolePort() {
        return List.<ConsoleTypeSchema>of(new AlnicoType(), new CopperType(), new CoralType(),
                new CrystallineType(), new HartnellType(), new HudolinType(), new RenaissanceType(),
                new SteamType(), new ToyotaType()).stream().map(type -> DynamicTest.dynamicTest(type.name(), () -> {
                    ControlTypes port = Arrays.stream(type.getControlTypes())
                            .filter(control -> control.getControl() instanceof ConsolePortControl)
                            .findFirst().orElseThrow();
                    Vector3f position = CONSOLE.toCenterPos().toVector3f().add(port.getOffset());
                    Vec3d expected = new Vec3d(position.x(), position.y(), position.z());

                    assertEquals(expected, WaypointHandler.getCartridgeSpawnPos(CONSOLE, type));
                }));
    }

    @Test
    void missingConsoleTypeKeepsCornerPosition() {
        assertEquals(Vec3d.of(CONSOLE), WaypointHandler.getCartridgeSpawnPos(CONSOLE, null));
    }

    @Test
    void consoleWithoutPortKeepsCornerPosition() {
        ConsoleTypeSchema type = new BorealisType();
        assertFalse(Arrays.stream(type.getControlTypes())
                .anyMatch(control -> control.getControl() instanceof ConsolePortControl));

        assertEquals(Vec3d.of(CONSOLE), WaypointHandler.getCartridgeSpawnPos(CONSOLE, type));
    }
}
