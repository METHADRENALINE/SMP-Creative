# SMP&Creative

Dev repo of the SMP&Creative mc server by METHADRENALINE ᵍʳᵒᵘᵖ.

SMP&Creative is a private mc server project built for one specific environment. This repo contains server side code, resources and content made only for this server.

Production runtime files, compiled jars, logs, databases, player data, redeem codes, credentials, tokens, server config and machine specific deployment files are intentionally excluded.

## Contents

| Path                                   | Type             | Purpose                                                                 |
| -------------------------------------- | ---------------- | ----------------------------------------------------------------------- |
| [plugins/MACore](plugins/MACore)                         | Paper plugin     | Lightweight chat and teleport requests                                  |
| [plugins/MALang](plugins/MALang)                         | Paper plugin     | Per-player language layer                                               |
| [plugins/MADialogs](plugins/MADialogs)                      | Paper plugin     | GUI dialogues for SMP&Creative lobby NPCs                               |
| [plugins/MAAura](plugins/MAAura)                         | Paper plugin     | Player aura menu and particles for SMP&Creative                         |
| [plugins/MALobbyDesign](plugins/MALobbyDesign)                  | Paper plugin     | Lobby ambience tasks                                                    |
| [plugins/MAVeloCore](plugins/MAVeloCore)                     | Velocity plugin  | Proxy core for Velocity network                                         |
| [resourcepacks/SMP&Creative Content Pack](resource-packs/SMP&Creative-Content-Pack) | Resource pack    | Server logos, icons, and other cosmetic assets for SMP&Creative         |

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

Licensing is split by content type.

Code related files are licensed under the GNU General Public License v3.0 only.

See [LICENSE](LICENSE) for details.

The SMP&Creative Content Pack is covered by separate resource pack terms.

See its [TERMS.md](resourcepacks/SMP%26Creative%20Content%20Pack/TERMS.md) for details.
