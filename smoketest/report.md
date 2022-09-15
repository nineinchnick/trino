Benchmarks report
=======

# Validate environments
For every environment, check for statistical outliers on every metric across all executions of every run of every benchmark.
The number of runs should be equal across all environments and there should be no invalid executions and no outliers.
| name                           | attributes                                                                     | sequence_id             | statuses       | num_runs | num_executions | num_invalid_executions | num_measurements | num_outliers | num_driver_outliers | names_outliers |
| ------------------------------ | ------------------------------------------------------------------------------ | ----------------------- | -------------- | -------- | -------------- | ---------------------- | ---------------- | ------------ | ------------------- | -------------- |
| trino-393-a501945b463          | {"(commit,a501945b4630fa0d5e645e3cc57ac03e52c74257)","(version,393)"}          | 2022-09-06T14:48:09:134 | {ENDED}        | 35       | 175            | 0                      | 2450             | 0            | 0                   |                |
| trino-393-ddd08e24265          | {"(commit,ddd08e242659b5bab1d6a700ff45fcd0ae95775b)","(version,393)"}          | 2022-09-07T08:30:14:790 | {ENDED}        | 88       | 440            | 0                      | 6160             | 0            | 0                   |                |
| trino-394-7b70997bde4          | {"(commit,7b70997bde41aa9cb6e8b090d1332b71382a8a6d)","(version,394)"}          | 2022-09-14T11:38:01:254 | {STARTED}      | 1        |                |                        |                  |              |                     |                |
| trino-394-7b70997bde4          | {"(commit,7b70997bde41aa9cb6e8b090d1332b71382a8a6d)","(version,394)"}          | 2022-09-14T13:47:37:664 | {STARTED}      | 1        |                |                        |                  |              |                     |                |
| trino-394-7b70997bde4          | {"(commit,7b70997bde41aa9cb6e8b090d1332b71382a8a6d)","(version,394)"}          | 2022-09-14T13:59:12:768 | {STARTED}      | 1        |                |                        |                  |              |                     |                |
| trino-394-7b70997bde4          | {"(commit,7b70997bde41aa9cb6e8b090d1332b71382a8a6d)","(version,394)"}          | 2022-09-14T14:03:20:780 | {FAILED}       | 1        |                |                        |                  |              |                     |                |
| trino-394-7b70997bde4          | {"(commit,7b70997bde41aa9cb6e8b090d1332b71382a8a6d)","(version,394)"}          | 2022-09-14T15:06:00:313 | {ENDED}        | 22       |                |                        |                  |              |                     |                |
| trino-394-a501945b463          | {"(commit,a501945b4630fa0d5e645e3cc57ac03e52c74257)","(version,394)"}          | 2022-09-06T15:09:00:583 | {ENDED}        | 88       | 440            | 0                      | 6160             | 0            | 0                   |                |
| trino-395-7b70997bde4          | {"(commit,7b70997bde41aa9cb6e8b090d1332b71382a8a6d)","(version,395)"}          | 2022-09-14T15:26:41:368 | {ENDED,FAILED} | 7        |                |                        |                  |              |                     |                |
| trino-395-7b70997bde4          | {"(commit,7b70997bde41aa9cb6e8b090d1332b71382a8a6d)","(version,395)"}          | 2022-09-15T08:32:57:932 | {ENDED}        | 19       |                |                        |                  |              |                     |                |
| trino-395-7b70997bde4          | {"(commit,7b70997bde41aa9cb6e8b090d1332b71382a8a6d)","(version,395)"}          | 2022-09-15T09:03:32:060 | {ENDED}        | 26       |                |                        |                  |              |                     |                |
| trino-395-SNAPSHOT-a501945b463 | {"(commit,a501945b4630fa0d5e645e3cc57ac03e52c74257)","(version,395-SNAPSHOT)"} | 2022-09-06T15:22:21:436 | {ENDED}        | 88       | 440            | 0                      | 6160             | 0            | 0                   |                |

