{ pkgs ? import <nixpkgs> { } }:

pkgs.mkShell { buildInputs = [ pkgs.bashInteractive pkgs.openjdk11 ]; }
