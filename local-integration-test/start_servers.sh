#!/usr/bin/env bash

readonly BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "${BASE_DIR}"
chmod -R 777 ./ftp-content-root
docker-compose up -d
