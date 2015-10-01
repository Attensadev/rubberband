package com.attensa.rubberband.examples;

import com.attensa.rubberband.RubberbandClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class CreateTest {

    private RubberbandClient client;

    @Before
    public void setup() {
        client = TestUtilities.buildClient();
    }

    @Test
    public void testCreate() throws Exception {
        Cat simon = new Cat(null, "Simon", "mail", "unknown", "Well loved and now deceased.");
        String id = client.create("animals", "cat", simon);
        simon = simon.withId(id);

        Cat modified = simon.withBreed("Maine Coon");
        client.save("animals", "cat", modified.getId(), modified);

        Optional<Cat> retrieved = client.get("animals", "cat", id, Cat.class);
        assertTrue(retrieved.isPresent());
        assertEquals("Main Coon", retrieved.get().getBreed());
    }
}
