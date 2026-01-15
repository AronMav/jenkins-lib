package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.utils.Logger

class SendNotifications implements Serializable {

    private final JobConfiguration config;

    SendNotifications(JobConfiguration config) {
        this.config = config
    }

    def run() {

        Logger.printLocation()

        if (config == null) {
            Logger.println("jobConfiguration is not initialized")
            return
        }

        try {
            def emailNotification = new EmailNotification(config)
            emailNotification.run()
        } catch (Exception e) {
            Logger.println("Ошибка при отправке email уведомления: ${e.message}")
        }

        try {
            def telegramNotification = new TelegramNotification(config)
            telegramNotification.run()
        } catch (Exception e) {
            Logger.println("Ошибка при отправке telegram уведомления: ${e.message}")
        }

    }
}
