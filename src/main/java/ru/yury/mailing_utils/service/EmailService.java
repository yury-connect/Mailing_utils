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

    // Простая регулярка для поиска email в тексте
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");

    public List<String> withoutFilteringEmails(String rawText) {
        log.debug("Запуск withoutFilteringEmails. Длина текста: {}", rawText != null ? rawText.length() : 0);
        validate(rawText);

        Matcher matcher = EMAIL_PATTERN.matcher(rawText);

        LinkedHashSet<String> uniqueEmails = matcher.results()
                .map(match -> match.group().toLowerCase().trim()) // Приводим к нижнему регистру
                // Собираем в LinkedHashSet, который сохраняет порядок и убирает дубли
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> result = new ArrayList<>(uniqueEmails);
        log.info("Метод withoutFilteringEmails успешно завершен. Найдено уникальных email: {}", result.size());

        return result;
    }

    public List<String> filterEmailsByName(String rawText) {
        log.debug("Запуск filterEmailsByName. Длина текста: {}", rawText != null ? rawText.length() : 0);
        validate(rawText);

        Matcher matcher = EMAIL_PATTERN.matcher(rawText);
        Set<String> uniqueEmails = matcher.results()
                .map(match -> match.group().toLowerCase().trim()) // Приводим к нижнему регистру
                .collect(Collectors.toSet()); // Set автоматически удаляет дубликаты
        List<String> result = uniqueEmails.stream().sorted(String::compareTo).toList();
        log.info("Метод filterEmailsByName успешно завершен. Найдено email: {}", result.size());

        return result;    }

    public List<String> filterEmailsByDomainAndName(String rawText) {
        log.debug("Запуск filterEmailsByDomainAndName. Длина текста: {}", rawText != null ? rawText.length() : 0);
        validate(rawText);

        Matcher matcher = EMAIL_PATTERN.matcher(rawText);

        List<String> result = matcher.results()
                .map(match -> match.group().toLowerCase().trim()) // Приводим к нижнему регистру
                .distinct() // Убираем дубликаты (чище, чем через Set)
                .sorted((email1, email2) -> {
                    // Разделяем на имя и домен
                    String[] parts1 = email1.split("@", 2);
                    String[] parts2 = email2.split("@", 2);

                    // Сначала сравниваем домены
                    int domainCompare = parts1[1].compareTo(parts2[1]);
                    if (domainCompare != 0) {
                        return domainCompare;
                    }
                    // Если домены одинаковые, сравниваем имена
                    return parts1[0].compareTo(parts2[0]);
                })
                .toList();

        log.info("Метод filterEmailsByDomainAndName успешно завершен. Найдено email: {}", result.size());
        return result;
    }

    private void validate(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            log.warn("Валидация не пройдена: передан пустой текст или null");
            throw new InvalidEmailDataException("Переданный текст пуст или равен null");
        }
    }
}