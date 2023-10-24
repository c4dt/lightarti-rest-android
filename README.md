# lightarti-rest-android

Android wrapper based on [arti](https://gitlab.torproject.org/tpo/core/arti), "An
implementation of Tor, in Rust".

> :warning: **Warning: lightarti-rest-android is not secure in all situations**
> lightarti-rest-android builds on top of [lightarti-rest](https://github.com/c4dt/lightarti-rest),
> which modifies several core parts of `arti`. It therefore does not have the
> same security guarantees as arti or the stock Tor client would. Before
> integrating this library check the reliability considerations in the
> [lightarti-rest](https://github.com/c4dt/lightarti-rest) repository to make sure that
> the security offered by this library is sufficient for your use case. In case
> of doubt, contact us in this repo. We'll be happy to discuss enhancements and
> limitations of our solution.

## Using the wrapper

Library releases are published on
[Maven](https://search.maven.org/search?q=lightarti-rest).
To use the library in your project, add the following to your `build.gradle`:

```
implementation 'io.github.c4dt:lightarti-rest:<version>'
```

In the repository, you can find:

- An [example
  application](https://github.com/c4dt/lightarti-rest-android/blob/main/artiwrapper/app/src/main/java/org/c4dt/myapplication/MainActivity.java)
  that uses the documented wrapper API:
  [here](https://github.com/c4dt/lightarti-rest-android/blob/main/artiwrapper/src/main/java/org/c4dt/artiwrapper/TorLibApi.java)
  and [here](https://github.com/c4dt/lightarti-rest-android/blob/main/artiwrapper/src/main/java/org/c4dt/artiwrapper/Client.java).
- An [instrumented test
  suite](https://github.com/c4dt/lightarti-rest-android/blob/main/artiwrapper/src/androidTest/java/org/c4dt/artiwrapper/JniTest.java)
  to run on emulators and devices.

### Supported versions

* Android SDK 30
* Android SDK 31
* Android SDK 32
* Android SDK 33
* Android SDK 34

Android NDK is pinned to version 25.2.9519653 due to missing dependencies in later
versions, please refer to this [cargo-ndk issue](https://github.com/bbqsrc/cargo-ndk/issues/94)
for details. Also a thank you to the [meli project](https://github.com/p2panda/meli) for proposing
a [workaround](https://github.com/p2panda/meli/pull/21) that we integrated as well.

## Releasing on Maven

In order to publish a new release on Maven, follow these steps:

- Go to the [rust](./rust) directory and update to the latest release tag
- Update `VERSION_NAME` and `VERSION_CODE` in the [properties
  file](https://github.com/c4dt/lightarti-rest-android/blob/main/artiwrapper/gradle.properties).
- Commit to `main`  
- Tag the repository (with a tag following [semver](https://semver.org/),
  format `v[0-9]+.[0-9]+.[0-9]+)`).
- Push the tag to the repository (must be on the `main` branch).
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
- After 2h-3h, the release will be available in
  [Maven](https://search.maven.org/search?q=lightarti-rest), with Group ID
  `io.github.c4dt` and Artifact ID `lightarti-rest`.

# Running the example application

To run the example application locally, it is recommended that you install [Android Studio](https://developer.android.com/studio)
following the installation instructions for your distribution/operating system.

Once installed, run the Android Studio application and import this project via "Open" and selecting the project from
your system's directory tree.

Make sure to have the Android SDK/CMake installed, by going to "File" > "Settings" > "Languages & Frameworks" > "Android SDK"
and in the tab "SDK Tools" checking for "Android SDK Build-Tools" the version you want to run (must be one of the supported
versions), for "NDK" the version 25.2.9519653 and for "CMake" the latest available version. Click then on "Apply" to install
these dependencies.

You then need to add device(s) to run the example application on. Choose "Create Device" in the "Device Manager" menu and
follow the instructions. You will need to install the emulators for the chosen devices which may each take up to
10GB of disk space once installed and set up. Make sure that the devices correspond to one of the supported versions of
the Android SDK.

Android Studio should have found the ArtiWrapper example app automatically, so that you can now choose which of your devices
to run it on from the drop-down menu left of the app in the toolbar and clicking on the green "Run" button. The project
will be build and the emulator will be started and after a brief start-up period will show a confirmation screen that
it retrieved the updated lightArti directory cache.

# Contributors

`lightarti-rest-android` is maintained by the [Center for Digital Trust](https://c4dt.org/). The following people contributed to the implementation of `lightarti-rest`:

- Linus Gasser, C4DT
- Valérian Rousset, C4DT
- Christian Grigis, C4DT
- Laurent Girod, SPRING Lab, EPFL

Analysis and design by:

- Wouter Lueks, SPRING Lab, EPFL
- Carmela Troncoso, SPRING Lab, EPFL
