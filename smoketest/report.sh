#!/usr/bin/env bash

set -euo pipefail
shopt -s extglob

for cmd in csv2md docker pandoc psql; do
    if ! command -v "$cmd" >/dev/null; then
        echo >&2 "Missing the $cmd command"
        exit 1
    fi
done

# TODO document ALTER USER postgres SET IntervalStyle = 'postgres_verbose';
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

title="Benchmarks report"
date=$(date)

function query_report() {
    local query=$1
    shift
    docker exec -i \
        $container_name \
        psql \
        "$db_name" \
        "$db_user" \
        --csv "$@" \
        <<<"$query" |
        csv2md
}

function query_expanded() {
    local query=$1
    shift
    docker exec -i \
        $container_name \
        psql \
        "$db_name" \
        "$db_user" \
        --expanded "$@" \
        <<<"$query"
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
        "$@" <<<"$query"
}

# create a report by executing sql files in order
function report() {
    local target=report.md
    local queries=(??-*.sql)
    local section comments desc

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
        section=$(echo "$comments" | head -1)
        desc=$(echo "$comments" | tail -n +2)
        section=${section#--}
        {
            echo "## $section"
            echo ""
            echo "$desc"
            echo ""
            echo >&2 "Executing query from $file"
            query_report "$(<"$file")"
            echo ""
            echo ""
        } >>"$target"
    done

    echo "Generated on $date" >>"$target"
}

function env_details() {
    local id=$1
    local target=envs/$id.md

    mkdir -p "$(dirname "$target")"
    echo >&2 "Generating report for env $id"
    {
        echo "Environment details"
        echo "==========="
        echo ""
        echo "## Properties"
        query_expanded "$(<env_details.sql)" -v "id=$id"
        echo ""
        echo "Generated on $date"
    } >"$target"
}

# dump all env details into separate files
function dump_envs() {
    echo >&2 "Listing all environments"
    read -r -d '' query <<SQL || true
SELECT id
FROM environments
ORDER BY id;
SQL
    mapfile -t ids < <(query_tuples "$query")
    echo >&2 "Got ${#ids[@]} environments"

    for id in "${ids[@]}"; do
        id=${id//[[:blank:]]/}
        [ -n "$id" ] || continue
        env_details "$id"
    done
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
        echo "Generated on $date"
    } >"$target"
}

# dump all run details into separate files
function dump_runs() {
    echo >&2 "Listing all runs"
    read -r -d '' query <<SQL || true
SELECT id
FROM benchmark_runs
WHERE status = 'ENDED'
ORDER BY id;
SQL
    mapfile -t ids < <(query_tuples "$query")
    echo >&2 "Got ${#ids[@]} runs"

    for id in "${ids[@]}"; do
        id=${id//[[:blank:]]/}
        [ -n "$id" ] || continue
        run_details "$id"
    done
}

report
pandoc --standalone --toc \
    --template=template.html \
    --metadata title="$title" \
    --metadata date="$date" \
    -o report.html \
    report.md
dump_envs
dump_runs
