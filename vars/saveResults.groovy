import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.PublishAllure

def call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def publishAllure = new PublishAllure(config)
    publishAllure.run()
}