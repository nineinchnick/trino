#!/usr/bin/env python3

import argparse
import collections
import csv
import logging
import os
import sys
import xml.etree.ElementTree as xml

JAVA_PACKAGE_SEPARATOR = '.'
PATH_TO_PACKAGE_TRANSLATION = str.maketrans(os.sep, JAVA_PACKAGE_SEPARATOR)
COVERAGE_TO_CLASSFILE_TRANSLATION = str.maketrans(JAVA_PACKAGE_SEPARATOR, '$')


def main():
    parser = argparse.ArgumentParser(
        description="List ignored modules for a PT suite based on coverage and Trino root POM."
    )
    parser.add_argument(
        "-c",
        "--coverage",
        type=argparse.FileType("r"),
        help="A Jacoco CSV file with coverage",
    )
    parser.add_argument(
        "-p",
        "--pom",
        type=argparse.FileType("r"),
        help="Trino root POM file",
    )
    parser.add_argument(
        "-o",
        "--output",
        type=argparse.FileType("w"),
        default=sys.stdout,
        help="Filename to write output to",
    )
    parser.add_argument(
        "-d",
        "--debug",
        action="store_const",
        dest="loglevel",
        const=logging.DEBUG,
        default=logging.WARNING,
        help="Print lots of debugging statements",
    )

    args = parser.parse_args()
    logging.basicConfig(level=args.loglevel)
    build(args.coverage, args.pom, args.output)


def build(coverage_file, pom_file, output_file):
    coverage_reader = csv.DictReader(coverage_file)
    coverage = collections.defaultdict(lambda: collections.defaultdict(lambda: collections.defaultdict(int)))
    keys = set(coverage_reader.fieldnames) - set(["GROUP", "PACKAGE", "CLASS"])
    logging.debug("keys: %s", keys)
    for row in coverage_reader:
        logging.debug("row: %s", row)
        class_name = row["CLASS"].split("new ")[0].translate(COVERAGE_TO_CLASSFILE_TRANSLATION)
        for key in keys:
            coverage[row["PACKAGE"]][class_name][key] += int(row[key])
    logging.debug("coverage: %s", coverage)

    root_path = os.path.dirname(pom_file.name)
    pom = xml.parse(pom_file)
    ns = {'xmlns': pom.getroot().tag.split("{")[1].split("}")[0]}
    for element in pom.findall('xmlns:modules/xmlns:module', namespaces=ns):
        module = element.text
        module_root = os.path.join(root_path, module)
        logging.debug("module_root: %s", module_root)
        module_classes_root = os.path.join(module_root, "target", "classes")
        module_test_root = os.path.join(module_root, "target", "test-classes")
        if unused(module_classes_root, coverage) and unused(module_test_root, coverage):
            output_file.write("%s\n" % module)


def unused(source_root, coverage):
    for root, dirs, files in os.walk(source_root):
        logging.debug("root: %s, dirs: %s, files: %s", root, dirs, files)
        java_package = root[len(source_root)+1:].translate(PATH_TO_PACKAGE_TRANSLATION)
        logging.debug("java_package: %s", java_package)
        if java_package not in coverage:
            continue
        for file in files:
            if not file.endswith(".class"):
                continue
            java_class = file[:-6]
            logging.debug("java_class: %s.%s", java_package, java_class)
            class_name_parts = java_class.split('$')
            coverage_class_name = java_class
            if class_name_parts[-1].isnumeric():
                # Anonymous classes don't have the index in our coverage lookup map, since we can't tell them apart
                coverage_class_name = "$".join(class_name_parts[:-1]) + "$"
            class_coverage = coverage[java_package][coverage_class_name]
            logging.debug("Found java_class: %s in java_package: %s, with coverage: %s", java_class, java_package, class_coverage)
            if class_coverage["INSTRUCTION_COVERED"] > 0:
                return False
    return True


if __name__ == "__main__":
    main()
