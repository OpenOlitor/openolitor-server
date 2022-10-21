#!/usr/bin/env bash
# shellcheck disable=SC2046,SC2086,SC2116
mysql -u tegonal -ptegonal -A -D replace_me < db_schema.sql