# Security Policy

## Supported versions

This repository contains custom plugins for the SMP&Creative Minecraft network.

Only the latest public state of the `main` branch is considered supported. Older commits, local deployments, generated data and modified third-party builds are not supported.

## Reporting a vulnerability

If you find a security issue in the plugin source code, please report it through GitHub Security Advisories for this repository.

Please do not open a public issue for vulnerabilities that could expose private data, allow unauthorized access, bypass permissions, crash the proxy/backend servers, or leak generated player/storage data.

## What counts as a security issue

Examples of relevant security issues:

* permission bypasses;
* unsafe command handling;
* unintended access to player data or generated storage files;
* crashes or denial-of-service issues caused by malformed input;
* unsafe handling of plugin messages between Velocity and Paper servers.

## What is out of scope

The following are not covered by this security policy:

* production server configs;
* local machine setup;
* logs, databases, player files and generated runtime data;