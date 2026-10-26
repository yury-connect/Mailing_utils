package ru.yury.mailing_utils.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Ловим наше кастомное исключение
    @ExceptionHandler(InvalidEmailDataException.class)
    public String handleInvalidData(InvalidEmailDataException ex, Model model) {
        // Возвращаем пользователя на страницу фильтрации и показываем ошибку
        model.addAttribute("error", "Ошибка валидации данных: " + ex.getMessage());
        // Важно вернуть то же имя шаблона, откуда пришел запрос
        return "email_filter";
    }

    // Ловим любые другие непредвиденные ошибки (NullPointer, ArrayIndexOutOfBounds и т.д.)
    @ExceptionHandler(Exception.class)
    public String handleAllErrors(Exception ex, Model model) {
        model.addAttribute("error", "Системная ошибка: " + ex.getMessage());
        return "email_filter";
    }
}
