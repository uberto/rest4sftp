#!/usr/bin/env bash

readonly BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "${BASE_DIR}"
chmod -R 755 ./ftp-content-root
chmod 400 ./ssh-keys/*
docker-compose up -d
