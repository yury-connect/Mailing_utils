package ru.yury.mailing_utils.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.yury.mailing_utils.enums.TelegramOutputType;
import ru.yury.mailing_utils.service.TelegramService;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/tools/telegram-filter")
public class TelegramController {

    private final TelegramService telegramService;

    public TelegramController(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @GetMapping
    public String showForm() {
        log.info("Открыта страница парсинга Telegram-профилей (/tools/telegram-filter)");
        return "telegram_filter";
    }

    @PostMapping("/parse")
    public String parse(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "excludeItems", required = false) String excludeItems,
            @RequestParam(value = "outputMode", required = false) String outputMode,
            Model model) {

        TelegramOutputType type = TelegramOutputType.fromCode(outputMode);

        log.info("Запрос: 'Парсинг Telegram'. Входной текст: {} символов, исключения: {} символов, режим вывода: {}",
                text != null ? text.length() : 0,
                excludeItems != null ? excludeItems.length() : 0,
                type.getCode());

        model.addAttribute("rawText", text);
        model.addAttribute("excludeItems", excludeItems);
        model.addAttribute("outputMode", type.getCode());

        List<String> found = telegramService.parseTelegramUsernames(text, excludeItems);
        model.addAttribute("itemsFound", found.size());

        log.info("Найдено Telegram-профилей: {}", found.size());

        List<String> formed = telegramService.applyOutputFormatting(found, type);
        model.addAttribute("foundItems", formed);
        return "telegram_filter";
    }
}
