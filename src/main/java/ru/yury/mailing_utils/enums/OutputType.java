package ru.yury.mailing_utils.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * Режим отображения результата фильтрации email-адресов.
 * Код ({@link #getCode()}) совпадает со значением value в radio-кнопках формы (outputMode).
 */
@Getter
public enum OutputType {

    /** Без разделения — просто список адресов подряд. */
    PLAIN("plain"),

    /** Блоками по 10 штук (по умолчанию). */
    BLOCKS_10("blocks10"),

    /** Блоки по 10; каждые 50 → 3 пустых строки, каждые 100 → 10 пустых строк. */
    BLOCKS_10_50_100("blocks10_50_100"),

    /** Разносить только по доменам. */
    BY_DOMAINS("domains");

    /** Код режима — совпадает с value в HTML-форме. */
    private final String code;

    OutputType(String code) {
        this.code = code;
    }

    /** Режим по умолчанию, если ничего не передано. */
    public static final OutputType DEFAULT = BLOCKS_10;

    /**
     * Безопасный парсинг: возвращает {@link #DEFAULT}, если строка null/пустая/неизвестная.
     */
    public static OutputType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return DEFAULT;
        }
        return Arrays.stream(values())
                .filter(t -> t.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElse(DEFAULT);
    }
}
