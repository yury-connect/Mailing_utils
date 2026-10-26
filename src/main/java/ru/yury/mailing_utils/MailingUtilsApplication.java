package ru.yury.mailing_utils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class MailingUtilsApplication {

	public static void main(String[] args) {
		SpringApplication.run(MailingUtilsApplication.class, args);
	}

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        try {
            // Windows команда для открытия браузера по умолчанию
            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler http://localhost:54321/");
        } catch (Exception e) {
            System.out.println("Не удалось открыть браузер автоматически: " + e.getMessage());
        }
    }
}
