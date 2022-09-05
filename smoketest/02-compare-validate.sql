-- Comparison validation
-- Check which benchmark runs are comparable across environments and if not, which attributes are different
WITH
attributes AS (
    SELECT
        benchmark_run_id
      , array_agg(row(name, value) ORDER BY name) AS tuples
    FROM benchmark_runs_attributes
    GROUP BY 1
)
, variables AS (
    SELECT
        benchmark_run_id
      , array_agg(row(name, value) ORDER BY name) AS tuples
    FROM benchmark_runs_variables
    GROUP BY 1
)
, runs AS (
    SELECT
        runs.id
      , runs.environment_id
      , runs.status
      , attrs.tuples AS attributes
      , vars.tuples AS variables
      , attrs.tuples || vars.tuples AS parameters
    FROM benchmark_runs runs
    LEFT JOIN attributes attrs ON attrs.benchmark_run_id = runs.id
    LEFT JOIN variables vars ON vars.benchmark_run_id = runs.id
)
--select parameters,status,count(*) from runs group by 1,2 order by 1,2
SELECT
   *
/*
    env_left.name AS env_name
  , count(distinct runs_left.id) AS num_runs
  , count(distinct runs_left.id) FILTER (WHERE runs_left.status != 'ENDED') AS num_invalid_runs
  , env_right.name AS comparable_env_name
  , count(distinct runs_right.id) FILTER (WHERE runs_right.id IS NOT NULL) AS num_candidate_runs
  , count(distinct runs_right.id) FILTER (WHERE runs_right.status = 'ENDED' AND runs_right.id IS NOT NULL) AS num_comparable_runs
  , count(distinct runs_right.id) FILTER (WHERE runs_left.status = 'ENDED' AND runs_right.id IS NULL) AS num_missing_runs
  , count(distinct runs_right.id) FILTER (WHERE runs_right.status = 'ENDED' AND runs_left.id IS NULL) AS num_extra_runs
  */
-- TODO add more details on differences
FROM environments env_left
CROSS JOIN environments env_right
LEFT JOIN runs runs_left ON runs_left.environment_id = env_left.id
-- TODO can we do this without a full join?
FULL OUTER JOIN runs runs_right ON runs_right.environment_id = env_right.id AND runs_left.parameters = runs_right.parameters
WHERE env_left.id != env_right.id
-- TODO for debugging
and env_left.name = 'trino-394-SNAPSHOT-3c60fd41f2e'
--GROUP BY env_left.name, env_right.name, env_left.id, env_right.id
-- TODO order by most comparable env on right, then by name
ORDER BY env_left.name, env_left.id, env_right.name, env_right.id
;
