# redis

**A Redis-compatible in-memory key-value server built from scratch in Java 21. It speaks the real Redis protocol (RESP), serves many clients at once with virtual threads, expires keys, and handles pipelined requests at close to half a million operations per second. It has zero external dependencies.**

![Java](https://img.shields.io/badge/java-21-orange) ![Dependencies](https://img.shields.io/badge/dependencies-0-brightgreen) ![License: MIT](https://img.shields.io/badge/license-MIT-blue)

[Features](#features) · [Quick start](#quick-start) · [Benchmarks](#benchmarks) · [How I got there](#how-i-got-there) · [Architecture](#architecture) · [Design decisions](#design-decisions)

## Features

- **Real Redis protocol (RESP).** The official `redis-benchmark` connects and runs unchanged. Inline commands (`PING\r\n`) also work, so `nc` and `telnet` can talk to it.
- **Concurrent clients.** One virtual thread per connection (Java 21), so every client has simple blocking code and idle connections cost almost nothing.
- **Pipelining.** Many commands can arrive in a single packet, and the replies go back together.
- **Key expiry**, both lazy (checked on read) and active (a background sweep every 100 ms).
- **Zero external dependencies.** Only the JDK.
- **Scope:** string keys and values, held in memory.

**Supported commands**

| Command | What it does |
| --- | --- |
| `PING [message]` | Replies `PONG`, or echoes `message` back |
| `ECHO message` | Replies with `message` |
| `SET key value [PX ms]` | Stores `value` under `key`, optionally expiring after `ms` milliseconds |
| `GET key` | Returns the value, or an error reply (`ERR no key found`) if the key is missing or expired |
| `DEL key [key ...]` | Deletes the keys and returns how many existed |
| `EXISTS key` | Returns 1 if the key exists, else 0 |
| `INFO` | Returns basic server info |
| `COMMAND` | Returns an empty array (client handshake support) |

## Quick start

You need **JDK 21+** and **Maven**.

```bash
git clone https://github.com/caals666/redis
cd redis/redis
mvn compile
java -cp target/classes org.example.Server --port 6969
```

The server listens on port **6969** by default; `--port` changes it.

Talk to it with `redis-cli` (or any RESP client):

```
$ redis-cli -p 6969 ping
PONG
$ redis-cli -p 6969 set name sriram
OK
$ redis-cli -p 6969 get name
"sriram"
$ redis-cli -p 6969 set session abc px 5000
OK
$ redis-cli -p 6969 exists session
(integer) 1
```

Five seconds later, `exists session` returns `(integer) 0`.

No `redis-cli`? Use `nc`:

```bash
printf 'PING\r\n' | nc localhost 6969
```

## Benchmarks

**Setup:** Windows, Java 21, server and `redis-benchmark` on the same machine over loopback, 50 parallel clients, 1,000,000 requests.

| Command | No pipelining | Pipelined (`-P 16`) |
| --- | --- | --- |
| SET | **86,192** req/s | **496,771** req/s |
| GET | **84,760** req/s | **462,320** req/s |

```bash
# No pipelining
redis-benchmark -h 127.0.0.1 -p 6969 -t set,get -n 1000000 -c 50 -q

# Pipelined, 16 commands per batch
redis-benchmark -h 127.0.0.1 -p 6969 -t set,get -n 1000000 -c 50 -P 16 -q
```

Pipelining gives roughly a **5.5x** gain, because the server reads a whole batch of commands with a single socket read and parses them straight out of memory.

## How I got there

Each step was measured with the same `redis-benchmark` command.

| Step | Change | No pipelining | Pipelined (`-P 16`) |
| --- | --- | --- | --- |
| 1 | First working version, with a log line printed for every command and every reply | ~12k SET / ~17k GET | ~12k SET / ~16k GET |
| 2 | Removed hot-path `println` calls (stdout is synchronized and slow, so 50 clients were queuing on it), enabled `TCP_NODELAY`, raised stream buffers to 64 KB | ~80k | ~80k |
| 3 | Parser reads from its own 64 KB buffer: one `read()` call on the socket pulls in many bytes, and the parser walks the array instead of calling `read()` once per byte | ~86k | ~500k |

Removing the logging and tuning the sockets moved the non-pipelined numbers from the low tens of thousands to about 80k. Pipelining stayed flat at ~80k until the per-byte reads were gone. That showed the bottleneck was the parser, not the network, and fixing it took pipelined throughput to about 500k.

## Architecture

```
redis-cli / any client
        │  TCP, RESP
        ▼
 Server (accept loop)  ──►  one virtual thread per client (clientHandler)
                                   │
                    RespReader: bytes ─► RespValue
                                   │
                    Command.fromRespValue: RespValue ─► Command
                                   │
                    CommandHandler: Command ─► store call ─► RespValue
                                   │
                    RespWriter: RespValue ─► bytes ─► client

 RedisStore (ConcurrentHashMap<String, ValueEntry>)
      ▲
      └── cleanup task every 100 ms removes expired keys
```

| Class | Responsibility |
| --- | --- |
| `Server` | Parses `--port`, accepts TCP connections, starts a virtual thread per client |
| `clientHandler` | Read, execute, reply loop for one connection |
| `protocol/RespReader` | Parses RESP simple strings, bulk strings, arrays, and inline commands from a 64 KB buffer |
| `protocol/RespValue`, `RespType` | In-memory form of a RESP value, with its encoder |
| `protocol/RespWriter` | Writes replies to the socket in RESP format |
| `command/Command` | Turns a parsed RESP array into a command name plus arguments |
| `command/CommandHandler` | Maps command names to store operations and builds the reply |
| `storage/RedisStore` | The data, plus lazy and active expiry |
| `storage/ValueEntry` | A value with an optional absolute expiry timestamp |

**What a command looks like on the wire.** `redis-cli set name redis` sends:

```
*3\r\n$3\r\nSET\r\n$4\r\nname\r\n$5\r\nredis\r\n
```

That is an array (`*`) of 3 bulk strings (`$`), each prefixed by its length. The server replies `+OK\r\n`.

## Design decisions

### Virtual threads instead of a thread pool or an event loop

Each client gets its own blocking read loop on a virtual thread (`Executors.newVirtualThreadPerTaskExecutor()`). The code stays simple and sequential, and a blocked read parks the virtual thread instead of holding an OS thread.

### A hand-written parser with its own buffer

`RespReader` owns a reusable 64 KB `byte[]`. A single `read()` call on the socket fills it with as many bytes as have arrived, and parsing then walks the array in memory. That is what makes pipelining cheap: a batch of 16 commands costs one socket read instead of dozens of per-byte calls. A command split across two packets works too, because the reader refills the buffer when it runs out mid-command.

### Output is flushed only when needed

The server flushes its output buffer only when no more input is waiting on the socket, so a pipelined batch goes back in few writes. `TCP_NODELAY` is on, so a single non-pipelined reply is never held back by Nagle's algorithm.

### `ConcurrentHashMap` instead of one global lock

Single-key operations (`SET`, `GET`, `DEL`, `EXISTS`) are atomic and need no explicit locking, and reads do not block each other.

### Expiry stored as an absolute timestamp

`ValueEntry` stores the moment a key dies (`now + ttl`), not "N ms from now". Every expiry check becomes one comparison against the clock.

### Lazy plus active expiry

- **Lazy:** `GET` checks the entry's deadline and removes the key if it has passed, so an expired key is never returned.
- **Active:** a daemon thread sweeps the map every 100 ms and removes expired entries, so keys nobody reads again still free their memory.

### Layered protocol code

Parsing bytes, interpreting a command and encoding a reply are separate classes (`RespReader`, `Command` and `CommandHandler`, `RespWriter`). Adding a command means adding one handler method and touching nothing in the protocol code.

## License

[MIT](LICENSE)