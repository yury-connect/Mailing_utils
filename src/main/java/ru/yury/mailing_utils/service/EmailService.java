package ru.yury.mailing_utils.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yury.mailing_utils.enums.OutputType;
import ru.yury.mailing_utils.exception.InvalidEmailDataException;

import java.nio.charset.StandardCharsets;
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

    // Используем регулярку с \b для точного поиска границ слова. Разрешаем любые буквы Unicode (\p{L}) в имени и домене
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[\\p{L}0-9._%+-]+@[\\p{L}0-9.-]+\\.[\\p{L}]{2,}\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );


//    public List<String> withoutFilteringEmails(String rawText) {
//        return withoutFilteringEmails(rawText, null);
//    }

    public List<String> withoutFilteringEmails(String rawText, String excludeEmails) {
        log.debug("Запуск withoutFilteringEmails. Длина текста: {}, исключений: {}",
                rawText != null ? rawText.length() : 0,
                excludeEmails != null ? excludeEmails.length() : 0);
        validate(rawText);

        // 1. Принудительно чиним кодировку
        String utf8Text = forceUtf8(rawText);

        // 2. Убираем мусор
        String cleanText = sanitizeText(utf8Text);

        // 3. Ищем email
        Matcher matcher = EMAIL_PATTERN.matcher(cleanText);

        Set<String> excludeSet = parseExcludeEmails(excludeEmails);

        LinkedHashSet<String> uniqueEmails = matcher.results()
                .map(match -> match.group().toLowerCase().trim())
                .filter(email -> !excludeSet.contains(email))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> result = new ArrayList<>(uniqueEmails);
        log.info("Метод withoutFilteringEmails завершен. Найдено уникальных email: {} (в списке исключений: {})", result.size(), excludeSet.size());
        return result;
    }


//    public List<String> filterEmailsByName(String rawText) {
//        return filterEmailsByName(rawText, null);
//    }

    public List<String> filterEmailsByName(String rawText, String excludeEmails) {
        log.debug("Запуск filterEmailsByName. Длина текста: {}, исключений: {}",
                rawText != null ? rawText.length() : 0,
                excludeEmails != null ? excludeEmails.length() : 0);
        validate(rawText);

        String utf8Text = forceUtf8(rawText);
        String cleanText = sanitizeText(utf8Text);
        Matcher matcher = EMAIL_PATTERN.matcher(cleanText);

        Set<String> excludeSet = parseExcludeEmails(excludeEmails);

        List<String> result = matcher.results()
                .map(match -> match.group().toLowerCase().trim())
                .filter(email -> !excludeSet.contains(email))
                .distinct()
                .sorted(String::compareTo)
                .toList();

        log.info("Метод filterEmailsByName завершен. Найдено email: {} (в списке исключений: {})",
                result.size(), excludeSet.size());
        return result;
    }


//    public List<String> filterEmailsByDomainAndName(String rawText) {
//        return filterEmailsByDomainAndName(rawText, null);
//    }

    public List<String> filterEmailsByDomainAndName(String rawText, String excludeEmails) {
        log.debug("Запуск filterEmailsByDomainAndName. Длина текста: {}, исключений: {}",
                rawText != null ? rawText.length() : 0,
                excludeEmails != null ? excludeEmails.length() : 0);
        validate(rawText);

        String utf8Text = forceUtf8(rawText);
        String cleanText = sanitizeText(utf8Text);
        Matcher matcher = EMAIL_PATTERN.matcher(cleanText);

        Set<String> excludeSet = parseExcludeEmails(excludeEmails);

        List<String> result = matcher.results()
                .map(match -> match.group().toLowerCase().trim())
                .filter(email -> !excludeSet.contains(email))
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

        log.info("Метод filterEmailsByDomainAndName завершен. Найдено email: {} (в списке исключений: {})",
                result.size(), excludeSet.size());
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
     * Очищает текст от HTML-тегов и мусорных символов, заменяя их на пробелы. Не стирает кириллицу
     * Оставляет только буквы, цифры, точки, дефисы, подчеркивания, плюсы и собаку.
     */
    private String sanitizeText(String rawText) {
        if (rawText == null) return "";

        // Оставляем только разрешенные символы, все остальные заменяем на пробел
        String cleaned = rawText.replaceAll("[^\\p{L}\\p{N}@._%+\\-\\s]", " ");

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
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Ошибка при конвертации в UTF-8", e);
            return text; // Если что-то пошло не так, возвращаем как есть
        }
    }

    /**
     * Парсит строку исключаемых email-адресов в Set (lowercase).
     * Разделители: перевод строки, запятая, точка с запятой, пробел.
     */
    private Set<String> parseExcludeEmails(String excludeEmails) {
        if (excludeEmails == null || excludeEmails.isBlank()) {
            return Set.of();
        }
        return Pattern.compile("[\\s,;]+")
                .splitAsStream(excludeEmails)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }



    /**
     * Применяет форматирование к списку email-адресов согласно выбранному режиму.
     * Возвращает новый список, в который вставлены пустые строки ("") как разделители.
     * Пустые строки НЕ добавляются после последнего элемента.
     */
    public List<String> applyOutputFormatting(List<String> emails, OutputType type) {
        if (emails == null || emails.isEmpty()) {
            return emails == null ? List.of() : emails;
        }
        OutputType mode = (type != null) ? type : OutputType.DEFAULT;

        List<String> result = new ArrayList<>();
        int size = emails.size();

        switch (mode) {
            case PLAIN -> {
                result.addAll(emails);
            }
            case BLOCKS_10 -> {
                for (int i = 0; i < size; i++) {
                    result.add(emails.get(i));
                    boolean isLast = (i == size - 1);
                    if (!isLast && (i + 1) % 10 == 0) {
                        result.add("");
                    }
                }
            }
            case BLOCKS_10_50_100 -> {
                for (int i = 0; i < size; i++) {
                    result.add(emails.get(i));
                    boolean isLast = (i == size - 1);
                    if (isLast) {
                        break;
                    }
                    int pos = i + 1; // номер элемента, считая с 1
                    if (pos % 100 == 0) {
                        addEmpty(result, 10);
                    } else if (pos % 50 == 0) {
                        addEmpty(result, 3);
                    } else if (pos % 10 == 0) {
                        addEmpty(result, 1);
                    }
                }
            }
            case BY_DOMAINS -> {
                String prevDomain = null;
                for (int i = 0; i < size; i++) {
                    String email = emails.get(i);
                    String domain = extractDomain(email);
                    if (i > 0 && !domain.equals(prevDomain)) {
                        result.add("");
                    }
                    result.add(email);
                    prevDomain = domain;
                }
            }
        }

        log.debug("applyOutputFormatting: режим {}, исходно {} email, после форматирования {} строк",
                mode.getCode(), size, result.size());
        return result;
    }

    private void addEmpty(List<String> list, int count) {
        for (int k = 0; k < count; k++) {
            list.add("");
        }
    }

    private String extractDomain(String email) {
        int at = email.lastIndexOf('@');
        return (at >= 0 && at < email.length() - 1) ? email.substring(at + 1) : "";
    }
}