# Validate metrics
For every metric, check for statistical outliers across all executions of every run (query?) of every benchmark.
The number of runs should be equal across all metrics and there should be no invalid executions and no outliers.
| name                         | attributes                              | num_executions | num_invalid_executions | num_measurements | num_outliers | num_driver_outliers | names_outliers |
| ---------------------------- | --------------------------------------- | -------------- | ---------------------- | ---------------- | ------------ | ------------------- | -------------- |
| analysisTime                 | {"(scope,prestoQuery)"}                 | 1495           | 0                      | 1495             | 0            | 0                   |                |
| cpu                          | {"(aggregate,max)","(scope,cluster)"}   |                |                        |                  |              |                     |                |
| cpu                          | {"(aggregate,mean)","(scope,cluster)"}  |                |                        |                  |              |                     |                |
| duration                     | {"(scope,driver)"}                      | 1495           | 0                      | 1495             | 0            | 0                   |                |
| finishingTime                | {"(scope,prestoQuery)"}                 | 1495           | 0                      | 1495             | 0            | 0                   |                |
| internalNetworkInputDataSize | {"(scope,prestoQuery)"}                 | 1495           | 0                      | 1495             | 0            | 0                   |                |
| memory                       | {"(aggregate,max)","(scope,cluster)"}   |                |                        |                  |              |                     |                |
| memory                       | {"(aggregate,mean)","(scope,cluster)"}  |                |                        |                  |              |                     |                |
| network                      | {"(aggregate,max)","(scope,cluster)"}   |                |                        |                  |              |                     |                |
| network                      | {"(aggregate,mean)","(scope,cluster)"}  |                |                        |                  |              |                     |                |
| network                      | {"(aggregate,total)","(scope,cluster)"} |                |                        |                  |              |                     |                |
| outputDataSize               | {"(scope,prestoQuery)"}                 | 1495           | 0                      | 1495             | 0            | 0                   |                |
| peakTotalMemoryReservation   | {"(scope,prestoQuery)"}                 | 1495           | 0                      | 1495             | 0            | 0                   |                |
| physicalInputDataSize        | {"(scope,prestoQuery)"}                 | 1495           | 0                      | 1495             | 0            | 0                   |                |
| physicalWrittenDataSize      | {"(scope,prestoQuery)"}                 | 1495           | 0                      | 1495             | 0            | 0                   |                |
| planningTime                 | {"(scope,prestoQuery)"}                 | 1495           | 0                      | 1495             | 0            | 0                   |                |
| processedInputDataSize       | {"(scope,prestoQuery)"}                 | 1495           | 0                      | 1495             | 0            | 0                   |                |
| rawInputDataSize             | {"(scope,prestoQuery)"}                 | 1495           | 0                      | 1495             | 0            | 0                   |                |
| throughput                   | {"(scope,driver)"}                      |                |                        |                  |              |                     |                |
| totalBlockedTime             | {"(scope,prestoQuery)"}                 | 1495           | 0                      | 1495             | 0            | 0                   |                |
| totalCpuTime                 | {"(scope,prestoQuery)"}                 | 1495           | 0                      | 1495             | 0            | 0                   |                |
| totalScheduledTime           | {"(scope,prestoQuery)"}                 | 1495           | 0                      | 1495             | 0            | 0                   |                |

