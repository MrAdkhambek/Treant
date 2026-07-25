import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    id("com.vanniktech.maven.publish")
}

extensions.configure<MavenPublishBaseExtension> {
    // Deployments stay staged for a manual release, so the irreversible step remains a
    // deliberate human action. Note that this also means the plugin never waits for the
    // Portal verdict — validateDeployment is only honoured when automaticRelease is true
    // — so the publish workflow checks the deployment status itself afterwards.
    publishToMavenCentral()
    signAllPublications()
}
