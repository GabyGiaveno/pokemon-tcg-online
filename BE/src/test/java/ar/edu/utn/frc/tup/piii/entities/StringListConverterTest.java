package ar.edu.utn.frc.tup.piii.entities;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringListConverterTest {

    private final StringListConverter converter = new StringListConverter();

    @Test
    void toDbColumnWithValues() {
        assertEquals("a,b,c", converter.convertToDatabaseColumn(List.of("a", "b", "c")));
    }

    @Test
    void toDbColumnWithNull() {
        assertEquals("", converter.convertToDatabaseColumn(null));
    }

    @Test
    void toDbColumnWithEmpty() {
        assertEquals("", converter.convertToDatabaseColumn(List.of()));
    }

    @Test
    void toEntityWithValues() {
        assertEquals(List.of("a", "b", "c"), converter.convertToEntityAttribute("a, b , c"));
    }

    @Test
    void toEntityWithNull() {
        assertTrue(converter.convertToEntityAttribute(null).isEmpty());
    }

    @Test
    void toEntityWithEmpty() {
        assertTrue(converter.convertToEntityAttribute("").isEmpty());
    }
}
