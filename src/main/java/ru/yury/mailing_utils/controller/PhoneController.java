package ru.yury.mailing_utils.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.yury.mailing_utils.enums.PhoneOutputType;
import ru.yury.mailing_utils.service.PhoneService;

import java.io.IOException;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/tools/phone-filter")
public class PhoneController {

    private final PhoneService phoneService;

    public PhoneController(PhoneService phoneService) {
        this.phoneService = phoneService;
    }

    @GetMapping
    public String showForm() {
        log.info("Открыта страница парсинга телефонов (/tools/phone-filter)");
        return "phone_filter";
    }

    @PostMapping("/parse")
    public String parse(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "excludeItems", required = false) String excludeItems,
            @RequestParam(value = "outputMode", required = false) String outputMode,
            Model model) {

        PhoneOutputType type = PhoneOutputType.fromCode(outputMode);

        log.info("Запрос: 'Парсинг телефонов'. Входной текст: {} символов, исключения: {} символов, режим вывода: {}",
                text != null ? text.length() : 0,
                excludeItems != null ? excludeItems.length() : 0,
                type.getCode());

        model.addAttribute("rawText", text);
        model.addAttribute("excludeItems", excludeItems);
        model.addAttribute("outputMode", type.getCode());

        List<String> found = phoneService.parsePhones(text, excludeItems);
        model.addAttribute("itemsFound", found.size());

        log.info("Найдено телефонов: {}", found.size());

        List<String> formed = phoneService.applyOutputFormatting(found, type);
        model.addAttribute("foundItems", formed);
        return "phone_filter";
    }


    @PostMapping("/parse-file")
    public String parseFile(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "excludeItems", required = false) String excludeItems,
            @RequestParam(value = "outputMode", required = false) String outputMode,
            Model model) {

        PhoneOutputType type = PhoneOutputType.fromCode(outputMode);

        log.info("Запрос: 'Парсинг телефонов из файла'. Исключения: {} символов, режим вывода: {}",
                excludeItems != null ? excludeItems.length() : 0,
                type.getCode());

        if (file == null || file.isEmpty()) {
            log.warn("Файл не выбран или пуст");
            model.addAttribute("error", "Файл не выбран или пуст");
            model.addAttribute("excludeItems", excludeItems);
            model.addAttribute("outputMode", type.getCode());
            return "phone_filter";
        }

        log.info("Загружен файл: {} ({} байт)", file.getOriginalFilename(), file.getSize());

        List<String> found;
        try {
            found = phoneService.parsePhonesFromStream(file.getInputStream(), excludeItems);
        } catch (IOException e) {
            log.error("Ошибка чтения файла: {}", e.getMessage(), e);
            model.addAttribute("error", "Не удалось прочитать файл: " + e.getMessage());
            model.addAttribute("excludeItems", excludeItems);
            model.addAttribute("outputMode", type.getCode());
            return "phone_filter";
        }

        model.addAttribute("excludeItems", excludeItems);
        model.addAttribute("outputMode", type.getCode());
        model.addAttribute("itemsFound", found.size());
        log.info("Найдено телефонов из файла: {}", found.size());

        List<String> formed = phoneService.applyOutputFormatting(found, type);
        model.addAttribute("foundItems", formed);
        return "phone_filter";
    }
}