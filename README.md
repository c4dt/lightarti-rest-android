# arti-android

> :warning: **Warning: arti-android is not secure in all situations** Arti-android builds on top of [arti-rest]((https://github.com/c4dt/arti-rest), which modifies several core parts of `arti`. It therefore does not have the same security guarantees as arti or the stock Tor client would. For more information, see the [arti-rest]((https://github.com/c4dt/arti-rest) respository.

Android wrapper based on [arti](https://gitlab.torproject.org/tpo/core/arti), "An
implementation of Tor, in Rust".

Library releases are published on Maven.
To use the library in your project, add the following to your `build.gradle`:

```
implementation 'io.github.c4dt:artiwrapper:<version>'
```

## Releasing on Maven

When a new release should be published on Maven, use the following steps:

- Update `VERSION_NAME` and `VERSION_CODE` in the [properties
  file](https://github.com/c4dt/arti-android/blob/main/artiwrapper/gradle.properties).
- Tag the repository (with a tag following [semver](https://semver.org/),
  format `v[0-9]+.[0-9]+.[0-9]+)`).
- Push the tag to the repository.
- The GitHub action will build, create a GitHub release, and publish on Maven
  **in the staging area**.
- Log into the [Nexus Repository Manager](https://s01.oss.sonatype.org/) with
  the `c4dt-services` credentials.
- Click on "Staging Repositories" on the left, under "Build Promotion".
- Find the release in the top area of the screen, possibly clicking "Refresh".
- If there is an issue with the release, click on "Drop". Fix it and restart
  this procedure.
- Otherwise, if all looks good, click on "Close", giving an optional message
  when prompted. This will take a bit of time, after which the "Release" button
  will become available.
- Click on "Release", again giving an optional message when prompted.
- After some time, the release will be available in
  [Maven](https://search.maven.org/search?q=artiwrapper), with Group ID
  `io.github.c4dt` and Artifact ID `artiwrapper`.

**WARNING**

- Arti is **not ready for production use**; see
  [here](https://gitlab.torproject.org/tpo/core/arti#status) for Arti's current
  status.
- This wrapper uses a [modified](https://github.com/c4dt/arti-rest) version of
  Arti. In particular, an extension is that it allows to sideload the nodes
  information (consensus, microdescriptors).
