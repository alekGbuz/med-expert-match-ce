package com.berdachuk.medexpertmatch.ingestion.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvDataLoaderTest {

    @Test
    @DisplayName("parseCommaSeparatedList returns empty list for null")
    void parseCommaSeparatedList_Null() {
        assertTrue(CsvDataLoader.parseCommaSeparatedList(null).isEmpty());
    }

    @Test
    @DisplayName("parseCommaSeparatedList returns empty list for empty string")
    void parseCommaSeparatedList_Empty() {
        assertTrue(CsvDataLoader.parseCommaSeparatedList("").isEmpty());
    }

    @Test
    @DisplayName("parseCommaSeparatedList returns empty list for whitespace")
    void parseCommaSeparatedList_Whitespace() {
        assertTrue(CsvDataLoader.parseCommaSeparatedList("   ").isEmpty());
    }

    @Test
    @DisplayName("parseCommaSeparatedList splits single value")
    void parseCommaSeparatedList_SingleValue() {
        List<String> result = CsvDataLoader.parseCommaSeparatedList("Cardiology");
        assertEquals(1, result.size());
        assertEquals("Cardiology", result.get(0));
    }

    @Test
    @DisplayName("parseCommaSeparatedList splits multiple values")
    void parseCommaSeparatedList_MultipleValues() {
        List<String> result = CsvDataLoader.parseCommaSeparatedList("Cardiology, Neurology, Oncology");
        assertEquals(3, result.size());
        assertEquals("Cardiology", result.get(0));
        assertEquals("Neurology", result.get(1));
        assertEquals("Oncology", result.get(2));
    }

    @Test
    @DisplayName("parseCommaSeparatedList handles extra whitespace")
    void parseCommaSeparatedList_ExtraWhitespace() {
        List<String> result = CsvDataLoader.parseCommaSeparatedList("  Cardiology  ,  Neurology  ");
        assertEquals(2, result.size());
        assertEquals("Cardiology", result.get(0));
        assertEquals("Neurology", result.get(1));
    }

    @Test
    @DisplayName("parseCommaSeparatedList filters empty entries")
    void parseCommaSeparatedList_FiltersEmptyEntries() {
        List<String> result = CsvDataLoader.parseCommaSeparatedList("Cardiology,,Neurology");
        assertEquals(2, result.size());
        assertEquals("Cardiology", result.get(0));
        assertEquals("Neurology", result.get(1));
    }

    @Test
    @DisplayName("normalize returns empty string for null")
    void normalize_Null() {
        assertEquals("", CsvDataLoader.normalize(null));
    }

    @Test
    @DisplayName("normalize lowercases and trims")
    void normalize_Standard() {
        assertEquals("cardiology", CsvDataLoader.normalize("  Cardiology  "));
    }

    @Test
    @DisplayName("normalize collapses multiple spaces")
    void normalize_CollapsesSpaces() {
        assertEquals("cardiac surgery", CsvDataLoader.normalize("Cardiac   Surgery"));
    }
}
