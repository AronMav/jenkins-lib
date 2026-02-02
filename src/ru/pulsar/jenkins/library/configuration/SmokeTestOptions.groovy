package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class SmokeTestOptions extends StepCoverageOptions implements Serializable {

    @JsonPropertyDescription("""Путь к конфигурационному файлу vanessa-runner.
    По умолчанию содержит значение "./tools/vrunner.json".
    """)
    String vrunnerSettings

    @JsonPropertyDescription("""Путь к конфигурационному файлу для xddTestRunner.
    По умолчанию содержит значение "./tools/xUnitParams.json".
    """)
    String xddConfigPath

    @JsonPropertyDescription("""Использовать Vanessa-Automation вместо xddTestRunner для дымовых тестов.
    По умолчанию выключено (используется xddTestRunner).
    """)
    boolean useVanessaAutomation

    @JsonPropertyDescription("""Путь к конфигурационному файлу Vanessa-Automation для дымовых тестов.
    По умолчанию содержит значение "./tools/VASmokeParams.json".
    """)
    String vanessaSettings

    @JsonPropertyDescription("""Путь к каталогу feature-файлов дымовых тестов.
    По умолчанию содержит значение "/storage/features/smoke".
    """)
    String smokeFeaturesPath

    @JsonPropertyDescription("""Путь к обработке vanessa-automation.epf.
    По умолчанию содержит значение "/storage/vanessa-automation/vanessa-automation.epf".
    """)
    String pathVanessa

    @JsonPropertyDescription("""Выполнять публикацию результатов в отчет Allure.
    По умолчанию выключено.
    """)
    boolean publishToAllureReport

    @JsonPropertyDescription("""Выполнять публикацию результатов в отчет JUnit.
    По умолчанию включено.
    """)
    boolean publishToJUnitReport

    @Override
    @NonCPS
    String toString() {
        return "SmokeTestOptions{" +
            "vrunnerSettings='" + vrunnerSettings + '\'' +
            ", xddConfigPath='" + xddConfigPath + '\'' +
            ", useVanessaAutomation=" + useVanessaAutomation +
            ", vanessaSettings='" + vanessaSettings + '\'' +
            ", smokeFeaturesPath='" + smokeFeaturesPath + '\'' +
            ", pathVanessa='" + pathVanessa + '\'' +
            ", publishToAllureReport=" + publishToAllureReport +
            ", publishToJUnitReport=" + publishToJUnitReport +
            ", coverage=" + coverage +
            ", dbgsPort=" + dbgsPort +
            '}'
    }
}
