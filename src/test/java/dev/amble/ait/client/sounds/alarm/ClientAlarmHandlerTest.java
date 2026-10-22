package dev.amble.ait.client.sounds.alarm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dev.amble.ait.client.sounds.PlayerFollowingLoopingSound;
import dev.amble.ait.client.tardis.ClientTardis;
import dev.amble.ait.client.util.ClientShakeUtil;
import dev.amble.ait.client.util.ClientTardisUtil;
import dev.amble.ait.core.tardis.handler.ServerAlarmHandler;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import net.minecraft.client.sound.SoundInstance;

public class ClientAlarmHandlerTest {

    @BeforeClass
    public static void loadAlarmClasses() throws ClassNotFoundException {
        // Initialize the agent-suppressed classes before creating mocks.
        Class.forName("dev.amble.ait.core.tardis.handler.ServerAlarmHandler");
        Class.forName("dev.amble.ait.core.AITSounds");
    }

    @Test
    public void noTardisDoesNotPlay() throws Throwable {
        assertDecision(null, false);
        assertDecision(enabledTardis(), true);
        assertDecision(null, false);
    }

    @Test
    public void alarmNotReadyDoesNotPlay() throws Throwable {
        ClientTardis tardis = mock(ClientTardis.class);
        ServerAlarmHandler alarm = mock(ServerAlarmHandler.class);
        when(alarm.isEnabled()).thenReturn(true);

        assertDecision(tardis, false);
        when(tardis.alarm()).thenReturn(alarm);
        assertDecision(tardis, true);
        when(tardis.alarm()).thenReturn(null);
        assertDecision(tardis, false);
    }

    @Test
    public void disabledAlarmDoesNotPlay() throws Throwable {
        ClientTardis tardis = enabledTardis();
        ServerAlarmHandler alarm = tardis.alarm();
        when(alarm.isEnabled()).thenReturn(false);

        assertDecision(tardis, false);
        when(alarm.isEnabled()).thenReturn(true);
        assertDecision(tardis, true);
        when(alarm.isEnabled()).thenReturn(false);
        assertDecision(tardis, false);
    }

    @Test
    public void enabledAlarmPlays() throws Throwable {
        assertDecision(enabledTardis(), true);
    }

    private ClientTardis enabledTardis() {
        ClientTardis tardis = mock(ClientTardis.class);
        ServerAlarmHandler alarm = mock(ServerAlarmHandler.class);
        when(tardis.alarm()).thenReturn(alarm);
        when(alarm.isEnabled()).thenReturn(true);
        return tardis;
    }

    private void assertDecision(ClientTardis tardis, boolean expected) throws Throwable {
        RecordingHandler handler = new RecordingHandler();
        Method predicate = ClientAlarmHandler.class.getDeclaredMethod("shouldPlaySound", ClientTardis.class);
        predicate.setAccessible(true);
        ClientAlarmHandler.CLOISTER_INTERIOR = null;

        try (MockedStatic<ClientTardisUtil> current = mockStatic(ClientTardisUtil.class);
                MockedStatic<ClientShakeUtil> shake = mockStatic(ClientShakeUtil.class);
                MockedConstruction<PlayerFollowingLoopingSound> audio = mockConstruction(PlayerFollowingLoopingSound.class)) {
            current.when(ClientTardisUtil::getCurrentTardis).thenReturn(tardis);
            boolean actual;
            try {
                actual = (boolean) predicate.invoke(handler, tardis);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }

            assertEquals("Alarm playback decision", expected, actual);
            assertTrue("The predicate must not construct audio", audio.constructed().isEmpty());
            assertNull("The predicate must not initialize audio", ClientAlarmHandler.CLOISTER_INTERIOR);
            assertEquals("The predicate must not start audio", 0, handler.starts);

            handler.tick(null);

            assertEquals("Tick must only construct audio for an enabled alarm", expected ? 1 : 0, audio.constructed().size());
            assertEquals("Tick must only start audio for an enabled alarm", expected ? 1 : 0, handler.starts);
            assertEquals("Tick must stop audio when the alarm cannot play", expected ? 0 : 1, handler.stops);
            if (!expected)
                assertNull("A non-playing alarm must not initialize audio", ClientAlarmHandler.CLOISTER_INTERIOR);
        } finally {
            ClientAlarmHandler.CLOISTER_INTERIOR = null;
        }
    }

    private static class RecordingHandler extends ClientAlarmHandler {
        private int starts;
        private int stops;

        @Override
        public void startIfNotPlaying(SoundInstance sound) {
            starts++;
        }

        @Override
        public void stopSounds() {
            stops++;
        }
    }
}
