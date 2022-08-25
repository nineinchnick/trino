#!/usr/bin/env bash

set -euo pipefail

for cmd in csv2md docker; do
    if ! command -v "$cmd" >/dev/null; then
        echo >&2 "Missing the $cmd command"
        exit 1
    fi
done

# TODO replace psql with trino
container_name=benchto-postgres
if ! status=$(docker inspect $container_name --format "{{json .State.Status}}") || [ "$status" != '"running"' ]; then
    echo >&2 "Container '$container_name' does not exist or is not running (status is $status)"
    exit 1
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
cd "$SCRIPT_DIR" || exit 1

target=report.md
title="Benchmarks report"
queries=(*.sql)

function run_query() {
    local file=$1
    echo >&2 "Executing query from $file"
    docker cp "$file" $container_name:/tmp/
    docker exec \
        $container_name \
        psql \
        benchto \
        postgres \
        -f "/tmp/$(basename "$file")" \
        --csv | csv2md
}

mkdir -p "$(dirname "$target")"
{
    echo "$title"
    echo "======="
    echo ""
} >"$target"

for file in "${queries[@]}"; do
    # get all comments from the top of the file
    comments=$(sed -n '/^-/!q;p' "$file" | sed 's/^-- //g')
    if [ -z "$comments" ]; then
        echo >&2 "First line of file $file needs to be an SQL comment with the report title (should start with --), query will run but results will be ignored"
        run_query "$file"
        continue
    fi
    title=$(echo "$comments" | head -1)
    desc=$(echo "$comments" | tail -n +2)
    title=${title#--}
    {
        echo "# $title"
        echo "$desc"
        run_query "$file"
        echo ""
    } >>"$target"
done

echo "Generated on $(date)" >>"$target"
