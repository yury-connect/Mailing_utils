package ru.yury.mailing_utils.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yury.mailing_utils.service.EmailService;

import java.util.List;

@Slf4j
@Controller
public class MainController {

    @GetMapping("/")
    public String index() {
        log.info("Запрошена главная страница");
        return "index"; // возвращает index.html
    }
}