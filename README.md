# arti-android

Android wrapper based on [arti](https://gitlab.torproject.org/tpo/core/arti), "An
implementation of Tor, in Rust".

> :warning: **Warning: arti-android is not secure in all situations**
> Arti-android builds on top of [arti-rest](https://github.com/c4dt/arti-rest),
> which modifies several core parts of `arti`. It therefore does not have the
> same security guarantees as arti or the stock Tor client would. Before
> integrating this library check the reliability considerations in the
> [arti-rest](https://github.com/c4dt/arti-rest) repository to make sure that
> the security offered by this library is sufficient for your use case. In case
> of doubt, contact us in this repo. We'll be happy to discuss enhancements and
> limitations of our solution.

## Using the wrapper

Library releases are published on Maven.
To use the library in your project, add the following to your `build.gradle`:

```
implementation 'io.github.c4dt:artiwrapper:<version>'
```

In the repository, you can find:

- An [example
  application](https://github.com/c4dt/arti-android/blob/main/artiwrapper/app/src/main/java/org/c4dt/myapplication/MainActivity.java)
  that uses the [documented wrapper
  API](https://github.com/c4dt/arti-android/blob/main/artiwrapper/src/main/java/org/c4dt/artiwrapper/TorLibApi.java).
- An [instrumented test
  suite](https://github.com/c4dt/arti-android/blob/main/artiwrapper/src/androidTest/java/org/c4dt/artiwrapper/JniTest.java)
  to run on emulators and devices.

## Releasing on Maven

In order to publish a new release on Maven, follow these steps:

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
