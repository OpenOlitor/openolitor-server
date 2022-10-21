#!/usr/bin/env bash

set -e

# shellcheck disable=SC2046,SC2086,SC2116
mysql -u root -ptegonal -e "DROP DATABASE IF EXISTS $1;"
mysql -u root -ptegonal -e "CREATE DATABASE $1;"
mysql -u root -ptegonal -e "GRANT ALL PRIVILEGES ON $1.* TO 'tegonal'@'%';"
mysql -u root -ptegonal -e "FLUSH PRIVILEGES;"
mysql -u tegonal -ptegonal -A -D"$1" < dump.sql