# Comparison validation
Check which benchmark runs are comparable across environments and if not, which attributes are different
The number of runs should be equal across environments, there should be no invalid runs and all runs should be comparable.
| env_name                       | num_runs | num_invalid_runs | num_comparable_runs | num_missing_runs | num_extra_runs | comparable_envs                                                              | missing_run_properties                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | extra_run_properties                                                                                                                                                            |
| ------------------------------ | -------- | ---------------- | ------------------- | ---------------- | -------------- | ---------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| trino-393-a501945b463          | 35       | 0                | 35                  | 79               | 0              | {trino-395-SNAPSHOT-a501945b463,trino-393-ddd08e24265,trino-394-a501945b463} | {"(tpch_3000,tpch_sf10_orc)","(query,q14)","(scale_3000,10)","(query,q15)","(query-names,presto/tpch/q15.sql)","(query-names,presto/tpch/q20.sql)","(query,q22)","(tpch_300,tpch_sf10_orc)","(query-names,presto/tpch/q22.sql)","(query-names,presto/tpch/q14.sql)","(query-names,presto/tpch/q11.sql)","(tpch_1000,tpch_sf10_orc)","(query,q20)","(query-names,presto/tpch/q16.sql)","(schema,tpch_sf10_orc)","(scale_1000,10)","(scale,10)","(query,q11)","(query,q16)","(scale_300,10)","(query,q06)","(query-names,presto/tpch/q19.sql)","(query-names,presto/tpch/q06.sql)","(query,q19)"}                                                                                                                                                                                                                                                                                                                                                       | {}                                                                                                                                                                              |
| trino-393-ddd08e24265          | 88       | 0                | 88                  | 26               | 0              | {trino-395-SNAPSHOT-a501945b463,trino-393-a501945b463,trino-394-a501945b463} | {"(tpch_3000,tpch_sf10_orc)","(scale_3000,10)","(schema,tpch_sf10_orc)","(scale_1000,10)","(scale,10)","(tpch_300,tpch_sf10_orc)","(scale_300,10)","(tpch_1000,tpch_sf10_orc)"}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | {}                                                                                                                                                                              |
| trino-394-7b70997bde4          | 24       | 2                | 22                  | 92               | 0              | {trino-395-7b70997bde4}                                                      | {"(query,q02)","(query,q14)","(query,q04)","(query,q03)","(scale,1)","(query,q15)","(query-names,presto/tpch/q15.sql)","(schema,tpch_sf1_orc)","(query-names,presto/tpch/q20.sql)","(query,q22)","(scale_300,1)","(query-names,presto/tpch/q12.sql)","(query-names,presto/tpch/q22.sql)","(tpch_300,tpch_sf1_orc)","(query-names,presto/tpch/q10.sql)","(query-names,presto/tpch/q01.sql)","(query-names,presto/tpch/q13.sql)","(query,q13)","(query-names,presto/tpch/q14.sql)","(query-names,presto/tpch/q11.sql)","(query,q20)","(query-names,presto/tpch/q16.sql)","(scale_1000,1)","(query-names,presto/tpch/q02.sql)","(query,q10)","(tpch_3000,tpch_sf1_orc)","(query,q12)","(tpch_1000,tpch_sf1_orc)","(query,q11)","(query,q01)","(query,q16)","(query-names,presto/tpch/q03.sql)","(query,q06)","(scale_3000,1)","(query-names,presto/tpch/q04.sql)","(query-names,presto/tpch/q19.sql)","(query-names,presto/tpch/q06.sql)","(query,q19)"} | {}                                                                                                                                                                              |
| trino-394-a501945b463          | 88       | 0                | 88                  | 26               | 0              | {trino-395-SNAPSHOT-a501945b463,trino-393-ddd08e24265,trino-393-a501945b463} | {"(tpch_3000,tpch_sf10_orc)","(scale_3000,10)","(schema,tpch_sf10_orc)","(scale_1000,10)","(scale,10)","(tpch_300,tpch_sf10_orc)","(scale_300,10)","(tpch_1000,tpch_sf10_orc)"}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | {}                                                                                                                                                                              |
| trino-395-7b70997bde4          | 27       | 1                | 22                  | 88               | 4              | {trino-394-7b70997bde4}                                                      | {"(query,q02)","(query,q14)","(query,q04)","(query,q03)","(scale,1)","(query,q15)","(query-names,presto/tpch/q15.sql)","(schema,tpch_sf1_orc)","(query-names,presto/tpch/q20.sql)","(query,q22)","(scale_300,1)","(query-names,presto/tpch/q12.sql)","(query-names,presto/tpch/q22.sql)","(tpch_300,tpch_sf1_orc)","(query-names,presto/tpch/q10.sql)","(query-names,presto/tpch/q01.sql)","(query-names,presto/tpch/q13.sql)","(query,q13)","(query-names,presto/tpch/q14.sql)","(query-names,presto/tpch/q11.sql)","(query,q20)","(query-names,presto/tpch/q16.sql)","(scale_1000,1)","(query-names,presto/tpch/q02.sql)","(query,q10)","(tpch_3000,tpch_sf1_orc)","(query,q12)","(tpch_1000,tpch_sf1_orc)","(query,q11)","(query,q01)","(query,q16)","(query-names,presto/tpch/q03.sql)","(query,q06)","(scale_3000,1)","(query-names,presto/tpch/q04.sql)","(query-names,presto/tpch/q19.sql)","(query-names,presto/tpch/q06.sql)","(query,q19)"} | {"(scale_300,10)","(scale_3000,10)","(schema,tpch_sf10_orc)","(tpch_1000,tpch_sf10_orc)","(scale_1000,10)","(scale,10)","(tpch_3000,tpch_sf10_orc)","(tpch_300,tpch_sf10_orc)"} |
| trino-395-SNAPSHOT-a501945b463 | 88       | 0                | 88                  | 26               | 0              | {trino-393-ddd08e24265,trino-393-a501945b463,trino-394-a501945b463}          | {"(tpch_3000,tpch_sf10_orc)","(scale_3000,10)","(schema,tpch_sf10_orc)","(scale_1000,10)","(scale,10)","(tpch_300,tpch_sf10_orc)","(scale_300,10)","(tpch_1000,tpch_sf10_orc)"}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | {}                                                                                                                                                                              |

