package by.yurnerix.currencyclient.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MockUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    public static <T> T readJson(String resourcePath, Class<T> resultClass) {
        try (InputStream inputStream = MockUtils.class
                .getClassLoader()
                .getResourceAsStream(resourcePath)) {

            if (inputStream == null) {
                throw new IllegalArgumentException("Test resource not found: " + resourcePath);
            }

            return OBJECT_MAPPER.readValue(inputStream, resultClass);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read test resource: " + resourcePath, exception);
        }
    }
}
