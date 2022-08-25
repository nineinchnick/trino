CREATE OR REPLACE FUNCTION array_unique (a TEXT[])
RETURNS TEXT[] AS
$$
  SELECT array(
    SELECT DISTINCT v
    FROM unnest(a) AS b(v)
    ORDER BY v
  )
$$ LANGUAGE SQL;

CREATE OR REPLACE FUNCTION array_union(a ANYARRAY, b ANYARRAY)
RETURNS ANYARRAY AS
$$
  SELECT array_agg(x)
  FROM (
    SELECT x
    FROM (
      SELECT unnest(a) x
      UNION
      SELECT unnest(b)
    ) u
    ORDER BY x
  ) AS u
$$ LANGUAGE SQL;

CREATE OR REPLACE AGGREGATE array_union_agg(ANYARRAY) (
  SFUNC = array_union,
  STYPE = ANYARRAY,
  INITCOND = '{}'
);

CREATE OR REPLACE FUNCTION array_subtraction(ANYARRAY, ANYARRAY)
RETURNS anyarray AS
$$
  SELECT ARRAY(SELECT unnest($1)
               EXCEPT
               SELECT unnest($2))
$$ LANGUAGE SQL;
