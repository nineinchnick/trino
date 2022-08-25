#!/usr/bin/env python

import argparse

def generate(factors, formats, tables):
    schemas = [
        # (new_schema, source_schema)
        ('tpcds_sf10_orc', 'tpcds.sf10'),
        ('tpcds_sf30_orc', 'tpcds.sf30'),
        ('tpcds_sf100_orc', 'tpcds.sf100'),
        ('tpcds_sf300_orc', 'tpcds.sf300'),
        ('tpcds_sf1000_orc', 'tpcds.sf1000'),
        ('tpcds_sf3000_orc', 'tpcds.sf3000'),
        ('tpcds_sf10000_orc', 'tpcds.sf10000'),
    ]

    tables = [
        'call_center',
        'catalog_page',
        'catalog_returns',
        'catalog_sales',
        'customer',
        'customer_address',
        'customer_demographics',
        'date_dim',
        'household_demographics',
        'income_band',
        'inventory',
        'item',
        'promotion',
        'reason',
        'ship_mode',
        'store',
        'store_returns',
        'store_sales',
        'time_dim',
        'warehouse',
        'web_page',
        'web_returns',
        'web_sales',
        'web_site',
    ]

    for (new_schema, source_schema) in schemas:

        if new_schema.endswith('_orc'):
            format = 'ORC'
        elif new_schema.endswith('_text'):
            format = 'TEXTFILE'
        else:
            raise ValueError(new_schema)

        print('CREATE SCHEMA IF NOT EXISTS hive.{};'.format(new_schema,))
        for table in tables:
            print('CREATE TABLE IF NOT EXISTS "hive"."{}"."{}" WITH (format = \'{}\') AS SELECT * FROM {}."{}";'.format(
                  new_schema, table, format, source_schema, table))

def main():

    parser = argparse.ArgumentParser(description='Generate test data.')
    parser.add_argument('--factors', type=csvtype(['tiny', 'sf1', 'sf100', 'sf300', 'sf1000', 'sf3000']), default=['tiny'])
    parser.add_argument('--formats', type=csvtype(['orc', 'text']), default=['orc'])
    default_tables = ['customer', 'lineitem', 'nation', 'orders', 'part', 'partsupp', 'region', 'supplier']
    parser.add_argument('--tables', type=csvtype(default_tables), default=default_tables)

    args = parser.parse_args()
    generate(args.factors, args.formats, args.tables)


def csvtype(choices):
    """Return a function that splits and checks comma-separated values."""
    def splitarg(arg):
        values = arg.split(',')
        for value in values:
            if value not in choices:
                raise argparse.ArgumentTypeError(
                    'invalid choice: {!r} (choose from {})'
                    .format(value, ', '.join(map(repr, choices))))
        return values
    return splitarg


if __name__ == '__main__':
    main()
