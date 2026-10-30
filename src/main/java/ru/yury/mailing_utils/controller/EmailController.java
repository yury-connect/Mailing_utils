package ru.yury.mailing_utils.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.yury.mailing_utils.constant.ApiConstants;
import ru.yury.mailing_utils.enums.OutputType;
import ru.yury.mailing_utils.service.EmailService;

import java.util.List;

@Slf4j
@Controller
@RequestMapping(ApiConstants.EMAIL_TOOLS_BASE) // Базовый путь из константы
public class EmailController {

    private final EmailService emailService;


    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }


    @GetMapping
    public String showForm() {
        log.info("Открыта страница фильтрации email (/tools/email-filter)");
        return "email_filter"; // Имя HTML-файла в папке templates
    }


    // Обработка кнопки "Без фильтрации"
    @PostMapping(ApiConstants.EMAIL_WITHOUT_FILTER)
    public String withoutFilteringEmails(
            @RequestParam(value = "emails", required = false) String emails,
            @RequestParam(value = "excludeEmails", required = false) String excludeEmails,
            @RequestParam(value = "outputMode", required = false) String outputMode,
            Model model) {

        OutputType type = OutputType.fromCode(outputMode);

        log.info("Запрос: 'Фильтр по домену и имени'. Входной текст: {} символов, исключения: {} символов, режим вывода: {}",
                emails != null ? emails.length() : 0,
                excludeEmails != null ? excludeEmails.length() : 0,
                type.getCode());

        model.addAttribute("rawEmails", emails);
        model.addAttribute("excludeEmails", excludeEmails);
        model.addAttribute("outputMode", type.getCode());
        List<String> filtered = emailService.withoutFilteringEmails(emails, excludeEmails);
        model.addAttribute("itemsFound", filtered.size());

        log.info("Найдено уникальных email (без фильтрации): {}", filtered.size());

        List<String> formedList = emailService.applyOutputFormatting(filtered, type);
        model.addAttribute("filteredEmails", formedList);
        return "email_filter";
    }


    // Обработка кнопки "Фильтр по имени"
    @PostMapping(ApiConstants.EMAIL_FILTER_BY_NAME)
    public String filterByName(
            @RequestParam(value = "emails", required = false) String emails,
            @RequestParam(value = "excludeEmails", required = false) String excludeEmails,
            @RequestParam(value = "outputMode", required = false) String outputMode,
            Model model) {

        OutputType type = OutputType.fromCode(outputMode);

        log.info("Запрос: 'Фильтр по домену и имени'. Входной текст: {} символов, исключения: {} символов, режим вывода: {}",
                emails != null ? emails.length() : 0,
                excludeEmails != null ? excludeEmails.length() : 0,
                type.getCode());

        model.addAttribute("rawEmails", emails);
        model.addAttribute("excludeEmails", excludeEmails);
        model.addAttribute("outputMode", type.getCode());
        List<String> filtered = emailService.filterEmailsByName(emails, excludeEmails);
        model.addAttribute("itemsFound", filtered.size());

        log.info("Найдено email (фильтр по имени): {}", filtered.size());

        List<String> formedList = emailService.applyOutputFormatting(filtered, type);
        model.addAttribute("filteredEmails", formedList);
        return "email_filter";
    }


    // Обработка кнопки "Фильтр по домену и имени"
    @PostMapping(ApiConstants.EMAIL_FILTER_BY_DOMAIN_AND_NAME)
    public String filterByDomainAndName(
            @RequestParam(value = "emails", required = false) String emails,
            @RequestParam(value = "excludeEmails", required = false) String excludeEmails,
            @RequestParam(value = "outputMode", required = false) String outputMode,
            Model model) {

        OutputType type = OutputType.fromCode(outputMode);

        log.info("Запрос: 'Фильтр по домену и имени'. Входной текст: {} символов, исключения: {} символов, режим вывода: {}",
                emails != null ? emails.length() : 0,
                excludeEmails != null ? excludeEmails.length() : 0,
                type.getCode());

        model.addAttribute("rawEmails", emails);
        model.addAttribute("excludeEmails", excludeEmails);
        model.addAttribute("outputMode", type.getCode());
        List<String> filtered = emailService.filterEmailsByDomainAndName(emails, excludeEmails);
        model.addAttribute("itemsFound", filtered.size());

        log.info("Найдено email (фильтр по домену и имени): {}", filtered.size());

        List<String> formedList = emailService.applyOutputFormatting(filtered, type);
        model.addAttribute("filteredEmails", formedList);
        return "email_filter";
    }
}
