package ru.yury.mailing_utils.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * Режим отображения результата парсинга телефонов.
 */
@Getter
public enum PhoneOutputType {

    /** Без разделения — просто список подряд. */
    PLAIN("plain"),

    /** Блоками по 10 штук (по умолчанию). */
    BLOCKS_10("blocks10"),

    /** Блоки по 10; каждые 50 → 3 пустых строки, каждые 100 → 10 пустых строк. */
    BLOCKS_10_50_100("blocks10_50_100"),

    /** Группировать по коду страны (первая цифра(ы) после +). */
    BY_COUNTRY_CODE("byCountryCode");

    private final String code;

    PhoneOutputType(String code) {
        this.code = code;
    }

    public static final PhoneOutputType DEFAULT = BLOCKS_10;

    public static PhoneOutputType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return DEFAULT;
        }
        return Arrays.stream(values())
                .filter(t -> t.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElse(DEFAULT);
    }
}