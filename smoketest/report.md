Benchmarks report
=======

# Validate benchmarks
Check for statistical outliers on every metric across all executions of every run (query?) of every benchmark.
| name                           | sequence_id             | statuses       | num_runs | num_executions | num_invalid_executions | num_measurements | num_outliers | num_duration_outliers | names_outliers | names_ok                                                                                                                                                                                                                                                         |
| ------------------------------ | ----------------------- | -------------- | -------- | -------------- | ---------------------- | ---------------- | ------------ | --------------------- | -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| trino                          | 2022-08-29T12:00:30:065 | {FAILED}       | 6        | 16             | 0                      | 16               | 0            | 0                     |                | {duration}                                                                                                                                                                                                                                                       |
| trino                          | 2022-08-29T12:02:51:034 | {ENDED,FAILED} | 52       | 156            | 0                      | 1248             | 0            | 0                     |                | {totalBlockedTime,internalNetworkInputDataSize,planningTime,outputDataSize,analysisTime,physicalWrittenDataSize,physicalInputDataSize,finishingTime,processedInputDataSize,duration,peakTotalMemoryReservation,totalScheduledTime,totalCpuTime,rawInputDataSize} |
| trino-393-a30ae746141          | 2022-08-31T06:24:49:717 | {FAILED}       | 1        | 3              | 0                      | 3                | 0            | 0                     |                | {duration}                                                                                                                                                                                                                                                       |
| trino-393-a30ae746141          | 2022-08-31T06:28:54:158 | {FAILED}       | 6        | 17             | 0                      | 17               | 0            | 0                     |                | {duration}                                                                                                                                                                                                                                                       |
| trino-393-a30ae746141          | 2022-08-31T06:32:55:236 | {ENDED}        | 50       | 150            | 0                      | 2100             | 0            | 0                     |                | {totalBlockedTime,internalNetworkInputDataSize,planningTime,outputDataSize,analysisTime,physicalWrittenDataSize,physicalInputDataSize,finishingTime,processedInputDataSize,duration,peakTotalMemoryReservation,totalScheduledTime,totalCpuTime,rawInputDataSize} |
| trino-394-SNAPSHOT-3c60fd41f2e | 2022-08-31T07:05:37:938 | {ENDED}        | 38       | 114            | 0                      | 1596             | 0            | 0                     |                | {totalBlockedTime,internalNetworkInputDataSize,planningTime,outputDataSize,analysisTime,physicalWrittenDataSize,physicalInputDataSize,finishingTime,processedInputDataSize,duration,peakTotalMemoryReservation,totalScheduledTime,totalCpuTime,rawInputDataSize} |

# Comparison validation
Check which benchmark runs are comparable across environments and if not, which attributes are different
| env_name                       | num_runs | comparable_env_name            | num_comparable_runs | num_missing_runs | num_extra_runs |
| ------------------------------ | -------- | ------------------------------ | ------------------- | ---------------- | -------------- |
| trino                          | 58       | trino-393-a30ae746141          | 0                   | 0                | 0              |
| trino                          | 58       | trino-394-SNAPSHOT-3c60fd41f2e | 0                   | 0                | 0              |
| trino-393-a30ae746141          | 57       | trino                          | 0                   | 0                | 0              |
| trino-393-a30ae746141          | 57       | trino-394-SNAPSHOT-3c60fd41f2e | 0                   | 0                | 0              |
| trino-394-SNAPSHOT-3c60fd41f2e | 38       | trino                          | 0                   | 0                | 0              |
| trino-394-SNAPSHOT-3c60fd41f2e | 38       | trino-393-a30ae746141          | 0                   | 0                | 0              |

Generated on Mon Sep  5 14:54:28 CEST 2022
