#!/usr/bin/env python

import argparse


def generate(factors, formats, tables):
    limits = {
        # nation and region don't depend on the scale factor
        "customer": 150000,
        "lineitem": 6000000,
        "orders": 1500000,
        "part": 200000,
        "partsupp": 800000,
        "supplier": 10000,
    }
    source_schemas = {
        "sf10": "sf100",
    }
    for format in formats:
        for factor in factors:
            num_factor = int("".join(filter(str.isdigit, factor)))
            new_schema = "tpch_" + factor + "_" + format
            source_schema = "tpch." + source_schemas.get(factor, factor)

            print(
                "CREATE SCHEMA IF NOT EXISTS hive.{};".format(
                    new_schema,
                )
            )
            for table in tables:
                limit = (
                    ("LIMIT " + str(limits[table] * num_factor)) if table in limits else ""
                )
                print(
                    'CREATE TABLE IF NOT EXISTS "hive"."{}"."{}" WITH (format = \'{}\') AS SELECT * FROM {}."{}" {};'.format(
                        new_schema, table, format, source_schema, table, limit
                    )
                )


def main():

    parser = argparse.ArgumentParser(description="Generate test data.")
    parser.add_argument(
        "--factors",
        type=csvtype(
            [
                "tiny",
                "sf1",
                "sf10",
                "sf100",
                "sf300",
                "sf1000",
                "sf3000",
                "sf10000",
                "sf30000",
                "sf100000",
            ]
        ),
        default=["tiny"],
    )
    parser.add_argument("--formats", type=csvtype(["orc", "text"]), default=["orc"])
    default_tables = [
        "customer",
        "lineitem",
        "nation",
        "orders",
        "part",
        "partsupp",
        "region",
        "supplier",
    ]
    parser.add_argument(
        "--tables", type=csvtype(default_tables), default=default_tables
    )

    args = parser.parse_args()
    generate(args.factors, args.formats, args.tables)


def csvtype(choices):
    """Return a function that splits and checks comma-separated values."""

    def splitarg(arg):
        values = arg.split(",")
        for value in values:
            if value not in choices:
                raise argparse.ArgumentTypeError(
                    "invalid choice: {!r} (choose from {})".format(
                        value, ", ".join(map(repr, choices))
                    )
                )
        return values

    return splitarg


if __name__ == "__main__":
    main()
