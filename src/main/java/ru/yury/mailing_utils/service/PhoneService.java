package ru.yury.mailing_utils.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yury.mailing_utils.enums.PhoneOutputType;
import ru.yury.mailing_utils.exception.InvalidEmailDataException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PhoneService {

    // Кандидат: опциональный +, 10..15 цифр с допустимыми разделителями (-, пробел, (, ), .)
    private static final Pattern PHONE_CANDIDATE = Pattern.compile(
            "(?U)" +                        // флаг юникода
            "(?<![\\w+])" +                 // # слева не буква, не цифра и не знак "+" (защита от id8916..., a+7...)
                    "(" +                       // # начало захвата кандидата (group 1 для matcher.group(1))
                    "(?:\\+?\\d{1,3}|8)?" +     // # опционально: код страны (+7, 7, +375, 375, 8)
                    "[\\s\\-]*" +               // # пробелы ИЛИ дефис между кодом страны и кодом оператора
                    "\\(?" +                    // # опциональная '('
                    "\\d{2,4}" +                // # 2..4 цифры кода оператора/города (напр. 29, 495, 916, 0232)
                    "\\)?" +                    // # опциональная ')'
                    "[\\d\\s\\-.]{5,14}" +      // # 5..14 цифр и допустимых разделителей (пробел, дефис, точка)
                    "\\d" +                     // # обязательная цифра в конце (номер не должен оканчиваться на дефис или точку)
                    ")" +                       // # конец захвата кандидата
                    "(?![\\w])"                 // # справа не буква и не цифра (защита от склейки с артикулами и словами)
    );

    private static final Pattern ALLOWED_CHARS = Pattern.compile("[^\\d+]"); // всё, что НЕ цифра и НЕ плюс


    public List<String> parsePhones(String rawText, String excludeItems) {
        log.debug("Запуск parsePhones. Длина текста: {}, исключений: {}",
                rawText != null ? rawText.length() : 0,
                excludeItems != null ? excludeItems.length() : 0);
        validate(rawText);

        Set<String> excludeSet = parseExcludeItems(excludeItems);
        Matcher matcher = PHONE_CANDIDATE.matcher(rawText);

        LinkedHashSet<String> unique = new LinkedHashSet<>();
        while (matcher.find()) { // ищет следующее вхождение регулярки в строке rawText. Возвр. true, если нашёл, и false, если больше вхождений нет.
            String normalized = normalize(matcher.group(1)); // первая захватывающая группа, весь кандидат (внутри одной внешней скобки (...)),
            if (normalized != null && !excludeSet.contains(normalized)) {
                unique.add(normalized);
            }
        }

        List<String> result = new ArrayList<>(unique);
        log.info("parsePhones завершен. Найдено уникальных: {} (в списке исключений всего: {} тел. номеров.)",
                result.size(), excludeSet.size());
        return result;
    }


    public List<String> parsePhonesFromStream(InputStream inputStream, String excludeItems) throws IOException {
        Set<String> excludeSet = parseExcludeItems(excludeItems);
        LinkedHashSet<String> unique = new LinkedHashSet<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                Matcher matcher = PHONE_CANDIDATE.matcher(line);
                while (matcher.find()) {
                    String normalized = normalize(matcher.group(1));
                    if (normalized != null && !excludeSet.contains(normalized)) {
                        unique.add(normalized);
                    }
                }
            }
        }

        log.info("parsePhonesFromStream завершен. Найдено уникальных: {} (исключений: {})",
                unique.size(), excludeSet.size());
        return new ArrayList<>(unique);
    }



    /**
     * Нормализация: убрать всё кроме + и цифр.
     * РФ-правило: 11 цифр, начинается с 8 -> +7...
     * Меньше 10 цифр -> null.
     */
    private String normalize(String raw) {
        String cleaned = ALLOWED_CHARS.matcher(raw).replaceAll(""); // выкинуть из строки все символы, кроме цифр 0-9 и знака +.
        String onlyDigits = cleaned.replace("+", "");

        // 8 цифр допускаем только для валидных мобильных Армении
        if (onlyDigits.length() < 8) return null; // невалидный номер -> отбросим


        // ========== ПРЕДВАРИТЕЛЬНАЯ СТАНДАРТИЗАЦИЯ ПРЕФИКСОВ ==========
        if (onlyDigits.startsWith("00")) {
            onlyDigits = onlyDigits.substring(2);
        }

        // Переводим внутренний выход на межгород в международный формат
        if (onlyDigits.length() == 11 && onlyDigits.startsWith("80")) {
            onlyDigits = "375" + onlyDigits.substring(2); // РБ: 8029... -> 37529...
        } else if (onlyDigits.length() == 11 && ((onlyDigits.startsWith("8801") || onlyDigits.startsWith("8803") || onlyDigits.startsWith("8902")))) {
            onlyDigits = "375" + onlyDigits.substring(1);
        } else if (onlyDigits.length() == 11 && onlyDigits.startsWith("8")) { // РФ (8916... -> 7916..., 8800... -> 7800...) и Казахстан (8701... -> 7701...)
            // Для Казахстана '8' тоже используется, но мобильные там начинаются на 7, а в РФ на 9.
            // Если после 8 идет 7, это Казахстан (8 701... -> 7 701...). Если 9 или стационарный — РФ.
            onlyDigits = "7" + onlyDigits.substring(1); // 8916... -> 7916... | 8701... -> 7701...
        }

        // Исправление частой пользовательской ошибки: +78916... -> 78916... -> 7916...
        if (onlyDigits.length() == 12 && onlyDigits.startsWith("78") && !onlyDigits.startsWith("7812")) {
            onlyDigits = "7" + onlyDigits.substring(2);
        }


        // ========== ВАЛИДАЦИЯ И СБОРКА ПО СТРАНАМ ==========

        // --- РОССИЯ / КАЗАХСТАН (11 цифр) ---
        // --- РОССИЯ / КАЗАХСТАН (11 цифр: 1 цифра '7' + 3 цифры кода + 7 цифр номера) ---
//            if (onlyDigits.length() == 11 && onlyDigits.matches("^7(9\\d{2}|8\\d{2}|49\\d|7\\d{2}|3[4589]\\d)\\d{7}$")) {
        if (onlyDigits.length() == 11 && onlyDigits.matches("^7(9\\d{2}|8\\d{2}|49\\d|7\\d{2}|3[4589]\\d)[1-9]\\d{6}$")) {
            return "+" + onlyDigits;
        }


        // --- РОССИЯ (10 цифр, без 7/8: 3 цифры кода + 7 цифр номера) ---
//            if (onlyDigits.length() == 10 && onlyDigits.matches("^(9\\d{2}|8\\d{2}|49\\d|3[4589]\\d)\\d{7}$")){
        if (onlyDigits.length() == 10 && onlyDigits.matches("^(9\\d{2}|8\\d{2}|49\\d|3[4589]\\d)[1-9]\\d{6}$")){
            return "+7" + onlyDigits;
        }


        // --- БЕЛАРУСЬ ---

        // Замените проверку РБ:
        // сотовые (24, 25, 29, 33, 44), Минск (17), Брест (16), Гродно (15), Гомель (23), Могилев (22), Витебск (21[2-6])
        if (onlyDigits.length() == 12 && onlyDigits.matches("^375(1[5-7]|2[2-59]|21[2-6]|33|44)[1-9]\\d{6}$")) {
            return "+" + onlyDigits;
        }

        // Сокращенный РБ (9 цифр: сотовые и областные)
        if (onlyDigits.length() == 9 && onlyDigits.matches("^(1[5-7]|2[1-59]|33|44)[1-9]\\d{6}$")) {
            return "+375" + onlyDigits;
        }

        // Сокращенный стационарный РБ (10 цифр с нулем, напр. 017..., 0232...)
        if (onlyDigits.length() == 10 && onlyDigits.matches("^0(1[5-7]|2[1-59]|33|44)[1-9]\\d{6}$")) {
            return "+375" + onlyDigits.substring(1);
        }




        // --- ГРУЗИЯ ---
        if (onlyDigits.length() == 12 && onlyDigits.startsWith("995")) {
            return "+" + onlyDigits;
        }
        if (onlyDigits.length() == 9 && onlyDigits.startsWith("5")) {
            return "+995" + onlyDigits;
        }

        // --- АРМЕНИЯ ---
        if (onlyDigits.length() == 11 && onlyDigits.startsWith("374")) {
            return "+" + onlyDigits;
        }
        // 8 цифр — только с реальными префиксами мобильных операторов Армении
        if (onlyDigits.length() == 8 && onlyDigits.matches("^(33|4[134]|55|77|9[1-9])\\d{6}$")) {
            return "+374" + onlyDigits;
        }

//        // --- ОСТАЛЬНЫЕ СТРАНЫ ---
//        // ЭТОТ БЛОК ПРОПУСКАЛ +735124503514388 (15 цифр) КАК "МЕЖДУНАРОДНЫЙ" НОМЕР!
//        if (onlyDigits.length() >= 11) {
//            return "+" + onlyDigits;
//        }

        return null;
    }

    /**
     * Применяет форматирование к списку номеров согласно выбранному режиму.
     */
    public List<String> applyOutputFormatting(List<String> items, PhoneOutputType type) {
        if (items == null || items.isEmpty()) {
            return items == null ? List.of() : items;
        }
        PhoneOutputType mode = (type != null) ? type : PhoneOutputType.DEFAULT;

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

            case BY_COUNTRY_CODE -> {
                String prevKey = null;
                for (String item : items) {
                    String key = countryCodeKey(item);
                    if (prevKey != null && !key.equals(prevKey)) {
                        result.add("");
                    }
                    result.add(item);
                    prevKey = key;
                }
            }
        }

        log.debug("applyOutputFormatting(Phone): режим {}, исходно {} элементов, после форматирования {} строк",
                mode.getCode(), size, result.size());
        return result;
    }

    private void addEmpty(List<String> list, int count) {
        for (int k = 0; k < count; k++) list.add("");
    }

    /**
     * Эвристический код страны: первые 1-3 цифры после +.
     * Точный список кодов стран не тащим — группируем по фактическому префиксу.
     */
    private String countryCodeKey(String phone) {
        if (phone == null || !phone.startsWith("+")) return "";
        String digits = phone.substring(1);
        if (digits.startsWith("7")) return "7";     // РФ и Казахстан объединяем под +7
        if (digits.startsWith("1")) return "1";     // США / Канада (+1)
        if (digits.startsWith("375")) return "375"; // Беларусь (+375)
        if (digits.startsWith("374")) return "374"; // Армения (+374)
        if (digits.startsWith("995")) return "995"; // Грузия (+995)
        return digits.length() >= 3 ? digits.substring(0, 3) : digits;
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
        return Pattern.compile("[,;\\n\\r]+")
                .splitAsStream(excludeItems)
                .map(String::trim)
                .map(this::normalize)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