# Differences summary
Histogram of difference percentage between runs with same properties but from different environments, for every environment pair and metric
The distribution of difference percentage should be centered around 0.
| metric                       | unit         | bucket | range           | freq | bar                            |
| ---------------------------- | ------------ | ------ | --------------- | ---- | ------------------------------ |
| analysisTime                 | MILLISECONDS | 1      | [-38.61,-29.48] | 9    | ■                              |
| analysisTime                 | MILLISECONDS | 2      | [-28.75,-19.49] | 25   | ■■                             |
| analysisTime                 | MILLISECONDS | 3      | [-19.42,-9.95]  | 49   | ■■■■                           |
| analysisTime                 | MILLISECONDS | 4      | [-9.82,-0.32]   | 86   | ■■■■■■■                        |
| analysisTime                 | MILLISECONDS | 5      | [-0.14,9.34]    | 122  | ■■■■■■■■■■                     |
| analysisTime                 | MILLISECONDS | 6      | [9.41,18.58]    | 53   | ■■■■                           |
| analysisTime                 | MILLISECONDS | 7      | [18.95,27.60]   | 14   | ■                              |
| analysisTime                 | MILLISECONDS | 8      | [28.79,36.86]   | 8    | ■                              |
| analysisTime                 | MILLISECONDS | 9      | [41.15,47.54]   | 2    |                                |
| analysisTime                 | MILLISECONDS | 10     | [47.73,47.73]   | 1    |                                |
| duration                     | MILLISECONDS | 1      | [-21.55,-15.90] | 2    |                                |
| duration                     | MILLISECONDS | 2      | [-12.54,-9.24]  | 8    | ■                              |
| duration                     | MILLISECONDS | 3      | [-8.63,-3.07]   | 52   | ■■■■                           |
| duration                     | MILLISECONDS | 4      | [-2.89,3.09]    | 125  | ■■■■■■■■■■                     |
| duration                     | MILLISECONDS | 5      | [3.20,9.06]     | 127  | ■■■■■■■■■■■                    |
| duration                     | MILLISECONDS | 6      | [9.80,15.35]    | 27   | ■■                             |
| duration                     | MILLISECONDS | 7      | [16.09,21.61]   | 10   | ■                              |
| duration                     | MILLISECONDS | 8      | [22.14,27.73]   | 13   | ■                              |
| duration                     | MILLISECONDS | 9      | [29.26,33.20]   | 4    |                                |
| duration                     | MILLISECONDS | 10     | [34.02,34.02]   | 1    |                                |
| finishingTime                | MILLISECONDS | 1      | [-82.57,-64.07] | 7    | ■                              |
| finishingTime                | MILLISECONDS | 2      | [-63.69,-45.83] | 18   | ■                              |
| finishingTime                | MILLISECONDS | 3      | [-45.50,-27.17] | 39   | ■■■                            |
| finishingTime                | MILLISECONDS | 4      | [-25.95,-9.19]  | 47   | ■■■■                           |
| finishingTime                | MILLISECONDS | 5      | [-8.51,9.92]    | 56   | ■■■■■                          |
| finishingTime                | MILLISECONDS | 6      | [10.25,28.20]   | 61   | ■■■■■                          |
| finishingTime                | MILLISECONDS | 7      | [29.16,46.93]   | 68   | ■■■■■■                         |
| finishingTime                | MILLISECONDS | 8      | [47.41,65.46]   | 61   | ■■■■■                          |
| finishingTime                | MILLISECONDS | 9      | [66.56,83.02]   | 11   | ■                              |
| finishingTime                | MILLISECONDS | 10     | [84.05,84.05]   | 1    |                                |
| internalNetworkInputDataSize | BYTES        | 1      | [-2.70,-2.67]   | 4    |                                |
| internalNetworkInputDataSize | BYTES        | 2      |                 | 0    |                                |
| internalNetworkInputDataSize | BYTES        | 3      |                 | 0    |                                |
| internalNetworkInputDataSize | BYTES        | 4      | [-1.75,-1.73]   | 4    |                                |
| internalNetworkInputDataSize | BYTES        | 5      |                 | 0    |                                |
| internalNetworkInputDataSize | BYTES        | 6      |                 | 0    |                                |
| internalNetworkInputDataSize | BYTES        | 7      |                 | 0    |                                |
| internalNetworkInputDataSize | BYTES        | 8      |                 | 0    |                                |
| internalNetworkInputDataSize | BYTES        | 9      | [-0.03,0.05]    | 360  | ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■ |
| internalNetworkInputDataSize | BYTES        | 10     | [0.06,0.06]     | 1    |                                |
| outputDataSize               | BYTES        | 1      | [0.00,0.00]     | 361  | ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■ |
| outputDataSize               | BYTES        | 2      |                 | 0    |                                |
| outputDataSize               | BYTES        | 3      | [20.00,20.00]   | 2    |                                |
| outputDataSize               | BYTES        | 4      |                 | 0    |                                |
| outputDataSize               | BYTES        | 5      | [40.00,40.00]   | 2    |                                |
| outputDataSize               | BYTES        | 6      |                 | 0    |                                |
| outputDataSize               | BYTES        | 7      | [60.00,60.00]   | 2    |                                |
| outputDataSize               | BYTES        | 8      |                 | 0    |                                |
| outputDataSize               | BYTES        | 9      |                 | 0    |                                |
| outputDataSize               | BYTES        | 10     | [80.00,80.00]   | 2    |                                |
| peakTotalMemoryReservation   | BYTES        | 1      | [-99.31,-84.27] | 7    | ■                              |
| peakTotalMemoryReservation   | BYTES        | 2      | [-74.58,-56.83] | 11   | ■                              |
| peakTotalMemoryReservation   | BYTES        | 3      | [-53.58,-35.18] | 22   | ■■                             |
| peakTotalMemoryReservation   | BYTES        | 4      | [-33.73,-12.50] | 44   | ■■■■                           |
| peakTotalMemoryReservation   | BYTES        | 5      | [-11.86,9.32]   | 168  | ■■■■■■■■■■■■■■                 |
| peakTotalMemoryReservation   | BYTES        | 6      | [10.27,30.81]   | 68   | ■■■■■■                         |
| peakTotalMemoryReservation   | BYTES        | 7      | [31.80,52.51]   | 29   | ■■                             |
| peakTotalMemoryReservation   | BYTES        | 8      | [53.67,73.44]   | 19   | ■■                             |
| peakTotalMemoryReservation   | BYTES        | 9      |                 | 0    |                                |
| peakTotalMemoryReservation   | BYTES        | 10     | [96.53,96.53]   | 1    |                                |
| physicalInputDataSize        | BYTES        | 1      | [-0.19,-0.19]   | 8    | ■                              |
| physicalInputDataSize        | BYTES        | 2      |                 | 0    |                                |
| physicalInputDataSize        | BYTES        | 3      | [-0.10,-0.10]   | 8    | ■                              |
| physicalInputDataSize        | BYTES        | 4      | [-0.09,-0.07]   | 2    |                                |
| physicalInputDataSize        | BYTES        | 5      | [-0.06,-0.03]   | 35   | ■■■                            |
| physicalInputDataSize        | BYTES        | 6      | [-0.02,0.00]    | 223  | ■■■■■■■■■■■■■■■■■■■            |
| physicalInputDataSize        | BYTES        | 7      | [0.01,0.03]     | 44   | ■■■■                           |
| physicalInputDataSize        | BYTES        | 8      | [0.04,0.06]     | 32   | ■■■                            |
| physicalInputDataSize        | BYTES        | 9      | [0.07,0.09]     | 16   | ■                              |
| physicalInputDataSize        | BYTES        | 10     | [0.10,0.10]     | 1    |                                |
| physicalWrittenDataSize      | BYTES        | 1      |                 | 0    |                                |
| physicalWrittenDataSize      | BYTES        | 2      |                 | 0    |                                |
| physicalWrittenDataSize      | BYTES        | 3      |                 | 0    |                                |
| physicalWrittenDataSize      | BYTES        | 4      |                 | 0    |                                |
| physicalWrittenDataSize      | BYTES        | 5      |                 | 0    |                                |
| physicalWrittenDataSize      | BYTES        | 6      |                 | 0    |                                |
| physicalWrittenDataSize      | BYTES        | 7      |                 | 0    |                                |
| physicalWrittenDataSize      | BYTES        | 8      |                 | 0    |                                |
| physicalWrittenDataSize      | BYTES        | 9      |                 | 0    |                                |
| physicalWrittenDataSize      | BYTES        | 10     |                 | 0    |                                |
| planningTime                 | MILLISECONDS | 1      | [-22.04,-15.02] | 7    | ■                              |
| planningTime                 | MILLISECONDS | 2      | [-13.25,-7.07]  | 21   | ■■                             |
| planningTime                 | MILLISECONDS | 3      | [-6.96,0.47]    | 63   | ■■■■■                          |
| planningTime                 | MILLISECONDS | 4      | [0.62,7.94]     | 117  | ■■■■■■■■■■                     |
| planningTime                 | MILLISECONDS | 5      | [8.15,15.57]    | 95   | ■■■■■■■■                       |
| planningTime                 | MILLISECONDS | 6      | [15.86,22.50]   | 37   | ■■■                            |
| planningTime                 | MILLISECONDS | 7      | [23.38,30.48]   | 14   | ■                              |
| planningTime                 | MILLISECONDS | 8      | [31.06,36.33]   | 8    | ■                              |
| planningTime                 | MILLISECONDS | 9      | [38.46,44.60]   | 6    |                                |
| planningTime                 | MILLISECONDS | 10     | [45.80,45.80]   | 1    |                                |
| processedInputDataSize       | BYTES        | 1      | [-0.39,-0.39]   | 2    |                                |
| processedInputDataSize       | BYTES        | 2      | [-0.29,-0.29]   | 2    |                                |
| processedInputDataSize       | BYTES        | 3      | [-0.17,-0.17]   | 8    | ■                              |
| processedInputDataSize       | BYTES        | 4      |                 | 0    |                                |
| processedInputDataSize       | BYTES        | 5      | [-0.03,0.03]    | 355  | ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■ |
| processedInputDataSize       | BYTES        | 6      |                 | 0    |                                |
| processedInputDataSize       | BYTES        | 7      |                 | 0    |                                |
| processedInputDataSize       | BYTES        | 8      | [0.29,0.29]     | 1    |                                |
| processedInputDataSize       | BYTES        | 9      |                 | 0    |                                |
| processedInputDataSize       | BYTES        | 10     | [0.39,0.39]     | 1    |                                |
| rawInputDataSize             | BYTES        | 1      | [-0.21,-0.19]   | 8    | ■                              |
| rawInputDataSize             | BYTES        | 2      |                 | 0    |                                |
| rawInputDataSize             | BYTES        | 3      |                 | 0    |                                |
| rawInputDataSize             | BYTES        | 4      | [-0.10,-0.08]   | 9    | ■                              |
| rawInputDataSize             | BYTES        | 5      | [-0.07,-0.04]   | 15   | ■                              |
| rawInputDataSize             | BYTES        | 6      | [-0.03,-0.01]   | 61   | ■■■■■                          |
| rawInputDataSize             | BYTES        | 7      | [0.00,0.03]     | 233  | ■■■■■■■■■■■■■■■■■■■            |
| rawInputDataSize             | BYTES        | 8      | [0.04,0.06]     | 26   | ■■                             |
| rawInputDataSize             | BYTES        | 9      | [0.07,0.09]     | 16   | ■                              |
| rawInputDataSize             | BYTES        | 10     | [0.10,0.10]     | 1    |                                |
| totalBlockedTime             | MILLISECONDS | 1      | [-25.60,-20.97] | 2    |                                |
| totalBlockedTime             | MILLISECONDS | 2      | [-13.57,-12.31] | 3    |                                |
| totalBlockedTime             | MILLISECONDS | 3      | [-11.92,-5.57]  | 42   | ■■■                            |
| totalBlockedTime             | MILLISECONDS | 4      | [-5.36,1.15]    | 94   | ■■■■■■■■                       |
| totalBlockedTime             | MILLISECONDS | 5      | [1.26,7.97]     | 143  | ■■■■■■■■■■■■                   |
| totalBlockedTime             | MILLISECONDS | 6      | [8.04,14.65]    | 54   | ■■■■                           |
| totalBlockedTime             | MILLISECONDS | 7      | [14.73,21.11]   | 12   | ■                              |
| totalBlockedTime             | MILLISECONDS | 8      | [21.59,27.23]   | 12   | ■                              |
| totalBlockedTime             | MILLISECONDS | 9      | [28.99,33.85]   | 6    |                                |
| totalBlockedTime             | MILLISECONDS | 10     | [34.83,34.83]   | 1    |                                |
| totalCpuTime                 | MILLISECONDS | 1      | [-33.33,-26.33] | 2    |                                |
| totalCpuTime                 | MILLISECONDS | 2      | [-18.72,-18.72] | 1    |                                |
| totalCpuTime                 | MILLISECONDS | 3      | [-17.31,-11.79] | 7    | ■                              |
| totalCpuTime                 | MILLISECONDS | 4      | [-10.29,-2.93]  | 60   | ■■■■■                          |
| totalCpuTime                 | MILLISECONDS | 5      | [-2.91,4.65]    | 158  | ■■■■■■■■■■■■■                  |
| totalCpuTime                 | MILLISECONDS | 6      | [4.71,11.93]    | 106  | ■■■■■■■■■                      |
| totalCpuTime                 | MILLISECONDS | 7      | [12.35,19.87]   | 22   | ■■                             |
| totalCpuTime                 | MILLISECONDS | 8      | [19.92,26.79]   | 11   | ■                              |
| totalCpuTime                 | MILLISECONDS | 9      | [30.42,30.42]   | 1    |                                |
| totalCpuTime                 | MILLISECONDS | 10     | [35.10,35.10]   | 1    |                                |
| totalScheduledTime           | MILLISECONDS | 1      | [-29.84,-23.40] | 4    |                                |
| totalScheduledTime           | MILLISECONDS | 2      | [-21.93,-21.93] | 1    |                                |
| totalScheduledTime           | MILLISECONDS | 3      | [-14.71,-8.22]  | 16   | ■                              |
| totalScheduledTime           | MILLISECONDS | 4      | [-8.11,-1.03]   | 87   | ■■■■■■■                        |
| totalScheduledTime           | MILLISECONDS | 5      | [-0.95,6.19]    | 157  | ■■■■■■■■■■■■■                  |
| totalScheduledTime           | MILLISECONDS | 6      | [6.27,12.92]    | 70   | ■■■■■■                         |
| totalScheduledTime           | MILLISECONDS | 7      | [13.54,20.48]   | 16   | ■                              |
| totalScheduledTime           | MILLISECONDS | 8      | [21.54,26.35]   | 11   | ■                              |
| totalScheduledTime           | MILLISECONDS | 9      | [28.75,35.02]   | 6    |                                |
| totalScheduledTime           | MILLISECONDS | 10     | [35.08,35.08]   | 1    |                                |

