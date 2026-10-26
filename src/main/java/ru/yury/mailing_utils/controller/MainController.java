package ru.yury.mailing_utils.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import ru.yury.mailing_utils.constant.ApiConstants;

@Slf4j
@Controller
public class MainController {

    @GetMapping(ApiConstants.MAIN_BASE)
    public String index() {
        log.info("Запрошена главная страница");
        return "index"; // возвращает index.html
    }

    @GetMapping(ApiConstants.ABOUT)
    public String about() {
        log.info("Запрошена страница 'О проекте'");
        return "about";
    }
}
