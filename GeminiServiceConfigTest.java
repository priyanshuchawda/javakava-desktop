import service.GeminiService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public class GeminiServiceConfigTest {
    public static void main(String[] args) throws Exception {
        assertDefaultModelOrder();
        assertRetryConfiguration();
        assertTlsHandshakeDetection();
        System.out.println("Gemini config test passed.");
    }

    private static void assertDefaultModelOrder() throws Exception {
        Field field = GeminiService.class.getDeclaredField("MODEL_CANDIDATES");
        field.setAccessible(true);
        String[] models = (String[]) field.get(null);
        if (models == null || models.length == 0) {
            throw new IllegalStateException("MODEL_CANDIDATES is empty.");
        }
        if (!"gemini-3.1-flash-lite".equals(models[0])) {
            throw new IllegalStateException("Default model should be gemini-3.1-flash-lite, found: " + models[0]);
        }
        if (!Arrays.asList(models).contains("gemini-2.5-flash")) {
            throw new IllegalStateException("Fallback chain must include gemini-2.5-flash.");
        }
    }

    private static void assertTlsHandshakeDetection() throws Exception {
        GeminiService service = new GeminiService();
        Method method = GeminiService.class.getDeclaredMethod("isTlsHandshakeFailure", Throwable.class);
        method.setAccessible(true);

        boolean handshakeDetected = (boolean) method.invoke(service, new IOException("Received fatal alert: handshake_failure"));
        if (!handshakeDetected) {
            throw new IllegalStateException("Expected handshake failure detection for TLS handshake error.");
        }

        boolean falsePositive = (boolean) method.invoke(service, new IOException("Connection reset by peer"));
        if (falsePositive) {
            throw new IllegalStateException("Unexpected TLS handshake detection for non-handshake error.");
        }
    }

    private static void assertRetryConfiguration() throws Exception {
        Field retriesField = GeminiService.class.getDeclaredField("MAX_RETRIES");
        retriesField.setAccessible(true);
        int retries = retriesField.getInt(null);
        if (retries < 1) {
            throw new IllegalStateException("MAX_RETRIES should be at least 1 to handle transient Gemini outages.");
        }
    }
}
