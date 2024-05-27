# What is this?
This is an opinionated [Nix](https://nixos.org/) [flake](https://nixos.wiki/wiki/Flakes) for getting started with the [Scala](https://scala-lang.org/) programming language. It creates a development subshell with the following Scala tools on the path:

* [Ammonite](https://ammonite.io/)
* [Bloop](https://scalacenter.github.io/bloop/)
* [Coursier](https://get-coursier.io/)
* [GraalVM CE](https://www.graalvm.org/) based on [OpenJDK](https://openjdk.org/) 21
* [Mill](https://com-lihaoyi.github.io/mill/mill/Intro_to_Mill.html)
* [sbt](https://www.scala-sbt.org/)
* [Scala CLI](https://scala-cli.virtuslab.org/)
* [Scalafmt CLI](https://scalameta.org/scalafmt/)

In fact it can create alternative subshells with these instead:
* [Temurin](https://adoptium.net/temurin/releases/) 17
* [Temurin](https://adoptium.net/temurin/releases/) 11
* [OpenJDK](https://openjdk.org/) 8

The first time you use this subshell these tools will be downloaded and cached. Once you exit the subshell they will no longer be on your path. The second run is instantaneous.


# Installation
1. Install the Nix package manager by selecting your OS in the [official guide](https://nixos.org/download.html). Don't forget to reopen the terminal!

1. Enable the flakes feature:

    ```bash
    sh <(curl -L https://nixos.org/nix/install) --no-daemon
    ```
    ```bash
    echo 'eval "$(direnv hook zsh)"' >> ~/.zshrc
    ```
    ```bash
    mkdir -p ~/.config/nix
    ```
    ```bash
    echo 'experimental-features = nix-command flakes' >> ~/.config/nix/nix.conf
    ```
    If the Nix installation is in multi-user mode, don’t forget to restart the `nix-daemon` by running:
    ```bash
    sudo systemctl restart nix-daemon
    ```
# Usage
For [direnv](https://direnv.net/)/[nix-direnv](https://github.com/nix-community/nix-direnv) users put the following into your `.envrc`:
```bash
use flake ./nix#java17
```
