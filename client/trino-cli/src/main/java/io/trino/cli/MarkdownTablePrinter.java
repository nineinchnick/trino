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
package io.trino.cli;

import com.google.common.collect.ImmutableSet;
import io.trino.client.Column;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.renderer.text.TextContentRenderer;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.trino.cli.AlignedTablePrinter.formatValue;
import static io.trino.client.ClientStandardTypes.BIGINT;
import static io.trino.client.ClientStandardTypes.DECIMAL;
import static io.trino.client.ClientStandardTypes.DOUBLE;
import static io.trino.client.ClientStandardTypes.INTEGER;
import static io.trino.client.ClientStandardTypes.REAL;
import static io.trino.client.ClientStandardTypes.SMALLINT;
import static io.trino.client.ClientStandardTypes.TINYINT;
import static java.util.Objects.requireNonNull;

public class MarkdownTablePrinter
        implements OutputPrinter
{
    private static final Set<String> NUMERIC_TYPES = ImmutableSet.of(TINYINT, SMALLINT, INTEGER, BIGINT, REAL, DOUBLE, DECIMAL);
    private final List<String> fieldNames;
    private final List<TableCell.Alignment> alignments;
    private final Writer writer;

    public MarkdownTablePrinter(List<Column> columns, Writer writer)
    {
        requireNonNull(columns, "columns is null");
        this.fieldNames = columns.stream()
                .map(Column::getName)
                .collect(toImmutableList());
        this.alignments = columns.stream()
                .map(Column::getTypeSignature)
                .map(signature -> NUMERIC_TYPES.contains(signature.getRawType()) ? TableCell.Alignment.RIGHT : TableCell.Alignment.LEFT)
                .collect(toImmutableList());
        this.writer = requireNonNull(writer, "writer is null");
    }

    @Override
    public void printRows(List<List<?>> rows, boolean complete)
            throws IOException
    {
        Node document = buildTable(rows);
        String output = TextContentRenderer.builder()
                .extensions(Collections.singletonList(TablesExtension.create()))
                .build()
                .render(document);

        writer.append(output);
        writer.append('\n');
    }

    private Node buildTable(List<List<?>> rows)
    {
        Node document = new TableBlock();

        Node head = new TableHead();
        Node headRow = new TableRow();
        fieldNames.forEach(name -> {
            TableCell headCell = new TableCell();
            headCell.setHeader(true);
            headCell.setAlignment(TableCell.Alignment.CENTER);
            headCell.appendChild(new Text(name));
            headRow.appendChild(headCell);
        });
        head.appendChild(headRow);
        document.appendChild(head);

        Node body = new TableBody();
        rows.forEach(row -> {
            Node bodyRow = new TableRow();
            for (int i = 0; i < row.size(); i++) {
                TableCell rowCell = new TableCell();
                rowCell.setAlignment(alignments.get(i));
                rowCell.appendChild(new Text(formatValue(row.get(i))));
                bodyRow.appendChild(rowCell);
            }
            body.appendChild(bodyRow);
        });
        document.appendChild(body);
        return document;
    }

    @Override
    public void finish()
            throws IOException
    {
        writer.flush();
    }
}
