package t1tanic.nutritionicu.dto;

/**
 * A reference document an insight was grounded on: its source PDF file name (for opening) and a
 * human-readable title (from PDF metadata, falling back to the file name).
 */
public record KnowledgeRef(String fileName, String title) {
}
