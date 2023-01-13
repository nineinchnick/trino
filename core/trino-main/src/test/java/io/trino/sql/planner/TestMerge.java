/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.sql.planner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.trino.Session;
import io.trino.connector.MockConnectorFactory;
import io.trino.connector.MockConnectorTableHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.sql.planner.assertions.BasePlanTest;
import io.trino.sql.planner.plan.TableWriterNode;
import io.trino.testing.LocalQueryRunner;
import org.testng.annotations.Test;

import static io.trino.spi.type.IntegerType.INTEGER;
import static io.trino.sql.planner.assertions.PlanMatchPattern.anyTree;
import static io.trino.sql.planner.assertions.PlanMatchPattern.exchange;
import static io.trino.sql.planner.assertions.PlanMatchPattern.node;
import static io.trino.sql.planner.assertions.PlanMatchPattern.values;
import static io.trino.sql.planner.plan.ExchangeNode.Scope.LOCAL;
import static io.trino.sql.planner.plan.ExchangeNode.Scope.REMOTE;
import static io.trino.sql.planner.plan.ExchangeNode.Type.REPARTITION;
import static io.trino.testing.TestingSession.testSessionBuilder;

public class TestMerge
        extends BasePlanTest
{
    @Override
    protected LocalQueryRunner createLocalQueryRunner()
    {
        Session.SessionBuilder sessionBuilder = testSessionBuilder()
                .setCatalog("mock")
                .setSchema("schema");

        LocalQueryRunner queryRunner = LocalQueryRunner.create(sessionBuilder.build());
        queryRunner.createCatalog(
                "mock",
                MockConnectorFactory.builder()
                        .withGetTableHandle((session, schemaTableName) -> new MockConnectorTableHandle(schemaTableName))
                        .withGetColumns(name -> ImmutableList.of(
                                new ColumnMetadata("column1", INTEGER),
                                new ColumnMetadata("column2", INTEGER),
                                new ColumnMetadata("column3", INTEGER)))
                        .build(),
                ImmutableMap.of());
        return queryRunner;
    }

    @Test
    public void testMergeWithUpdate()
    {
        assertDistributedPlan(
                """
MERGE INTO test_table USING (VALUES (1, 11), (2, 22)) t(x, y) ON x = column1
    WHEN MATCHED AND y = 11 THEN UPDATE SET column1 = x
    WHEN MATCHED AND y = 22 THEN UPDATE SET column2 = y
    WHEN NOT MATCHED THEN INSERT (column1, column2, column3) VALUES (x, y, 0)
""",
                getQueryRunner().getDefaultSession(),
                /*

Output[columnNames = [rows]]
│   Layout: [rows:bigint]
└─ TableCommit[target = mock:io.trino.connector.MockConnectorTableHandle@4da82401]
   │   Layout: [rows:bigint]
   └─ LocalExchange[partitioning = SINGLE]
      │   Layout: [partialrows:bigint, fragment:varbinary]
      └─ RemoteExchange[type = GATHER]
         │   Layout: [partialrows:bigint, fragment:varbinary]
         └─ MergeWriter[table = mock:io.trino.connector.MockConnectorTableHandle@4da82401]
            │   Layout: [partialrows:bigint, fragment:varbinary]
            └─ LocalExchange[partitioning = SINGLE]
               │   Layout: [column1:integer, column2:integer, column3:integer, operation:tinyint, field:bigint, insert_from_update:tinyint]
               └─ RemoteExchange[type = REPARTITION]
                  │   Layout: [column1:integer, column2:integer, column3:integer, operation:tinyint, field:bigint, insert_from_update:tinyint]
                  └─ MergeProcessor[]
                     │   Layout: [column1:integer, column2:integer, column3:integer, operation:tinyint, field:bigint, insert_from_update:tinyint]
                     │   target: mock:io.trino.connector.MockConnectorTableHandle@4da82401
                     │   merge row column: merge_row
                     │   row id column: field
                     │   redistribution columns: []
                     │   data columns: [column1, column2, column3]
                     └─ Filter[filterPredicate = (CASE WHEN ((NOT "is_distinct") AND (NOT ("unique_id" IS NULL))) THEN CAST(fail(119, VARCHAR 'One MERGE target table row matched more than one source row') AS boolean) ELSE true END)]
                        │   Layout: [unique_id:bigint, field:bigint, merge_row:row(column1 integer, column2 integer, column3 integer, boolean, tinyint, integer), case_number:integer, is_distinct:boolean]
                        └─ Project[]
                           │   Layout: [unique_id:bigint, field:bigint, merge_row:row(column1 integer, column2 integer, column3 integer, boolean, tinyint, integer), case_number:integer, is_distinct:boolean]
                           └─ MarkDistinct[distinct = [unique_id:bigint, case_number:integer], marker = is_distinct, hash = [$hashvalue]]
                              │   Layout: [unique_id:bigint, field:bigint, merge_row:row(column1 integer, column2 integer, column3 integer, boolean, tinyint, integer), case_number:integer, $hashvalue:bigint, is_distinct:boolean]
                              └─ LocalExchange[partitioning = HASH, hashColumn = [$hashvalue], arguments = ["unique_id", "case_number"]]
                                 │   Layout: [unique_id:bigint, field:bigint, merge_row:row(column1 integer, column2 integer, column3 integer, boolean, tinyint, integer), case_number:integer, $hashvalue:bigint]
                                 └─ RemoteExchange[type = REPARTITION, hashColumn = [$hashvalue_2]]
                                    │   Layout: [unique_id:bigint, field:bigint, merge_row:row(column1 integer, column2 integer, column3 integer, boolean, tinyint, integer), case_number:integer, $hashvalue_2:bigint]
                                    └─ Project[]
                                       │   Layout: [unique_id:bigint, field:bigint, merge_row:row(column1 integer, column2 integer, column3 integer, boolean, tinyint, integer), case_number:integer, $hashvalue_8:bigint]
                                       │   $hashvalue_8 := combine_hash(combine_hash(bigint '0', COALESCE("$operator$hash_code"("unique_id"), 0)), COALESCE("$operator$hash_code"("case_number"), 0))
                                       └─ Project[]
                                          │   Layout: [unique_id:bigint, field:bigint, merge_row:row(column1 integer, column2 integer, column3 integer, boolean, tinyint, integer), case_number:integer]
                                          │   case_number := "merge_row"[6]
                                          └─ Project[]
                                             │   Layout: [unique_id:bigint, field:bigint, merge_row:row(column1 integer, column2 integer, column3 integer, boolean, tinyint, integer)]
                                             │   merge_row := (CASE WHEN ("present" AND ("field_1" = 11)) THEN ROW ("field_0", "column2", "column3", (NOT ("present" IS NULL)), TINYINT '3', 0) WHEN ("present" AND ("field_1" = 22)) THEN ROW ("column1", "field_1", "column3", (NOT ("present" IS NULL)), TINYINT '3', 1) WHEN ("present" IS NULL) THEN ROW ("field_0", "field_1", 0, (NOT ("present" IS NULL)), TINYINT '1', 2) ELSE ROW (CAST(null AS integer), CAST(null AS integer), CAST(null AS integer), (NOT ("present" IS NULL)), TINYINT '-1', -1) END)
                                             └─ RightJoin[criteria = ("column1" = "field_0"), hash = [$hashvalue_3, $hashvalue_5], distribution = PARTITIONED]
                                                │   Layout: [column1:integer, unique_id:bigint, field:bigint, column3:integer, column2:integer, present:boolean, field_0:integer, field_1:integer]
                                                │   Distribution: PARTITIONED
                                                │   dynamicFilterAssignments = {field_0 -> #df_750}
                                                ├─ RemoteExchange[type = REPARTITION, hashColumn = [$hashvalue_3]]
                                                │  │   Layout: [column1:integer, unique_id:bigint, field:bigint, column3:integer, column2:integer, present:boolean, $hashvalue_3:bigint]
                                                │  └─ Project[]
                                                │     │   Layout: [column1:integer, unique_id:bigint, field:bigint, column3:integer, column2:integer, present:boolean, $hashvalue_4:bigint]
                                                │     │   present := true
                                                │     │   $hashvalue_4 := combine_hash(bigint '0', COALESCE("$operator$hash_code"("column1"), 0))
                                                │     └─ AssignUniqueId[]
                                                │        │   Layout: [column1:integer, column2:integer, column3:integer, field:bigint, unique_id:bigint]
                                                │        └─ ScanFilter[table = mock:io.trino.connector.MockConnectorTableHandle@4da82401, filterPredicate = ("column1" IN (1, 2)), dynamicFilters = {"column1" = #df_750}]
                                                │               Layout: [column1:integer, column2:integer, column3:integer, field:bigint]
                                                │               column1 := MockConnectorColumnHandle{name=column1, type=integer}
                                                │               field := MockConnectorColumnHandle{name=merge_row_id, type=bigint}
                                                │               column3 := MockConnectorColumnHandle{name=column3, type=integer}
                                                │               column2 := MockConnectorColumnHandle{name=column2, type=integer}
                                                └─ LocalExchange[partitioning = SINGLE]
                                                   │   Layout: [field_0:integer, field_1:integer, $hashvalue_5:bigint]
                                                   └─ RemoteExchange[type = REPARTITION, hashColumn = [$hashvalue_6]]
                                                      │   Layout: [field_0:integer, field_1:integer, $hashvalue_6:bigint]
                                                      └─ Project[]
                                                         │   Layout: [field_0:integer, field_1:integer, $hashvalue_7:bigint]
                                                         │   $hashvalue_7 := combine_hash(bigint '0', COALESCE("$operator$hash_code"("field_0"), 0))
                                                         └─ Values[]
                                                                Layout: [field_0:integer, field_1:integer]
                                                                (1, 11)
                                                                (2, 22)

                 */
                anyTree(
                        node(TableWriterNode.class,
                                anyTree(
                                        exchange(LOCAL, REPARTITION, ImmutableList.of(), ImmutableSet.of("column1"),
                                                exchange(REMOTE, REPARTITION, ImmutableList.of(), ImmutableSet.of("column1"),
                                                        anyTree(values("column1", "column2"))))))));
    }
}
