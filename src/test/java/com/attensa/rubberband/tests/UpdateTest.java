package com.attensa.rubberband.tests;

import com.attensa.rubberband.RubberbandClient;
import com.attensa.rubberband.data.DocumentUpdate;
import com.attensa.rubberband.Cat;
import com.attensa.rubberband.TestUtilities;
import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

public class UpdateTest {
    private RubberbandClient client;

    @Before
    public void setUp() throws Exception {
        client = TestUtilities.buildClient();
    }

    @Test
    public void testUpdate() throws Exception {
        Cat tigger = new Cat("1234", "Tigger", "male", "American Shorthair", null);

        client.save("animals", "cat", tigger.getId(), tigger);

        client.update("animals", "cat", new DocumentUpdate(tigger.getId(), singletonMap("description", "Feral cat who got left behind.")));

        Cat updated = client.get("animals", "cat", tigger.getId(), Cat.class).orElseThrow(() -> new RuntimeException("Failed to retrieve Tigger"));

        assertEquals("Feral cat who got left behind.", updated.getDescription());
    }

}
