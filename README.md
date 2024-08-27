# Octi Sync Server

This is a synchronization server for [Octi](https://github.com/d4rken-org/octi)

## Setup

### Build server

```bash
./gradlew clean installDist
```

The binaries you can copy to a server will be placed under `./build/install/octi-sync-server-kotlin`.
More details [here](https://ktor.io/docs/3.0.0-beta-2/server-packaging.html).

### Run server

```bash
./build/install/octi-sync-server-kotlin/bin/octi-sync-server-kotlin --datapath=./octi-data
```

The following flags are available:

* `--datapath` (required) where the server should store its data
* `--debug` to enable additional log output
* `--port` to change the default port (8080)
