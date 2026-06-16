package t1tanic.nutritionicu.model.enums;

/**
 * Out-of-range indicator printed beside a value. Maps the arrow glyphs:
 * down = low, up = high, doubled = critically out of range.
 */
public enum ResultFlag {
    NORMAL,
    LOW,         // ↓
    VERY_LOW,    // ↓↓
    HIGH,        // ↑
    VERY_HIGH    // ↑↑
}
