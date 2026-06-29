# SMP&Creative

Development repo for the SMP&Creative mc server.

SMP&Creative is a private mc server project built for one specific environment. This repo contains server side code, resources and content made for this server, not a general purpose plugin collection.

Production runtime files, compiled jars, logs, databases, player data, redeem codes, credentials, tokens, server config and machine specific deployment files are intentionally excluded.

## Contents

| Path                                   | Type             | Purpose                                                                 |
| -------------------------------------- | ---------------- | ----------------------------------------------------------------------- |
| plugins/MACore                         | Paper plugin     | Lightweight chat and teleport requests for SMP&Creative backend servers |
| plugins/MALang                         | Paper plugin     | Per-player language layer for SMP&Creative backend servers              |
| plugins/MADialogs                      | Paper plugin     | Book GUI dialogues for SMP&Creative lobby NPCs                          |
| plugins/MAAura                         | Paper plugin     | Per-player aura menu and particles for SMP&Creative                     |
| plugins/MALobbyDesign                  | Paper plugin     | Lobby-only ambience tasks for SMP&Creative                              |
| plugins/MAVeloCore                     | Velocity plugin  | Proxy core for SMP&Creative Velocity network                            |
| resourcepacks/smpcreative-content-pack | mc resource pack | Server logos, icons, and other cosmetic assets for SMP&Creative         |

## Build

Use JDK 25 and the Gradle Wrapper.

### Linux

```bash
./gradlew build
```

### Windows

```powershell
.\gradlew.bat build
```

All plugins are built from the repo root.

## License

Copyright (C) 2026 METHADRENALINE

Licensing is split by content type.

Source code, plugin resources, Gradle build files, and other code-related files are licensed under the GNU General Public License v3.0 only.

See [LICENSE](LICENSE) for details.

The SMP&Creative Content Pack is covered by separate resource pack terms.

See its [TERMS.md](resourcepacks/SMP%26Creative%20Content%20Pack/TERMS.md) for details.
