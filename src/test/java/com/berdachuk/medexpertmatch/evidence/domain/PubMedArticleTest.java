package com.berdachuk.medexpertmatch.evidence.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PubMedArticleTest {

    @Test
    @DisplayName("record construction with all fields")
    void recordConstruction() {
        PubMedArticle article = new PubMedArticle(
                "Treatment of Acute Myocardial Infarction",
                "This study examines outcomes of PCI vs thrombolysis...",
                List.of("Smith J", "Jones K"),
                "New England Journal of Medicine",
                2023,
                "12345678");

        assertEquals("Treatment of Acute Myocardial Infarction", article.title());
        assertTrue(article.abstractText().contains("PCI"));
        assertEquals(2, article.authors().size());
        assertEquals("New England Journal of Medicine", article.journal());
        assertEquals(2023, article.year());
        assertEquals("12345678", article.pmid());
    }

    @Test
    @DisplayName("article with single author")
    void singleAuthor() {
        PubMedArticle article = new PubMedArticle(
                "Case Report", "Abstract text",
                List.of("Doe J"), "Lancet", 2022, "87654321");

        assertEquals(1, article.authors().size());
        assertEquals("Doe J", article.authors().getFirst());
    }
}
