package com.attensa.rubberband.tests;

import com.attensa.rubberband.Cat;
import com.attensa.rubberband.RubberbandClient;
import com.attensa.rubberband.TestUtilities;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GeneralizedTypesTest {

    private RubberbandClient client;

    @Before
    public void setup() {
        client = TestUtilities.buildClient();
    }

    @Test
    public void testGetMap() throws Exception {
        Cat simon = new Cat(null, "Simon", "mail", "unknown", "Well loved and now deceased.");
        String id = client.create("animals", "cat", simon);

        Type type = TypeUtils.parameterize(Map.class, String.class, Object.class);
        Optional<Map<String, Object>> retrieved = client.get("animals", "cat", id, type);
        assertTrue(retrieved.isPresent());
        assertEquals("unknown", retrieved.get().get("breed"));
    }
}
