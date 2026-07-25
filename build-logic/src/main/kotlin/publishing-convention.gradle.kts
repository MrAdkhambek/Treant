import com.vanniktech.maven.publish.DeploymentValidation
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    id("com.vanniktech.maven.publish")
}

extensions.configure<MavenPublishBaseExtension> {
    // Wait for Central to validate the upload instead of the default NONE, which only
    // uploads a bundle and reports success. Validation happens asynchronously on the
    // Portal, so without this a release could be rejected — bad signature, incomplete
    // POM, duplicate version — while the workflow still showed a green tick.
    //
    // VALIDATED, not PUBLISHED: the deployment stays staged for a manual release, which
    // keeps the final "this version is permanent" step a deliberate human action.
    publishToMavenCentral(
        automaticRelease = false,
        validateDeployment = DeploymentValidation.VALIDATED,
    )
    signAllPublications()
}
