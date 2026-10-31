package ru.yury.mailing_utils.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yury.mailing_utils.enums.TelegramOutputType;
import ru.yury.mailing_utils.exception.InvalidEmailDataException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TelegramService {

    // t.me/username | telegram.me/username | https://t.me/username | @username
    private static final Pattern TG_PATTERN = Pattern.compile(
            "(?:https?://)?(?:t\\.me|telegram\\.me)/([A-Za-z0-9_]{3,32})" +
                    "|@([A-Za-z0-9_]{3,32})",
            Pattern.CASE_INSENSITIVE
    );

    public List<String> parseTelegramUsernames(String rawText, String excludeItems) {
        log.debug("Запуск parseTelegramUsernames. Длина текста: {}, исключений: {}",
                rawText != null ? rawText.length() : 0,
                excludeItems != null ? excludeItems.length() : 0);
        validate(rawText);

        Set<String> excludeSet = parseExcludeItems(excludeItems);

        Matcher matcher = TG_PATTERN.matcher(rawText);
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        while (matcher.find()) {
            String username = (matcher.group(1) != null) ? matcher.group(1) : matcher.group(2);
            String normalized = "@" + username.toLowerCase();
            if (!excludeSet.contains(normalized)) {
                unique.add(normalized);
            }
        }

        List<String> result = new ArrayList<>(unique);
        log.info("parseTelegramUsernames завершен. Найдено уникальных: {} (в списке исключений: {})",
                result.size(), excludeSet.size());
        return result;
    }

    /**
     * Применяет форматирование к списку username'ов согласно выбранному режиму.
     * Возвращает новый список с пустыми строками ("") как разделителями.
     * Пустые строки НЕ добавляются после последнего элемента.
     */
    public List<String> applyOutputFormatting(List<String> items, TelegramOutputType type) {
        if (items == null || items.isEmpty()) {
            return items == null ? List.of() : items;
        }
        TelegramOutputType mode = (type != null) ? type : TelegramOutputType.DEFAULT;

        List<String> result = new ArrayList<>();
        int size = items.size();

        switch (mode) {
            case PLAIN -> result.addAll(items);

            case BLOCKS_10 -> {
                for (int i = 0; i < size; i++) {
                    result.add(items.get(i));
                    if (i < size - 1 && (i + 1) % 10 == 0) {
                        result.add("");
                    }
                }
            }

            case BLOCKS_10_50_100 -> {
                for (int i = 0; i < size; i++) {
                    result.add(items.get(i));
                    if (i == size - 1) break;
                    int pos = i + 1;
                    if (pos % 100 == 0) {
                        addEmpty(result, 10);
                    } else if (pos % 50 == 0) {
                        addEmpty(result, 3);
                    } else if (pos % 10 == 0) {
                        addEmpty(result, 1);
                    }
                }
            }

            case BY_FIRST_LETTER -> {
                String prevKey = null;
                for (String item : items) {
                    String key = firstLetterKey(item);
                    if (prevKey != null && !key.equals(prevKey)) {
                        result.add("");
                    }
                    result.add(item);
                    prevKey = key;
                }
            }
        }

        log.debug("applyOutputFormatting(Telegram): режим {}, исходно {} элементов, после форматирования {} строк",
                mode.getCode(), size, result.size());
        return result;
    }

    private void addEmpty(List<String> list, int count) {
        for (int k = 0; k < count; k++) list.add("");
    }

    /** Первая буква username без @. Для @abc -> "a". */
    private String firstLetterKey(String username) {
        if (username == null) return "";
        String s = username.startsWith("@") ? username.substring(1) : username;
        return s.isEmpty() ? "" : s.substring(0, 1).toLowerCase();
    }

    // *************************** служебное ***************************

    private void validate(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            log.warn("Валидация не пройдена: передан пустой текст или null");
            throw new InvalidEmailDataException("Переданный текст пуст или равен null");
        }
    }

    private Set<String> parseExcludeItems(String excludeItems) {
        if (excludeItems == null || excludeItems.isBlank()) {
            return Set.of();
        }
        return Pattern.compile("[\\s,;]+")
                .splitAsStream(excludeItems)
                .map(String::trim)
                .map(String::toLowerCase)
                .map(s -> s.startsWith("@") ? s : "@" + s)
                .filter(s -> s.length() > 1)
                .collect(Collectors.toSet());
    }
}