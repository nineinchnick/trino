#!/usr/bin/env bash

set -euo pipefail
shopt -s extglob

for cmd in csv2md docker; do
    if ! command -v "$cmd" >/dev/null; then
        echo >&2 "Missing the $cmd command"
        exit 1
    fi
done

# TODO replace psql with trino
container_name=benchto-postgres
db_name=benchto
db_user=postgres
if ! status=$(docker inspect $container_name --format "{{json .State.Status}}") || [ "$status" != '"running"' ]; then
    echo >&2 "Container '$container_name' does not exist or is not running (status is $status)"
    exit 1
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
cd "$SCRIPT_DIR" || exit 1

function query_report() {
    local query=$1
    shift
    docker exec -i \
        $container_name \
        psql \
        "$db_name" \
        "$db_user" \
        --csv "$@" \
        <<<"$query" \
        | csv2md
}

function query_tuples() {
    local query=$1
    shift
    docker exec -i \
        $container_name \
        psql \
        "$db_name" \
        "$db_user" \
        -t \
        -q \
        <<<"$query" \
        "$@"
}

# create a report by executing sql files in order
function report() {
    local target=report.md
    local title="Benchmarks report"
    local queries=([0-9][0-9]-*.sql)
    local comments desc

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
            query_report "$(<"$file")"
            continue
        fi
        title=$(echo "$comments" | head -1)
        desc=$(echo "$comments" | tail -n +2)
        title=${title#--}
        {
            echo "# $title"
            echo "$desc"
            echo >&2 "Executing query from $file"
            query_report "$(<"$file")"
            echo ""
        } >>"$target"
    done

    echo "Generated on $(date)" >>"$target"
}

function run_details() {
    local id=$1
    local target=runs/$id.md

    mkdir -p "$(dirname "$target")"
    echo >&2 "Generating report for run $id"
    {
        echo "Run details"
        echo "==========="
        echo ""
        echo "## Properties"
        query_report "$(<run_details.sql)" -v "id=$id"
        echo ""
        echo "## Aggregated measurements"
        query_report "$(<run_measurements.sql)" -v "id=$id"
        echo ""
        echo "## Executions"
        query_report "$(<run_executions.sql)" -v "id=$id"
        echo ""
        echo "Generated on $(date)"
    } >"$target"
}

# dump all run details into separate files
function dump_runs() {
    echo >&2 "Listing all runs"
    mapfile -t run_ids < <(query_tuples "$(<runs.sql)")
    echo >&2 "Got ${#run_ids[@]} runs"

    for id in "${run_ids[@]}"; do
        id=${id//[[:blank:]]/}
        [ -n "$id" ] || continue
        run_details "$id"
    done
}

report
dump_runs
