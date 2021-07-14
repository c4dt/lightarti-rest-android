# arti-android

> :warning: **Warning: arti-android is not secure in all situations** Arti-android builds on top of [arti-rest]((https://github.com/c4dt/arti-rest), which modifies several core parts of `arti`. It therefore does not have the same security guarantees as arti or the stock Tor client would. For more information, see the [arti-rest]((https://github.com/c4dt/arti-rest) respository.

Android wrapper based on [arti](https://gitlab.torproject.org/tpo/core/arti), "An
implementation of Tor, in Rust".

Library releases are published on Maven.
To use the library in your project, add the following to your `build.gradle`:

```
implementation 'io.github.c4dt:artiwrapper:<version>'
```

**WARNING**

- Arti is **not ready for production use**; see
  [here](https://gitlab.torproject.org/tpo/core/arti#status) for Arti's current
  status.
- This wrapper uses a [modified](https://github.com/c4dt/arti-rest) version of
  Arti. In particular, an extension is that it allows to sideload the nodes
  information (consensus, microdescriptors).