# Runs comparison
For every metric scope, find top 5 runs with same properties but from different environments with mean differences
greater than the standard deviation of the other metric and 5%.
There should be relatively few results with low difference percentage.
| left_env_name         | right_env_name                 | left_run_id | right_run_id | metric                     | metric_scope | unit         | diff               | diff_pct           | left_mean  | left_stddev        | left_stddev_pct    | left_min | left_max | right_mean | right_stddev       | right_stddev_pct   | right_min | right_max | rownum |
| --------------------- | ------------------------------ | ----------- | ------------ | -------------------------- | ------------ | ------------ | ------------------ | ------------------ | ---------- | ------------------ | ------------------ | -------- | -------- | ---------- | ------------------ | ------------------ | --------- | --------- | ------ |
| trino-393-ddd08e24265 | trino-395-SNAPSHOT-a501945b463 | 382         | 294          | duration                   | driver       | MILLISECONDS | 407.79999999999995 | 34.0173492984623   | 791        | 15.795568998931314 | 1.9969113778674228 | 782      | 819      | 1198.8     | 67.85793984494372  | 5.660488578664771  | 1141      | 1290      | 1      |
| trino-393-a501945b463 | trino-395-SNAPSHOT-a501945b463 | 171         | 294          | duration                   | driver       | MILLISECONDS | 398                | 33.199865180941636 | 800.8      | 17.73978579352073  | 2.2152579999922537 | 786      | 831      | 1198.8     | 67.85793984494372  | 5.660488578664771  | 1141      | 1290      | 2      |
| trino-393-ddd08e24265 | trino-395-SNAPSHOT-a501945b463 | 442         | 354          | duration                   | driver       | MILLISECONDS | 128.2              | 32.63747555602516  | 264.6      | 11.104053313993049 | 4.19654310576583   | 252      | 279      | 392.8      | 28.270125574535392 | 7.19707903591779   | 369       | 441       | 3      |
| trino-393-ddd08e24265 | trino-395-SNAPSHOT-a501945b463 | 384         | 296          | duration                   | driver       | MILLISECONDS | 621.8000000000002  | 31.09932941919528  | 1377.6     | 17.242389625571047 | 1.2516252853617615 | 1352     | 1392     | 1999.4     | 136.28389486656155 | 6.816239531981623  | 1810      | 2164      | 4      |
| trino-394-a501945b463 | trino-395-SNAPSHOT-a501945b463 | 206         | 294          | duration                   | driver       | MILLISECONDS | 350.79999999999995 | 29.262594737372673 | 848        | 11.445523142259598 | 1.349707917719292  | 833      | 860      | 1198.8     | 67.85793984494372  | 5.660488578664771  | 1141      | 1290      | 5      |
| trino-394-a501945b463 | trino-395-SNAPSHOT-a501945b463 | 235         | 323          | peakTotalMemoryReservation | prestoQuery  | BYTES        | -28821205.2        | 99.30862859416217  | 29021854.2 | 26019786.410296008 | 89.65583801192028  | 36368    | 48664801 | 200649     | 429957.83474894374 | 214.2835671989114  | 1287      | 969534    | 1      |
| trino-393-ddd08e24265 | trino-395-SNAPSHOT-a501945b463 | 411         | 323          | peakTotalMemoryReservation | prestoQuery  | BYTES        | -9727245           | 97.97893692257391  | 9927894    | 21119555.02193627  | 212.7294572437646  | 2574     | 47697828 | 200649     | 429957.83474894374 | 214.2835671989114  | 1287      | 969534    | 2      |
| trino-393-ddd08e24265 | trino-394-a501945b463          | 399         | 223          | peakTotalMemoryReservation | prestoQuery  | BYTES        | -57416094          | 97.86497015239202  | 58668688.8 | 13292166.33802719  | 22.6563210993012   | 44612242 | 77858822 | 1252594.8  | 431451.54228047444 | 34.444623233529796 | 480810    | 1448037   | 3      |
| trino-394-a501945b463 | trino-395-SNAPSHOT-a501945b463 | 223         | 311          | peakTotalMemoryReservation | prestoQuery  | BYTES        | 34891358.2         | 96.53443043527726  | 1252594.8  | 431451.54228047444 | 34.444623233529796 | 480810   | 1448037  | 36143953   | 48832332.44172704  | 135.10512752376124 | 481655    | 90582712  | 4      |
| trino-393-a501945b463 | trino-394-a501945b463          | 188         | 223          | peakTotalMemoryReservation | prestoQuery  | BYTES        | -34438413          | 96.49044655729533  | 35691007.8 | 40062683.84237436  | 112.24867575153485 | 2407507  | 80113142 | 1252594.8  | 431451.54228047444 | 34.444623233529796 | 480810    | 1448037   | 5      |

Generated on Thu Sep 15 11:26:08 CEST 2022
