package ru.yury.mailing_utils.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
public class EmailService {

    // Используем регулярку с \b для точного поиска границ слова
//    private static final Pattern EMAIL_PATTERN = Pattern.compile(
//            "\\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}\\b",
//            Pattern.CASE_INSENSITIVE
//    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            // 1. Начало (исключаем точки в начале)
            "(?<![\\w.-])" +
                    // 2. Имя (буквы, цифры, спецсимволы, без точек в начале и в конце)
                    "[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*" +
                    // 3. Собака
                    "@" +
                    // 4. Домен (без точек и дефисов в начале/конце)
                    "(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+" +
                    // 5. Доменная зона (минимум 2 буквы, максимум сколько угодно)
                    "[a-zA-Z]{2,}" +
                    // 6. Конец (исключаем захват точек и дефисов в конце)
                    "(?![\\w.-])",
            Pattern.CASE_INSENSITIVE
    );


    public List<String> withoutFilteringEmails(String rawText) {
        log.debug("Запуск withoutFilteringEmails. Длина текста: {}", rawText != null ? rawText.length() : 0);
        validate(rawText);

        // 1. Принудительно чиним кодировку
        String utf8Text = forceUtf8(rawText);

        // 2. Убираем мусор
        String cleanText = sanitizeText(utf8Text);

        // 3. Ищем email
        Matcher matcher = EMAIL_PATTERN.matcher(cleanText);

        LinkedHashSet<String> uniqueEmails = matcher.results()
                .map(match -> match.group().toLowerCase().trim())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> result = new ArrayList<>(uniqueEmails);
        log.info("Метод withoutFilteringEmails завершен. Найдено уникальных email: {}", result.size());

        return result;
    }


    public List<String> filterEmailsByName(String rawText) {
        log.debug("Запуск filterEmailsByName. Длина текста: {}", rawText != null ? rawText.length() : 0);
        validate(rawText);

        // 1. Принудительно чиним кодировку
        String utf8Text = forceUtf8(rawText);

        // 2. Убираем мусор
        String cleanText = sanitizeText(utf8Text);

        // 3. Ищем email
        Matcher matcher = EMAIL_PATTERN.matcher(cleanText);

        Set<String> uniqueEmails = matcher.results()
                .map(match -> match.group().toLowerCase().trim())
                .collect(Collectors.toSet());

        List<String> result = uniqueEmails.stream().sorted(String::compareTo).toList();
        log.info("Метод filterEmailsByName завершен. Найдено email: {}", result.size());

        return result;
    }


    public List<String> filterEmailsByDomainAndName(String rawText) {
        log.debug("Запуск filterEmailsByDomainAndName. Длина текста: {}", rawText != null ? rawText.length() : 0);
        validate(rawText);

        // 1. Принудительно чиним кодировку
        String utf8Text = forceUtf8(rawText);

        // 2. Убираем мусор
        String cleanText = sanitizeText(utf8Text);

        // 3. Ищем email
        Matcher matcher = EMAIL_PATTERN.matcher(cleanText);

        List<String> result = matcher.results()
                .map(match -> match.group().toLowerCase().trim())
                .distinct()
                .sorted((email1, email2) -> {
                    String[] parts1 = email1.split("@", 2);
                    String[] parts2 = email2.split("@", 2);

                    int domainCompare = parts1[1].compareTo(parts2[1]);
                    if (domainCompare != 0) {
                        return domainCompare;
                    }
                    return parts1[0].compareTo(parts2[0]);
                })
                .toList();

        log.info("Метод filterEmailsByDomainAndName завершен. Найдено email: {}", result.size());
        return result;
    }


    // ************************************************** Сервисные **************************************************

    private void validate(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            log.warn("Валидация не пройдена: передан пустой текст или null");
            throw new InvalidEmailDataException("Переданный текст пуст или равен null");
        }
    }

    /**
     * Очищает текст от HTML-тегов и мусорных символов, заменяя их на пробелы.
     * Оставляет только буквы, цифры, точки, дефисы, подчеркивания, плюсы и собаку.
     */
    private String sanitizeText(String rawText) {
        if (rawText == null) return "";

        // Оставляем только разрешенные символы, все остальные заменяем на пробел
        String cleaned = rawText.replaceAll("[^a-zA-Z0-9@._%+\\-\\s]", " ");

        // Убираем множественные пробелы (опционально, для красоты)
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    /**
     * Принудительно прогоняет строку через UTF-8.
     * Это спасет от "кракозябр", если исходный текст пришел в другой кодировке (например, Windows-1251).
     */
    private String forceUtf8(String text) {
        if (text == null) return "";
        try {
            // Берем байты строки в UTF-8 и создаем из них новую строку
            byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Ошибка при конвертации в UTF-8", e);
            return text; // Если что-то пошло не так, возвращаем как есть
        }
    }
}
