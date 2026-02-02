package ru.pulsar.jenkins.library.steps

import hudson.FilePath
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.StringJoiner
import ru.pulsar.jenkins.library.utils.VRunner

class SmokeTest implements Serializable, Coverable {

    public static final String ALLURE_STASH = 'smoke-allure'
    public static final String COVERAGE_STASH_NAME = 'smoke-coverage'
    public static final String COVERAGE_STASH_PATH = 'build/out/smoke-coverage.xml'
    public static final String COVERAGE_PIDS_PATH = 'build/smoke-pids'

    private final JobConfiguration config

    SmokeTest(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (!config.stageFlags.smoke) {
            Logger.println("Smoke test step is disabled")
            return
        }

        List<String> logosConfig = ["LOGOS_CONFIG=$config.logosConfig"]
        steps.withEnv(logosConfig) {
            steps.installLocalDependencies()
        }

        def options = config.smokeTestOptions

        if (options.useVanessaAutomation) {
            runWithVanessaAutomation(steps, options, logosConfig)
        } else {
            runWithXddTestRunner(steps, options, logosConfig)
        }
    }

    private def runWithVanessaAutomation(IStepExecutor steps, def options, List<String> logosConfig) {
        Logger.println("Запуск дымовых тестов через Vanessa-Automation")

        String vrunnerPath = VRunner.getVRunnerPath()
        String command = "$vrunnerPath vanessa --ibconnection \"/F./build/ib\""

        String vrunnerSettings = options.vrunnerSettings
        if (vrunnerSettings != null && !vrunnerSettings.isEmpty() && steps.fileExists(vrunnerSettings)) {
            command += " --settings $vrunnerSettings"
        }

        String vanessaSettings = options.vanessaSettings
        if (vanessaSettings == null || vanessaSettings.isEmpty()) {
            vanessaSettings = "./tools/VASmokeParams.json"
        }
        if (steps.fileExists(vanessaSettings)) {
            command += " --vanessasettings $vanessaSettings"
        }

        String smokeFeaturesPath = options.smokeFeaturesPath
        if (smokeFeaturesPath == null || smokeFeaturesPath.isEmpty()) {
            smokeFeaturesPath = "/storage/features/smoke"
        }
        command += " --path $smokeFeaturesPath"

        String pathVanessa = options.pathVanessa
        if (pathVanessa == null || pathVanessa.isEmpty()) {
            pathVanessa = "/storage/vanessa-automation/vanessa-automation.epf"
        }
        command += " --pathvanessa $pathVanessa"

        String allureReportDir = "build/out/allure/smoke"
        steps.createDir(allureReportDir)
        steps.createDir('build/out')

        steps.withEnv(logosConfig) {
            List<Integer> returnStatuses = []

            steps.withCoverage(config, this, options) {
                Logger.println("Выполнение дымовых тестов командой: ${command}")
                Integer smokeReturnStatus = VRunner.exec(command, true)
                returnStatuses.add(smokeReturnStatus)
            }

            if (returnStatuses.isEmpty()) {
                Logger.println("Нет шагов для выполнения дымовых тестов")
            } else if (Collections.max(returnStatuses) > 2) {
                steps.error("Получен неожиданный/неверный результат работы. Возможно, работа 1С:Предприятие завершилась некорректно")
            } else if (returnStatuses.contains(1)) {
                steps.unstable("Дымовое тестирование завершилось, но часть тестов упала")
            } else {
                Logger.println("Дымовое тестирование завершилось успешно")
            }
        }

        if (options.publishToAllureReport) {
            steps.stash(ALLURE_STASH, "$allureReportDir/**", true)
            steps.archiveArtifacts("$allureReportDir/**")
        }
    }

    private def runWithXddTestRunner(IStepExecutor steps, def options, List<String> logosConfig) {
        Logger.println("Запуск дымовых тестов через xddTestRunner")

        def env = steps.env()
        String vrunnerPath = VRunner.getVRunnerPath()
        String command = "$vrunnerPath xunit --ibconnection \"/F./build/ib\""

        String vrunnerSettings = options.vrunnerSettings
        if (vrunnerSettings != null && !vrunnerSettings.isEmpty() && steps.fileExists(vrunnerSettings)) {
            command += " --settings $vrunnerSettings"
        }

        String xddTestRunnerPath = "./oscript_modules/add/xddTestRunner.epf"
        if (steps.fileExists(xddTestRunnerPath)) {
            command += " --pathxunit $xddTestRunnerPath"
        }

        String xddConfigPath = options.xddConfigPath
        if (xddConfigPath != null && !xddConfigPath.isEmpty() && steps.fileExists(xddConfigPath)) {
            command += " --xddConfig $xddConfigPath"
        }

        String junitReport = "build/out/jUnit/smoke/smoke.xml"
        FilePath pathToJUnitReport = FileUtils.getFilePath("$env.WORKSPACE/$junitReport")
        String junitReportDir = FileUtils.getLocalPath(pathToJUnitReport.getParent())

        String allureReport = "build/out/allure/smoke/allure.xml"
        FilePath pathToAllureReport = FileUtils.getFilePath("$env.WORKSPACE/$allureReport")
        String allureReportDir = FileUtils.getLocalPath(pathToAllureReport.getParent())

        StringJoiner reportsConfigConstructor = new StringJoiner(";")

        if (options.publishToJUnitReport) {
            steps.createDir(junitReportDir)
            String junitReportCommand = "ГенераторОтчетаJUnitXML{$junitReport}"
            reportsConfigConstructor.add(junitReportCommand)
        }

        if (options.publishToAllureReport) {
            steps.createDir(allureReportDir)
            String allureReportCommand = "ГенераторОтчетаAllureXMLВерсия2{$allureReport}"
            reportsConfigConstructor.add(allureReportCommand)
        }

        if (reportsConfigConstructor.length() > 0) {
            String reportsConfig = reportsConfigConstructor.toString()
            command += " --reportsxunit \"$reportsConfig\""
        }

        if (steps.isUnix()) {
            command = command.replace(';', '\\;')
        }

        if (vrunnerSettings == null || !VRunner.configContainsSetting(vrunnerSettings, "testsPath")) {
            String testsPath = "oscript_modules/add/tests/smoke"
            if (!steps.fileExists(testsPath)) {
                testsPath = '$addRoot/tests/smoke'
                if (steps.isUnix()) {
                    testsPath = '\\' + testsPath
                }
            }
            command += " $testsPath"
        }

        steps.withEnv(logosConfig) {
            steps.withCoverage(config, this, options) {
                VRunner.exec(command, true)
            }

            if (options.publishToAllureReport) {
                steps.stash(ALLURE_STASH, "$allureReportDir/**", true)
                steps.archiveArtifacts("$allureReportDir/**")
            }

            if (options.publishToJUnitReport) {
                steps.junit("$junitReportDir/*.xml", true)
                steps.archiveArtifacts("$junitReportDir/**")
            }
        }
    }

    @Override
    String getStageSlug() {
        return "smoke"
    }

    @Override
    String getCoverageStashPath() {
        return COVERAGE_STASH_PATH
    }

    @Override
    String getCoverageStashName() {
        return COVERAGE_STASH_NAME
    }

    @Override
    String getCoveragePidsPath() {
        return COVERAGE_PIDS_PATH
    }
}
