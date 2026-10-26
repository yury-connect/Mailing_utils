package ru.yury.mailing_utils.constant;

public abstract class ApiConstants {

    // Базовые пути (для контроллеров)
    public static final String MAIN_BASE = "/";
    public static final String ABOUT = "/about";

    public static final String EMAIL_TOOLS_BASE = "/tools/email-filter";
    public static final String EMAIL_WITHOUT_FILTER = "/without";
    public static final String EMAIL_FILTER_BY_NAME = "/name";
    public static final String EMAIL_FILTER_BY_DOMAIN_AND_NAME = "/domain-and-name";

    // Для управления приложением (Actuator)
    public static final String ACTUATOR_SHUTDOWN = "/actuator/shutdown";

    // Приватный конструктор, чтобы никто случайно не создал экземпляр абстрактного класса с константами
    private ApiConstants() {
    }

}
