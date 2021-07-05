# arti-android

Android wrapper based on [arti](https://gitlab.torproject.org/tpo/core/arti), "An
implementation of Tor, in Rust".

**WARNING**

- Arti is **not ready for production use**; see
  [here](https://gitlab.torproject.org/tpo/core/arti#status) for Arti's current
  status.
- This wrapper uses a [modified](https://github.com/c4dt/arti-rest) version of
  Arti. In particular, an extension is that it allows to sideload the nodes
  information (consensus, microdescriptors).
